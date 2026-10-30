package com.example.dairy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import java.util.Locale

class EntryActivity : BtActivity() {
    private lateinit var spCustomer: Spinner
    private lateinit var etDate: EditText
    private lateinit var rbMorning: RadioButton
    private lateinit var etLiter: EditText
    private lateinit var etFat: EditText
    private lateinit var etSnf: EditText
    private lateinit var etDegree: EditText
    private lateinit var etRate: EditText
    private lateinit var tvTotal: TextView
    private lateinit var tvCount: TextView
    private var customers = listOf<Customer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Collection Entry"

        spCustomer = findViewById(R.id.spCustomer)
        etDate = findViewById(R.id.etDate)
        rbMorning = findViewById(R.id.rbMorning)
        etLiter = findViewById(R.id.etLiter)
        etFat = findViewById(R.id.etFat)
        etSnf = findViewById(R.id.etSnf)
        etDegree = findViewById(R.id.etDegree)
        etRate = findViewById(R.id.etRate)
        tvTotal = findViewById(R.id.tvTotal)
        tvCount = findViewById(R.id.tvCount)

        customers = Store.loadCustomers(this)
        val labels = if (customers.isEmpty()) listOf("No customers - use Upload Data first")
        else customers.map { "${it.name} (${it.id})" }
        spCustomer.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)

        etDate.setText(Store.today())
        etDate.setOnClickListener { Store.pickDate(this, etDate); }
        etDate.doAfterTextChanged { updateCount() }
        etLiter.doAfterTextChanged { updateTotal() }
        etRate.doAfterTextChanged { updateTotal() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { save(false) }
        findViewById<Button>(R.id.btnSavePrint).setOnClickListener { save(true) }
        updateCount()
    }

    private fun num(e: EditText) = e.text.toString().trim().toDoubleOrNull() ?: 0.0

    private fun updateTotal() {
        tvTotal.text = String.format(Locale.US, "Total Amount: %.2f", num(etLiter) * num(etRate))
    }

    private fun updateCount() {
        val n = Store.loadRecords(this).count { it.date == etDate.text.toString() }
        tvCount.text = "Entries on this date: $n"
    }

    private fun save(print: Boolean) {
        if (customers.isEmpty()) return toast("Upload customer file first")
        if (etLiter.text.isBlank()) return toast("Enter liter")
        val c = customers[spCustomer.selectedItemPosition]
        val rec = Record(
            etDate.text.toString(), if (rbMorning.isChecked) "Morning" else "Evening",
            c.id, c.name, num(etLiter), num(etFat), num(etSnf), num(etDegree),
            num(etRate), num(etLiter) * num(etRate)
        )
        val all = Store.loadRecords(this).toMutableList()
        val dup = all.indexOfFirst { it.date == rec.date && it.session == rec.session && it.id == rec.id }
        if (dup >= 0) {
            AlertDialog.Builder(this)
                .setMessage("${c.name} already has a ${rec.session} entry on ${rec.date}. Replace it?")
                .setPositiveButton("Replace") { _, _ -> all[dup] = rec; commit(all, rec, print) }
                .setNegativeButton("Cancel", null).show()
        } else {
            all.add(rec); commit(all, rec, print)
        }
    }

    private fun commit(list: List<Record>, rec: Record, print: Boolean) {
        Store.saveRecords(this, list)
        toast("Saved")
        listOf(etLiter, etFat, etSnf, etDegree).forEach { it.text.clear() }
        updateCount()
        if (print) withBt { Printer.printOrChoose(this, Printer.receipt(rec)) }
        askSms(rec)
    }

    // Optional: ask whether to SMS the customer (only when the customer has a mobile number)
    private fun askSms(rec: Record) {
        val mobile = customers.firstOrNull { it.id == rec.id }?.mobile ?: ""
        if (mobile.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("Send SMS?")
            .setMessage("Send collection details to ${rec.name} ($mobile)?")
            .setPositiveButton("Send SMS") { _, _ ->
                val msg = Sms.text(rec)
                withSms({ smsBlocked(mobile, msg) }) {
                    if (!Sms.send(this, mobile, msg)) smsBlocked(mobile, msg)
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    // Android can block SMS permission for apps installed from an APK file
    private fun smsBlocked(mobile: String, msg: String) {
        AlertDialog.Builder(this)
            .setTitle("SMS is blocked")
            .setMessage("Android is not allowing this app to send SMS directly. You can open your Messages app with the text ready and press Send, or open App settings to allow it (menu at top right > Allow restricted settings, then Permissions > SMS > Allow).")
            .setPositiveButton("Open Messages") { _, _ -> Sms.openComposer(this, mobile, msg) }
            .setNeutralButton("App Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
