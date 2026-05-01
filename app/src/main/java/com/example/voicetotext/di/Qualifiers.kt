package com.example.voicetotext.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FallbackParser

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TimerParser

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AlarmParser
