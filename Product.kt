package com.shopizzo.data.model

import com.google.firebase.Timestamp

/**
 * Represents a single product stored in Firestore under the 'products' collection.
 */
data class Product(
    val id                 : String  = "",
    val name               : String  = "",
    val description        : String  = "",
    val category           : String  = "",
    val price              : Double  = 0.0,
    val oldPrice           : Double? = null,
    val imageUrl           : String  = "",
    val additionalImages   : List<String> = emptyList(),
    val productVideoUrl    : String? = null,
    val stockQuantity      : Int     = 0,
    val rating             : Double  = 0.0,
    val numberOfReviews    : Int     = 0,
    val featured           : Boolean = false,
    val discountPercentage : Int?    = null,
    val brand              : String  = "",
    val specifications     : Map<String, String> = emptyMap(),
    val createdAt          : Timestamp? = null
)
