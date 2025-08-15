package com.eno.protokolle
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.eno.protokolle.databinding.ActivityMainBinding
import com.inqbarna.tablefixheaders.TableFixHeaders
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.eno.protokolle.network.ReceiveAndDecode
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var tableFix: TableFixHeaders
    lateinit var tableData: Array<Array<String>>
    private lateinit var tableDataMelder: Array<Array<String>>

    companion object {
        var editMode = false
        var aktuellesQuartal = "Q1" // oder dynamisch initialisieren
        var wartungsTyp = "Quartal"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tableData = Array(1) { Array(1) { "" } } // Leeres Dummy-Array
        tableDataMelder = Array(1) { Array(1) { "" } } // Leeres Dummy-Array
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Startdialog erst NACH setContentView
        if (intent?.data == null) {
            showStartDialog()
        } else {

        }

        val textFabMain = findViewById<TextView>(R.id.textFabMain)
        val spinnerMelderTyp = findViewById<Spinner>(R.id.spinnerMelderTyp)

        val melderTypen = listOf("Normal", "TD-Max", "TD-Diff", "ZD", "Nicht Vorhanden")
        val adapterSpinner =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, melderTypen)
        spinnerMelderTyp.adapter = adapterSpinner

        var quartalVisible = false
        textFabMain.setOnClickListener {
            quartalVisible = !quartalVisible
            toggleQuartalsButtons()
        }

        tableFix = findViewById(R.id.tableFix)

        // ✅ Editmodus anlegen
        val editButton = findViewById<ImageButton>(R.id.buttonEdit)
        editButton.setOnClickListener {
            editMode = !editMode  // Edit-Modus umschalten
            reloadTable()
            editButton.setImageResource(if (editMode) android.R.drawable.ic_menu_save else android.R.drawable.ic_menu_edit)
            spinnerMelderTyp.visibility = if (editMode) View.VISIBLE else View.INVISIBLE
        }

        // ✅ settings button anlegen
        val settingsButton = findViewById<ImageButton>(R.id.buttonMenu)
        settingsButton.setOnClickListener {
            showSettingsDialog()
        }

        val adapter = MelderMatrixAdapter(
            this,
            tableData,
            tableDataMelder,
            getQuartal = { binding.textFabMain.text.toString() },
            getMelderTyp = { spinnerMelderTyp.selectedItem.toString() }
        )
        tableFix.adapter = adapter

        binding.fabDone.setOnClickListener {
            Toast.makeText(this, "Bearbeitung abgeschlossen!", Toast.LENGTH_SHORT).show()
            val adapter = tableFix.adapter as? MelderMatrixAdapter
            adapter?.getData()
            adapter?.getMelderTypMatrix()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showSettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val server = dialogView.findViewById<EditText>(R.id.editServer)
        val port = dialogView.findViewById<EditText>(R.id.editPort)
        val key = dialogView.findViewById<EditText>(R.id.editKey)
        val user = dialogView.findViewById<EditText>(R.id.editUser)
        val password = dialogView.findViewById<EditText>(R.id.editPassword)

        // 🔄 Bereits gespeicherte Werte anzeigen
        server.setText(prefs.getString("server", ""))
        port.setText(prefs.getString("port", ""))
        key.setText(prefs.getString("key", ""))
        user.setText(prefs.getString("user", ""))
        password.setText(prefs.getString("password", ""))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Einstellungen")
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<Button>(R.id.btnTest).setOnClickListener {
            Toast.makeText(this, "Verbindung wird getestet...", Toast.LENGTH_SHORT).show()
        }

        dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.edit()
                .putString("server", server.text.toString())
                .putString("port", port.text.toString())
                .putString("key", key.text.toString())
                .putString("user", user.text.toString())
                .putString("password", password.text.toString())
                .apply()

            Toast.makeText(this, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showStartDialog() {
        val optionen = arrayOf("VN Nummer manuell", "Demo-Daten laden", "Einstellungen")

        AlertDialog.Builder(this)
            .setTitle("Startoptionen")
            .setItems(optionen) { _, which ->
                when (which) {
                    0 -> {
                        // Eingabe der VN-Nummer
                        val input = EditText(this).apply {
                            hint = "Vertragsnummer"
                        }

                        AlertDialog.Builder(this)
                            .setTitle("VN Nummer eingeben")
                            .setView(input)
                            .setPositiveButton("OK") { _, _ ->
                                val vertragsnummer = input.text.toString().trim()
                                if (vertragsnummer.isNotBlank()) {
                                    ladeProtokollVomServer(vertragsnummer)
                                } else {
                                    Toast.makeText(this, "Bitte eine gültige VN eingeben", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton("Abbrechen", null)
                            .show()
                    }

                    1 -> {
                        // ✅ Nur hier Dummy-Daten laden
                        ladeDemoDaten()
                        Toast.makeText(this, "Demo-Daten geladen", Toast.LENGTH_SHORT).show()
                    }

                    2 -> showSettingsDialog()
                }
            }
            .setCancelable(false)
            .show()
    }
    private fun ladeProtokollVomServer(vertragsnummer: String) {
        lifecycleScope.launch {
            val protokoll = ReceiveAndDecode.receiveProtokoll(this@MainActivity, vertragsnummer)
            if (protokoll != null) {
                Toast.makeText(this@MainActivity, "Daten erfolgreich geladen", Toast.LENGTH_SHORT).show()

                tableData = erstelleMatrixDaten(protokoll)
                tableDataMelder = erstelleMelderTypMatrix(protokoll)

                wartungsTyp = protokoll.wartungstyp
                aktuellesQuartal = bestimmeQuartal()
                setupQuartalsButtons(wartungsTyp)

                findViewById<TextView>(R.id.textCustomer).text = protokoll.kundenname
                findViewById<TextView>(R.id.textVertragsnummer).text = "VN: ${protokoll.vertragsnummer}"

                reloadTable()
            } else {
                Toast.makeText(this@MainActivity, "Fehler beim Abrufen der Daten", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun ladeDemoDaten() {
        val dummyProtokoll = ProtokollListe(
            vertragsnummer = "123456",
            kundenname = "Feuerwehr HQ",
            wartungstyp = "Halbjahr",
            gruppen = listOf(
                MeldeGruppe("AM", "1", 15, mutableListOf("", "", "", "", "", "", "", "", "", "", "", "", "", "", ""), mutableListOf("", "ZD", "ZD", "T-Diff", "T-MAX", "", "", "", "", "", "", "", "", "", "")),
                MeldeGruppe("NAM", "2", 2, mutableListOf("Q1", "", "-"), mutableListOf("", "", "")),
                MeldeGruppe("RAS", "103", 4, mutableListOf("", "-", "", ""), mutableListOf("", "-", "", ""))
            )
        )

        wartungsTyp = dummyProtokoll.wartungstyp
        aktuellesQuartal = bestimmeQuartal()
        setupQuartalsButtons(dummyProtokoll.wartungstyp)

        findViewById<TextView>(R.id.textCustomer).text = dummyProtokoll.kundenname
        findViewById<TextView>(R.id.textVertragsnummer).text = "VN: ${dummyProtokoll.vertragsnummer}"

        tableData = erstelleMatrixDaten(dummyProtokoll)
        tableDataMelder = erstelleMelderTypMatrix(dummyProtokoll)

        reloadTable()
    }

    private fun erstelleMatrixDaten(protokoll: ProtokollListe): Array<Array<String>> {
        val maxMelder = protokoll.gruppen.maxOfOrNull { it.anzahl } ?: 0

        val spalten = 3 + maxMelder // 3 feste Spalten: Typ, Gruppe, Anzahl + Melder
        val zeilen = 1 + protokoll.gruppen.size // Kopfzeile + jede Gruppe

        val matrix = Array(zeilen) { Array(spalten) { "" } }

        // Kopfzeile
        matrix[0][0] = "Gruppe"
        matrix[0][1] = "Typ"
        matrix[0][2] = "Anz"
        for (i in 1..maxMelder) {
            matrix[0][i + 2] = i.toString()
        }

        // Datenzeilen
        for ((rowIndex, gruppe) in protokoll.gruppen.withIndex()) {
            val row = rowIndex + 1 // wegen Kopfzeile

            matrix[row][0] = gruppe.gruppe
            matrix[row][1] = gruppe.typ
            matrix[row][2] = gruppe.anzahl.toString()

            for (i in 0 until maxMelder) {
                matrix[row][i + 3] = if (i < gruppe.werte.size) {
                    gruppe.werte[i].ifBlank { " " }
                } else {
                    "-" // Feld existiert nicht → sperren
                }
            }
        }

        return matrix
    }
    private fun erstelleMelderTypMatrix(protokoll: ProtokollListe): Array<Array<String>> {
        val maxMelder = protokoll.gruppen.maxOfOrNull { it.anzahl } ?: 0
        val zeilen = 1 + protokoll.gruppen.size
        val spalten = 3 + maxMelder

        return Array(zeilen) { rowIndex ->
            Array(spalten) { columnIndex ->
                if (rowIndex == 0 || columnIndex < 3) {
                    ""  // Kopfzeile und feste Spalten leer lassen
                } else {
                    val gruppe = protokoll.gruppen[rowIndex - 1]
                    val melderIndex = columnIndex - 3
                    if (melderIndex < gruppe.melderTypen.size) {
                        gruppe.melderTypen[melderIndex].ifBlank { " " }
                    } else {
                        "-"
                    }
                }
            }
        }
    }

    //Tabellen Funktionen--------------------------------------------------------------------------
    fun reloadTable() {
        tableFix.adapter = MelderMatrixAdapter(
            this,
            tableData,
            tableDataMelder,
            getQuartal = { findViewById<TextView>(R.id.textFabMain).text.toString() },
            getMelderTyp = { findViewById<Spinner>(R.id.spinnerMelderTyp).selectedItem.toString() }
        )
    }
    private fun collapseQuartalsButtons(buttons: List<View>) {
        buttons.forEach { btn ->
            if (btn.visibility != View.GONE) {
                btn.animate()
                    .translationY(0f)
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction { btn.visibility = View.INVISIBLE }
                    .start()
            }
        }
    }
    private var quartalsVisible = false

    private fun toggleQuartalsButtons() {
        val buttons = listOf(binding.textDefect, binding.textQ1, binding.textQ2, binding.textQ3, binding.textQ4)
        val animDistance = 180  // Abstand pro Button (in px)

        if (!quartalsVisible) {
            buttons.forEachIndexed { index, btn ->
                if (btn.visibility != View.GONE) {
                    btn.visibility = View.VISIBLE
                    btn.translationY = 0f
                    btn.alpha = 0f
                    btn.animate()
                        .translationY((-1 * animDistance * (index + 1)).toFloat()) // gestaffelt nach oben
                        .alpha(1f)
                        .setDuration(300)
                        .start()
                }
            }
        } else {
            buttons.forEach { btn ->
                if (btn.visibility != View.GONE) {
                    btn.animate()
                        .translationY(0f)
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction { btn.visibility = View.INVISIBLE }
                        .start()
                }
            }
        }
        quartalsVisible = !quartalsVisible
    }
    private fun bestimmeQuartal(): String {
        val monat = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        return when (wartungsTyp) {
            "Quartal" -> {
                when (monat) {
                    in 1..3 -> "Q1"
                    in 4..6 -> "Q2"
                    in 7..9 -> "Q3"
                    else -> "Q4"
                }
            }
            "Halbjahr" -> {
                when (monat) {
                    in 1..6 -> "H1"
                    else -> "H2"
                }
            }
            else -> {
                "i.O."
            }
        }

    }


    private fun setupQuartalsButtons(wartungstyp: String) {
        val textDefect = findViewById<TextView>(R.id.textDefect)
        val textQ1 = findViewById<TextView>(R.id.textQ1)
        val textQ2 = findViewById<TextView>(R.id.textQ2)
        val textQ3 = findViewById<TextView>(R.id.textQ3)
        val textQ4 = findViewById<TextView>(R.id.textQ4)
        val quartalButtons = listOf(textDefect,textQ1, textQ2, textQ3, textQ4)
        val mainTextFab = findViewById<TextView>(R.id.textFabMain)
        quartalButtons.forEach { button ->
            button.setOnClickListener {
                val selectedQuartal = button.text.toString()
                aktuellesQuartal = selectedQuartal
                mainTextFab.text = aktuellesQuartal
                if (aktuellesQuartal=="n.i.O."){
                    mainTextFab.setBackgroundResource(R.drawable.bg_defekt_fab)
                } else {
                    mainTextFab.setBackgroundResource(R.drawable.bg_quartal_fab)
                }
                reloadTable()
                collapseQuartalsButtons(quartalButtons)
            }
        }
        when (wartungstyp) {
            "Quartal" -> {
                // Quartale anzeigen
                textQ1.text = "Q1"
                textQ2.text = "Q2"
                textQ3.text = "Q3"
                textQ4.text = "Q4"

                textQ1.visibility = View.INVISIBLE
                textQ2.visibility = View.INVISIBLE
                textQ3.visibility = View.INVISIBLE
                textQ4.visibility = View.INVISIBLE

                mainTextFab.text = bestimmeQuartal()
                mainTextFab.isEnabled = true
            }
            "Halbjahr" -> {
                textQ1.text = "1.HJ"
                textQ2.text = "2.HJ"

                textQ1.visibility = View.INVISIBLE
                textQ2.visibility = View.INVISIBLE
                textQ3.visibility = View.GONE
                textQ4.visibility = View.GONE

                mainTextFab.text = aktuellesQuartal
                mainTextFab.isEnabled = true
            }
            "Jahr" -> {
                // Nur ein Button mit "i.O."
                textQ1.text = "i.O."
                textQ1.visibility = View.GONE
                textQ2.visibility = View.GONE
                textQ3.visibility = View.GONE
                textQ4.visibility = View.GONE

                mainTextFab.text = "i.O."
                mainTextFab.isEnabled = false
            }
        }
    }
}