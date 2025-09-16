package com.eno.protokolle.newmodel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

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
)  {
    /**
     * Baut eine dichte String-Matrix (rowCount × colCount) aus den sparse Zellen.
     * Nulls werden zu "", Zahlen/Bools korrekt zu String gerendert.
     */
    fun buildDenseBodyAsString(): List<List<String>> {
        val body = Array(rowCount) { Array(colCount) { "" } }
        for (cell in cells) {
            val rr = cell.r
            val cc = cell.c
            if (rr !in 0 until rowCount || cc !in 0 until colCount) continue
            val v = cell.v
            body[rr][cc] = when (v) {
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
        return body.map { it.toList() }
    }
}

@Serializable
data class Cell(
    val r: Int,
    val c: Int,
    /** in JSON kann das string/number/bool/null sein */
    val v: JsonElement? = null,
    /** optionaler Typ-Hinweis aus der Quelle (z.B. "AT","ZT",...) */
    val t: String? = null
)
