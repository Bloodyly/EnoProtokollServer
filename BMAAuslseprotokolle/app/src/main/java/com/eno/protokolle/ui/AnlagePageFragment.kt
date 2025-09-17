// AnlagePageFragment.kt
package com.eno.protokolle.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.eno.protokolle.R
import com.celerysoft.tablefixheaders.TableFixHeaders

class AnlagePageFragment : Fragment(R.layout.frag_anlage_page) {

    companion object {
        private const val KEY_INDEX = "anlage_index"
        fun new(index: Int) = AnlagePageFragment().apply {
            arguments = bundleOf(KEY_INDEX to index)
        }
    }

    private val vm: ProtokollViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val index = requireArguments().getInt(KEY_INDEX)
        val construct = vm.construct ?: return
        val anlage = construct.anlagen.getOrNull(index) ?: return

        val melder = view.findViewById<TableFixHeaders>(R.id.tableMelder)
        melder.adapter = EnvelopeTableAdapter(requireContext(), anlage.melder)

        val hw = view.findViewById<TableFixHeaders>(R.id.tableHardware)
        if (anlage.hardware != null) {
            hw.adapter = EnvelopeTableAdapter(requireContext(), anlage.hardware)
            hw.visibility = View.VISIBLE
        } else {
            hw.visibility = View.GONE
        }
    }
}
