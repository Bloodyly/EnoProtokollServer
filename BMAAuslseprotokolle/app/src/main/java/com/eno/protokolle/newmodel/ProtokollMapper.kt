package com.eno.protokolle.newmodel

object ProtokollMapper {

    fun toConstruct(env: ProtokollEnvelope): ProtokollConstruct {
        val pType = env.meta.pType
        val wType = env.meta.wType
        val vN = env.protokoll.VN
        val kdn = env.protokoll.Kdn
        val melderTypes = env.protokoll.MelderTypes

        val uiAnlagen = env.protokoll.Anlagen.map { anlage ->
            // Melder-Tabelle
            val melderDense = ProtokollCodec.run { anlage.Melder.grid.buildDenseBodyAsString() }
            val melderEditors = ProtokollCodec.run { anlage.Melder.grid.columnsEditableAsIntMap() }
            val melderTable = UiTable(
                header = anlage.Melder.head.rows,
                spans = anlage.Melder.head.spans,
                rows = melderDense,
                editors = melderEditors
            )

            // Hardware (optional)
            val hwTable = anlage.Hardware?.let { hw ->
                val dense = ProtokollCodec.run { hw.grid.buildDenseBodyAsString() }
                val editors = ProtokollCodec.run { hw.grid.columnsEditableAsIntMap(defaultKind = ProtokollCodec.EditKind.string) }
                UiTable(
                    header = hw.head.rows,
                    spans = hw.head.spans,
                    rows = dense,
                    editors = editors
                )
            }

            UiAnlage(
                name = anlage.Name,
                melder = melderTable,
                hardware = hwTable
            )
        }

        return ProtokollConstruct(
            pType = pType,
            wType = wType,
            kdn = kdn,
            vN = vN,
            melderTypes = melderTypes,
            anlagen = uiAnlagen
        )
    }
}
