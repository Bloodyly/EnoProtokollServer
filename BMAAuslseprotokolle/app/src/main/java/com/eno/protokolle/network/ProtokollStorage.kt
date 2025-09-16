package com.eno.protokolle.network

import android.content.Context
import com.eno.protokolle.newmodel.ProtokollCodec
import com.eno.protokolle.newmodel.ProtokollEnvelope
import java.io.File

object ProtokollStorage {
    private const val DIR = "protokoll_cache"

    private fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { mkdirs() }

    private fun fileOf(context: Context, key: String): File =
        File(dir(context), safeName(key) + ".json")

    fun save(context: Context, key: String, json: String) {
        fileOf(context, key).writeText(json, Charsets.UTF_8)
    }

    /** Bequemlichkeit: Key aus Meta.VNnr ableiten */
    fun saveEnvelope(context: Context, env: ProtokollEnvelope, fallbackKey: String = "protokoll") {
        val key = (env.meta.VNnr.takeIf { it.isNotBlank() } ?: fallbackKey)
        val json = ProtokollCodec.encode(env)
        save(context, key, json)
    }

    /** Lädt ein gespeichertes Protokoll (oder null, falls keins vorhanden/kaputt). */
    fun load(context: Context, key: String): ProtokollEnvelope? {
        val f = fileOf(context, key)
        if (!f.exists()) return null
        val json = runCatching { f.readText(Charsets.UTF_8) }.getOrNull() ?: return null
        return runCatching { ProtokollCodec.decode(json) }.getOrNull()
    }

    /** Liste aller Keys (Dateinamen ohne .json) */
    fun listKeys(context: Context): List<String> =
        dir(context).listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.map { it.name.removeSuffix(".json") }
            ?.sorted()
            ?: emptyList()

    /** Neueste Datei per lastModified (Key + Envelope) */
    fun loadLatest(context: Context): Pair<String, ProtokollEnvelope>? {
        val latest = dir(context).listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json") }
            ?.maxByOrNull { it.lastModified() }
            ?: return null
        val key = latest.name.removeSuffix(".json")
        val env = runCatching {
            ProtokollCodec.decode(latest.readText(Charsets.UTF_8))
        }.getOrNull() ?: return null
        return key to env
    }

    fun delete(context: Context, key: String): Boolean =
        fileOf(context, key).delete()

    fun clear(context: Context): Boolean {
        var ok = true
        dir(context).listFiles()?.forEach { ok = ok && it.delete() }
        return ok
    }

    private fun safeName(s: String) = s.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
