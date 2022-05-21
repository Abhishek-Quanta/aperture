package com.example.aperture

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import com.example.aperture.databinding.FragmentMovieListBinding
import com.example.aperture.movie.Movie
import com.example.aperture.network.WifiP2pService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.lang.Exception

const val TAG="MovieListFragment"
class MovieListFragment : Fragment() {
    private lateinit var binding:FragmentMovieListBinding
    private val viewModel:MovieViewModel by activityViewModels()
    private var myService:WifiP2pService?=null
    private val connection=object:ServiceConnection{
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder=p1 as WifiP2pService.LocalBinder
            myService=binder.getService()
            myService?.addStopListener {
                viewModel.setPeerMode(PeerMode.NONE)
            }
        }
        override fun onServiceDisconnected(p0: ComponentName?) {

        }
    }
    private val requestPermissionLauncher=registerForActivityResult(
        ActivityResultContracts.RequestPermission()){
        isGranted->
        if(isGranted) {
            Log.d(TAG, "Media Permission Granted")
            onPermissionGranted()
        }
        else {
            Log.e(TAG, "Media Permission Denied")
            onPermissionDenied("Media Permission Denied")
        }
    }
    private fun requestPermissions(){
        when{
            ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                    ==PackageManager.PERMISSION_GRANTED-> {
                Log.d(TAG, "Media Permission is Already Granted")
                onPermissionGranted()
            }
            shouldShowRequestPermissionRationale(
                Manifest.permission.READ_EXTERNAL_STORAGE)->{
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Media Permissions")
                        .setMessage("Please Give Media Permissions for proper experience")
                        .setNegativeButton("CANCEL"){dialog,_->
                            dialog.cancel()
                        }
                        .setPositiveButton("OK") {_,_->
                            requestPermissionLauncher.launch(
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                        }.show()
                }
            else->{
                requestPermissionLauncher.launch(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
        }
    }
    private fun onPermissionDenied(err:String){
        binding.movieRecyclerList.visibility=View.GONE
        binding.denialText.visibility=View.VISIBLE
        binding.denialText.text=getString(R.string.err_txt,err)
    }
    private fun onPermissionGranted(){
        if(!binding.denialText.isGone) {
            binding.denialText.visibility = View.GONE
        }
        if(binding.movieRecyclerList.isGone)
            binding.movieRecyclerList.visibility=View.VISIBLE
        val adapter=binding.movieRecyclerList.adapter as MovieListAdapter

        val movieList= mutableListOf<Movie>()
        val collection=
            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.Q){
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            }
            else
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection= arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
        )
        val selection=null
        val sortOrd="${MediaStore.Video.Media.DISPLAY_NAME} ASC"
        val resolver=context?.contentResolver
        val query=resolver?.query(collection,
            projection,
            selection,
            null,
            sortOrd
        )
        try{
            query?.use {cursor->
                val idCol=cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol=cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol=cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol=cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol)
                    val dur = cursor.getInt(durCol)
                    val size = cursor.getInt(sizeCol)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    )
                    movieList.add(Movie(contentUri,name,dur,size))
                }
            }
            if(Build.VERSION.SDK_INT<29){
                val path=Environment.getExternalStorageDirectory().absolutePath+
                        "/Aperture"
                val dirs= File(path)
                dirs.listFiles()?.also {files->
                    for(file in files){
                        val uri=Uri.fromFile(file)
                        val name=file.nameWithoutExtension
                        val size=file.length().toInt()
                        movieList.add(Movie(uri,name,1,size))
                    }
                }
            }
            adapter.submitList(movieList)
            viewModel.movieList=movieList
            Log.i(TAG, "Got the List!!")
        }
        catch (e:Exception){
            Log.e(TAG,"Error Fetching List")
            onPermissionDenied("Error Fetching List")
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        Log.i(TAG,"Fragment started successfully")
        viewModel.tabItem.value=TabItem.MOVIES
        binding= FragmentMovieListBinding.inflate(inflater,container,false)
        binding.lifecycleOwner=this
        binding.movieRecyclerList.adapter=MovieListAdapter()
        requestPermissions()
        val intent=Intent(context,WifiP2pService::class.java)
        activity?.bindService(intent,connection,Context.BIND_AUTO_CREATE)
        return binding.root
    }
}