package com.shopizzo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.shopizzo.data.model.CartItem
import com.shopizzo.data.model.Order
import com.shopizzo.data.model.OrderProduct
import com.shopizzo.data.model.PaymentRecord
import com.shopizzo.data.model.PaymentStatus
import com.shopizzo.data.model.Product
import com.shopizzo.data.model.UserProfile
import com.shopizzo.data.repository.ProductRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map as flowMap
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ProductsUiState {
    object Loading                            : ProductsUiState()
    data class Success(val products: List<Product>) : ProductsUiState()
    data class Error(val message: String)     : ProductsUiState()
}

sealed class DetailUiState {
    object Idle                              : DetailUiState()
    object Loading                           : DetailUiState()
    data class Success(val product: Product) : DetailUiState()
    data class Error(val message: String)    : DetailUiState()
}

sealed class PaymentAuthState {
    object Idle : PaymentAuthState()
    object RequestBiometricEnable : PaymentAuthState() // For first-time informational popup
    object BiometricRequired : PaymentAuthState()
    object PinRequired : PaymentAuthState()
    object Loading : PaymentAuthState()
    object Success : PaymentAuthState()
    data class Error(val message: String) : PaymentAuthState()
}

/**
 * Shared ViewModel for product browsing, cart management, and checkout.
 */
class ProductViewModel : ViewModel() {

    private val repository = ProductRepository()
    private val authRepository = com.shopizzo.data.repository.AuthRepository()

    // ─── Real-time States ───────────────────────────────────────────────────
    
    val productsState: StateFlow<ProductsUiState> = repository.getAllProductsFlow()
        .flowMap { products -> ProductsUiState.Success(products) as ProductsUiState }
        .onStart { emit(ProductsUiState.Loading) }
        .catch { e -> emit(ProductsUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductsUiState.Loading)

    val featuredState: StateFlow<ProductsUiState> = repository.getFeaturedProductsFlow()
        .flowMap { products -> ProductsUiState.Success(products) as ProductsUiState }
        .onStart { emit(ProductsUiState.Loading) }
        .catch { e -> emit(ProductsUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProductsUiState.Loading)

    // ─── Detail screen ───────────────────────────────────────────────────────
    private val _detailState     = MutableStateFlow<DetailUiState>(DetailUiState.Idle)
    val detailState: StateFlow<DetailUiState> = _detailState.asStateFlow()

    // ─── Shopping cart ────────────────────────────────────────────────────────
    private val _cartItems       = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val cartTotal: Double get() = _cartItems.value.sumOf { it.lineTotal }
    val cartCount: Int get() = _cartItems.value.sumOf { it.quantity }

    // ─── Rating / Payment / Search ───────────────────────────────────────────
    private val _paymentStatus   = MutableStateFlow(PaymentStatus.IDLE)
    val paymentStatus: StateFlow<PaymentStatus> = _paymentStatus.asStateFlow()

    private val _orderError = MutableStateFlow<String?>(null)
    val orderError: StateFlow<String?> = _orderError.asStateFlow()

    private val _checkoutUrl     = MutableStateFlow<String?>(null)
    val checkoutUrl: StateFlow<String?> = _checkoutUrl.asStateFlow()

    private val _placedOrder = MutableStateFlow<Order?>(null)
    val placedOrder: StateFlow<Order?> = _placedOrder.asStateFlow()

    private val _paymentAuthState = MutableStateFlow<PaymentAuthState>(PaymentAuthState.Idle)
    val paymentAuthState: StateFlow<PaymentAuthState> = _paymentAuthState.asStateFlow()

    private val _searchQuery     = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _userOrders = MutableStateFlow<List<Order>>(emptyList())
    val userOrders: StateFlow<List<Order>> = _userOrders.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private var cartJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedProductsIfEmpty()
        }
    }

    fun observeUserCart(userId: String) {
        cartJob?.cancel()
        cartJob = viewModelScope.launch {
            repository.getCartFlow(userId).collect { items: List<CartItem> ->
                _cartItems.value = items
            }
        }
    }

    fun observeUserOrders(userId: String) {
        viewModelScope.launch {
            repository.getUserOrdersFlow(userId).collect { orders: List<Order> ->
                _userOrders.value = orders
            }
        }
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    fun loadProductById(id: String) {
        viewModelScope.launch {
            _detailState.value = DetailUiState.Loading
            val result = repository.getProductById(id)
            _detailState.value = if (result.isSuccess) {
                DetailUiState.Success(result.getOrNull()!!)
            } else {
                DetailUiState.Error(result.exceptionOrNull()?.message ?: "Product not found.")
            }
        }
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun updateCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun filteredProducts(): List<Product> {
        val successState = productsState.value as? ProductsUiState.Success
        val all: List<Product> = successState?.products ?: return emptyList()
        val q: String = _searchQuery.value.trim().lowercase()
        val cat: String? = _selectedCategory.value

        return all.filter { p: Product ->
            val nameMatch = p.name.lowercase().contains(q)
            val brandMatch = p.brand.lowercase().contains(q)
            val categoryMatch = cat == null || p.category == cat
            (q.isEmpty() || nameMatch || brandMatch) && categoryMatch
        }
    }

    fun rateProduct(product: Product, userId: String, rating: Int) {
        viewModelScope.launch {
            val result = repository.rateProduct(product, userId, rating)
            if (result.isSuccess) {
                _snackbarMessage.emit("Thank you for rating this product $rating out of 5!")
                loadProductById(product.id)
            } else {
                _snackbarMessage.emit("Rating failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun clearRatingMessage() { /* Deprecated */ }

    // ─── Cart Management ─────────────────────────────────────────────────────

    fun addToCart(product: Product, userId: String? = null) {
        // Optimistic UI Update for both Guest and Logged-in
        val current = _cartItems.value.toMutableList()
        val index   = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1)
        } else {
            current.add(CartItem(product = product, quantity = 1))
        }
        _cartItems.value = current

        if (userId != null) {
            viewModelScope.launch { 
                val result = repository.addToCart(userId, product)
                if (result.isSuccess) {
                    _snackbarMessage.emit("Added to Cart")
                } else {
                    // Revert on failure
                    observeUserCart(userId)
                    _snackbarMessage.emit("Failed to add to cart")
                }
            }
        } else {
            viewModelScope.launch { _snackbarMessage.emit("Added to Cart") }
        }
    }

    fun removeFromCart(productId: String, userId: String? = null) {
        // Optimistic UI Update
        val current = _cartItems.value.toMutableList()
        val index   = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = current[index]
            if (item.quantity > 1) {
                current[index] = item.copy(quantity = item.quantity - 1)
            } else {
                current.removeAt(index)
            }
            _cartItems.value = current
        }

        if (userId != null) {
            viewModelScope.launch { 
                val result = repository.removeFromCart(userId, productId)
                if (result.isFailure) observeUserCart(userId)
            }
        }
    }

    fun deleteFromCart(productId: String, userId: String? = null) {
        // Optimistic UI Update
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }

        if (userId != null) {
            viewModelScope.launch { 
                val result = repository.deleteFromCart(userId, productId)
                if (result.isFailure) observeUserCart(userId)
            }
        }
    }

    fun clearCart(userId: String? = null) {
        if (userId != null) {
            viewModelScope.launch { repository.clearCart(userId) }
        } else {
            _cartItems.value = emptyList()
        }
    }

    // ─── Payment / Checkout ──────────────────────────────────────────────────

    fun placeOrder(
        user: UserProfile?,
        deliveryAddress: String,
        phoneNumber: String,
        deliveryNotes: String,
        paymentMethod: String,
        courier: String,
        subtotal: Double,
        deliveryFee: Double,
        discount: Double,
        totalAmount: Double
    ) {
        if (_cartItems.value.isEmpty()) {
            _orderError.value = "Your cart is empty. Please add items to checkout."
            return
        }

        val userId = user?.uid ?: "GUEST"
        viewModelScope.launch {
            try {
                _paymentStatus.value = PaymentStatus.LOADING
                
                // Simulate processing time
                kotlinx.coroutines.delay(2000)

                val orderId = "ORD-${UUID.randomUUID().toString().take(8).uppercase()}"
                val orderProducts = _cartItems.value.map { item ->
                    OrderProduct(
                        productId = item.product.id,
                        name = item.product.name,
                        quantity = item.quantity,
                        price = item.product.price,
                        imageUrl = item.product.imageUrl
                    )
                }

                val transactionRef = "TXN-${UUID.randomUUID().toString().take(12).uppercase()}"

                val order = Order(
                    id = orderId,
                    userId = userId,
                    customerName = user?.fullName ?: "Guest User",
                    customerEmail = user?.email ?: "",
                    phoneNumber = phoneNumber,
                    products = orderProducts,
                    totalAmount = totalAmount,
                    subtotal = subtotal,
                    deliveryFee = deliveryFee,
                    discount = discount,
                    paymentMethod = paymentMethod,
                    paymentStatus = if (paymentMethod == "Cash on Delivery") "Pending" else "Paid",
                    deliveryAddress = deliveryAddress,
                    deliveryNotes = deliveryNotes,
                    orderStatus = "Confirmed",
                    orderDate = Timestamp.now(),
                    lastUpdated = Timestamp.now(),
                    courier = courier,
                    transactionReference = transactionRef
                )

                val paymentRecord = PaymentRecord(
                    paymentId = UUID.randomUUID().toString(),
                    orderId = orderId,
                    userId = userId,
                    amount = totalAmount,
                    paymentMethod = paymentMethod,
                    transactionReference = transactionRef,
                    paymentStatus = order.paymentStatus,
                    timestamp = Timestamp.now()
                )

                val result = repository.completeCheckout(order, paymentRecord, _cartItems.value)
                if (result.isSuccess) {
                    // Clear the cart only after successful order confirmation
                    if (userId != "GUEST") {
                        repository.clearCart(userId)
                    }

                    _placedOrder.value = order
                    _paymentStatus.value = PaymentStatus.SUCCESS
                    _cartItems.value = emptyList()
                    
                    // Optimistic update for the order list in profile
                    if (userId != "GUEST") {
                        val currentOrders = _userOrders.value.toMutableList()
                        currentOrders.add(0, order) // Add new order at the top
                        _userOrders.value = currentOrders
                    }
                    _snackbarMessage.emit("Order Placed Successfully")
                } else {
                    _paymentStatus.value = PaymentStatus.IDLE
                    _orderError.value = result.exceptionOrNull()?.message ?: "Checkout failed. Please try again."
                }
                // Always reset auth state so the dialogs close and user can retry if needed
                _paymentAuthState.value = PaymentAuthState.Idle
            } catch (e: Exception) {
                _paymentStatus.value = PaymentStatus.IDLE
                _orderError.value = "Error: ${e.localizedMessage}"
                _paymentAuthState.value = PaymentAuthState.Idle
            }
        }
    }

    fun clearOrderError() { _orderError.value = null }

    fun resetPaymentStatus() {
        _paymentStatus.value = PaymentStatus.IDLE
        _checkoutUrl.value   = null
        _placedOrder.value   = null
        _paymentAuthState.value = PaymentAuthState.Idle
    }

    fun startPaymentAuth(method: String) {
        // Here we decide based on user preference or just default to selection
        _paymentAuthState.value = PaymentAuthState.Idle // Reset to show selection
    }

    fun setPaymentAuthState(state: PaymentAuthState) {
        _paymentAuthState.value = state
    }

    fun verifyPin(userId: String, pin: String, onVerified: () -> Unit) {
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            _paymentAuthState.value = PaymentAuthState.Error("PIN must be exactly 4 digits.")
            viewModelScope.launch {
                delay(2000)
                _paymentAuthState.value = PaymentAuthState.Idle
            }
            return
        }

        viewModelScope.launch {
            _paymentAuthState.value = PaymentAuthState.Loading
            
            // Simulation logic:
            // 1. Check if it's the master PIN "1234" (Always works for user convenience)
            // 2. Check repository (real logic)
            val isVerified = if (pin == "1234") {
                true
            } else {
                val repoResult = authRepository.validatePaymentPin(userId, pin)
                repoResult.isSuccess && repoResult.getOrDefault(false)
            }

            if (isVerified) {
                _paymentAuthState.value = PaymentAuthState.Success
                onVerified()
            } else {
                _paymentAuthState.value = PaymentAuthState.Error("Incorrect PIN. Please try again.")
                delay(2000)
                _paymentAuthState.value = PaymentAuthState.Idle
            }
        }
    }
}
