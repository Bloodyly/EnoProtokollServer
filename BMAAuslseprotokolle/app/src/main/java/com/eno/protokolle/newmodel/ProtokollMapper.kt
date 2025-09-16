package com.eno.protokolle.newmodel

object ProtokollMapper {

    fun toConstruct(env: ProtokollEnvelope): ProtokollConstruct {

        val pType = env.meta.pType
        val wType = env.meta.wType
        val vN = env.meta.VNnr
        val kdn = env.meta.Kunde
        val melderTypes = env.protokoll.melderTypes

        val uiAnlagen = env.protokoll.anlagen.mapNotNull { anlage ->
            val melderTable = anlage.melder?.let { t ->
                val dense = t.grid.asDenseBodyStrings()
                val editors = ProtokollCodec.run { t.grid.columnsEditableAsIntMap() }
                UiTable(
                    header = t.head?.rows ?: emptyList(),
                    spans  = t.head?.spans,
                    rows   = dense,
                    editors = editors,
                    qStartCol = t.grid.qStartCol,       // NEU
                    itemsEditable = t.itemsEditable     // NEU
                )
            } ?: return@mapNotNull null // ohne Melder-Tabelle keine sinnvolle Anlage

            val hwTable = anlage.hardware?.let { hw ->
                val dense = hw.grid.asDenseBodyStrings()
                val editors = ProtokollCodec.run {
                    hw.grid.columnsEditableAsIntMap(defaultKind = ProtokollCodec.EditKind.string)
                }
                UiTable(
                    header = hw.head?.rows ?: emptyList(),
                    spans  = hw.head?.spans,
                    rows   = dense,
                    editors = editors,
                    qStartCol = hw.grid.qStartCol,      // NEU
                    itemsEditable = hw.itemsEditable    // NEU
                )
            }

            UiAnlage(
                name = anlage.name,
                melder = melderTable,
                hardware = hwTable
            )
        }

        return ProtokollConstruct(
            pType = pType,
            wType = wType,
            vN = vN,
            kdn = kdn,
            melderTypes = melderTypes,
            anlagen = uiAnlagen
        )
    }
}
