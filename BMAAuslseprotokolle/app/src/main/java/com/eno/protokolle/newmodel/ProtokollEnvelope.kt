package com.eno.protokolle.newmodel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class Meta(
    @SerialName("PType") val pType: String,  // EMA, BMA LR....
    @SerialName("WType") val wType: String,  // "1J","2Q","4Q"
    val schemaVersion: String,
    val generatedAt: String
)

@Serializable
data class HeadSpan(
    val r0: Int,
    val c0: Int,
    val r1: Int,
    val c1: Int,
    val label: String? = null
)

@Serializable
data class Head(
    val rows: List<List<String>>,
    val spans: List<HeadSpan>? = null
)

@Serializable
data class Cell(
    val r: Int,
    val c: Int,
    val v: JsonElement? = null, // String/Number/Bool/Null
    val t: String? = null
)

@Serializable
data class Grid(
    val rowCount: Int,
    val colCount: Int,
    // JSON-Objekt => Schlüssel sind Strings (z.B. "1":"string")
    val columnsEditable: Map<String, String> = emptyMap(),
    val cells: List<Cell>
)

@Serializable
data class Tabelle(
    val head: Head,
    val grid: Grid
)

@Serializable
data class Anlage(
    val Name: String,
    val Melder: Tabelle,
    val Hardware: Tabelle? = null
)

@Serializable
data class Protokoll(
    val MelderTypes: List<String>,
    val Anlagen: List<Anlage>,
    val VN: String,
    val Kdn: String
)

@Serializable
data class ProtokollEnvelope(
    val meta: Meta,
    @SerialName("Protokoll") val protokoll: Protokoll
)

