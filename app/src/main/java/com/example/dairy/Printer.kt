package com.example.dairy

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID

@SuppressLint("MissingPermission")
object Printer {
    private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val W = 32 // characters per line: 32 for 58mm paper, 48 for 80mm paper

    private fun prefs(c: Context) = c.getSharedPreferences("dairy", Context.MODE_PRIVATE)
    fun savedName(c: Context): String? = prefs(c).getString("printer_name", null)
    private fun savedAddress(c: Context): String? = prefs(c).getString("printer_addr", null)

    private fun adapter(c: Context): BluetoothAdapter? =
        (c.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    // ---------- choose printer from paired devices ----------
    fun choose(a: Activity, onDone: () -> Unit = {}) {
        val ad = adapter(a)
        if (ad == null || !ad.isEnabled) {
            Toast.makeText(a, "Turn on Bluetooth first", Toast.LENGTH_LONG).show(); return
        }
        val devices: List<BluetoothDevice> = ad.bondedDevices.toList()
        if (devices.isEmpty()) {
            Toast.makeText(a, "No paired devices. Pair the printer in phone Bluetooth settings first.", Toast.LENGTH_LONG).show()
            return
        }
        val labels = devices.map { "${it.name ?: "Unknown"}\n${it.address}" }.toTypedArray()
        AlertDialog.Builder(a)
            .setTitle("Select Printer")
            .setItems(labels) { _, i ->
                prefs(a).edit().putString("printer_name", devices[i].name ?: devices[i].address)
                    .putString("printer_addr", devices[i].address).apply()
                onDone()
            }
            .setNegativeButton("Cancel", null).show()
    }

    // ---------- print ----------
    fun printOrChoose(a: Activity, data: ByteArray) {
        if (savedAddress(a) == null) choose(a) { print(a, data) } else print(a, data)
    }

    private fun print(a: Activity, data: ByteArray) {
        val addr = savedAddress(a) ?: return
        Toast.makeText(a, "Printing...", Toast.LENGTH_SHORT).show()
        Thread {
            var err: String? = null
            var sock: BluetoothSocket? = null
            try {
                val ad = adapter(a)
                if (ad == null || !ad.isEnabled) throw Exception("Bluetooth is off")
                ad.cancelDiscovery()
                sock = ad.getRemoteDevice(addr).createRfcommSocketToServiceRecord(SPP)
                sock.connect()
                sock.outputStream.write(data)
                sock.outputStream.flush()
                Thread.sleep(1500) // let the printer finish before closing
            } catch (e: Exception) {
                err = e.message ?: "Print failed"
            } finally {
                try { sock?.close() } catch (_: Exception) {}
            }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(a, err?.let { "Print failed: $it" } ?: "Receipt printed", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    // ---------- receipt (ESC/POS) ----------
    fun receipt(r: Record, reprint: Boolean = false): ByteArray {
        val out = ByteArrayOutputStream()
        fun raw(vararg b: Int) = out.write(b.map { it.toByte() }.toByteArray())
        fun text(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))
        fun row(label: String, v: String) = text(String.format(Locale.US, "%-8s: %s\n", label, v))
        val sep = "-".repeat(W) + "\n"

        raw(0x1B, 0x40)                       // initialise
        raw(0x1B, 0x61, 1)                    // center
        raw(0x1B, 0x45, 1); raw(0x1D, 0x21, 0x01) // bold + double height
        text(Store.SHOP_NAME + "\n")
        raw(0x1D, 0x21, 0x00); raw(0x1B, 0x45, 0)
        text("MILK COLLECTION RECEIPT\n")
        if (reprint) text("*** DUPLICATE COPY ***\n")
        raw(0x1B, 0x61, 0)                    // left
        text(sep)
        row("Date", r.date)
        row("Shift", r.session)
        row("Cust", "${r.name} (${r.id})")
        text(sep)
        row("Liter", String.format(Locale.US, "%.2f", r.liter))
        row("Fat", String.format(Locale.US, "%.1f", r.fat))
        row("SNF", String.format(Locale.US, "%.1f", r.snf))
        row("Degree", String.format(Locale.US, "%.1f", r.degree))
        row("Rate", String.format(Locale.US, "%.2f", r.rate))
        text(sep)
        raw(0x1B, 0x45, 1)
        row("TOTAL", String.format(Locale.US, "%.2f", r.total))
        raw(0x1B, 0x45, 0)
        text(sep)
        raw(0x1B, 0x61, 1)
        text("Thank you\n")
        raw(0x1B, 0x64, 4)                    // feed 4 lines
        return out.toByteArray()
    }

    // ---------- daily collection report (ESC/POS) ----------
    fun report(date: String, list: List<Record>): ByteArray {
        val out = ByteArrayOutputStream()
        fun raw(vararg b: Int) = out.write(b.map { it.toByte() }.toByteArray())
        fun text(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))
        fun f(fmt: String, vararg a: Any) = String.format(Locale.US, fmt, *a)
        val sep = "-".repeat(W) + "\n"

        raw(0x1B, 0x40)
        raw(0x1B, 0x61, 1)
        raw(0x1B, 0x45, 1); raw(0x1D, 0x21, 0x01)
        text(Store.SHOP_NAME + "\n")
        raw(0x1D, 0x21, 0x00); raw(0x1B, 0x45, 0)
        text("DAILY COLLECTION REPORT\n")
        raw(0x1B, 0x61, 0)
        text(sep)
        text("Date: $date\n")
        text(sep)
        text(f("%-12s %s %6s %8s\n", "Name", "S", "Liter", "Amount"))
        text(sep)
        list.sortedWith(compareBy({ it.session != "Morning" }, { it.name })).forEach {
            text(f("%-12s %s %6.2f %8.2f\n", it.name.take(12), if (it.session == "Morning") "M" else "E", it.liter, it.total))
        }
        text(sep)
        val m = list.filter { it.session == "Morning" }
        val e = list.filter { it.session == "Evening" }
        text(f("%-8s%8.2f L%9.2f\n", "Morning", m.sumOf { it.liter }, m.sumOf { it.total }))
        text(f("%-8s%8.2f L%9.2f\n", "Evening", e.sumOf { it.liter }, e.sumOf { it.total }))
        text(sep)
        raw(0x1B, 0x45, 1)
        text(f("%-8s%8.2f L%9.2f\n", "TOTAL", list.sumOf { it.liter }, list.sumOf { it.total }))
        raw(0x1B, 0x45, 0)
        text("Entries: ${list.size}\n")
        text(f("Avg Fat: %.2f  Avg SNF: %.2f\n", Store.avg(list) { it.fat }, Store.avg(list) { it.snf }))
        text(sep)
        raw(0x1B, 0x64, 4)
        return out.toByteArray()
    }
}
