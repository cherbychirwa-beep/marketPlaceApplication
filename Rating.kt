package com.shopizzo.data.model

import com.google.firebase.Timestamp

data class Rating(
    val ratingId: String = "",
    val productId: String = "",
    val userId: String = "",
    val ratingValue: Int = 0,
    val timestamp: Timestamp? = null
)
