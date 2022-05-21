package com.example.aperture

import android.content.*
import android.os.IBinder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aperture.databinding.ItemInstanceBinding
import com.example.aperture.movie.Movie
import com.example.aperture.network.WifiP2pService
import com.example.aperture.player.MoviePlayer
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MovieListAdapter:ListAdapter<Movie,MovieListAdapter.MovieHolder>(DiffCallback){
    class MovieHolder(val binding: ItemInstanceBinding):
        RecyclerView.ViewHolder(binding.root){
        fun bind(movie:Movie){
            binding.itemName.text=movie.name
            //binding.executePendingBindings()
            //Log.i(TAG,"ListAdapter:${movie.name}")
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
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MovieHolder {
        val binding=ItemInstanceBinding.inflate(
            LayoutInflater.from(parent.context),parent,false)
        if(myService==null){
            val context=parent.context
            val intent=Intent(context,WifiP2pService::class.java)
            context.bindService(intent,connection,Context.BIND_AUTO_CREATE)
        }
        return MovieHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieHolder,
                                  position: Int) {
        val movieInstance=getItem(position)
        holder.bind(movieInstance)
        holder.binding.root.setOnClickListener {
            val intent=Intent(it.context, MoviePlayer::class.java)
            intent.apply {
                putExtra(MOVIE_NAME,movieInstance.name)
                putExtra(MOVIE_URI,movieInstance.uri.toString())
                putExtra(MOVIE_DUR,movieInstance.duration)
            }
            it.context.startActivity(intent)
        }
        holder.binding.root.setOnLongClickListener {root->
            val items= arrayOf("Details","Send File")
            val details= arrayOf(
                "Name:${movieInstance.name}",
                "Uri:${movieInstance.uri.path}",
                "Dur:${movieInstance.duration}",
                "Size:${movieInstance.size}"
            )
            MaterialAlertDialogBuilder(root.context)
                .setTitle(movieInstance.name)
                .setItems(items){dialog,which->
                    when(which){
                        0->MaterialAlertDialogBuilder(root.context)
                            .setTitle(movieInstance.name)
                            .setItems(details){_,_->

                            }.show()
                        1->myService?.sendVidData(movieInstance.uri,movieInstance.name
                            ,movieInstance.size)
                    }
                    dialog.cancel()
                }
                .show()
            false
        }

    }
    companion object DiffCallback:DiffUtil.ItemCallback<Movie>(){
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem.uri==newItem.uri
        }

        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return false//oldItem.duration==newItem.duration
        }
    }
}