package com.example.aperture.network

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.aperture.MovieViewModel
import com.example.aperture.PeerMode
import com.example.aperture.R
import com.example.aperture.databinding.FragmentWifiEnableBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

const val TAG="WifiEnableFragment"
class WifiEnableFragment : Fragment() {
    private lateinit var binding:FragmentWifiEnableBinding
    private val viewModel:MovieViewModel by activityViewModels()
    private val requestPermissionLauncher=registerForActivityResult(
        ActivityResultContracts.RequestPermission()){
        isGranted->
        if(isGranted){
            Log.i(TAG,"Wifi Permissions Granted")
            onPermissionGranted()
        }
        else{
            Log.e(TAG,"Wifi Permissions Denied")
            onPermissionDenied()
        }
    }
    private fun requestPermission(){
        when{
            ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                    ==PackageManager.PERMISSION_GRANTED->{
                        Log.i(TAG,"Wifi Permissions Already Granted")
                        onPermissionGranted()
                    }
            shouldShowRequestPermissionRationale(Manifest.permission
                .ACCESS_FINE_LOCATION)->{
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Wifi Permissions")
                        .setMessage("Please Grant Wifi Permissions")
                        .setNegativeButton("CANCEL"){dialog,_->
                            dialog.cancel()
                        }
                        .setPositiveButton("OK"){_,_->
                            requestPermissionLauncher.launch(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        }.show()
                }
            else->{
                requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }
    private fun onPermissionGranted(){
        binding.ibClient.visibility=View.VISIBLE
        binding.ibHost.visibility=View.VISIBLE
        binding.tvDenial.visibility=View.GONE
    }
    private fun onPermissionDenied(){
        binding.ibClient.visibility=View.GONE
        binding.ibHost.visibility=View.GONE
        binding.tvDenial.visibility=View.VISIBLE
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding= FragmentWifiEnableBinding.inflate(inflater,container,false)
        Log.i(TAG,"Fragment started successfully")
        requestPermission()
        viewModel.peerMode.observe(viewLifecycleOwner) {
            if (it == PeerMode.CONNECTED) {
                findNavController().navigate(R.id.peerListFragment)
            }
            else{
                viewModel.setServiceRunning(false)
            }
        }
        binding.ibHost.setOnClickListener {
            viewModel.setPeerMode(PeerMode.HOST)
            findNavController().navigate(R.id.peerListFragment)
        }
        binding.ibClient.setOnClickListener {
            viewModel.setPeerMode(PeerMode.CLIENT)
            findNavController().navigate(R.id.peerListFragment)
        }
        binding.lifecycleOwner=this
        return binding.root
    }
}