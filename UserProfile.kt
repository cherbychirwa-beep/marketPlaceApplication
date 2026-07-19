package com.shopizzo.data.model

data class UserProfile(
    val uid           : String  = "",
    val fullName      : String  = "",
    val email         : String  = "",
    val phoneNumber   : String  = "",
    val country       : String  = "",
    val profileImage  : String  = "",
    val paymentPin    : String? = null,
    val preferredAuthMethod: String = "PIN", // PIN, BIOMETRIC
    val notificationsEnabled: Boolean = true,
    val favouriteIds  : List<String> = emptyList(),
    val purchasedProductIds: List<String> = emptyList(),
    val createdAt     : com.google.firebase.Timestamp? = null
)
