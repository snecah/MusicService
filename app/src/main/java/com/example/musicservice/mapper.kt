package com.example.musicservice

fun Long.secondsToMinutesSeconds(): CharSequence {
    val minutes = this / 60
    val seconds = this % 60
    return if (seconds < 10) {
        "$minutes:0$seconds"
    } else "$minutes:$seconds"
}