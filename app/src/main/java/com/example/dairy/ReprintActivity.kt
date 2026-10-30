package com.example.dairy

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged

class ReprintActivity : BtActivity() {
    private lateinit var etDate: EditText
    private lateinit var tvInfo: TextView
    private lateinit var lv: ListView
    private var shown = listOf<Record>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reprint)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Reprint Receipt"
        etDate = findViewById(R.id.etDate)
        tvInfo = findViewById(R.id.tvInfo)
        lv = findViewById(R.id.lvEntries)

        etDate.setOnClickListener { Store.pickDate(this, etDate) }
        etDate.doAfterTextChanged { refresh() }
        etDate.setText(Store.today())

        lv.setOnItemClickListener { _, _, pos, _ ->
            val r = shown[pos]
            AlertDialog.Builder(this)
                .setMessage("Print receipt for ${r.name} (${r.session}, ${r.date})?")
                .setPositiveButton("Print") { _, _ ->
                    withBt { Printer.printOrChoose(this, Printer.receipt(r, reprint = true)) }
                }
                .setNegativeButton("Cancel", null).show()
        }
    }

    private fun refresh() {
        shown = Store.sorted(Store.loadRecords(this).filter { it.date == etDate.text.toString() })
        tvInfo.text = if (shown.isEmpty()) "No entries on this date" else "${shown.size} entries - tap one to reprint"
        lv.adapter = ArrayAdapter(this, R.layout.item_row, R.id.text1, shown.map { Store.listLine(it) })
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
