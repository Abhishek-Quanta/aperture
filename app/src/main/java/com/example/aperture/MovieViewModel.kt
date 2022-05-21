package com.example.aperture

import android.net.wifi.p2p.WifiP2pDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.aperture.movie.Movie

enum class PeerMode{HOST,CLIENT,CONNECTED,NONE}
enum class TabItem{MOVIES,PEERS}
class MovieViewModel:ViewModel(){
    private val _peerMode=MutableLiveData(PeerMode.CLIENT)
    private val _serviceRunning=MutableLiveData(false)
    val tabItem=MutableLiveData(TabItem.MOVIES)
    val testDisplay=MutableLiveData("Example")
    val serviceRunning:LiveData<Boolean> =_serviceRunning
    val hostDevice=MutableLiveData("Example")
    val peerMode:LiveData<PeerMode> =_peerMode
    var movieList= listOf<Movie>()
    var peerList=listOf<WifiP2pDevice>()
    fun setPeerMode(mode:PeerMode){
        _peerMode.value=mode
    }
    fun setServiceRunning(bool:Boolean){
        _serviceRunning.value=bool
    }
}