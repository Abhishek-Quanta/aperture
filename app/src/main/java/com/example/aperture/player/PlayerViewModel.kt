package com.example.aperture.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayerViewModel: ViewModel() {
    private val _movieName=MutableLiveData<String>()
    val movieName:LiveData<String> =_movieName
    private val _duration=MutableLiveData<Int>()
    val duration:LiveData<Int> =_duration
    private val _progress=MutableLiveData<Int>()
    val progress:LiveData<Int> =_progress
    init {
        _movieName.value="SomeThing"
        _duration.value=100
        _progress.value=0
    }
    fun setMovieName(name:String){
        _movieName.value=name
    }
    fun setDuration(dur:Int){
        _duration.value=dur
    }
    fun setProgress(pro:Int){
        _progress.value=pro
    }
}