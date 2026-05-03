package me.capcom.smsgateway.domain

enum class ProcessingState {
    Pending,
    Processing,
    Processed,
    Sent,
    Delivered,
    Failed
}