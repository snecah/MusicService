package com.example.musicservice

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.musicservice.ui.TracksFragment

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onStart() {
        super.onStart()
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container, TracksFragment())
            .commit()
    }
}