package com.example.dairy

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import java.util.Locale

class ReportActivity : BtActivity() {
    private lateinit var etDate: EditText
    private lateinit var tvSummary: TextView
    private lateinit var lv: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Daily Collection Report"
        etDate = findViewById(R.id.etDate)
        tvSummary = findViewById(R.id.tvSummary)
        lv = findViewById(R.id.lvEntries)

        etDate.setOnClickListener { Store.pickDate(this, etDate) }
        etDate.doAfterTextChanged { refresh() }
        etDate.setText(Store.today())

        findViewById<Button>(R.id.btnPrintReport).setOnClickListener {
            val l = entries()
            if (l.isEmpty()) {
                Toast.makeText(this, "No entries on this date", Toast.LENGTH_SHORT).show()
            } else {
                withBt { Printer.printOrChoose(this, Printer.report(etDate.text.toString(), l)) }
            }
        }
    }

    private fun entries() = Store.sorted(Store.loadRecords(this).filter { it.date == etDate.text.toString() })

    private fun refresh() {
        val l = entries()
        val m = l.filter { it.session == "Morning" }
        val e = l.filter { it.session == "Evening" }
        tvSummary.text = String.format(
            Locale.US,
            "Entries: %d\nTotal: %.2f L, %.2f\nMorning: %.2f L, %.2f\nEvening: %.2f L, %.2f\nAvg Fat: %.2f   Avg SNF: %.2f",
            l.size, l.sumOf { it.liter }, l.sumOf { it.total },
            m.sumOf { it.liter }, m.sumOf { it.total },
            e.sumOf { it.liter }, e.sumOf { it.total },
            Store.avg(l) { it.fat }, Store.avg(l) { it.snf }
        )
        lv.adapter = ArrayAdapter(this, R.layout.item_row, R.id.text1, l.map { Store.listLine(it) })
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
