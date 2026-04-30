package com.example.voicetotext.reminder.domain

fun interface ReminderParser {
    fun parse(input: String): ReminderIntent
}
