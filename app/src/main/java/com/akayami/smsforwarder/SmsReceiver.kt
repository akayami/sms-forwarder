package com.akayami.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "========================")
        Log.d("SmsReceiver", "onReceive triggered!")
        Log.d("SmsReceiver", "Intent action: ${intent.action}")
        Log.d("SmsReceiver", "========================")
        
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            Log.d("SmsReceiver", "SMS_RECEIVED_ACTION matched! Messages count: ${messages?.size ?: 0}")
            val sharedPref = context.getSharedPreferences("SMSForwarderPrefs", Context.MODE_PRIVATE)
            val rulesJson = sharedPref.getString("forwarding_rules", "[]")
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<ForwardingRule>>() {}.type
            val rules: List<ForwardingRule> = gson.fromJson(rulesJson, type) ?: emptyList()

            if (rules.isEmpty()) {
                Log.w("SmsReceiver", "No forwarding rules found.")
                return
            }

            for (sms in messages) {
                val sender = sms.displayOriginatingAddress
                val messageBody = sms.displayMessageBody
                val forwardedMessage = "Fwd from $sender: $messageBody"
                val messagePreview = if (messageBody.length > 50) "${messageBody.substring(0, 50)}..." else messageBody
                
                Log.d("SmsReceiver", "Processing SMS from $sender")
                var matched = false

                for (rule in rules) {
                    try {
                        // 1. Try Regex match on original sender string
                        val regex = rule.sourcePattern.toRegex()
                        var isMatch = regex.containsMatchIn(sender)

                        // 2. If no match, try normalized match (ignoring formatting)
                        if (!isMatch) {
                            val normalizedSender = sender.filter { it.isDigit() }
                            val normalizedPattern = rule.sourcePattern.filter { it.isDigit() }
                            if (normalizedPattern.isNotEmpty() && normalizedSender.contains(normalizedPattern)) {
                                isMatch = true
                                Log.d("SmsReceiver", "Normalized match found: $normalizedPattern in $normalizedSender")
                            }
                        }

                        if (isMatch) {
                             matched = true
                             Log.d("SmsReceiver", "Match found for rule: ${rule.sourcePattern}")
                             val smsManager = context.getSystemService(SmsManager::class.java)
                             for (dest in rule.destinationNumbers) {
                                 Log.d("SmsReceiver", "Forwarding to $dest")
                                 try {
                                     // Split long messages into multiple parts
                                     val parts = smsManager.divideMessage(forwardedMessage)
                                     if (parts.size == 1) {
                                         // Single SMS
                                         smsManager.sendTextMessage(dest, null, forwardedMessage, null, null)
                                     } else {
                                         // Multi-part SMS
                                         smsManager.sendMultipartTextMessage(dest, null, parts, null, null)
                                     }
                                     addLog(context, sender, messagePreview, "FORWARDED to $dest")
                                 } catch (e: Exception) {
                                     Log.e("SmsReceiver", "Failed to forward to $dest", e)
                                     addLog(context, sender, messagePreview, "ERROR: ${e.message}")
                                 }
                             }
                        }
                    } catch (e: Exception) {
                        Log.e("SmsReceiver", "Error processing rule: ${rule.sourcePattern}", e)
                        addLog(context, sender, messagePreview, "ERROR: ${e.message}")
                    }
                }
                
                if (!matched) {
                    Log.d("SmsReceiver", "No matching rule for $sender")
                    addLog(context, sender, messagePreview, "NO_MATCH")
                }
            }
        }
    }

    private fun addLog(context: Context, sender: String, messagePreview: String, action: String) {
        val sharedPref = context.getSharedPreferences("SMSForwarderPrefs", Context.MODE_PRIVATE)
        val logsJson = sharedPref.getString("activity_logs", "[]")
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<MutableList<LogEntry>>() {}.type
        val logs: MutableList<LogEntry> = gson.fromJson(logsJson, type) ?: mutableListOf()
        
        // Add new log entry
        logs.add(LogEntry(System.currentTimeMillis(), sender, messagePreview, action))
        
        // Keep only last 100 entries
        if (logs.size > 100) {
            logs.removeAt(0)
        }
        
        // Save back
        val newLogsJson = gson.toJson(logs)
        sharedPref.edit().putString("activity_logs", newLogsJson).apply()
    }
}
