package com.eno.protokolle.network

import android.content.Context
import com.eno.protokolle.newmodel.ProtokollCodec
import com.eno.protokolle.newmodel.ProtokollEnvelope
import java.io.File

object ProtokollStorage {
    private const val DIR = "protokoll_cache"

    fun save(context: Context, key: String, json: String) {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        File(dir, safeName(key) + ".json").writeText(json, Charsets.UTF_8)
    }

    fun load(context: Context, key: String): ProtokollEnvelope? {
        val f = File(File(context.filesDir, DIR), safeName(key) + ".json")
        if (!f.exists()) return null
        val json = f.readText(Charsets.UTF_8)
        return runCatching { ProtokollCodec.decode(json) }.getOrNull()
    }

    fun listKeys(context: Context): List<String> {
        val dir = File(context.filesDir, DIR)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.map { it.name.removeSuffix(".json") }
            ?: emptyList()
    }

    private fun safeName(s: String) = s.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
