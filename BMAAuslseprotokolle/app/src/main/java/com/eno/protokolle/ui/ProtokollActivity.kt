package com.eno.protokolle.ui

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.inqbarna.tablefixheaders.TableFixHeaders
import com.eno.protokolle.R
import com.eno.protokolle.newmodel.UiAnlage

class ProtokollActivity : ComponentActivity() {

    // Titel-Leiste
    private lateinit var textCustomer: TextView
    private lateinit var textVertragsnummer: TextView
    private lateinit var buttonEdit: ImageButton
    private lateinit var buttonMenu: ImageButton

    // Tabs & Tabelle
    private lateinit var tabLayoutAnlagen: TabLayout
    private lateinit var tableFix: TableFixHeaders

    // Bottom + FABs + Spinner
    private lateinit var bottomAppBar: BottomAppBar
    private lateinit var fabDone: FloatingActionButton
    private lateinit var textFabMain: TextView
    private lateinit var textDefect: TextView
    private lateinit var textQ1: TextView
    private lateinit var textQ2: TextView
    private lateinit var textQ3: TextView
    private lateinit var textQ4: TextView
    private lateinit var spinnerMelderTyp: Spinner

    private var tableAdapter: TableAnlageAdapter? = null

    // TODO: ersetze durch echte Daten (Mapper → Construct → anlagen)
    private val demoAnlagen: List<UiAnlage> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_protokoll) // dein XML

        bindViews()
        initUiBasics()

        // Tabs + Tabelle initialisieren
        setupTabsAndTable(demoAnlagen)
    }

    private fun bindViews() {
        textCustomer = findViewById(R.id.textCustomer)
        textVertragsnummer = findViewById(R.id.textVertragsnummer)
        buttonEdit = findViewById(R.id.buttonEdit)
        buttonMenu = findViewById(R.id.buttonMenu)

        tabLayoutAnlagen = findViewById(R.id.tabLayoutAnlagen)
        tableFix = findViewById(R.id.tableFix)

        bottomAppBar = findViewById(R.id.bottomAppBar)
        fabDone = findViewById(R.id.fabDone)
        textFabMain = findViewById(R.id.textFabMain)
        textDefect = findViewById(R.id.textDefect)
        textQ1 = findViewById(R.id.textQ1)
        textQ2 = findViewById(R.id.textQ2)
        textQ3 = findViewById(R.id.textQ3)
        textQ4 = findViewById(R.id.textQ4)
        spinnerMelderTyp = findViewById(R.id.spinnerMelderTyp)
    }

    private fun initUiBasics() {
        // Beispiel-Befüllung der Kopfzeile – ersetze mit echten Werten
        textCustomer.text = "Kunde XY"
        textVertragsnummer.text = "VN: 123456"

        // einfache Clicks (kannst du später mit Logik füllen)
        buttonEdit.setOnClickListener { /* TODO: Edit-Modus */ }
        buttonMenu.setOnClickListener { /* TODO: Menü */ }
        fabDone.setOnClickListener { /* TODO: Speichern/Senden */ }

        textFabMain.setOnClickListener { toggleQuarterButtons(true) }
        // Wenn man einen Quartals-Button antippt, wieder einklappen:
        val collapse = { toggleQuarterButtons(false) }
        textDefect.setOnClickListener { collapse() }
        textQ1.setOnClickListener { collapse() }
        textQ2.setOnClickListener { collapse() }
        textQ3.setOnClickListener { collapse() }
        textQ4.setOnClickListener { collapse() }
    }

    private fun toggleQuarterButtons(show: Boolean) {
        val v = if (show) android.view.View.VISIBLE else android.view.View.INVISIBLE
        textDefect.visibility = v
        textQ1.visibility = v
        textQ2.visibility = v
        textQ3.visibility = v
        textQ4.visibility = v
    }

    private fun setupTabsAndTable(anlagen: List<UiAnlage>) {
        tabLayoutAnlagen.removeAllTabs()

        if (anlagen.isEmpty()) {
            // leere Tabelle anzeigen
            val headers = listOf("Nr.", "Bereich", "Text", "Status", "Zeit")
            tableAdapter = TableAnlageAdapter(headers, emptyList(), emptyList())
            tableFix.setAdapter(tableAdapter)
            return
        }

        anlagen.forEachIndexed { index, anlage ->
            val title = anlage.name.ifBlank { "Anlage ${index + 1}" }
            tabLayoutAnlagen.addTab(tabLayoutAnlagen.newTab().setText(title), index == 0)
        }

        tabLayoutAnlagen.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                bindAnlage(anlagen[tab.position])
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // initial
        bindAnlage(anlagen.first())
    }

    private fun bindAnlage(anlage: UiAnlage) {
        val headers = listOf("Nr.", "Bereich", "Text", "Status", "Zeit")
        val rowCount = anlage.melder.rows.size
        val left = List(rowCount) { (it + 1).toString() }
        val rows = anlage.melder.rows.map { r ->
            if (r.size >= headers.size) r.take(headers.size)
            else r + List(headers.size - r.size) { "" }
        }

        if (tableAdapter == null) {
            tableAdapter = TableAnlageAdapter(headers, left, rows)
            tableFix.setAdapter(tableAdapter)
        } else {
            tableAdapter!!.update(headers, left, rows)
            tableAdapter!!.notifyDataSetChanged()
        }
    }
}
