package com.urlstream.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoInfo(
    val id: Int,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val resolution: String,
    val size: String,
    val format: String
) : Parcelable
