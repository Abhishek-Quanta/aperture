package com.example.aperture.movie

import android.net.Uri

data class Movie(
    val uri:Uri,
    val name:String,
    val duration:Int,
    val size:Int
)