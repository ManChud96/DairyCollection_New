package com.example.dairy

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class UploadActivity : AppCompatActivity() {
    private lateinit var tvInfo: TextView
    private lateinit var lv: ListView

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importFile(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Upload Data"
        tvInfo = findViewById(R.id.tvInfo)
        lv = findViewById(R.id.lvCustomers)
        findViewById<Button>(R.id.btnPick).setOnClickListener { pickFile.launch(arrayOf("text/plain", "text/*")) }
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("Delete the customer list? Saved collection entries are not deleted.")
                .setPositiveButton("Delete") { _, _ -> Store.clearCustomers(this); refresh() }
                .setNegativeButton("Cancel", null).show()
        }
        refresh()
    }

    private fun importFile(uri: Uri) {
        try {
            val lines = contentResolver.openInputStream(uri)!!.bufferedReader().readLines()
            val list = Store.parseCustomers(lines)
            if (list.isEmpty()) {
                Toast.makeText(this, "No valid lines found. Use: Name,CustomerId,Mobile", Toast.LENGTH_LONG).show()
                return
            }
            Store.saveCustomers(this, list)
            refresh()
            Toast.makeText(this, "${list.size} customers uploaded", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not read file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun refresh() {
        val list = Store.loadCustomers(this)
        tvInfo.text = if (list.isEmpty()) "No customers uploaded yet" else "${list.size} customers loaded"
        lv.adapter = ArrayAdapter(this, R.layout.item_row, R.id.text1, list.map { "${it.name}  (${it.id})" + if (it.mobile.isNotEmpty()) "  ${it.mobile}" else "" })
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
