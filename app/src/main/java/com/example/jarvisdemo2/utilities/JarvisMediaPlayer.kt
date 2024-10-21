package com.example.jarvisdemo2.utilities

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri

object JarvisMediaPlayer {
    var mediaPlayer: MediaPlayer? = null
    var lastResource: Uri? = null
    var lastVolume: Int = 0
    var volumeToDecrease: Boolean = false
    private lateinit var audioManager: AudioManager

    fun playAudio(context: Context, audio: Uri, isLooping: Boolean = true, raiseVolume: Boolean = false) {
        createMediaPlayer(context, audio)
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        mediaPlayer?.let {
            it.isLooping = isLooping

            if (!it.isPlaying) {
                it.start()
            }
        }

        volumeToDecrease = raiseVolume

        if (raiseVolume) {
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2,
                0
            )
        }
    }

    private fun createMediaPlayer(context: Context, audio: Uri) {
        // in case it's already playing something
        mediaPlayer?.stop()

        mediaPlayer = MediaPlayer.create(context, audio)
        lastResource = audio
    }

    fun isPlaying(): Boolean? {
        return mediaPlayer?.isPlaying
    }

    // usually used inside the Activity's onResume method
    fun continuePlaying(context: Context, specificResource: Uri? = null) {
        specificResource?.let {
            if (lastResource != specificResource) {
                createMediaPlayer(context, specificResource)
            }
        }

        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
            }
        }
    }

    fun pauseAudio() {
        mediaPlayer?.pause()
    }

    fun stopAudio() {
        mediaPlayer?.stop()
        if (volumeToDecrease)
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                lastVolume,
                0
            )
    }

}