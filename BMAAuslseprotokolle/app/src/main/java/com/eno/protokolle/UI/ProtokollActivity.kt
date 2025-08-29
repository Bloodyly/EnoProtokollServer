package com.eno.protokolle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.inqbarna.tablefixheaders.TableFixHeaders
import com.eno.protokolle.network.ReceiveAndDecode
import com.eno.protokolle.newmodel.ProtokollEnvelope
import com.eno.protokolle.ui.TableAnlageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProtokollActivity : ComponentActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var table: TableFixHeaders

    private var envelope: ProtokollEnvelope? = null
    private var selectedIndex: Int = 0
    private var tableAdapter: TableAnlageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_protokoll)

        findViewById<MaterialToolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
        tabLayout = findViewById(R.id.tabLayoutAnlagen)
        table = findViewById(R.id.tableFix)

        selectedIndex = savedInstanceState?.getInt("selectedIndex") ?: 0

        // Vertragsnummer kommt als Extra (z. B. aus Intro/Settings)
        val vertragsnummer = intent.getStringExtra("vertragsnummer") ?: ""

        // Protokoll laden (nutzt AppPrefs intern bereits in ReceiveAndDecode)
        lifecycleScope.launch {
            val env = withContext(Dispatchers.IO) {
                ReceiveAndDecode.receiveProtokollNew(this@ProtokollActivity, vertragsnummer)
            }
            envelope = env
            setupTabsAndTable()
        }
    }

    private fun setupTabsAndTable() {
        val env = envelope ?: return

        tabLayout.removeAllTabs()

        // >>>> PASSE diesen Zugriff an DEINE Datenstruktur an:
        val anlagen = env.anlagen ?: emptyList()   // z. B. List<Anlage>
        // <<<<

        if (anlagen.isEmpty()) {
            tabLayout.visibility = TabLayout.GONE
            tableAdapter = TableAnlageAdapter(emptyList(), emptyList(), emptyList())
            table.setAdapter(tableAdapter)
            return
        } else {
            tabLayout.visibility = TabLayout.VISIBLE
        }

        anlagen.forEachIndexed { i, anlage ->
            val title = anlagenTabTitle(anlage, i) // s. helper unten
            tabLayout.addTab(tabLayout.newTab().setText(title), i == selectedIndex)
        }

        bindTableForIndex(selectedIndex)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedIndex = tab.position
                bindTableForIndex(selectedIndex)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        if (anlagen.size == 1) tabLayout.visibility = TabLayout.GONE
    }

    private fun bindTableForIndex(index: Int) {
        val env = envelope ?: return
        val anlagen = env.anlagen ?: return
        if (index !in anlagen.indices) return

        val anlage = anlagen[index]

        // ---- Mapper: baue aus deiner Modellstruktur die Tabelle ----
        val headers: List<String> = mapHeaders(anlage)
        val leftCol: List<String> = mapLeftColumn(anlage)
        val rows: List<List<String>> = mapRows(anlage, headers)

        if (tableAdapter == null) {
            tableAdapter = TableAnlageAdapter(headers, leftCol, rows)
            table.setAdapter(tableAdapter)
        } else {
            tableAdapter!!.update(headers, leftCol, rows)
            tableAdapter!!.notifyDataSetChanged()
        }
    }

    // ===== Helpers: Diese bitte an DEINE Felder anpassen =====

    private fun anlagenTabTitle(anlage: Any, index: Int): String {
        // Beispiel: wenn anlage einen Namen hat -> anlage.name
        // return (anlage as Anlage).name ?: "Anlage ${index+1}"
        return "Anlage ${index + 1}"
    }

    private fun mapHeaders(anlage: Any): List<String> {
        // z. B. listOf("Nr.", "Bereich", "Text", "Status", "Zeit")
        return listOf("Nr.", "Bereich", "Text", "Status", "Zeit")
    }

    private fun mapLeftColumn(anlage: Any): List<String> {
        // z. B. laufende Nummern
        // return (anlage as Anlage).melder.mapIndexed { i, _ -> (i+1).toString() }
        return emptyList()
    }

    private fun mapRows(anlage: Any, headers: List<String>): List<List<String>> {
        // z. B.:
        // return (anlage as Anlage).melder.map { m -> listOf(m.nr, m.bereich, m.text, m.status, m.zeit) }
        return emptyList()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedIndex", selectedIndex)
    }
}
