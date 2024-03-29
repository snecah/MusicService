package com.example.musicservice.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.AssetFileDescriptor
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.example.musicservice.ActionPlaying
import com.example.musicservice.MusicService
import com.example.musicservice.R
import com.example.musicservice.databinding.FragmentTracksBinding
import com.example.musicservice.secondsToMinutesSeconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val LAST_TRACK_POS = 3
private const val FIRST_TRACK_POS = 0

class TracksFragment : Fragment(R.layout.fragment_tracks), ActionPlaying {

    private val binding by viewBinding(FragmentTracksBinding::bind)

    private lateinit var musicService: MusicService
    private var isServiceBound: Boolean = false
    private val tracks = listOf(R.raw.phonk, R.raw.phonk2, R.raw.phonk3, R.raw.phonk4)

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isServiceBound = true

            musicService.setCallBack(this@TracksFragment)

            musicService.getPlayer().setOnCompletionListener {
                onTrackEnd()
            }

            if (musicService.getState()) {
                restoreScreen()
            }
            binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?, progress: Int, fromUser: Boolean
                ) {
                    if (fromUser) {
                        musicService.seekTo(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            })

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }
    }

    private fun onTrackEnd() {
        with(binding) {
            pauseImage.visibility = View.INVISIBLE
            playImage.visibility = View.VISIBLE
            currentTime.text = musicService.getDuration().milliseconds.inWholeSeconds.secondsToMinutesSeconds()
            seekBar.progress = musicService.getCurrentPosition()
        }
    }

    private fun restoreScreen() {
        initializeSeekBar()
        with(binding) {
            seekBar.progress = musicService.getCurrentPosition()
            trackTitle.text = getString(R.string.phonk, musicService.getTrackPosInList())
            if (musicService.isPlaying()) {
                pauseImage.visibility = View.VISIBLE
                playImage.visibility = View.INVISIBLE
            } else {
                pauseImage.visibility = View.INVISIBLE
                playImage.visibility = View.VISIBLE
            }
        }
    }

    override fun onStart() {
        val intent = Intent(requireContext(), MusicService::class.java)
        requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val intent = Intent(requireContext(), MusicService::class.java)

        binding.playImage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) requireContext().startForegroundService(
                intent
            )
            else requireContext().startService(intent)

            if (isServiceBound) {
                onPlayPauseButtonClicked()
            }
        }

        binding.pauseImage.setOnClickListener {
            if (isServiceBound) {
                onPlayPauseButtonClicked()
            }
        }

        binding.nextTrack.setOnClickListener {
            if (isServiceBound) {
                if (!musicService.getState()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) requireContext().startForegroundService(
                        intent
                    )
                    else requireContext().startService(intent)
                }
                onNextButtonClicked()
            }
        }

        binding.previousTrack.setOnClickListener {
            if (isServiceBound) {
                onPrevButtonClicked()
            }
        }

    }


    override fun onStop() {
        if (isServiceBound) {
            requireContext().unbindService(connection)
            isServiceBound = false
        }
        super.onStop()
    }

    private fun initializeSeekBar() {
        MainScope().cancel()
        with(binding) {
            currentTime.text = musicService.getCurrentPosition().milliseconds.inWholeSeconds.secondsToMinutesSeconds()
            seekBar.progress = 0
            seekBar.max = musicService.getDuration()
            trackLength.text = musicService.getDurationInMilli().secondsToMinutesSeconds()
        }
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

    private fun setTrack(openRawResourceFd: AssetFileDescriptor) {
        musicService.setTrack(openRawResourceFd)
    }


    private fun onPlayButtonClicked() {

        if (!musicService.getState()) {
            setTrack(resources.openRawResourceFd(tracks[0]))
            initializeSeekBar()
        }
        musicService.playTrack()
        binding.pauseImage.visibility = View.VISIBLE
        binding.playImage.visibility = View.INVISIBLE
        musicService.showNotification()
    }

    override fun onPlayPauseButtonClicked() {
        if (musicService.isPlaying()) onPauseButtonClicked()
        else onPlayButtonClicked()
        lifecycleScope.launch(Dispatchers.Main) {
            while (musicService.isPlaying()) {
                val currentTimeMilli = musicService.getCurrentPosition().milliseconds
                binding.seekBar.progress = musicService.getCurrentPosition()
                binding.currentTime.text = currentTimeMilli.inWholeSeconds.secondsToMinutesSeconds()
                delay(1000)
            }
        }
    }

    private fun onPauseButtonClicked() {
        binding.pauseImage.visibility = View.INVISIBLE
        binding.playImage.visibility = View.VISIBLE
        musicService.pauseTrack()
        musicService.showNotification()
    }

    override fun onNextButtonClicked() {
        if (musicService.getTrackPosInList() == LAST_TRACK_POS) Toast.makeText(
            requireContext(),
            R.string.last_track,
            Toast.LENGTH_SHORT
        ).show()
        else {
            musicService.incTrackPosInList()
            val currentTrackPosInList = musicService.getTrackPosInList()
            binding.trackTitle.text = getString(R.string.phonk, currentTrackPosInList)
            val currentTrack = resources.openRawResourceFd(tracks[musicService.getTrackPosInList()])
            musicService.setNewTrack(currentTrack)
            initializeSeekBar()
            binding.pauseImage.visibility = View.VISIBLE
            binding.playImage.visibility = View.INVISIBLE
            musicService.showNotification()
        }
    }

    override fun onPrevButtonClicked() {
        if (musicService.getTrackPosInList() == FIRST_TRACK_POS) Toast.makeText(
            requireContext(),
            R.string.first_track,
            Toast.LENGTH_SHORT
        ).show()
        else {
            musicService.decTrackPosInList()
            val currentTrackPosInList = musicService.getTrackPosInList()
            binding.trackTitle.text = getString(R.string.phonk, currentTrackPosInList)
            val currentTrack = resources.openRawResourceFd(tracks[musicService.getTrackPosInList()])
            musicService.setNewTrack(currentTrack)
            initializeSeekBar()
            binding.pauseImage.visibility = View.VISIBLE
            binding.playImage.visibility = View.INVISIBLE
            musicService.showNotification()
        }
    }
}