package com.shopizzo.data.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val productId: String? = null,
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false
)
