package ru.rrtry.silentdroid.services

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.*
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import ru.rrtry.silentdroid.event.EventBus
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class RingtonePlaybackService: Service() {

    @Inject
    lateinit var eventBus: EventBus

    private lateinit var audioManager: AudioManager

    private var streamVolume: Int = -1
    private var streamType: Int = AudioManager.STREAM_MUSIC

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

    fun setStreamVolume(streamType: Int, streamVolume: Int, flags: Int = 0) {
        audioManager.setStreamVolume(
            streamType,
            streamVolume,
            flags
        )
    }

    private fun restorePreviousVolume() {
        if (streamVolume >= 0) {
            if (streamVolume != audioManager.getStreamVolume(streamType)) {
                try {
                    setStreamVolume(
                        streamType,
                        streamVolume,
                        AudioManager.FLAG_SHOW_UI
                    )
                } catch (e: SecurityException) {
                    Log.e("RingtonePlayer", "Failed to restore volume")
                }
            }
        }
    }

    private fun prepareMediaPlayer(
        listener: MediaPlayer.OnCompletionListener,
        mediaUri: Uri,
        type: Int,
        volume: Int)
    {
        release()

        streamType = type
        streamVolume = audioManager.getStreamVolume(type)

        setStreamVolume(
            type,
            volume,
            AudioManager.FLAG_SHOW_UI
        )

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                .setLegacyStreamType(type)
                .build()
            )
            setVolume(1f, 1f)
            setDataSource(this@RingtonePlaybackService, mediaUri)
            prepare()
        }
        mediaPlayer?.setOnCompletionListener(listener)
    }

    fun start(listener: MediaPlayer.OnCompletionListener, mediaUri: Uri, streamType: Int, streamVolume: Int) {
        prepareMediaPlayer(listener, mediaUri, streamType, streamVolume)
        mediaPlayer?.start()
    }

    fun resume(listener: MediaPlayer.OnCompletionListener, uri: Uri, streamType: Int, streamVolume: Int, position: Int) {
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
        restorePreviousVolume()
        mediaPlayer?.let { player ->
            player.stop()
            player.release()
        }
        mediaPlayer = null
    }
}