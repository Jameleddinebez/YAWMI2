package com.yawmi.domain

import java.util.Locale

object TimeFormat {

    /** 0..1439 → "HH:mm" */
    fun minutes(totalMinutes: Int): String =
        "%02d:%02d".format(Locale.ROOT, (totalMinutes / 60) % 24, totalMinutes % 60)

    /** ثوانٍ → "HH:MM:SS" للعد التنازلي (القيم السالبة تُعامل كصفر). */
    fun countdown(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        return "%02d:%02d:%02d".format(Locale.ROOT, s / 3600, (s % 3600) / 60, s % 60)
    }

    /** دقائق → «5 س 30 د» / «45 د» / «2 س». */
    fun durationLabel(totalMinutes: Int): String {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h > 0 && m > 0 -> "$h س $m د"
            h > 0 -> "$h س"
            else -> "$m د"
        }
    }

    /**
     * يحلل "HH:mm" أو "H:mm" أو "HH.mm" مع قبول الأرقام الهندية (٠-٩ و ۰-۹).
     * يعيد null إن كان النص غير صالح.
     */
    fun parseMinutes(input: String): Int? {
        val normalized = buildString {
            for (c in input.trim()) {
                append(
                    when (c) {
                        in '٠'..'٩' -> '0' + (c - '٠')
                        in '۰'..'۹' -> '0' + (c - '۰')
                        '：', '٫' -> ':'
                        else -> c
                    },
                )
            }
        }
        val match = Regex("^(\\d{1,2})[:.](\\d{2})$").matchEntire(normalized) ?: return null
        val h = match.groupValues[1].toInt()
        val m = match.groupValues[2].toInt()
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
    }
}
