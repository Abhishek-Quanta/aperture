package com.example.aperture.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.widget.Toast

class WifiDirectBroadcastReceiver(
    private val manager:WifiP2pManager,
    private val channel:WifiP2pManager.Channel,
    private val service:WifiP2pService
):BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        val action=intent?.action
        when(action){
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION->{
                val state=intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1)
                when(state){
                    WifiP2pManager.WIFI_P2P_STATE_ENABLED->{
                        Log.i(TAG,"Wifi Enabled")
                        Toast.makeText(service.applicationContext,
                            "Wifi Enabled",
                            Toast.LENGTH_LONG).show()
                    }
                    else->{
                        Log.e(TAG,"Wifi Disabled")
                        Toast.makeText(service.applicationContext,
                            "Please Enable Wifi",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION->{
                if(service.isConnected) {
                    manager.requestGroupInfo(channel) { group ->
                        if (group == null) {
                            service.isConnected = false
                            service.displayList?.also {
                                Log.i(TAG, "Peers List Displayed")
                                it(listOf())
                            }
                            service.onConnect?.also {
                                it("", "client")
                            }
                        }
                    }
                }
                if(!service.isConnected) {
                    manager.requestPeers(channel) { peers: WifiP2pDeviceList? ->
                        Log.i(TAG, "Peers Changed")
                        service.displayList?.also {
                            Log.i(TAG, "Peers List Displayed")
                            it(peers?.deviceList?.toList()!!)
                        }
                    }
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION-> {
                manager.requestGroupInfo(channel) { group ->
                    if(group!=null) {
                        group.owner.deviceAddress
                        service.isConnected = true
                        Log.i(TAG, "Connection Changed")
                        val list = group.clientList.toMutableList()
                        if(!group.isGroupOwner){
                            service.receiveData()
                            val device=WifiP2pDevice(group.owner)
                            device.deviceName="This Device"
                            list.add(device)
                        }
                        service.onConnect?.also {
                            it(
                                group.owner.deviceName.ifEmpty { group.networkName },
                                if(list.isNotEmpty()){
                                    "connected"
                                }
                                else{
                                    "host"
                                }
                            )
                        }
                        service.displayList?.also {
                            Log.i(TAG, "Group Members Displayed")
                            it(list)
                        }
                    }
                }
            }
        }
    }
}