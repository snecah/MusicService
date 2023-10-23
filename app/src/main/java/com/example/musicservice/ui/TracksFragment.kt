package com.example.musicservice.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.AssetFileDescriptor
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.example.musicservice.MusicService
import com.example.musicservice.R
import com.example.musicservice.databinding.FragmentTracksBinding
import com.example.musicservice.secondsToMinutesSeconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class TracksFragment : Fragment(R.layout.fragment_tracks) {

    private val binding by viewBinding(FragmentTracksBinding::bind)

    private lateinit var musicService: MusicService
    private var isServiceBound: Boolean = false
    private val tracks = listOf(R.raw.phonk, R.raw.phonk2, R.raw.phonk3, R.raw.phonk4)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        Intent(requireContext(), MusicService::class.java).also { intent ->
            requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }



        binding.playImage.setOnClickListener {
            if (isServiceBound) {

                musicService.getPlayer().setOnCompletionListener {
                    binding.pauseImage.visibility = View.INVISIBLE
                    binding.playImage.visibility = View.VISIBLE
                }

                if (!musicService.getState()) {
                    setTrack(resources.openRawResourceFd(tracks[0]))
                    initializeSeekBar()
                }
                musicService.playTrack()
                binding.pauseImage.visibility = View.VISIBLE
                binding.playImage.visibility = View.INVISIBLE

                lifecycleScope.launch(Dispatchers.Main) {
                    while (musicService.isPlaying()) {
                        val currentTimeMilli = musicService.getCurrentPosition().milliseconds
                        binding.seekBar.progress = musicService.getCurrentPosition()
                        binding.currentTime.text =
                            currentTimeMilli.inWholeSeconds.secondsToMinutesSeconds()
                        delay(1000)
                    }
                }

            }
        }

        binding.pauseImage.setOnClickListener {
            if (isServiceBound) {
                binding.pauseImage.visibility = View.INVISIBLE
                binding.playImage.visibility = View.VISIBLE
                musicService.pauseTrack()
            }
        }


        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

        binding.nextTrack.setOnClickListener {
            if (isServiceBound) {
                if (musicService.getTrackPosInList() == LAST_TRACK_POS)
                    Toast.makeText(requireContext(), R.string.last_track, Toast.LENGTH_SHORT)
                        .show()
                else {
                    musicService.incTrackPosInList()
                    val currentTrackPosInList = musicService.getTrackPosInList()
                    binding.trackTitle.text = getString(R.string.phonk, currentTrackPosInList)
                    val currentTrack =
                        resources.openRawResourceFd(tracks[currentTrackPosInList])
                    musicService.setNewTrack(currentTrack)
                    binding.pauseImage.visibility = View.VISIBLE
                    binding.playImage.visibility = View.INVISIBLE
                }
            }
        }

        binding.previousTrack.setOnClickListener {
            if (isServiceBound) {
                if (musicService.getTrackPosInList() == FIRST_TRACK_POS)
                    Toast.makeText(requireContext(), R.string.first_track, Toast.LENGTH_SHORT)
                        .show()
                else {
                    musicService.decTrackPosInList()
                    val currentTrackPosInList = musicService.getTrackPosInList()
                    binding.trackTitle.text = getString(R.string.phonk, currentTrackPosInList)
                    val currentTrack =
                        resources.openRawResourceFd(tracks[musicService.getTrackPosInList()])
                    musicService.setNewTrack(currentTrack)
                    binding.pauseImage.visibility = View.VISIBLE
                    binding.playImage.visibility = View.INVISIBLE
                }
            }

        }
    }

    private fun initializeSeekBar() {
        with(binding) {
            seekBar.progress = 0
            seekBar.max = musicService.getDuration()
            trackLength.text = musicService.getDurationInMilli().secondsToMinutesSeconds()
        }
    }

    private fun setTrack(openRawResourceFd: AssetFileDescriptor) {
        musicService.setTrack(openRawResourceFd)
    }

    companion object {
        private const val LAST_TRACK_POS = 3
        private const val FIRST_TRACK_POS = 0
    }

}