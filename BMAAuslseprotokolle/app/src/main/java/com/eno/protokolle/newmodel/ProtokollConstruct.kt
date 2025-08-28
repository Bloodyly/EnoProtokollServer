package com.eno.protokolle.newmodel

data class UiTable(
    val header: List<List<String>>,
    val spans: List<HeadSpan>?,           // aus Transportstruktur übernommen
    val rows: List<List<String>>,         // dichter Body als Strings (Grid bereits expandiert)
    val editors: Map<Int, ProtokollCodec.EditKind> // 0-basierte Spalten → Editortyp
)

data class UiAnlage(
    val name: String,
    val melder: UiTable,
    val hardware: UiTable? = null
)

data class ProtokollConstruct(
    val pType: String,               // meta.PType
    val wType: String,               // meta.WType
    val vN: String,                  // VN nummer
    val kdn: String,                  // Kundenname
    val melderTypes: List<String>,   // für Editor-Auswahl
    val anlagen: List<UiAnlage>
)
