package com.eno.protokolle.newmodel

import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

object ProtokollCodec {

    // robuste JSON-Instanz
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun decode(jsonText: String): ProtokollEnvelope =
        json.decodeFromString(jsonText)

    fun encode(envelope: ProtokollEnvelope): String =
        json.encodeToString(envelope)

    enum class EditKind { none, string, int, bool, melderType }

    /**
     * columnsEditable als Map<Int, EditKind> (0-basiert) – mit Default.
     * Default für NICHT definierte Spalten: melderType (wie gewünscht).
     */
    fun Grid.columnsEditableAsIntMap(defaultKind: EditKind = EditKind.melderType): Map<Int, EditKind> {
        val out = mutableMapOf<Int, EditKind>()
        // definierte Spalten
        for ((k, v) in columnsEditable) {
            val idx = k.toIntOrNull() ?: continue
            out[idx] = v.toEditKind(defaultKind)
        }
        // fehlende Spalten -> Default
        for (i in 0 until colCount) if (!out.containsKey(i)) out[i] = defaultKind
        return out
    }

    private fun String.toEditKind(default: EditKind): EditKind = when (lowercase()) {
        "none" -> EditKind.none
        "string" -> EditKind.string
        "int" -> EditKind.int
        "bool" -> EditKind.bool
        "meldertype" -> EditKind.melderType
        else -> default
    }

    /**
     * Sparse -> dichte Body-Matrix (Strings), gut fürs Rendern/Speichern.
     * Leere Zellen -> "".
     */
    fun Grid.buildDenseBodyAsString(): List<List<String>> {
        val body = Array(rowCount) { Array(colCount) { "" } }
        for (cell in cells) {
            val rr = cell.r
            val cc = cell.c
            if (rr !in 0 until rowCount || cc !in 0 until colCount) continue
            body[rr][cc] = when (val v = cell.v) {
                null -> ""
                is JsonNull -> ""
                is JsonPrimitive -> when {
                    v.isString -> v.content
                    v.booleanOrNull != null -> v.boolean.toString()
                    v.longOrNull != null -> v.long.toString()
                    v.doubleOrNull != null -> v.double.toString()
                    else -> v.toString()
                }
                else -> v.toString()
            }
        }
        return body.map { it.toList() }
    }
}
