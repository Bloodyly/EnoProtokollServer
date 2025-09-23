package com.eno.protokolle.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.eno.protokolle.R
import com.github.zardozz.FixedHeaderTableLayout.FixedHeaderTableLayout

class AnlagePageFragmentFixed : Fragment(R.layout.frag_anlage_page_fixed) {

    companion object {
        private const val KEY_INDEX = "anlage_index"
        fun new(index: Int) = AnlagePageFragmentFixed().apply {
            arguments = bundleOf(KEY_INDEX to index)
        }
    }

    private val vm: ProtokollViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val construct = vm.construct ?: return
        val index = requireArguments().getInt(KEY_INDEX)
        val anlage = construct.anlagen.getOrNull(index) ?: return

        val table = view.findViewById<FixedHeaderTableLayout>(R.id.tableCombined)

        val sections = buildList {
            add(UiTableSection("Melder", anlage.melder))
            anlage.hardware?.let { add(UiTableSection("Hardware", it)) }
        }

        MultiSectionFixedTable(requireContext(), textSizeSp = 14f, rowHeightDp = 40, padHDp = 8)
            .renderInto(table, sections)
    }
}
