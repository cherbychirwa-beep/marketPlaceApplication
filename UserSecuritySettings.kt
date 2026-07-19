package com.shopizzo.data.model

import com.google.firebase.Timestamp

data class UserSecuritySettings(
    val userId: String = "",
    val isBiometricEnabled: Boolean = false,
    val twoFactorEnabled: Boolean = false,
    val lastLogin: Timestamp? = null,
    val securityLevel: String = "Normal"
)
