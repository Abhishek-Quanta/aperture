package com.example.aperture.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import com.example.aperture.MOVIE_DUR
import com.example.aperture.MOVIE_NAME
import com.example.aperture.MOVIE_URI
import com.example.aperture.R
import com.example.aperture.databinding.ActivityMoviePlayerBinding
import com.example.aperture.network.WifiP2pService
import kotlinx.coroutines.*
import java.io.InputStream

const val TAG="MoviePlayerFragment"
class MoviePlayer : AppCompatActivity() {
    private lateinit var binding:ActivityMoviePlayerBinding
    private val viewModel:PlayerViewModel by viewModels()
    private val scope= CoroutineScope(Dispatchers.IO)
    private lateinit var movieName:String
    private lateinit var movieUri:Uri
    private var movieDur:Int=0
    private var inputStream:InputStream?=null
    private var myService:WifiP2pService?=null
    private val connection=object:ServiceConnection{
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder=p1 as WifiP2pService.LocalBinder
            myService=binder.getService()
//            myService?.sendVidData(movieUri)
        }
        override fun onServiceDisconnected(p0: ComponentName?) {

        }
    }
    private fun initializeViewModel(name:String, dur:Int){
        viewModel.setMovieName(name)
        viewModel.setDuration(dur)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var controlsVisible=true
        movieName=intent.getStringExtra(MOVIE_NAME)!!
        movieUri= Uri.parse(intent.getStringExtra(MOVIE_URI))
        movieDur=intent.getIntExtra(MOVIE_DUR,0)
        binding= ActivityMoviePlayerBinding.inflate(LayoutInflater.from(this))
        val intent=Intent(this,WifiP2pService::class.java)
        bindService(intent,connection,Context.BIND_AUTO_CREATE)
//        if(binding.tvMovieTitle.text!=movieName)
//            initializeViewModel(movieName,movieDur)
        binding.vvMovie.setVideoURI(movieUri)
        binding.vvMovie.setOnPreparedListener {
            val prog=binding.seekbar.progress
            movieDur=it.duration
            if(binding.tvMovieTitle.text!=movieName)
                initializeViewModel(movieName,movieDur)
            val time=formatTime(movieDur)
            binding.cur="00:00:00"
            binding.dur=time
            Log.i(TAG,"MovieDur: $time")
            binding.vvMovie.apply {
                start()
                seekTo(prog)
            }
            Log.i(TAG,"Movie Player Prepared")
            scope.launch {
                try{
                    Log.i(TAG,"progressBar running")
                    progressBar()
                }
                catch (e:Exception){
                    e.printStackTrace()
                    Log.e(TAG,"Error running progressBar")
                }
            }
        }
        binding.playerRoot.setOnClickListener {
            if(controlsVisible){
                Log.i(TAG,"Controls are Hidden Now")
                controlsVisible=false
                binding.tvMovieTitle.visibility=View.GONE
                binding.ibRewind.visibility=View.GONE
                binding.ibPlay.visibility=View.GONE
                binding.ibForward.visibility=View.GONE
                binding.seekbar.visibility=View.GONE
            }
            else{
                controlsVisible=true
                Log.i(TAG,"Controls are Visible Now")
                binding.tvMovieTitle.visibility=View.VISIBLE
                binding.ibRewind.visibility=View.VISIBLE
                binding.ibPlay.visibility=View.VISIBLE
                binding.ibForward.visibility=View.VISIBLE
                binding.seekbar.visibility=View.VISIBLE
            }
        }
        binding.vvMovie.setOnCompletionListener {
            binding.ibPlay.setImageResource(R.drawable.ic_play)
            Log.i(TAG,"Movie Ended")
        }
        binding.ibPlay.setOnClickListener{
            if(binding.vvMovie.isPlaying){
                binding.vvMovie.pause()
                binding.ibPlay.setImageResource(R.drawable.ic_play)
                Log.i(TAG,"Movie Paused")
            }
            else{
                binding.vvMovie.start()
                binding.ibPlay.setImageResource(R.drawable.ic_pause)
                Log.i(TAG,"Movie Played")
            }
        }
        binding.ibRewind.setOnClickListener{
            binding.vvMovie.apply {
                val time=this.currentPosition-10000
                if(time<0) seekTo(0)
                else seekTo(this.currentPosition-10000)
            }
        }
        binding.ibForward.setOnClickListener{
            binding.vvMovie.apply {
                val time=this.currentPosition+10000
                if(time>this.duration) seekTo(this.duration)
                else seekTo(time)
            }
        }
        binding.seekbar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(p2&&p0!=null){
                    binding.vvMovie.seekTo(p0.progress)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })
        binding.lifecycleOwner=this
        binding.viewModel=viewModel
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setProgress(binding.vvMovie.currentPosition)
        var msg=if(scope.isActive)
                "scope is active"
            else
                "scope is inactive"
        Log.i(TAG, "Going to end $msg")
        scope.cancel()
        msg=if(scope.isActive)
            "scope is active"
        else
            "scope is inactive"
        inputStream?.close()
        Log.i(TAG, "Activity Destroyed $msg")
    }
    private suspend fun progressBar(){
        val time=binding.vvMovie.currentPosition
        binding.seekbar.progress=time
        binding.cur=formatTime(time)
        delay(300)
        scope.launch(Dispatchers.IO){
//            val buf=ByteArray(1024)
//            val bytes=inputStream?.read(buf)//,time,1000)
//            if(bytes!=null&&bytes>0){
//                myService?.sendVidData(buf)
//            }
            progressBar()
        }
    }
    private fun formatTime(time:Int):String{
        var s:Int = time/1000
        var m=s/60
        var h=m/60
        h%=24;m%=60;s%=60
        val hh:String=if(h<10) "0$h" else h.toString()
        val mm=if(m<10) "0$m" else m.toString()
        val ss=if(s<10) "0$s" else s.toString()
        return if(h==0) "$mm:$ss" else "$hh:$mm:$ss"
    }
}