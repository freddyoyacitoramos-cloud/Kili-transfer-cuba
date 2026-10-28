package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import com.example.data.local.AppDatabase
import com.example.data.repository.BankRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.originatingAddress ?: ""
                val body = sms.messageBody ?: ""

                val upperBody = body.uppercase()
                // Check if message comes from Cuban banks or contains banking keywords
                if (upperBody.contains("BPA") || upperBody.contains("BANDEC") ||
                    upperBody.contains("BANMET") || upperBody.contains("PAGOX") ||
                    upperBody.contains("SALDO") || upperBody.contains("TARJETA")) {

                    Log.d("BankSmsReceiver", "Intercepted Bank SMS from $sender: $body")

                    val db = AppDatabase.getDatabase(context)
                    val repository = BankRepository(
                        db.bankCardDao(),
                        db.transactionDao(),
                        db.utilityDao()
                    )

                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = repository.processIncomingSms(body, sender)
                            if (result.success) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(
                                        context,
                                        "Transfermóvil Pro: Saldo actualizado (${result.cardLast4 ?: "Tarjeta"} -> $${result.remainingBalance ?: 0.0} ${result.currency.code})",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("BankSmsReceiver", "Error processing SMS", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
