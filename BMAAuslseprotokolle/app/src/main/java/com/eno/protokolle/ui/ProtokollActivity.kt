package com.eno.protokolle.ui

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.eno.protokolle.R
import com.eno.protokolle.network.ProtokollStorage
import com.eno.protokolle.newmodel.ProtokollMapper
import com.eno.protokolle.newmodel.UiAnlage
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.serialization.json.Json



class ProtokollActivity : AppCompatActivity(R.layout.layout_protokoll) {

    companion object {
        const val EXTRA_VN = "EXTRA_VN"   // optionaler Key zum gezielten Laden
    }

    private lateinit var textCustomer: TextView
    private lateinit var textVertragsnummer: TextView
    private lateinit var buttonEdit: ImageButton
    private lateinit var buttonMenu: ImageButton
    private lateinit var spinnerMelderTyp: Spinner
    private lateinit var tabLayout: TabLayout
    private lateinit var pager: ViewPager2

    private val vm: ProtokollViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindViews()
        // Wischen zwischen Tabs deaktivieren – nur Klick auf Tab erlaubt
        pager.isUserInputEnabled = false

        // 1) Protokoll aus Storage laden (per EXTRA_VN oder "neueste")
        val key = intent.getStringExtra(EXTRA_VN)
        val env = if (key != null) {
            ProtokollStorage.load(this, key)
        } else {
            ProtokollStorage.loadLatest(this)?.second
        }

        if (env == null) {
            Toast.makeText(this, "Kein gespeichertes Protokoll gefunden.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        // DEbug der Anlagen
        android.util.Log.d("Protokoll", "Srv anlagen=" +
                env.protokoll.anlagen.joinToString { it.name })

        // 2) Envelope -> UI-Modell mappen
        val construct = ProtokollMapper.toConstruct(env)
        vm.construct = construct
        //DEbug der Anlagen Nach Decoding
        android.util.Log.d("Protokoll", "UI anlagen=" +
                construct.anlagen.joinToString { it.name })

        // 3) Titelzeile füllen
        textCustomer.text = construct.kdn
        textVertragsnummer.text = "VN: ${construct.vN}"

        // 4) Tabs + Pager mit Anlagen
        setupPager()

        // 5) Buttons/Spinner wie gehabt
        initUiBasics(construct)
    }

    private fun bindViews() {
        textCustomer = findViewById(R.id.textCustomer)
        textVertragsnummer = findViewById(R.id.textVertragsnummer)
        buttonEdit = findViewById(R.id.buttonEdit)
        buttonMenu = findViewById(R.id.buttonMenu)
        spinnerMelderTyp = findViewById(R.id.spinnerMelderTyp)
        tabLayout = findViewById(R.id.tabLayoutAnlagen)
        pager = findViewById(R.id.pagerAnlagen)
    }

    private fun setupPager() {
        val anlagen = vm.construct?.anlagen ?: emptyList()

        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = anlagen.size
            override fun createFragment(position: Int) =
                AnlagePageFragmentFixed.new(position) // zum Testen
            // später wieder: AnlagePageFragment.new(position)
        }

        TabLayoutMediator(tabLayout, pager) { tab, pos ->
            tab.text = anlagen[pos].name.ifBlank { "Anlage ${pos + 1}" }
        }.attach()
    }

    private fun initUiBasics(construct: com.eno.protokolle.newmodel.ProtokollConstruct) {
        // TODO: Spinner für construct.melderTypes füllen, FAB-Logik je nach aktuellem Tab etc.
        // Beispiel:
        // spinnerMelderTyp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, construct.melderTypes)
        buttonEdit.setOnClickListener {
            val currentAnlage = construct.anlagen.getOrNull(pager.currentItem)
            // TODO: Editor öffnen für aktuelle Anlage
        }
        buttonMenu.setOnClickListener {
            // TODO: Menü
        }
    }
}
