package com.akayami.smsforwarder

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class LogAdapter(private val logs: List<LogEntry>) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTimestamp: TextView = view.findViewById(R.id.textTimestamp)
        val textSender: TextView = view.findViewById(R.id.textSender)
        val textMessage: TextView = view.findViewById(R.id.textMessage)
        val textAction: TextView = view.findViewById(R.id.textAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        holder.textTimestamp.text = dateFormat.format(Date(log.timestamp))
        holder.textSender.text = "From: ${log.sender}"
        holder.textMessage.text = log.messagePreview
        holder.textAction.text = log.action
        
        // Color code based on action
        when (log.action) {
            "FORWARDED" -> holder.textAction.setTextColor(Color.parseColor("#00AA00"))
            "NO_MATCH" -> holder.textAction.setTextColor(Color.parseColor("#FF6600"))
            "ERROR" -> holder.textAction.setTextColor(Color.parseColor("#FF0000"))
            else -> holder.textAction.setTextColor(Color.parseColor("#666666"))
        }
    }

    override fun getItemCount() = logs.size
}
