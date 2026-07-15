package com.akayami.smsforwarder

import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LogActivity : AppCompatActivity() {

    private lateinit var adapter: LogAdapter
    private var logs: MutableList<LogEntry> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewLog)
        val btnClear: Button = findViewById(R.id.btnClearLog)

        recyclerView.layoutManager = LinearLayoutManager(this)
        
        loadLogs()
        adapter = LogAdapter(logs)
        recyclerView.adapter = adapter

        btnClear.setOnClickListener {
            clearLogs()
            loadLogs()
            adapter = LogAdapter(logs)
            recyclerView.adapter = adapter
        }
    }

    private fun loadLogs() {
        val sharedPref = getSharedPreferences("SMSForwarderPrefs", Context.MODE_PRIVATE)
        val logsJson = sharedPref.getString("activity_logs", "[]")
        val gson = Gson()
        val type = object : TypeToken<MutableList<LogEntry>>() {}.type
        logs = gson.fromJson(logsJson, type) ?: mutableListOf()
        logs.reverse() // Show newest first
    }

    private fun clearLogs() {
        val sharedPref = getSharedPreferences("SMSForwarderPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("activity_logs", "[]").apply()
        logs.clear()
    }
}
