package com.example.dairy

import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.widget.EditText
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class Customer(val name: String, val id: String, val mobile: String = "")

data class Record(
    val date: String, val session: String, val id: String, val name: String,
    val liter: Double, val fat: Double, val snf: Double, val degree: Double,
    val rate: Double, val total: Double
) {
    fun toLine() = listOf(
        date, session, id, name, liter, fat, snf, degree, rate,
        String.format(Locale.US, "%.2f", total)
    ).joinToString(",")
}

object Store {
    // Change these two lines to change the login
    const val USER = "admin"
    const val PASS = "1234"
    const val SHOP_NAME = "DAIRY COLLECTION"   // printed on the receipt

    const val HEADER = "Date,Session,CustomerId,Name,Liter,Fat,SNF,Degree,Rate,TotalAmount"
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun today(): String = dateFmt.format(Date())
    fun monthStart(): String {
        val c = Calendar.getInstance()
        c.set(Calendar.DAY_OF_MONTH, 1)
        return dateFmt.format(c.time)
    }

    // ---------- login ----------
    private fun prefs(c: Context) = c.getSharedPreferences("dairy", Context.MODE_PRIVATE)
    fun isLoggedIn(c: Context) = prefs(c).getBoolean("in", false)
    fun setLoggedIn(c: Context, v: Boolean) = prefs(c).edit().putBoolean("in", v).apply()

    // ---------- customers ----------
    private fun customersFile(c: Context) = File(c.filesDir, "customers.txt")

    // each line: Name,CustomerId,Mobile   (comma, | or tab separated; Mobile is optional)
    fun parseCustomers(lines: List<String>): List<Customer> = lines.mapNotNull { line ->
        val p = line.split(",", "|", "\t").map { it.trim() }
        if (p.size >= 2 && p[0].isNotEmpty() && p[1].isNotEmpty())
            Customer(p[0], p[1], p.getOrNull(2)?.filter { it.isDigit() || it == '+' } ?: "")
        else null
    }

    fun saveCustomers(c: Context, list: List<Customer>) {
        customersFile(c).writeText(list.joinToString("\n") { "${it.name},${it.id},${it.mobile}" })
    }

    fun loadCustomers(c: Context): List<Customer> =
        if (customersFile(c).exists()) parseCustomers(customersFile(c).readLines()) else emptyList()

    fun clearCustomers(c: Context) { customersFile(c).delete() }

    // ---------- records ----------
    private fun recordsFile(c: Context) = File(c.filesDir, "collection.txt")

    private fun parseRecord(line: String): Record? {
        val p = line.split(",")
        if (p.size < 10) return null
        fun d(i: Int) = p[i].toDoubleOrNull() ?: 0.0
        return Record(p[0], p[1], p[2], p[3], d(4), d(5), d(6), d(7), d(8), d(9))
    }

    fun loadRecords(c: Context): List<Record> =
        if (recordsFile(c).exists()) recordsFile(c).readLines().mapNotNull { parseRecord(it) } else emptyList()

    fun saveRecords(c: Context, list: List<Record>) {
        recordsFile(c).writeText(list.joinToString("\n") { it.toLine() } + if (list.isEmpty()) "" else "\n")
    }

    fun sorted(list: List<Record>) = list.sortedWith(compareBy({ it.date }, { it.session != "Morning" }, { it.id }))

    fun avg(list: List<Record>, sel: (Record) -> Double): Double {
        val t = list.sumOf { it.liter }
        return if (t == 0.0) 0.0 else list.sumOf { sel(it) * it.liter } / t
    }

    fun listLine(r: Record) = String.format(
        Locale.US, "%s | %s (%s) | %.2f L | %.2f",
        if (r.session == "Morning") "M" else "E", r.name, r.id, r.liter, r.total
    )

    // ---------- export to Downloads ----------
    fun exportToDownloads(c: Context, list: List<Record>): String {
        val name = "dairy_collection_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) + ".txt"
        val content = (listOf(HEADER) + sorted(list).map { it.toLine() }).joinToString("\n")
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download")
        }
        val uri = c.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
        c.contentResolver.openOutputStream(uri)!!.use { it.write(content.toByteArray()) }
        return name
    }

    // ---------- date picker ----------
    fun pickDate(c: Context, et: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(c, { _, y, m, d ->
            et.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }
}
