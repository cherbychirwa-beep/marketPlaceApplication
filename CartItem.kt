package com.shopizzo.data.model

/**
 * Represents one line-item inside the user's shopping cart.
 */
data class CartItem(
    val product  : Product = Product(),
    val quantity : Int     = 1
) {
    val lineTotal: Double get() = product.price * quantity
}
