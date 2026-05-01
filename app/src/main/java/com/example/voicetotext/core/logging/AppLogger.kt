package com.example.voicetotext.core.logging

import android.util.Log

object AppLogger {
    fun d(tag: String, message: String) {
        runCatching { Log.d(tag, message) }
            .getOrElse { println("D/$tag: $message") }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable == null) {
                Log.w(tag, message)
            } else {
                Log.w(tag, message, throwable)
            }
        }.getOrElse {
            println("W/$tag: $message")
            throwable?.printStackTrace()
        }
    }
}
