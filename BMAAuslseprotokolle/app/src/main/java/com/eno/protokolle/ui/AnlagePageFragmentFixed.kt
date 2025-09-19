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
        private const val KEY_INDEX = "anlage_index" // gleich wie im alten Fragment
        fun new(index: Int) = AnlagePageFragmentFixed().apply {
            arguments = bundleOf(KEY_INDEX to index)
        }
    }

    private val vm: ProtokollViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val index = requireArguments().getInt(KEY_INDEX)
        val construct = vm.construct ?: return
        val anlage = construct.anlagen.getOrNull(index) ?: return

        val renderer = EnvelopeFixedTable(requireContext())

        val melder = view.findViewById<FixedHeaderTableLayout>(R.id.tableMelder)
        renderer.renderInto(melder, anlage.melder)

        val hw = view.findViewById<FixedHeaderTableLayout>(R.id.tableHardware)
        if (anlage.hardware != null) {
            renderer.renderInto(hw, anlage.hardware)
            hw.visibility = View.VISIBLE
        } else {
            hw.visibility = View.GONE
        }
    }
}
