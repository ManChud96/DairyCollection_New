package com.example.dairy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : BtActivity() {
    private lateinit var tvEntries: TextView
    private lateinit var tvLiters: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvInfoLine: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        tvEntries = findViewById(R.id.tvEntries)
        tvLiters = findViewById(R.id.tvLiters)
        tvAmount = findViewById(R.id.tvAmount)
        tvInfoLine = findViewById(R.id.tvInfoLine)
        findViewById<TextView>(R.id.tvDate).text =
            SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date())

        tile(R.id.tileUpload, "📤", "Upload Data", "Customer file") { go(UploadActivity::class.java) }
        tile(R.id.tileEntry, "🥛", "Collection Entry", "Add milk entry") { go(EntryActivity::class.java) }
        tile(R.id.tileExport, "📁", "Export Data", "Download text file") { go(ExportActivity::class.java) }
        tile(R.id.tileReport, "📊", "Daily Report", "Totals and print") { go(ReportActivity::class.java) }
        tile(R.id.tileReprint, "🧾", "Reprint Receipt", "Duplicate copy") { go(ReprintActivity::class.java) }
        tile(R.id.tilePrinter, "🖨️", "Printer", "Select Bluetooth") {
            withBt { Printer.choose(this) { refresh() } }
        }

        findViewById<View>(R.id.btnLogout).setOnClickListener {
            Store.setLoggedIn(this, false)
            startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }

    private fun tile(id: Int, icon: String, title: String, sub: String, onClick: () -> Unit) {
        val v = findViewById<View>(id)
        v.findViewById<TextView>(R.id.tvIcon).text = icon
        v.findViewById<TextView>(R.id.tvTitle).text = title
        v.findViewById<TextView>(R.id.tvSub).text = sub
        v.setOnClickListener { onClick() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val today = Store.loadRecords(this).filter { it.date == Store.today() }
        tvEntries.text = today.size.toString()
        tvLiters.text = String.format(Locale.US, "%.1f", today.sumOf { it.liter })
        tvAmount.text = String.format(Locale.US, "%.2f", today.sumOf { it.total })
        tvInfoLine.text = "Customers: ${Store.loadCustomers(this).size}   |   Printer: ${Printer.savedName(this) ?: "not selected"}"
    }

    private fun go(c: Class<*>) = startActivity(Intent(this, c))
}
