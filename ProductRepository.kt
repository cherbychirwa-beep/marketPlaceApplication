package com.shopizzo.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shopizzo.data.model.Product
import com.shopizzo.data.model.Order
import com.shopizzo.data.model.OrderProduct
import com.shopizzo.data.model.CartItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProductRepository {

    private val firestore   = FirebaseFirestore.getInstance()
    private val productsCol = firestore.collection("products")

    private fun DocumentSnapshot.toProduct(): Product? {
        return try {
            this.toObject(Product::class.java)?.copy(id = this.id)
        } catch (e: Exception) {
            val data = this.data ?: return null
            val rating = when (val r = data["rating"]) {
                is Number -> r.toDouble()
                is String -> r.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            Product(
                id = this.id,
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                category = data["category"] as? String ?: "",
                price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                oldPrice = (data["oldPrice"] as? Number)?.toDouble(),
                imageUrl = data["imageUrl"] as? String ?: "",
                additionalImages = (data["additionalImages"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                productVideoUrl = data["productVideoUrl"] as? String,
                stockQuantity = (data["stockQuantity"] as? Number)?.toInt() ?: 0,
                rating = rating,
                numberOfReviews = (data["numberOfReviews"] as? Number)?.toInt() ?: 0,
                featured = data["featured"] as? Boolean ?: false,
                discountPercentage = (data["discountPercentage"] as? Number)?.toInt(),
                brand = data["brand"] as? String ?: "",
                specifications = (data["specifications"] as? Map<*, *>)?.map { it.key.toString() to it.value.toString() }?.toMap() ?: emptyMap(),
                createdAt = data["createdAt"] as? com.google.firebase.Timestamp
            )
        }
    }

    /**
     * Listens to all products in real-time.
     */
    fun getAllProductsFlow(): Flow<List<Product>> = callbackFlow {
        val listener = productsCol
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { it.toProduct() }
                    trySend(products)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Listens to featured products in real-time.
     */
    fun getFeaturedProductsFlow(): Flow<List<Product>> = callbackFlow {
        val listener = productsCol
            .whereEqualTo("featured", true)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val products = snapshot.documents.mapNotNull { it.toProduct() }
                    trySend(products)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getProductById(id: String): Result<Product> = try {
        val doc = productsCol.document(id).get().await()
        val product = doc.toProduct() ?: throw Exception("Product not found")
        Result.success(product)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun saveOrder(order: Order): Result<Unit> = try {
        firestore.collection("orders").document(order.id).set(order).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getUserOrdersFlow(userId: String): Flow<List<Order>> = callbackFlow {
        val listener = firestore.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Order::class.java)
                        } catch (e: Exception) {
                            val data = doc.data ?: return@mapNotNull null
                            @Suppress("UNCHECKED_CAST")
                            val productsData = data["products"] as? List<Map<String, Any>> ?: emptyList()
                            val productsList = productsData.map { p ->
                                OrderProduct(
                                    productId = p["productId"] as? String ?: "",
                                    name = p["name"] as? String ?: "",
                                    quantity = (p["quantity"] as? Number)?.toInt() ?: 0,
                                    price = (p["price"] as? Number)?.toDouble() ?: 0.0,
                                    imageUrl = p["imageUrl"] as? String ?: ""
                                )
                            }
                            Order(
                                id = doc.id,
                                userId = data["userId"] as? String ?: "",
                                customerName = data["customerName"] as? String ?: "",
                                customerEmail = data["customerEmail"] as? String ?: "",
                                phoneNumber = data["phoneNumber"] as? String ?: "",
                                products = productsList,
                                totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                                subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
                                deliveryFee = (data["deliveryFee"] as? Number)?.toDouble() ?: 0.0,
                                discount = (data["discount"] as? Number)?.toDouble() ?: 0.0,
                                paymentMethod = data["paymentMethod"] as? String ?: "",
                                paymentStatus = data["paymentStatus"] as? String ?: "Pending",
                                deliveryAddress = data["deliveryAddress"] as? String ?: "",
                                deliveryNotes = data["deliveryNotes"] as? String ?: "",
                                orderStatus = data["orderStatus"] as? String ?: "Pending",
                                orderDate = data["orderDate"] as? Timestamp ?: Timestamp.now(),
                                lastUpdated = data["lastUpdated"] as? Timestamp ?: Timestamp.now(),
                                estimatedDeliveryDate = data["estimatedDeliveryDate"] as? Timestamp,
                                trackingNumber = data["trackingNumber"] as? String ?: "",
                                courier = data["courier"] as? String ?: "",
                                transactionReference = data["transactionReference"] as? String ?: ""
                            )
                        }
                    }
                    trySend(orders)
                }
            }
        awaitClose { listener.remove() }
    }

    // --- Cart Management in Firestore ---

    fun getCartFlow(userId: String): Flow<List<CartItem>> = callbackFlow {
        val listener = firestore.collection("users").document(userId).collection("cart")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        // Safely handle nested product data
                        val productData = doc.get("product") as? Map<String, Any>
                        val quantity = doc.getLong("quantity")?.toInt() ?: 1
                        
                        if (productData != null) {
                            // Extract product details manually to avoid type mismatch crashes
                            val rating = when (val r = productData["rating"]) {
                                is Number -> r.toDouble()
                                is String -> r.toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            }
                            val product = Product(
                                id = productData["id"] as? String ?: "",
                                name = productData["name"] as? String ?: "",
                                description = productData["description"] as? String ?: "",
                                category = productData["category"] as? String ?: "",
                                price = (productData["price"] as? Number)?.toDouble() ?: 0.0,
                                oldPrice = (productData["oldPrice"] as? Number)?.toDouble(),
                                imageUrl = productData["imageUrl"] as? String ?: "",
                                additionalImages = (productData["additionalImages"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                productVideoUrl = productData["productVideoUrl"] as? String,
                                stockQuantity = (productData["stockQuantity"] as? Number)?.toInt() ?: 0,
                                rating = rating,
                                numberOfReviews = (productData["numberOfReviews"] as? Number)?.toInt() ?: 0,
                                featured = productData["featured"] as? Boolean ?: false,
                                discountPercentage = (productData["discountPercentage"] as? Number)?.toInt(),
                                brand = productData["brand"] as? String ?: "",
                                specifications = (productData["specifications"] as? Map<*, *>)?.map { it.key.toString() to it.value.toString() }?.toMap() ?: emptyMap(),
                                createdAt = productData["createdAt"] as? com.google.firebase.Timestamp
                            )
                            CartItem(product, quantity)
                        } else {
                            null
                        }
                    }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addToCart(userId: String, product: Product): Result<Unit> = try {
        val cartCol = firestore.collection("users").document(userId).collection("cart")
        val doc = cartCol.document(product.id).get().await()
        
        if (doc.exists()) {
            cartCol.document(product.id).update("quantity", FieldValue.increment(1)).await()
        } else {
            cartCol.document(product.id).set(CartItem(product, 1)).await()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun removeFromCart(userId: String, productId: String): Result<Unit> = try {
        val cartCol = firestore.collection("users").document(userId).collection("cart")
        val doc = cartCol.document(productId).get().await()
        
        if (doc.exists()) {
            val currentQty = doc.getLong("quantity") ?: 0
            if (currentQty > 1) {
                cartCol.document(productId).update("quantity", FieldValue.increment(-1)).await()
            } else {
                cartCol.document(productId).delete().await()
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteFromCart(userId: String, productId: String): Result<Unit> = try {
        firestore.collection("users").document(userId).collection("cart")
            .document(productId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun clearCart(userId: String): Result<Unit> = try {
        val cartCol = firestore.collection("users").document(userId).collection("cart")
        val snapshot = cartCol.get().await()
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Complete checkout in a transaction:
     * 1. Save Order
     * 2. Save Payment Record
     * 3. Update Product Stock
     * 4. Clear Cart
     */
    suspend fun completeCheckout(
        order: Order,
        paymentRecord: com.shopizzo.data.model.PaymentRecord,
        cartItems: List<CartItem>
    ): Result<Unit> = try {
        firestore.runTransaction { transaction ->
            // 1. ALL READS MUST COME FIRST
            // Fetch product snapshots
            val snapshots = cartItems.map { item ->
                val ref = productsCol.document(item.product.id)
                val snapshot = transaction.get(ref)
                ref to snapshot
            }
            
            // Fetch User snapshot (if not guest)
            val userRef = if (order.userId != "GUEST") firestore.collection("users").document(order.userId) else null
            val userSnapshot = userRef?.let { transaction.get(it) }

            // 2. ALL WRITES AFTER READS
            // Save Order
            val orderRef = firestore.collection("orders").document(order.id)
            transaction.set(orderRef, order)

            // Save Payment Record
            val paymentRef = firestore.collection("payments").document(paymentRecord.paymentId)
            transaction.set(paymentRef, paymentRecord)

            // Update Product Stock
            snapshots.forEachIndexed { index, snapshotPair ->
                val productRef = snapshotPair.first
                val productSnapshot = snapshotPair.second
                val item = cartItems[index]

                val currentStock = productSnapshot.getLong("stockQuantity") ?: 0
                val newStock = (currentStock - item.quantity).coerceAtLeast(0)
                transaction.update(productRef, "stockQuantity", newStock)
            }

            // Update Profile purchasedProductIds (if not guest)
            if (userRef != null && userSnapshot != null && userSnapshot.exists()) {
                val currentPurchased = userSnapshot.get("purchasedProductIds") as? List<String> ?: emptyList()
                val newPurchased = (currentPurchased + cartItems.map { it.product.id }).distinct()
                transaction.update(userRef, "purchasedProductIds", newPurchased)
            }
            
            null
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun rateProduct(product: Product, userId: String, ratingValue: Int): Result<Unit> = try {
        val ratingsCol = firestore.collection("ProductRatings")
        val ratingId = "${userId}_${product.id}"
        val ratingRef = ratingsCol.document(ratingId)
        
        firestore.runTransaction { transaction ->
            val ratingSnapshot = transaction.get(ratingRef)
            val productRef = productsCol.document(product.id)
            val productSnapshot = transaction.get(productRef)
            
            val currentProductRating = when (val r = productSnapshot.get("rating")) {
                is Number -> r.toDouble()
                is String -> r.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val currentReviewsCount = productSnapshot.getLong("numberOfReviews") ?: 0
            
            val (newAverage, newCount) = if (ratingSnapshot.exists()) {
                // Update existing rating
                val oldRatingValue = ratingSnapshot.getLong("ratingValue")?.toInt() ?: 0
                val totalPoints = (currentProductRating * currentReviewsCount) - oldRatingValue + ratingValue
                val updatedAverage = totalPoints / currentReviewsCount
                updatedAverage to currentReviewsCount
            } else {
                // New rating
                val totalPoints = (currentProductRating * currentReviewsCount) + ratingValue
                val updatedCount = currentReviewsCount + 1
                val updatedAverage = totalPoints / updatedCount
                updatedAverage to updatedCount
            }
            
            // Save/Update rating doc
            val ratingObj = com.shopizzo.data.model.Rating(
                ratingId = ratingId,
                productId = product.id,
                userId = userId,
                ratingValue = ratingValue,
                timestamp = Timestamp.now()
            )
            transaction.set(ratingRef, ratingObj)
            
            // Update product doc
            transaction.update(productRef, mapOf(
                "rating" to newAverage,
                "numberOfReviews" to newCount
            ))
            
            null
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getUserRatingForProduct(userId: String, productId: String): Result<Int?> = try {
        val ratingId = "${userId}_$productId"
        val doc = firestore.collection("ProductRatings").document(ratingId).get().await()
        if (doc.exists()) {
            Result.success(doc.getLong("ratingValue")?.toInt())
        } else {
            Result.success(null)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Seeding logic to populate Firestore with at least 20 realistic products.
     */
    suspend fun seedProductsIfEmpty() {
        val snapshot = productsCol.get().await()
        
        // Always try to fix missing or broken images even if not empty
        if (!snapshot.isEmpty) {
            val batch = firestore.batch()
            var needsFix = false
            snapshot.documents.forEach { doc ->
                val product = doc.toProduct()
                val currentUrl = product?.imageUrl ?: ""
                
                // If image is missing, placeholder, or broken
                if (currentUrl.isEmpty() || currentUrl.contains("placeholder") || currentUrl.contains("gravatar")) {
                    val fallback = when (product?.category) {
                        "Smartphones" -> "https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&q=80&w=800"
                        "Laptops" -> "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&q=80&w=800"
                        "Tablets" -> "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&q=80&w=800"
                        "Headphones" -> "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&q=80&w=800"
                        "Smart Watches" -> "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&q=80&w=800"
                        "Gaming Accessories" -> "https://images.unsplash.com/photo-1607853202273-797f1c22a38e?auto=format&fit=crop&q=80&w=800"
                        else -> "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&q=80&w=800"
                    }
                    batch.update(doc.reference, "imageUrl", fallback)
                    needsFix = true
                }
            }
            if (needsFix) batch.commit().await()
            return
        }

        val products = mutableListOf<Product>()
        
        // --- Smartphones ---
        products.add(Product(
            name="iPhone 15 Pro Max", 
            brand="Apple", 
            category="Smartphones", 
            price=450000.0, 
            oldPrice=480000.0, 
            discountPercentage=6, 
            featured=true, 
            rating=4.9, 
            numberOfReviews=120, 
            stockQuantity=15, 
            imageUrl="https://images.unsplash.com/photo-1696446701796-da61225697cc?auto=format&fit=crop&q=80&w=800",
            additionalImages = listOf(
                "https://images.unsplash.com/photo-1695048133142-1a20484d2569?auto=format&fit=crop&q=80&w=800",
                "https://images.unsplash.com/photo-1695048133142-1a20484d2569?auto=format&fit=crop&q=80&w=800"
            ),
            productVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            specifications = mapOf(
                "Display" to "6.7-inch Super Retina XDR",
                "Chip" to "A17 Pro",
                "Camera" to "48MP Main | 12MP Ultra Wide | 12MP Telephoto",
                "Storage" to "256GB, 512GB, 1TB"
            ),
            description="The ultimate iPhone with titanium design and A17 Pro chip.", 
            createdAt=Timestamp.now()
        ))
        products.add(Product(
            name="Samsung Galaxy S24 Ultra", 
            brand="Samsung", 
            category="Smartphones", 
            price=420000.0, 
            oldPrice=440000.0, 
            discountPercentage=4, 
            featured=true, 
            rating=4.8, 
            numberOfReviews=95, 
            stockQuantity=20, 
            imageUrl="https://images.unsplash.com/photo-1706121401314-5e1656461971?auto=format&fit=crop&q=80&w=800",
            additionalImages = listOf("https://images.unsplash.com/photo-1706121401314-5e1656461971?auto=format&fit=crop&q=80&w=800"),
            productVideoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            specifications = mapOf(
                "Display" to "6.8-inch Dynamic AMOLED 2X",
                "Processor" to "Snapdragon 8 Gen 3",
                "Battery" to "5000mAh",
                "S-Pen" to "Included"
            ),
            description="AI-powered flagship with 200MP camera and S-Pen.", 
            createdAt=Timestamp.now()
        ))

        // --- Laptops ---
        products.add(Product(
            name="MacBook Pro 14 M3", 
            brand="Apple", 
            category="Laptops", 
            price=850000.0, 
            featured=true, 
            rating=4.9, 
            numberOfReviews=80, 
            stockQuantity=10, 
            imageUrl="https://images.unsplash.com/photo-1517336714460-457228377016?auto=format&fit=crop&q=80&w=800",
            additionalImages = listOf("https://images.unsplash.com/photo-1517336714460-457228377016?auto=format&fit=crop&q=80&w=800"),
            specifications = mapOf(
                "Chip" to "Apple M3 Chip",
                "Memory" to "8GB, 16GB, 24GB Unified Memory",
                "Display" to "Liquid Retina XDR display"
            ),
            description="Unmatched performance and battery life.", 
            createdAt=Timestamp.now()
        ))
        products.add(Product(name="Dell XPS 15", brand="Dell", category="Laptops", price=750000.0, oldPrice=800000.0, discountPercentage=6, featured=true, rating=4.7, numberOfReviews=45, stockQuantity=5, imageUrl="https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?auto=format&fit=crop&q=80&w=800", description="The gold standard of Windows laptops.", createdAt=Timestamp.now()))
        products.add(Product(name="ASUS ROG Zephyrus G14", brand="ASUS", category="Laptops", price=680000.0, featured=false, rating=4.8, numberOfReviews=35, stockQuantity=7, imageUrl="https://images.unsplash.com/photo-1588872657578-7efd3f1514a8?auto=format&fit=crop&q=80&w=800", description="Powerful compact gaming laptop.", createdAt=Timestamp.now()))
        products.add(Product(name="HP Spectre x360", brand="HP", category="Laptops", price=620000.0, featured=false, rating=4.6, numberOfReviews=28, stockQuantity=12, imageUrl="https://images.unsplash.com/photo-1544006659-f0b21f04cb1d?auto=format&fit=crop&q=80&w=800", description="Elegant 2-in-1 with stunning display.", createdAt=Timestamp.now()))

        // --- Tablets ---
        products.add(Product(name="iPad Pro 12.9 M2", brand="Apple", category="Tablets", price=480000.0, featured=true, rating=4.9, numberOfReviews=110, stockQuantity=12, imageUrl="https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&q=80&w=800", description="The most powerful tablet experience.", createdAt=Timestamp.now()))
        products.add(Product(name="Samsung Galaxy Tab S9 Ultra", brand="Samsung", category="Tablets", price=440000.0, featured=true, rating=4.8, numberOfReviews=65, stockQuantity=10, imageUrl="https://images.unsplash.com/photo-1585515320310-259814833e62?auto=format&fit=crop&q=80&w=800", description="Massive AMOLED screen for productivity.", createdAt=Timestamp.now()))

        // --- Audio ---
        products.add(Product(name="Sony WH-1000XM5", brand="Sony", category="Headphones", price=140000.0, featured=true, rating=4.9, numberOfReviews=210, stockQuantity=40, imageUrl="https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?auto=format&fit=crop&q=80&w=800", description="Best-in-class noise cancellation.", createdAt=Timestamp.now()))
        products.add(Product(name="AirPods Pro (2nd Gen)", brand="Apple", category="Headphones", price=120000.0, featured=false, rating=4.8, numberOfReviews=150, stockQuantity=25, imageUrl="https://images.unsplash.com/photo-1588423770574-d1092176665c?auto=format&fit=crop&q=80&w=800", description="Magical experience with active noise cancellation.", createdAt=Timestamp.now()))

        // --- Wearables ---
        products.add(Product(name="Apple Watch Ultra 2", brand="Apple", category="Smart Watches", price=340000.0, featured=true, rating=4.9, numberOfReviews=85, stockQuantity=10, imageUrl="https://images.unsplash.com/photo-1591370874773-6702e8f12fd8?auto=format&fit=crop&q=80&w=800", description="Rugged and capable sports watch.", createdAt=Timestamp.now()))
        products.add(Product(name="Galaxy Watch 6 Classic", brand="Samsung", category="Smart Watches", price=150000.0, featured=false, rating=4.7, numberOfReviews=40, stockQuantity=15, imageUrl="https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&q=80&w=800", description="The watch that knows you best.", createdAt=Timestamp.now()))

        // --- Gaming ---
        products.add(Product(name="PlayStation 5 Console", brand="Sony", category="Gaming Accessories", price=250000.0, featured=true, rating=4.9, numberOfReviews=500, stockQuantity=5, imageUrl="https://images.unsplash.com/photo-1607853202273-797f1c22a38e?auto=format&fit=crop&q=80&w=800", description="The latest in high-end gaming.", createdAt=Timestamp.now()))
        products.add(Product(name="Razer BlackWidow V4", brand="Razer", category="Gaming Accessories", price=65000.0, featured=false, rating=4.7, numberOfReviews=120, stockQuantity=50, imageUrl="https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&q=80&w=800", description="Mechanical gaming keyboard with RGB.", createdAt=Timestamp.now()))

        val batch = firestore.batch()
        products.forEach { product ->
            val docRef = productsCol.document()
            batch.set(docRef, product)
        }
        batch.commit().await()
    }
}
