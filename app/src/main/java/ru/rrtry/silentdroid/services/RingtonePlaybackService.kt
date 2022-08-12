package ru.rrtry.silentdroid.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import java.lang.ref.WeakReference

class RingtonePlaybackService: Service() {

    private lateinit var audioManager: AudioManager
    var mediaPlayer: MediaPlayer? = null
        private set

    internal class LocalBinder(
        private val service: WeakReference<RingtonePlaybackService>
    ): Binder() {

        fun getService(): RingtonePlaybackService? {
            return service.get()
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder(WeakReference(this))
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
        release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                .setLegacyStreamType(streamType)
                .build()
            )
            setVolume(1f, 1f)
            setDataSource(this@RingtonePlaybackService, mediaUri)
            prepare()
        }
        mediaPlayer?.setOnCompletionListener(listener)
    }

    fun start(listener: MediaPlayer.OnCompletionListener, mediaUri: Uri, streamType: Int, streamVolume: Int) {
        audioManager.setStreamVolume(streamType, streamVolume, 0)
        prepareMediaPlayer(listener, mediaUri, streamType, streamVolume)
        mediaPlayer?.start()
    }

    fun resume(listener: MediaPlayer.OnCompletionListener, uri: Uri, streamType: Int, streamVolume: Int, position: Int) {
        audioManager.setStreamVolume(streamType, streamVolume, 0)
        prepareMediaPlayer(listener, uri, streamType, streamVolume)
        mediaPlayer?.seekTo(position)
        mediaPlayer?.start()
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    fun release() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}