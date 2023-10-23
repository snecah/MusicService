package com.example.musicservice

import android.app.Service
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import kotlin.time.Duration.Companion.milliseconds

class MusicService : Service() {

    private var trackPosInList = 0

    private val binder = MusicBinder()

    private var isActivated = false

    private val mediaPlayer by lazy { MediaPlayer() }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun pauseTrack() {
        mediaPlayer.pause()
    }

    fun setTrack(trackId: AssetFileDescriptor) {
        with(mediaPlayer) {
            setDataSource(trackId)
            prepare()
        }
        isActivated = true
    }

    fun setNewTrack(currentTrack: AssetFileDescriptor) {
        with(mediaPlayer) {
            if(isPlaying)
                mediaPlayer.stop()
            mediaPlayer.reset()
            setTrack(currentTrack)
            mediaPlayer.start()
        }
    }

    fun playTrack() {
        mediaPlayer.start()
    }

    fun getState() = isActivated

    fun getDuration(): Int = mediaPlayer.duration
    fun getDurationInMilli() = getDuration().milliseconds.inWholeSeconds
    fun seekTo(progress: Int) {
        mediaPlayer.seekTo(progress)
    }

    fun isPlaying(): Boolean = mediaPlayer.isPlaying
    fun getCurrentPosition() = mediaPlayer.currentPosition
    fun getPlayer() = mediaPlayer

    fun incTrackPosInList() = trackPosInList++
    fun decTrackPosInList() = trackPosInList--
    fun getTrackPosInList() = trackPosInList
}