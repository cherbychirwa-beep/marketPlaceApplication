package com.shopizzo.data.model

import com.google.firebase.Timestamp

data class Order(
    val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val phoneNumber: String = "",
    val products: List<OrderProduct> = emptyList(),
    val totalAmount: Double = 0.0,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val discount: Double = 0.0,
    val paymentMethod: String = "",
    val paymentStatus: String = "Pending", // Pending, Paid, Failed, Refunded
    val deliveryAddress: String = "",
    val deliveryNotes: String = "",
    val orderStatus: String = "Pending", // Pending, Confirmed, Processing, Packed, Shipped, Out for Delivery, Delivered, Cancelled, Refunded
    val orderDate: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now(),
    val estimatedDeliveryDate: Timestamp? = null,
    val trackingNumber: String = "",
    val courier: String = "",
    val transactionReference: String = ""
)

data class OrderProduct(
    val productId: String = "",
    val name: String = "",
    val quantity: Int = 0,
    val price: Double = 0.0,
    val imageUrl: String = ""
)

data class PaymentRecord(
    val paymentId: String = "",
    val orderId: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val paymentMethod: String = "",
    val transactionReference: String = "",
    val paymentStatus: String = "Pending",
    val timestamp: Timestamp = Timestamp.now()
)
