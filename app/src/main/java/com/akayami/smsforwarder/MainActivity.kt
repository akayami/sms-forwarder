package com.akayami.smsforwarder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: RuleAdapter
    private var rules: MutableList<ForwardingRule> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewRules)
        val fab: FloatingActionButton = findViewById(R.id.fabAddRule)
        val btnRequest: Button = findViewById(R.id.btnRequestPermissions)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        loadRules()
        adapter = RuleAdapter(rules) { rule ->
            rules.remove(rule)
            saveRules()
            adapter.notifyDataSetChanged()
        }
        recyclerView.adapter = adapter

        fab.setOnClickListener {
            showAddRuleDialog()
        }

        btnRequest.setOnClickListener {
            checkAndRequestPermissions()
        }

        val btnViewLog: Button = findViewById(R.id.btnViewLog)
        btnViewLog.setOnClickListener {
            val intent = android.content.Intent(this, LogActivity::class.java)
            startActivity(intent)
        }
        
        // Add manual log entries for sent SMS (as a workaround for unreliable BroadcastReceiver on Android 14+)
        btnViewLog.setOnLongClickListener {
            checkSentMessages()
            android.widget.Toast.makeText(this, "Checked sent messages and updated log", android.widget.Toast.LENGTH_SHORT).show()
            true
        }

        checkAndRequestPermissions()
    }
    
    private fun checkSentMessages() {
        // Check for recently sent SMS from this app and log them
        try {
            val cursor = contentResolver.query(
                android.provider.Telephony.Sms.Sent.CONTENT_URI,
                arrayOf(
                    android.provider.Telephony.Sms._ID,
                    android.provider.Telephony.Sms.ADDRESS,
                    android.provider.Telephony.Sms.BODY,
                    android.provider.Telephony.Sms.DATE
                ),
                null, null,
                "${android.provider.Telephony.Sms.DATE} DESC LIMIT 10"
            )
            
            cursor?.use {
                val sharedPref = getSharedPreferences("SMSForwarderPrefs", android.content.Context.MODE_PRIVATE)
                val lastChecked = sharedPref.getLong("last_sent_check", 0)
                var newEntries = 0
                
                while (it.moveToNext()) {
                    val date = it.getLong(it.getColumnIndexOrThrow(android.provider.Telephony.Sms.DATE))
                    if (date <= lastChecked) break
                    
                    val address = it.getString(it.getColumnIndexOrThrow(android.provider.Telephony.Sms.ADDRESS)) ?: "Unknown"
                    val body = it.getString(it.getColumnIndexOrThrow(android.provider.Telephony.Sms.BODY)) ?: ""
                    
                    // Check if it's a forwarded message
                    if (body.startsWith("Fwd from ")) {
                        val preview = if (body.length > 50) "${body.substring(0, 50)}..." else body
                        addManualLog("SENT", preview, "FORWARDED to $address")
                        newEntries++
                    }
                }
                
                // Update last checked timestamp
                sharedPref.edit().putLong("last_sent_check", System.currentTimeMillis()).apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error checking sent messages", e)
        }
    }
    
    private fun addManualLog(sender: String, messagePreview: String, action: String) {
        val sharedPref = getSharedPreferences("SMSForwarderPrefs", android.content.Context.MODE_PRIVATE)
        val logsJson = sharedPref.getString("activity_logs", "[]")
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<MutableList<LogEntry>>() {}.type
        val logs: MutableList<LogEntry> = gson.fromJson(logsJson, type) ?: mutableListOf()
        
        logs.add(LogEntry(System.currentTimeMillis(), sender, messagePreview, action))
        
        if (logs.size > 100) {
            logs.removeAt(0)
        }
        
        val newLogsJson = gson.toJson(logs)
        sharedPref.edit().putString("activity_logs", newLogsJson).apply()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun loadRules() {
        val sharedPref = getSharedPreferences("SMSForwarderPrefs", Context.MODE_PRIVATE)
        val rulesJson = sharedPref.getString("forwarding_rules", "[]")
        val gson = Gson()
        val type = object : TypeToken<MutableList<ForwardingRule>>() {}.type
        rules = gson.fromJson(rulesJson, type) ?: mutableListOf()
    }

    private fun saveRules() {
        val sharedPref = getSharedPreferences("SMSForwarderPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val rulesJson = gson.toJson(rules)
        sharedPref.edit().putString("forwarding_rules", rulesJson).apply()
    }

    private fun showAddRuleDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_rule, null)
        val editSource = dialogView.findViewById<EditText>(R.id.editTextSource)
        val editDestinations = dialogView.findViewById<EditText>(R.id.editTextDestinations)

        AlertDialog.Builder(this)
            .setTitle("Add Forwarding Rule")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val source = editSource.text.toString()
                val dests = editDestinations.text.toString()
                
                if (source.isNotEmpty() && dests.isNotEmpty()) {
                    val destList = dests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    rules.add(ForwardingRule(source, destList))
                    saveRules()
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePermissionStatus() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS, 
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        val statusText = findViewById<TextView>(R.id.textPermissionStatus)
        val btnRequest = findViewById<Button>(R.id.btnRequestPermissions)

        if (missing.isEmpty()) {
            statusText.text = "Permissions: Granted"
            statusText.setTextColor(Color.GREEN)
            btnRequest.visibility = View.GONE
        } else {
            statusText.text = "Permissions: Missing (${missing.size})"
            statusText.setTextColor(Color.RED)
            btnRequest.visibility = View.VISIBLE
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 100)
        }
        updatePermissionStatus()
    }
}
