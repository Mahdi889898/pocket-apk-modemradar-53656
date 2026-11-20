package com.example.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PersianFormatters {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toPersianDigits(number: Number): String = toPersianDigits(number.toString())

    fun formatTimeOfDay(timestampMs: Long): String {
        if (timestampMs <= 0L) return "هنوز بررسی نشده"
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
        return toPersianDigits(formatter.format(Date(timestampMs)))
    }

    fun formatRelativeTime(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        if (timestampMs <= 0L) return "هنوز بررسی نشده"
        val diffSeconds = ((nowMs - timestampMs) / 1000L).coerceAtLeast(0L)
        return when {
            diffSeconds < 45 -> "لحظاتی پیش"
            diffSeconds < 3600 -> "${toPersianDigits(diffSeconds / 60)} دقیقه پیش"
            diffSeconds < 86400 -> "${toPersianDigits(diffSeconds / 3600)} ساعت پیش"
            else -> "${toPersianDigits(diffSeconds / 86400)} روز پیش"
        }
    }

    fun formatIntervalLabel(minutes: Long): String {
        return when {
            minutes < 60 -> "هر ${toPersianDigits(minutes)} دقیقه"
            minutes == 60L -> "هر ۱ ساعت"
            minutes % 60L == 0L -> "هر ${toPersianDigits(minutes / 60)} ساعت"
            else -> "هر ${toPersianDigits(minutes)} دقیقه"
        }
    }

    fun openSafeUrl(context: Context, url: String): Boolean {
        val trimmed = url.trim()
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmed)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
