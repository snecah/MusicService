package com.example.musicservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver:BroadcastReceiver() {

    companion object {
        private const val ACTION_PLAY_PAUSE = "PLAY_PAUSE"
        private const val ACTION_NEXT = "NEXT"
        private const val ACTION_PREVIOUS = "PREVIOUS"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val musicIntent = Intent(context, MusicService::class.java)
        when(intent?.action) {
            ACTION_PLAY_PAUSE -> {
                musicIntent.putExtra("ActionName", "Play")
                context?.startService(musicIntent)
            }
            ACTION_NEXT -> {
                musicIntent.putExtra("ActionName", "Next")
                context?.startService(musicIntent)
            }
            ACTION_PREVIOUS -> {
                musicIntent.putExtra("ActionName", "Previous")
                context?.startService(musicIntent)
            }
        }
    }
}