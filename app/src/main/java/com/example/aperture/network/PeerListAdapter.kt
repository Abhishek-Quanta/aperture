package com.example.aperture.network

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.wifi.p2p.WifiP2pDevice
import android.os.IBinder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aperture.databinding.ItemInstanceBinding

class PeerListAdapter: ListAdapter<WifiP2pDevice,PeerListAdapter.PeerHolder>(DiffCallback){
    class PeerHolder(val binding:ItemInstanceBinding):
        RecyclerView.ViewHolder(binding.root){
        fun bind(peer:WifiP2pDevice){
            binding.itemName.text=peer.deviceName
        }
    }
    private var myService:WifiP2pService?=null
    private val connection=object:ServiceConnection{
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder=p1 as WifiP2pService.LocalBinder
            if(myService==null){
                myService=binder.getService()
            }
        }
        override fun onServiceDisconnected(p0: ComponentName?) {

        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerHolder {
        val binding=ItemInstanceBinding.inflate(LayoutInflater.from(parent.context),
            parent,false
        )
        if(myService==null){
            val context=parent.context
            val intent=Intent(context,WifiP2pService::class.java)
            context.bindService(intent,connection,Context.BIND_AUTO_CREATE)
        }
        return PeerHolder(binding)
    }
    override fun onBindViewHolder(holder: PeerHolder, position: Int) {
        val peerInstance=getItem(position)
        holder.bind(peerInstance)
        holder.binding.root.setOnClickListener{
            myService?.connectPeer(peerInstance)
        }
    }
    companion object DiffCallback:DiffUtil.ItemCallback<WifiP2pDevice>(){
        override fun areItemsTheSame(oldItem: WifiP2pDevice, newItem: WifiP2pDevice): Boolean {
            return oldItem.deviceAddress==newItem.deviceAddress
        }
        override fun areContentsTheSame(oldItem: WifiP2pDevice, newItem: WifiP2pDevice): Boolean {
            return false
        }
    }
}