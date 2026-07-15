package com.akayami.smsforwarder

data class LogEntry(
    val timestamp: Long,
    val sender: String,
    val messagePreview: String,
    val action: String // "MATCHED", "NO_MATCH", "FORWARDED", "ERROR"
)
