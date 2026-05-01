package com.example.voicetotext.action.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

internal object AlarmTimeZoneConverter {

    fun detectInText(lowered: String): ZoneId? {
        for ((token, zoneId) in tokenToZone) {
            if (Regex("""\b${Regex.escape(token)}\b""").containsMatchIn(lowered)) {
                return zoneId
            }
        }
        return null
    }

    fun resolveZoneId(raw: String): ZoneId? {
        val lowered = raw.trim().lowercase()
        tokenToZone[lowered]?.let { return it }
        return try { ZoneId.of(raw.trim()) } catch (_: Exception) { null }
    }

    fun convertToDeviceLocal(hour: Int, minute: Int, sourceZone: ZoneId): Pair<Int, Int> {
        val deviceZone = ZoneId.systemDefault()
        if (sourceZone == deviceZone) return hour to minute
        val converted = ZonedDateTime
            .of(LocalDate.now(deviceZone), LocalTime.of(hour, minute), sourceZone)
            .withZoneSameInstant(deviceZone)
        return converted.hour to converted.minute
    }

    // Abbreviations with conflicts resolved toward most-common usage:
    // IST → Asia/Kolkata (India > Israel > Ireland by global user volume)
    // CST → America/Chicago (US Central > China Standard)
    private val tokenToZone: Map<String, ZoneId> = mapOf(
        // US
        "pst"      to ZoneId.of("America/Los_Angeles"),
        "pdt"      to ZoneId.of("America/Los_Angeles"),
        "pacific"  to ZoneId.of("America/Los_Angeles"),
        "mst"      to ZoneId.of("America/Denver"),
        "mdt"      to ZoneId.of("America/Denver"),
        "mountain" to ZoneId.of("America/Denver"),
        "cst"      to ZoneId.of("America/Chicago"),
        "cdt"      to ZoneId.of("America/Chicago"),
        "central"  to ZoneId.of("America/Chicago"),
        "est"      to ZoneId.of("America/New_York"),
        "edt"      to ZoneId.of("America/New_York"),
        "eastern"  to ZoneId.of("America/New_York"),
        // UTC / GMT
        "utc"      to ZoneId.of("UTC"),
        "gmt"      to ZoneId.of("GMT"),
        // Europe
        "bst"      to ZoneId.of("Europe/London"),
        "london"   to ZoneId.of("Europe/London"),
        "cet"      to ZoneId.of("Europe/Paris"),
        "cest"     to ZoneId.of("Europe/Paris"),
        "paris"    to ZoneId.of("Europe/Paris"),
        "berlin"   to ZoneId.of("Europe/Berlin"),
        // Asia
        "ist"      to ZoneId.of("Asia/Kolkata"),
        "india"    to ZoneId.of("Asia/Kolkata"),
        "jst"      to ZoneId.of("Asia/Tokyo"),
        "tokyo"    to ZoneId.of("Asia/Tokyo"),
        "sgt"      to ZoneId.of("Asia/Singapore"),
        "singapore" to ZoneId.of("Asia/Singapore"),
        // Australia
        "aest"     to ZoneId.of("Australia/Sydney"),
        "aedt"     to ZoneId.of("Australia/Sydney"),
        "sydney"   to ZoneId.of("Australia/Sydney"),
    )
}
