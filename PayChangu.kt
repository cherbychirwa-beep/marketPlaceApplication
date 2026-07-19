package com.shopizzo.data.model

/**
 * ─── PayChangu Payment Gateway Models ────────────────────────────────────────
 * These data classes map to the PayChangu Sandbox REST API request/response.
 * Docs: https://paychangu.readme.io/reference
 */

/** Request body sent to POST /payment */
data class PayChanguRequest(
    val amount          : String,          // e.g. "5000"
    val currency        : String = "MWK",  // Malawian Kwacha by default
    val email           : String,
    val first_name      : String,
    val last_name       : String,
    val callback_url    : String = "https://shopizzo.app/payment/callback",
    val return_url      : String = "https://shopizzo.app/payment/return",
    val tx_ref          : String,          // unique transaction reference
    val customization   : PayChanguCustomization = PayChanguCustomization()
)

/** Branding applied to the PayChangu checkout page */
data class PayChanguCustomization(
    val title       : String = "Shopizzo Checkout",
    val description : String = "Secure payment for your electronic products",
    val logo        : String = ""
)

/** Top-level response from PayChangu */
data class PayChanguResponse(
    val status  : String = "",
    val message : String = "",
    val data    : PayChanguData? = null
)

/** Nested data object inside the response */
data class PayChanguData(
    val checkout_url: String = "",  // redirect user here to complete payment
    val tx_ref      : String = ""
)
