package com.urlstream.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.urlstream.model.VideoInfo

class MainViewModel : ViewModel() {
    var urlInput by mutableStateOf("")
    var videos by mutableStateOf<List<VideoInfo>>(emptyList())
    var hasFetched by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
}
