package com.akayami.smsforwarder

data class ForwardingRule(
    val sourcePattern: String, // Regex or plain number
    val destinationNumbers: List<String>
)
