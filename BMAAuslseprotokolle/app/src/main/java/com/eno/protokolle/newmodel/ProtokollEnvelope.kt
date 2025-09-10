package com.eno.protokolle.newmodel

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class ProtokollEnvelope(
    val meta: Meta = Meta(),
    @SerialName("Protokoll")
    val protokoll: Protokoll = Protokoll()
)

@Serializable
data class Meta(
    /** Anlagentyp  (BMA,EMA,ELA,LR...)*/
    @SerialName("PType") val pType: String = "",
    /** Wartungstyp (1J, 2Q, 4Q, …) */
    @SerialName("Wtype") val wType: String = "",
    @SerialName("VNnr") val VNnr: String = "",
    @SerialName("Kunde") val Kunde: String = ""
)

@Serializable
data class Protokoll(
    @SerialName("MelderTypes") val melderTypes: List<String> = emptyList(),
    /** Immer Liste – wird im Codec ggf. aus Einzelobjekt normalisiert */
    @SerialName("Anlagen") val anlagen: List<Anlage> = emptyList()
)

@Serializable
data class Anlage(
    /** z.B. "Anlage Nr 1" */
    @SerialName("Name") val name: String = "",
    /** Erste Tabelle (Melder)  */
    @SerialName("Melder") val melder: Table? = null,
    /** Zweite Tabelle (Hardware) – optional */
    @SerialName("Hardware") val hardware: Table? = null,
    /** Bearbeiter pro Quartal – optional */
    @SerialName("EditedBy") val editedBy: EditedBy? = null
)

@Serializable
data class EditedBy(
    @SerialName("Q1") val q1: String = "",
    @SerialName("Q2") val q2: String = "",
    @SerialName("Q3") val q3: String = "",
    @SerialName("Q4") val q4: String = ""
)

@Serializable
data class Table(
    val head: Head? = null,
    val grid: Grid = Grid()
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
    val rowCount: Int = 0,
    val colCount: Int = 0,
    /**
     * Editierbarkeit pro Spalte (Key = Spaltenindex; JSON-Keys sind Strings, werden aber als Int geparst).
     * Werte: z.B. "string", "int", "bool", "none", …
     */
    val columnsEditable: Map<Int, String> = emptyMap(),
    /**
     * Sparse-Zellen (r=row, c=col, v=value, t=type).
     * v ist ein JsonElement, um String/Bool/Number/null zu erlauben.
     */
    val cells: List<Cell> = emptyList()
) {
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
