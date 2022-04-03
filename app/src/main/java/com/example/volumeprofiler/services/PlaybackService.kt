package com.example.volumeprofiler.services

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager.STREAM_MUSIC
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import com.example.volumeprofiler.util.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService: Service() {

    /*
       * Due to ServiceConnection not being garbage collected, LocalBinder leaks the whole service object
       * Setting LocalBinder as static class and storing service in WeakReference solves the problem
     */

    internal class LocalBinder(
        private val service: WeakReference<PlaybackService>
    ) : Binder() {

        fun getService(): PlaybackService? {
            return service.get()
        }
    }

    @Inject
    lateinit var profileManager: ProfileManager

    var mediaPlayer: MediaPlayer? = null
    private set

    private var streamVolume: Int = 3

    internal var mediaUri: Uri = Uri.EMPTY
    private set

    internal var streamType: Int = STREAM_MUSIC
    private set

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder(
            WeakReference(this)
        )
    }

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    private fun prepareMediaPlayer(
        listener: MediaPlayer.OnCompletionListener,
        mediaUri: Uri,
        streamType: Int,
        streamVolume: Int): Unit {

        this.mediaUri = mediaUri
        this.streamType = streamType
        this.streamVolume = streamVolume

        mediaPlayer?.let {
            it.stop()
            it.release()
        }

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                .setLegacyStreamType(streamType)
                .build()
            )
            setVolume(1f, 1f)
            setDataSource(this@PlaybackService, mediaUri)
            prepare()
        }
        mediaPlayer?.setOnCompletionListener(listener)
    }

    fun start(listener: MediaPlayer.OnCompletionListener, mediaUri: Uri, streamType: Int, streamVolume: Int): Unit {
        prepareMediaPlayer(listener, mediaUri, streamType, streamVolume)
        profileManager.setStreamVolume(streamType, streamVolume, 0)
        mediaPlayer?.start()
    }

    fun release(streamType: Int): Unit {
        mediaPlayer?.let {
            it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    fun resume(listener: MediaPlayer.OnCompletionListener, uri: Uri, streamType: Int, streamVolume: Int, position: Int): Unit {
        prepareMediaPlayer(listener, uri, streamType, streamVolume)
        profileManager.setStreamVolume(streamType, streamVolume, 0)
        mediaPlayer?.let {
            it.seekTo(position)
            it.start()
        }
    }

    fun getCurrentPosition(): Int {
        mediaPlayer?.let {
            return it.currentPosition
        }
        return 0
    }

    fun isPlaying(): Boolean {
        mediaPlayer?.let {
            return it.isPlaying
        }
        return false
    }

    private fun release(): Unit {
        mediaPlayer?.let {
            it.stop()
            it.release()
        }
        mediaPlayer = null
    }
}