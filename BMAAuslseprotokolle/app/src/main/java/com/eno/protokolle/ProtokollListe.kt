package com.eno.protokolle

data class ProtokollListe(
    val vertragsnummer: String,
    val kundenname: String,
    val wartungstyp: String,
    val gruppen: List<MeldeGruppe>
)