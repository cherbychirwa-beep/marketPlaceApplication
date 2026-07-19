package com.shopizzo.data.model

/**
 * Possible payment states shown to the user
 */
enum class PaymentStatus {
    IDLE,           // no payment in progress
    LOADING,        // API call in flight
    REDIRECTING,    // opening checkout URL in browser/WebView
    SUCCESS,        // payment confirmed
    FAILED          // payment failed or cancelled
}
