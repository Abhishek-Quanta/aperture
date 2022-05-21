package com.example.aperture.network

import android.annotation.SuppressLint
import android.app.Service
import android.content.*
import android.net.Uri
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.aperture.NOTIFICATION_ID
import com.example.aperture.STOP_SERVICE
import kotlinx.coroutines.*
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

@SuppressLint("MissingPermission")
class WifiP2pService : Service() {
    inner class LocalBinder: Binder(){
        fun getService():WifiP2pService=this@WifiP2pService
    }
    private lateinit var resolver:ContentResolver
    private var client:Socket?=null
    private var serverSocket:ServerSocket?=null
    private val clientSocket:Socket= Socket()
    private var outputStream:OutputStream?=null
    private var inputStream:InputStream?=null
    private val scope= CoroutineScope(Dispatchers.IO)
    private val binder=LocalBinder()
    private val manager:WifiP2pManager by lazy(LazyThreadSafetyMode.NONE){
        applicationContext.getSystemService(Context.WIFI_P2P_SERVICE) as
                WifiP2pManager
    }
    private var channel:WifiP2pManager.Channel?=null
    private var receiver: BroadcastReceiver?=null
    var displayList:((List<WifiP2pDevice>)->Unit)?=null
    var onConnect:((String,String)->Unit)?=null
    private var onStop:(()->Unit)?=null
    private var onDataReceive:((String)->Unit)?=null

    var isConnected=false
    private val intentFilter= IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    fun peerHost(cb:(List<WifiP2pDevice>)->Unit){
        manager.createGroup(channel,object:WifiP2pManager.ActionListener{
            override fun onSuccess() {
                scope.launch(Dispatchers.IO){
                    serverSocket = ServerSocket(8888)
                    Log.i(TAG, "ServerStarted")
                    client = serverSocket?.accept()
                    Log.i(TAG, "Client Accepted")
                    try{
                        outputStream=client?.getOutputStream()
                    }
                    catch(e:IOException){
                        e.printStackTrace()
                    }
                }
            }
            override fun onFailure(p0: Int) {

            }
        })
        displayList=cb
    }
    fun peerClient(cb:(List<WifiP2pDevice>)->Unit){
        manager.discoverPeers(channel,object:WifiP2pManager.ActionListener{
            override fun onSuccess() {
                Log.i(TAG,"Peers Discovered Successfully")
            }
            override fun onFailure(reasonCode: Int) {
                Log.e(TAG,"Failure Discovering Peers")
            }
        })
        displayList=cb
    }
    fun addDataReceiveListener(cb:((String)->Unit)){
        onDataReceive=cb
    }
    fun addConnectListener(cb:(String,String)->Unit){
        onConnect=cb
    }
    fun addStopListener(cb:()->Unit){
        onStop=cb
    }
    fun connectPeer(device:WifiP2pDevice){
        if(!isConnected) {
            val config = WifiP2pConfig()
            config.deviceAddress = device.deviceAddress
            channel?.also { channel ->
                manager.connect(channel, config,
                    object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {

                        }
                        override fun onFailure(p0: Int) {
                            Toast.makeText(
                                applicationContext, "Failed to connect",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    })
            }
        }
    }
    fun receiveData(){
        manager.requestConnectionInfo(channel) {
            scope.launch(Dispatchers.IO) {
                try {
                    if(it==null){
                        Log.e(TAG,"Connection Info is null")
                    }
                    val host=it.groupOwnerAddress.hostAddress
                    clientSocket.bind(null)
                    Log.i(TAG, "Connecting to Server..")
                    clientSocket.connect(
                        InetSocketAddress(host, 8888),
                        500
                    )
                    if(inputStream==null) {
                        inputStream = clientSocket.getInputStream()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error connecting to server")
                    e.printStackTrace()
                }
                val buf=ByteArray(1024)
                while(isConnected) {
                    try {
                        val bytes = inputStream?.read(buf)
                        if (bytes != null && bytes > 0) {
                            val msg = String(buf,0,bytes,Charsets.UTF_8)
                            Log.i(TAG, "received:$msg")
                            if (msg.startsWith("sendingvid")) {
                                val name=msg.split(',')[1]
                                val size=msg.split(",").last().toInt()
                                Log.i(TAG,"Vid name:$name size:$size")
                                createVidFile(name,size)
                                Log.i(TAG, "sendingvid event")
                                break
                            }
                        }
                    }
                    catch (e:IOException){
                        Log.e(TAG,"sendingvid event error")
                        e.printStackTrace()
                    }
                }

            }
        }
    }
    fun sendVidData(uri:Uri,name:String,size:Int){
        Toast.makeText(applicationContext,"sending file...",Toast.LENGTH_LONG)
            .show()
        scope.launch(Dispatchers.IO){
            outputStream?.write("sendingvid,$name,$size".toByteArray(Charsets.UTF_8))
            outputStream?.flush()
            //delay(500)
            try {
                val inStream = resolver.openInputStream(uri)
                val buf = ByteArray(1024)
                var len: Int
                Log.i(TAG, "sendVidData started$inStream")
                while (inStream?.read(buf).also { len = it!! } != -1) {
                    outputStream?.write(buf, 0, len)
                }
                inStream?.close()
                Log.i(TAG, "Vid Data Sent")
            }
            catch (e:IOException){
                Log.e(TAG,e.stackTraceToString())
            }
        }
        Toast.makeText(applicationContext,"File sent",Toast.LENGTH_LONG).show()
    }

    private fun fileReceived(){
        scope.launch(Dispatchers.Main){
            Toast.makeText(applicationContext,"File Received",
                Toast.LENGTH_LONG).show()
        }
    }
    private fun createVidFile(movieName:String,size:Int){
        val buf=ByteArray(1024)
        var len:Int
        var curSize=0
        val ext=movieName.split(".").last()
        Log.i(TAG,"Extension:$ext")
        val filename="p2p_$movieName"
//        val filename="p2pshared${System.currentTimeMillis()}.$ext"
        if(Build.VERSION.SDK_INT>=29){
            val collection=MediaStore.Downloads.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
            Log.i(TAG,"Video storing at ${collection.path}")
            val newVid=ContentValues().apply {
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,filename
                )

            }
            val vidUri = resolver.insert(collection, newVid)
            if(vidUri!=null){
                resolver.openFileDescriptor(vidUri,"w",
                    null)?.use { pfd->
                    val outStream=FileOutputStream(pfd.fileDescriptor)
                    while (inputStream?.read(buf).also { len = it!! } != -1) {
                        curSize+=len
                        outStream.write(buf, 0, len)
                        if(curSize==size){
                            Log.i(TAG, "File Copied")
                            fileReceived()
                        }
                    }
                }
            }
        }
        else{
            val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val newVid = ContentValues().apply {
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,filename
                )
                put(MediaStore.MediaColumns.MIME_TYPE,"video/$ext")
            }
            resolver.insert(collection, newVid)?.also { vidUri ->
                try {
                    resolver.openOutputStream(vidUri, "wt")?.also { outS ->
                        while (inputStream?.read(buf).also { len = it ?: -1 } != -1) {
                            curSize+=len
                            outS.write(buf, 0, len)
                            if(curSize==size){
                                Log.i(TAG, "File Copied")
                                fileReceived()
                            }
                        }
                        outS.close()
                        resolver.update(vidUri,newVid,null,null)
                        Log.i(TAG, "Video storing at ${collection.path}")
                    }
                }
                catch (e:FileNotFoundException){
//                    Log.e(TAG,e.stackTraceToString())
                    Log.e(TAG,"FileNotFoundException")
                    resolver.delete(vidUri,null,null)
                    val path= Environment.getExternalStorageDirectory().absolutePath+
                            "/Aperture"
                    val dirs=File(path)
                    dirs.takeIf { !it.exists() }?.apply {
                        mkdirs()
                    }
                    val file=File(dirs,filename)
                    file.createNewFile()
                    FileOutputStream(file).also {outS->
                        Log.i(TAG,"Copying File...")
                        while (inputStream?.read(buf).also { len=it?:-1 }!=-1){
                            curSize+=len
                            outS.write(buf,0,len)
                            if(curSize==size) {
                                Log.i(TAG,"File Received")
                                fileReceived()
                            }
                        }
                        Log.i(TAG,"File Copied")
                        fileReceived()
                        outS.close()
                        Log.i(TAG,"Video storing at $path")
                    }
                }
                catch (e:IOException){
                    Log.e(TAG,e.stackTraceToString())
                }
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action== STOP_SERVICE){
            Log.i(TAG,"Service Intent Received")
            NotificationManagerCompat.from(applicationContext)
                .cancel(NOTIFICATION_ID)
            manager.removeGroup(channel,object :WifiP2pManager.ActionListener{
                override fun onSuccess() {
                    Log.i(TAG,"Group Removed Successfully")
                }
                override fun onFailure(p0: Int) {
                    Log.e(TAG,"Error Removing Group")
                }
            })
            if(isConnected) onStop?.also { it() }
            isConnected=false
            if(client!=null) {
                client?.close()
                serverSocket?.close()
            }
            outputStream?.close()
            inputStream?.close()
            clientSocket.takeIf { it.isConnected }?.apply { close() }
            Toast.makeText(applicationContext,"Wifi Direct Stopped",
                Toast.LENGTH_LONG).show()
            stopSelf()
        }
        return START_STICKY
    }
    override fun onCreate() {
        super.onCreate()
        channel=manager.initialize(applicationContext, Looper.getMainLooper(),
            null)
        channel?.also {channel->
            receiver=WifiDirectBroadcastReceiver(manager,channel,this)
        }
        receiver?.also { receiver->
            application.registerReceiver(receiver,intentFilter)
        }
        resolver=contentResolver
        Log.i(TAG,"Wifi Service Started")
    }
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    override fun onDestroy() {
        super.onDestroy()
        receiver?.also { receiver->
            application.unregisterReceiver(receiver)
        }
        Log.i(TAG,"Service Destroyed")
    }
}