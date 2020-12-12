package com.technicalelvis.android.simplejamapp

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.google.android.exoplayer2.util.Util
import com.technicalelvis.android.simplejamapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var playbackStateListener: PlaybackStateListener? = null
    private lateinit var exoPlayer: SimpleExoPlayer
    lateinit var binding : ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var playerReleased = false
    private var playFile : Uri = RawResourceDataSource.buildRawResourceUri(DEFAULT_RESOURCE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playbackStateListener = PlaybackStateListener()
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        binding.arpeggiatorChordSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.findFile(arpeggiatorChordSwitchValue=isChecked)
        }

        viewModel.outputData.observe(this, Observer {
            // Update ExoPlayer
            Log.i(TAG, "[Observer] selectedFile=${it.url}")
            playFile = it.url ?: RawResourceDataSource.buildRawResourceUri(DEFAULT_RESOURCE)
            setCurrentMediaItem()
        })
    }

    private fun setCurrentMediaItem() {
        exoPlayer.setMediaItem(MediaItem.fromUri(playFile))
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Util.SDK_INT <= 23 || playerReleased) {
            initializePlayer()
        }
    }

    private fun initializePlayer() {
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        binding.playerView.defaultArtwork = AppCompatResources.getDrawable(this, DEFAULT_ARTWORK)
        binding.playerView.player = exoPlayer
        playbackStateListener?.let { exoPlayer.addListener(it) }

        setCurrentMediaItem()
        exoPlayer.playWhenReady = playWhenReady
        exoPlayer.seekTo(currentWindow, playbackPosition)
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer.prepare()
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        if (!playerReleased) {
            playbackPosition = exoPlayer.getCurrentPosition()
            currentWindow = exoPlayer.getCurrentWindowIndex()
            playWhenReady = exoPlayer.getPlayWhenReady()
            playbackStateListener?.let { exoPlayer.removeListener(it) }
            playerReleased = true
            exoPlayer.release()
        }
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        binding.playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    private fun buildRawMediaSource(resource : Uri): MediaSource? {
        val rawDataSource = RawResourceDataSource(this)
        // open the /raw resource file
        rawDataSource.open(DataSpec(resource))

        // Create media Item
        val mediaItem = MediaItem.fromUri(rawDataSource.uri!!)

        // create a media source with the raw DataSource
        return ProgressiveMediaSource.Factory { rawDataSource }
                .createMediaSource(mediaItem)
    }

    private class PlaybackStateListener : Player.EventListener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            Log.d(TAG, "changed state to $stateString")
        }
    }

    companion object {
        private const val TAG = "SimpleJamApp"
    }
}