package com.technicalelvis.android.simplejamapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import android.net.Uri
import com.google.android.exoplayer2.upstream.RawResourceDataSource

data class FileInfo(val url: Uri?)

class MainViewModel : ViewModel() {
    private lateinit var job : Job
    val outputData = MutableLiveData<FileInfo>()

    fun findFile(arpeggiatorChordSwitchValue : Boolean) {
        job = viewModelScope.launch {
            val playFile = if (arpeggiatorChordSwitchValue) {
                RawResourceDataSource.buildRawResourceUri(R.raw.c_maj7_arp_light_drums)
            } else {
                RawResourceDataSource.buildRawResourceUri(R.raw.c_maj7_light_drums)
            }

            outputData.value = FileInfo(url = playFile)
        }
    }
}