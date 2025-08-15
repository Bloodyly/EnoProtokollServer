package com.eno.protokolle

data class MeldeGruppe(
    val typ: String,
    val gruppe: String,
    val anzahl: Int,
    val werte: MutableList<String>,
    var melderTypen: MutableList<String>
)

