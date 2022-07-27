package ru.rrtry.silentdroid.services

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager.STREAM_MUSIC
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import ru.rrtry.silentdroid.core.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ref.WeakReference
import javax.inject.Inject
@AndroidEntryPoint
class PlaybackService: Service() {

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
    private var mediaUri: Uri = Uri.EMPTY
    private var streamType: Int = STREAM_MUSIC

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
        streamVolume: Int)
    {

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

    fun start(listener: MediaPlayer.OnCompletionListener, mediaUri: Uri, streamType: Int, streamVolume: Int) {
        profileManager.setStreamVolume(streamType, streamVolume, 0)
        prepareMediaPlayer(listener, mediaUri, streamType, streamVolume)
        mediaPlayer?.start()
    }

    fun resume(listener: MediaPlayer.OnCompletionListener, uri: Uri, streamType: Int, streamVolume: Int, position: Int) {
        profileManager.setStreamVolume(streamType, streamVolume, 0)
        prepareMediaPlayer(listener, uri, streamType, streamVolume)
        mediaPlayer?.seekTo(position)
        mediaPlayer?.start()
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

    fun release() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}