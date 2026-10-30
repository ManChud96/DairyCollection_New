package com.example.dairy

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import java.util.Locale

object Sms {
    fun text(r: Record): String = String.format(
        Locale.US,
        "%s\nDate: %s (%s)\nCustomer: %s\nLiter: %.2f  Fat: %.1f  SNF: %.1f\nRate: %.2f\nTotal: %.2f\nThank you",
        Store.SHOP_NAME, r.date, r.session, r.name, r.liter, r.fat, r.snf, r.rate, r.total
    )

    // Sends through the phone's default SMS SIM (uses the SIM's own SMS pack)
    fun send(a: Activity, mobile: String, msg: String): Boolean {
        try {
            @Suppress("DEPRECATION")
            val sm: SmsManager = if (Build.VERSION.SDK_INT >= 31) a.getSystemService(SmsManager::class.java)
            else SmsManager.getDefault()
            sm.sendMultipartTextMessage(mobile, null, sm.divideMessage(msg), null, null)
            Toast.makeText(a, "SMS sent to $mobile", Toast.LENGTH_LONG).show()
            return true
        } catch (e: Exception) {
            Toast.makeText(a, "SMS failed: ${e.message}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    // Fallback: open the phone's Messages app with the text ready (no SMS permission needed)
    fun openComposer(a: Activity, mobile: String, msg: String) {
        try {
            a.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$mobile")).putExtra("sms_body", msg)
            )
        } catch (e: Exception) {
            Toast.makeText(a, "No messaging app found", Toast.LENGTH_LONG).show()
        }
    }
}
