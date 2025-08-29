package com.eno.protokolle.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object DebugLog {
    data class Entry(val ts: Long, val level: String, val msg: String, val err: String? = null)

    private val logs = CopyOnWriteArrayList<Entry>()
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun d(msg: String) = add("D", msg, null)
    fun e(msg: String, t: Throwable? = null) = add("E", msg, t?.stackTraceToString())

    private fun add(level: String, msg: String, err: String?) {
        logs += Entry(System.currentTimeMillis(), level, msg, err)
        // Ringpuffer begrenzen (z. B. 1000 Einträge)
        if (logs.size > 1000) repeat(logs.size - 1000) { logs.removeAt(0) }
    }

    fun all(): List<Entry> = logs.toList()

    fun clear() { logs.clear() }

    fun asText(): String = buildString {
        logs.forEach { e ->
            append("[${fmt.format(Date(e.ts))}] ${e.level}  ${e.msg}")
            if (!e.err.isNullOrBlank()) append("\n${
                e.err
            }")
            append('\n')
        }
    }
}
