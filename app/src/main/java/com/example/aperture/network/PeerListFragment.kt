package com.example.aperture.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.aperture.*
import com.example.aperture.databinding.FragmentPeerListBinding

class PeerListFragment : Fragment(){
    private lateinit var binding:FragmentPeerListBinding
    private val viewModel:MovieViewModel by activityViewModels()
    private lateinit var adapter:PeerListAdapter
    private var myService:WifiP2pService?=null
    private val connection=object:ServiceConnection{
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder=service as WifiP2pService.LocalBinder
            myService=binder.getService()
            onBindService()
        }
        override fun onServiceDisconnected(arg0: ComponentName?) {

        }
    }
    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=26){
            val name=getString(R.string.channel_name)
            val descriptionText=getString(R.string.channel_description)
            val importance=NotificationManager.IMPORTANCE_HIGH
            val channel=NotificationChannel(CHANNEL_ID,name,importance).apply {
                description=descriptionText
            }
            val notificationManager:NotificationManager=activity?.
                getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun onBindService(){
        myService?.addConnectListener {name,peerMode->
            viewModel.hostDevice.value= name.ifEmpty { "Some Device" }
            when(peerMode) {
                "connected"->{
                    viewModel.setPeerMode(PeerMode.CONNECTED)
                    binding.tvPeerHost.text = getString(R.string.connection_status, name)
                }
                "host"->{
                    viewModel.setPeerMode(PeerMode.HOST)
                    binding.tvPeerHost.text = "$name is Hosting.."
                }
                else->{
                    viewModel.setPeerMode(PeerMode.CLIENT)
                }
            }
        }
        myService?.addDataReceiveListener {
            viewModel.testDisplay.value=it
        }
        myService?.addStopListener {
            viewModel.setPeerMode(PeerMode.NONE)
            findNavController().popBackStack()
        }

        viewModel.peerMode.observe(this) {
            when (it!!) {
                PeerMode.HOST -> {
                    binding.tvPeerHost.visibility = View.VISIBLE
                    binding.peerRecyclerList.visibility = View.VISIBLE

                    myService?.peerHost{list->
                        viewModel.peerList=list
                        adapter.submitList(list)
                    }
                }
                PeerMode.CLIENT -> {
                    binding.tvPeerHost.visibility = View.GONE
                    binding.peerRecyclerList.visibility = View.VISIBLE

                    myService?.peerClient { list ->
                        viewModel.peerList=list
                        adapter.submitList(list)
                    }
                }
                PeerMode.CONNECTED -> {
                    binding.tvPeerHost.visibility = View.VISIBLE
                    binding.peerRecyclerList.visibility = View.VISIBLE

                }
                else->{
                    Log.i(TAG,"Peers Disconnected")
                }
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding= FragmentPeerListBinding.inflate(inflater,container,false)
        Log.i(TAG,"Fragment started successfully")
        val intent = Intent(context, WifiP2pService::class.java)
        viewModel.tabItem.value = TabItem.PEERS
        viewModel.serviceRunning.observe(viewLifecycleOwner) { running ->
            if (!running) {
                activity?.startService(intent)
                createNotificationChannel()
                val stopIntent = Intent(context, WifiP2pService::class.java)
                    .apply {
                        action = STOP_SERVICE
                        putExtra("noteId", NOTIFICATION_ID)
                    }
                val pendingIntent = if(Build.VERSION.SDK_INT>=31)
                    PendingIntent.getService(
                        context, 0,
                        stopIntent, PendingIntent.FLAG_MUTABLE
                    )
                    else PendingIntent.getService(
                        context, 0,
                        stopIntent, 0
                    )
                val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Wifi Direct")
                    .setContentText("Wifi peer to peer service is running")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .addAction(0, "Stop", pendingIntent)
                with(NotificationManagerCompat.from(requireContext())) {
                    notify(NOTIFICATION_ID, builder.build())
                }
                viewModel.setServiceRunning(true)
                Log.i(TAG, "Service Running")
            }
        }
        activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        binding.peerRecyclerList.adapter=PeerListAdapter()
        adapter=binding.peerRecyclerList.adapter as PeerListAdapter
        binding.viewModel=viewModel
        binding.lifecycleOwner=this
        adapter.submitList(viewModel.peerList)
        return binding.root
    }
}