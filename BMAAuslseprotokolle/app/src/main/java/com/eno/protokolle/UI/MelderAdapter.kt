package com.eno.protokolle.UI

import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
//import android.util.Log
import android.animation.ValueAnimator
import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import com.inqbarna.tablefixheaders.adapters.BaseTableAdapter
import androidx.core.graphics.toColorInt
import com.eno.protokolle.MainActivity
import com.eno.protokolle.R

class MelderMatrixAdapter(
    private val context: Context,
    private var data: Array<Array<String>>,
    private var melderTypMatrix: Array<Array<String>>,
    private val getQuartal: () -> String,  // 🔁 Callback
    private val getMelderTyp: () -> String  // 🔁 Callback
) : BaseTableAdapter() {

    private val cellWidth = dpToPx(50)
    private val cellHeight = dpToPx(30)

    override fun getRowCount(): Int = data.size-1
    override fun getColumnCount(): Int = data[0].size -1


    override fun getView(row: Int, column: Int, convertView: View?, parent: ViewGroup?): View {
        val textView = (convertView as? TextView) ?: TextView(context).apply {
            gravity = Gravity.CENTER
            setPadding(4, 4, 4, 4)
            setTextColor(Color.BLACK)
        }
        if (MainActivity.editMode && (column <= 2)) {
            textView.isEnabled = true
            textView.setOnClickListener {
                // ✏️ Textfeld bearbeiten – gleich bauen wir dazu einen Dialog
            }
        }
        val value = data[row + 1][column + 1]
        val typ = melderTypMatrix[row + 1][column + 1]

        if (value.isBlank() || MainActivity.editMode ) {
            if (row<0 || column<=1){
                textView.text = value
                textView.setTextColor(ContextCompat.getColor(context, R.color.black))
            }else {
                textView.text = typ
                textView.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.hint
                    )
                )// 🟡 Heller Typ als Platzhalter
            }
        } else {
            textView.text = value
            textView.setTextColor(ContextCompat.getColor(context, R.color.black))
        }

        // Nur das Drawable setzen, NICHT zusätzlich setBackgroundColor!
        textView.setBackgroundResource(R.drawable.table_cell_background)
        textView.isEnabled = true
        textView.setOnClickListener(null)

            // ✏️ Textfeld bearbeiten
            if (MainActivity.editMode && (row>=0)) {
                textView.isEnabled = true
                if (column<=1) {
                    textView.setOnClickListener {
                        val currentValue = textView.text.toString()

                        val input = EditText(context).apply {
                            setText(currentValue)
                            if (column == 1) { // ✅ Nur Ganzzahlen erlauben bei melderanzahl}
                                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                            }
                        }

                        AlertDialog.Builder(context)
                            .setTitle("Wert ändern")
                            .setView(input)
                            .setPositiveButton("OK") { _, _ ->
                                val newValue = input.text.toString()
                                if (column == 1) {
                                    val neueAnzahl = newValue.toIntOrNull()
                                    if (neueAnzahl != null && neueAnzahl >= 0) {
                                        // ✅ Eingabe ist gültig
                                        textView.text = newValue
                                        data[row + 1][column + 1] = newValue
                                        // Aktuelle maximale Spaltenanzahl berechnen
                                        val aktuelleMelderSpalten =
                                            data[0].size - 3  // Fixspalten abziehen
                                        // ⬇️ Liste dynamisch anpassen
                                        if (neueAnzahl > aktuelleMelderSpalten) {
                                            erweitereSpalten(row + 1, neueAnzahl + 3)
                                        } else {
                                            // Tabelle einfach neu laden
                                            if (context is MainActivity) {
                                                context.reloadTable()
                                            }
                                        }

                                    } else {
                                        // ❌ Ungültig (keine Zahl)
                                        AlertDialog.Builder(context)
                                            .setMessage("Bitte eine gültige ganze Zahl eingeben.")
                                            .setPositiveButton("OK", null)
                                            .show()
                                    }
                                } else {
                                    textView.text = newValue
                                    data[row + 1][column + 1] = newValue
                                }
                            }
                            .setNegativeButton("Abbrechen", null)
                            .show()
                    }
                }else{
                    textView.isEnabled = true
                    val selectedMelderTyp= getMelderTyp()// wert aus dem Spinner
                    val typeToEnter = when (selectedMelderTyp) {
                        "Normal" -> ""
                        "TD-Diff" -> "T-Diff"
                        "TD-Max" -> "T-Max"
                        "ZD" -> "ZD"
                        "Nicht Vorhanden" -> "-"
                        else -> ""
                    }

                    //Log.d("DEBUG", "Neuer typ gesetzt! : $typeToEnter")
                    textView.setOnClickListener {
                        melderTypMatrix[row + 1][column + 1] = typeToEnter
                        if (typeToEnter!="-" && data[row+1][column+1]=="-"){
                            data[row+1][column+1] = ""
                        }
                        if (typeToEnter=="-"){
                            data[row+1][column+1]=typeToEnter
                        }
                        if (context is MainActivity) {
                            context.reloadTable()
                        }
                    }
                }
        }
        else if (value == "-" && !MainActivity.editMode) {
            // Kein Rahmen bei deaktivierter Zelle
            textView.setBackgroundColor(Color.LTGRAY)
            textView.isEnabled = false
        } else if (row >= 0 && column == 1 ){
            textView.isEnabled = true
            textView.setOnClickListener {
                val selectedQuartal = getQuartal()
                val zeile = row + 1

                // Prüfen: Ist der Modus "Befüllen" oder "Leeren"?
                val istBereitsAusgefuellt = data[zeile].any { it == selectedQuartal }
                //Log.d("DEBUG", "Listener für Gesamt : $istBereitsAusgefuellt")
                for (i in 2 until data[zeile].size) { // ab Spalte 3 → Melderfelder
                    val current = data[zeile][i]
                    if (istBereitsAusgefuellt) {
                        // Leeren, wenn bereits Quartal gesetzt war
                        if (current == selectedQuartal) {
                            data[zeile][i] = ""
                        }
                    } else {
                        // Nur befüllen, wenn leer
                        if (current.isBlank()) {
                            data[zeile][i] = selectedQuartal
                        }
                    }
                }
                if (context is MainActivity) {
                    context.reloadTable()
                }
            }
        } else if (row >= 0 && column > 1 ) {
            textView.isEnabled = true
            val textToEnter = getQuartal()
            textView.setOnClickListener {
                val current = textView.text.toString()
                val hinttext = melderTypMatrix[row+1][column+1]
                // 🛑 Quartals-Sperrprüfung
                val istVorherigesQuartal = when (textToEnter) {
                    "Q2" -> current == "Q1"
                    "Q3" -> current == "Q1" || current == "Q2"
                    "Q4" -> current == "Q1" || current == "Q2" || current == "Q3"
                    "2.HJ" -> current == "1.HJ"
                    else -> false
                }
                if (istVorherigesQuartal) {
                    textView.setTextColor(Color.GRAY)
                    Toast.makeText(
                        context,
                        "Frühere Quartale dürfen nicht geändert werden",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (current == textToEnter ){
                    textView.setTextColor(ContextCompat.getColor(context, R.color.hint))
                }else {
                    textView.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                val newValue = if (current == textToEnter )  hinttext else textToEnter
                textView.text = newValue
                if (newValue == "n.i.O.")
                    textView.setTextColor(Color.RED)
                data[row + 1][column + 1] = newValue

                // ✅ Sanfte Farb-Animation über Layer
                val background = textView.background
                if (background is android.graphics.drawable.LayerDrawable) {
                    val animLayer = background.findDrawableByLayerId(R.id.anim_background)
                    //Log.d("DEBUG", "animLayer=$animLayer")
                    if (animLayer is android.graphics.drawable.GradientDrawable) {
                        val fromColor = "#E0E0E0".toColorInt()  // z. B. hellgrau
                        val toColor = Color.WHITE

                        val anim = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
                            duration = 200
                            addUpdateListener { animator ->
                                val color = animator.animatedValue as Int
                                animLayer.setColor(color) // ✅ setze animierte Farbe
                            }
                        }
                        anim.start()
                    } else {
                        //Log.w("ZELLENKLICK", "animLayer ist kein GradientDrawable: $animLayer")
                    }
                }
            }
        }

        return textView
    }


    override fun getViewTypeCount(): Int = 1
    override fun getItemViewType(row: Int, column: Int): Int = 0
    override fun getHeight(row: Int): Int = cellHeight
    override fun getWidth(column: Int): Int = cellWidth

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
    private fun erweitereSpalten(zeile: Int, neueSpaltenAnzahl: Int) {
        val alteSpaltenAnzahl = data[0].size

        // ⛔️ Nur erweitern, wenn wirklich nötig
        if (neueSpaltenAnzahl <= alteSpaltenAnzahl) return

        val neueDaten = data.mapIndexed { rowIndex, row ->
            val differenz = neueSpaltenAnzahl - row.size
            if (differenz <= 0) return@mapIndexed row

            val zusatz = Array(differenz) { i ->
                when (rowIndex) {
                    0 -> ((alteSpaltenAnzahl + i) - 2).toString() // Spaltennummer: 1, 2, ...
                    zeile -> " " // Aktive Zeile → leer editierbar
                    else -> "-" // Rest → gesperrt
                }
            }
            row + zusatz
        }.toTypedArray()

        // 💾 Daten aktualisieren
        if (context is MainActivity) {
            context.tableData = neueDaten
            context.reloadTable()
            aktualisiereDaten(neueDaten)
        }
    }
    private fun aktualisiereDaten(neueDaten: Array<Array<String>>) {
        data = neueDaten
        notifyDataSetChanged()
    }
    fun getData(): Array<Array<String>> = data
    fun getMelderTypMatrix(): Array<Array<String>> = melderTypMatrix

}
