package com.eno.protokolle.newmodel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

@Serializable
data class ProtokollEnvelope(
    @SerialName("Meta")
    val meta: Meta = Meta(),
    @SerialName("Protokoll")
    val protokoll: Protokoll = Protokoll()
)

@Serializable
data class Meta(
    /** Anlagentyp  (BMA,EMA,ELA,LR...)*/
    @SerialName("pType") val pType: String = "",
    /** Wartungstyp (1J, 2Q, 4Q, …) */
    @SerialName("wType") val wType: String = "",
    @SerialName("VNnr") val VNnr: String = "",
    @SerialName("Kunde") val Kunde: String = ""
)

@Serializable
data class Protokoll(
    @SerialName("melderTypes") val melderTypes: List<String> = emptyList(),
    @SerialName("anlagen") val anlagen: List<Anlage> = emptyList(),
    @SerialName("editedBy") val editedBy: EditedBy = EditedBy()
)

@Serializable
data class EditedBy(
    val name: String = "",
    /** ISO-8601, z.B. 2025-09-12T10:11:12Z */
    val ts: String = ""
)
@Serializable
data class Anlage(
    /** z.B. "Haupthaus" (aus B4 je Blatt) */
    @SerialName("name") val name: String = "",
    /** Erste Tabelle (Melder)  */
    @SerialName("melder") val melder: Table? = null,
    /** Zweite Tabelle (Hardware) – optional */
    @SerialName("hardware") val hardware: Table? = null
)

@Serializable
data class Table(
    val head: Head? = null,
    val grid: Grid = Grid(),
    /** aus meta.json (Quittierung etc.) */
    val itemsEditable: Boolean = false
)

@Serializable
data class Head(
    /** Mehrzeiliger Tabellenkopf */
    val rows: List<List<String>> = emptyList(),
    /** Optional: zusammengefasste Kopfzellen */
    val spans: List<Span> = emptyList()
)

@Serializable
data class Span(
    @SerialName("r0") val r0: Int,
    @SerialName("c0") val c0: Int,
    @SerialName("r1") val r1: Int,
    @SerialName("c1") val c1: Int,
    val label: String = ""
)

@Serializable
data class Grid(
    /** Server: nRows/nCols + DICHTES body[][] */
    @SerialName("nRows") val rowCount: Int = 0,
    @SerialName("nCols") val colCount: Int = 0,
    /** 2D-Zellenmatrix in Zeilen */
    @SerialName("body") val body: List<List<Cell>> = emptyList(),
    /** String-Keys im JSON → im Code nach Int mappen */
    @SerialName("columnsEditable") private val columnsEditableRaw: Map<String, String> = emptyMap(),
    /** Startindex der Quartalsspalten (0-basiert) oder null */
    @SerialName("qStartCol") val qStartCol: Int? = null
)   {
    /** Bequemer Zugriff als Int-Map */
    val columnsEditable: Map<Int, String> by lazy {
        columnsEditableRaw.mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()
    }

    /** Dichtes String-Body (falls UI Strings erwartet) */
    fun asDenseBodyStrings(): List<List<String>> =
        body.map { row ->
            row.map { cell ->
                when (val v = cell.v) {
                    null, is JsonNull -> ""
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
        }

    fun isQuarterCol(c: Int): Boolean = qStartCol?.let { c >= it } ?: false
    fun isEditableCol(c: Int): Boolean = columnsEditable.containsKey(c)
}

@Serializable
data class Cell(
    val r: Int,
    val c: Int,
    /** v: Ist/Anzeige; in Quartalsspalten aus PROTOKOLL */
    val v: JsonElement? = null,
    /** t: Soll/Typ; in Quartalsspalten aus LISTE, sonst null */
    val t: JsonElement? = null
)
