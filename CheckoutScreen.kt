package com.shopizzo.ui.screens

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.shopizzo.data.model.CartItem
import com.shopizzo.data.model.Order
import com.shopizzo.data.model.OrderProduct
import com.shopizzo.data.model.PaymentStatus
import com.shopizzo.data.model.UserProfile
import com.shopizzo.ui.components.ShopizzoButton
import com.shopizzo.ui.components.ShopizzoTopBar
import com.shopizzo.ui.components.shopizzoFieldColors
import com.shopizzo.ui.theme.*
import com.shopizzo.viewmodel.AuthViewModel
import com.shopizzo.viewmodel.PaymentAuthState
import com.shopizzo.viewmodel.ProductViewModel
import kotlinx.coroutines.delay
import java.util.UUID

enum class PaymentMethod(val label: String, val icon: ImageVector) {
    AIRTEL("Airtel Money", Icons.Filled.Smartphone),
    MPAMBA("TNM Mpamba", Icons.Filled.MobileFriendly),
    CARD("Visa / MasterCard", Icons.Filled.CreditCard),
    PAYCHANGU("PayChangu", Icons.Filled.Payment),
    COD("Cash on Delivery", Icons.Filled.Payments)
}

enum class Courier(val label: String, val fee: Double, val days: String) {
    SPEED("Speed Courier", 5000.0, "1-2 Days"),
    CTS("CTS Courier", 4000.0, "2-3 Days"),
    AXA("AXA Courier", 3500.0, "3-4 Days"),
    SMART("SMART Courier", 3000.0, "2-4 Days")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    productViewModel : ProductViewModel,
    authViewModel    : AuthViewModel,
    onBack           : () -> Unit,
    onDone           : () -> Unit
) {
    val cartItems     by productViewModel.cartItems.collectAsState()
    val paymentStatus by productViewModel.paymentStatus.collectAsState()
    val paymentAuthState by productViewModel.paymentAuthState.collectAsState()
    val placedOrder   by productViewModel.placedOrder.collectAsState()
    val orderError    by productViewModel.orderError.collectAsState()
    val currentUser   by authViewModel.userProfile.collectAsState()
    
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    
    val biometricEnabled by authViewModel.biometricEnabled.collectAsState()
    var biometricPromptDismissedInSession by rememberSaveable { mutableStateOf(false) }

    var showAuthDialog by remember { mutableStateOf(false) } // This will be used for selection
    var showBiometricEnablePopup by remember { mutableStateOf(false) }
    var showPinInput by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }

    val subtotal = cartItems.sumOf { it.lineTotal }
    
    // Reset payment status when entering the screen to avoid stale SUCCESS states
    LaunchedEffect(Unit) {
        productViewModel.resetPaymentStatus()
    }

    var selectedCourier by remember { mutableStateOf(Courier.SPEED) }
    val deliveryFee = if (subtotal > 0) selectedCourier.fee else 0.0
    val total = subtotal + deliveryFee

    var address by remember { mutableStateOf(currentUser?.country ?: "") }
    var phone by remember { mutableStateOf(currentUser?.phoneNumber ?: "") }
    var notes by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.AIRTEL) }
    
    // Card fields
    var cardName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    val placeOrderAction = {
        productViewModel.placeOrder(
            user = currentUser,
            deliveryAddress = address.ifBlank { "N/A" },
            phoneNumber = phone.ifBlank { "N/A" },
            deliveryNotes = notes,
            paymentMethod = selectedMethod.label,
            courier = selectedCourier.label,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            discount = 0.0,
            totalAmount = total
        )
    }

    LaunchedEffect(paymentAuthState) {
        val state = paymentAuthState
        if (state is PaymentAuthState.Success) {
            showAuthDialog = false
            showPinInput = false
            pinValue = ""
            // Call order placement first
            placeOrderAction()
            // Then reset auth state so it's ready for next time
            productViewModel.setPaymentAuthState(PaymentAuthState.Idle)
        } else if (state is PaymentAuthState.Error) {
            snackbarHost.showSnackbar(state.message)
            pinValue = ""
            productViewModel.setPaymentAuthState(PaymentAuthState.Idle)
        }
    }

    fun handleBiometricAuth(isEnabling: Boolean = false) {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        productViewModel.setPaymentAuthState(PaymentAuthState.Idle)
                        pinValue = ""
                        showPinInput = true
                    } else {
                        productViewModel.setPaymentAuthState(PaymentAuthState.Error("Biometric authentication failed: $errString"))
                        if (isEnabling) {
                            pinValue = ""
                            showPinInput = true
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    if (isEnabling) {
                        authViewModel.saveBiometricPreference(true)
                        authViewModel.updatePreferredAuthMethod("BIOMETRIC")
                    }
                    productViewModel.setPaymentAuthState(PaymentAuthState.Success)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    productViewModel.setPaymentAuthState(PaymentAuthState.Error("Biometric authentication failed. Please try again or use PIN."))
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isEnabling) "Enable Biometric Payment" else "Payment Security")
            .setSubtitle(if (isEnabling) "Confirm your identity to enable biometric payments" else "Authenticate payment using biometric")
            .setNegativeButtonText("Use PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(orderError) {
        orderError?.let {
            snackbarHost.showSnackbar(it)
            productViewModel.clearOrderError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ShopizzoTopBar(
                title = "Checkout",
                showBack = true,
                onBackClick = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (paymentStatus == PaymentStatus.SUCCESS && placedOrder != null) {
                OrderSuccessScreen(placedOrder!!, onDone)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // 1. Order Summary
                    Text("Order Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            cartItems.forEach { item ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = item.product.imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                                        error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_dialog_alert),
                                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text("${item.quantity} x MK ${"%,.0f".format(item.product.price)}", style = MaterialTheme.typography.bodySmall, color = ShopizzoMidGray)
                                    }
                                    Text("MK ${"%,.0f".format(item.lineTotal)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                            SummaryRow("Subtotal", subtotal)
                            SummaryRow("Delivery Fee", deliveryFee)
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("MK ${"%,.0f".format(total)}", style = MaterialTheme.typography.titleLarge, color = ShopizzoBlue, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // 2. Delivery Information
                    Text("Delivery Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Delivery Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = shopizzoFieldColors()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = shopizzoFieldColors()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Delivery Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = shopizzoFieldColors()
                    )

                    Spacer(Modifier.height(24.dp))

                    // 3. Delivery Method
                    Text("Delivery Provider", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    CourierSelector(selectedCourier) { selectedCourier = it }

                    Spacer(Modifier.height(24.dp))

                    // 4. Payment Method
                    Text("Payment Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    PaymentMethodGrid(selectedMethod) { selectedMethod = it }

                    Spacer(Modifier.height(16.dp))

                    // 5. Dynamic Input Fields
                    AnimatedVisibility(visible = selectedMethod == PaymentMethod.AIRTEL || selectedMethod == PaymentMethod.MPAMBA) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("${selectedMethod.label} Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = shopizzoFieldColors()
                        )
                    }

                    AnimatedVisibility(visible = selectedMethod == PaymentMethod.CARD) {
                        Column {
                            OutlinedTextField(
                                value = cardName,
                                onValueChange = { cardName = it },
                                label = { Text("Cardholder Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = shopizzoFieldColors()
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { cardNumber = it },
                                label = { Text("Card Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = shopizzoFieldColors()
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = expiry,
                                    onValueChange = { expiry = it },
                                    label = { Text("MM/YY") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = shopizzoFieldColors()
                                )
                                OutlinedTextField(
                                    value = cvv,
                                    onValueChange = { cvv = it },
                                    label = { Text("CVV") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = shopizzoFieldColors()
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = selectedMethod == PaymentMethod.COD) {
                        Surface(
                            color = ShopizzoBlue.copy(0.05f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Info, null, tint = ShopizzoBlue)
                                Spacer(Modifier.width(12.dp))
                                Text("You will pay MK ${"%,.0f".format(total)} in cash when your order is delivered.", style = MaterialTheme.typography.bodyMedium, color = ShopizzoBlue)
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    ShopizzoButton(
                        text = if (paymentStatus == PaymentStatus.LOADING) "Processing Payment..." else "Place & Pay",
                        isLoading = paymentStatus == PaymentStatus.LOADING,
                        enabled = cartItems.isNotEmpty() && address.isNotBlank() && phone.isNotBlank() && (
                            selectedMethod != PaymentMethod.CARD || (
                                cardName.isNotBlank() && cardNumber.length >= 16 && expiry.isNotBlank() && cvv.length >= 3
                            )
                        ),
                        onClick = {
                            if (selectedMethod == PaymentMethod.COD) {
                                placeOrderAction()
                                return@ShopizzoButton
                            }

                            if (currentUser?.uid == null) {
                                // Guest checkout bypasses biometric/PIN for now
                                placeOrderAction()
                                return@ShopizzoButton
                            }

                            // Smart Biometric Flow
                            if (!biometricEnabled && !biometricPromptDismissedInSession) {
                                // First-time informational popup
                                showBiometricEnablePopup = true
                            } else if (biometricEnabled) {
                                // Biometric is enabled: Show options (Fingerprint or PIN)
                                showAuthDialog = true
                            } else {
                                // Not enabled and already prompted: Just PIN
                                productViewModel.setPaymentAuthState(PaymentAuthState.Idle)
                                pinValue = ""
                                showPinInput = true
                            }
                        }
                    )
                    
                    Spacer(Modifier.height(40.dp))
                }
            }

            // 1. Biometric Enable Informational Popup
            if (showBiometricEnablePopup) {
                AlertDialog(
                    onDismissRequest = { 
                        showBiometricEnablePopup = false
                        showPinInput = true 
                    },
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, null, tint = ShopizzoBlue)
                            Spacer(Modifier.width(12.dp))
                            Text("Secure Payments", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text("Protect your future payments with biometric authentication.\n\nYou can use your fingerprint or face recognition to approve future transactions quickly and securely.", style = MaterialTheme.typography.bodyMedium)
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showBiometricEnablePopup = false
                                handleBiometricAuth(isEnabling = true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ShopizzoBlue)
                        ) {
                            Text("Enable Biometric")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showBiometricEnablePopup = false
                            biometricPromptDismissedInSession = true
                            pinValue = ""
                            showPinInput = true
                        }) {
                            Text("Not Now", color = ShopizzoMidGray)
                        }
                    }
                )
            }

            // 2. Authentication Selection Dialog (For Repeat Users)
            if (showAuthDialog) {
                AlertDialog(
                    onDismissRequest = { showAuthDialog = false },
                    title = { Text("Verify Payment", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Choose how you would like to authorize this payment.", style = MaterialTheme.typography.bodySmall, color = ShopizzoMidGray)
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                onClick = {
                                    showAuthDialog = false
                                    handleBiometricAuth(isEnabling = false)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                ListItem(
                                    headlineContent = { Text("Fingerprint / Face ID", fontWeight = FontWeight.SemiBold) },
                                    leadingContent = { Icon(Icons.Default.Fingerprint, null, tint = ShopizzoBlue) },
                                    trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = ShopizzoMidGray) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                onClick = {
                                    showAuthDialog = false
                                    showPinInput = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                ListItem(
                                    headlineContent = { Text("Enter 4-Digit PIN", fontWeight = FontWeight.SemiBold) },
                                    leadingContent = { Icon(Icons.Default.Lock, null, tint = ShopizzoBlue) },
                                    trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = ShopizzoMidGray) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showAuthDialog = false }) { Text("Cancel") }
                    }
                )
            }

            // 3. PIN Input Dialog
            if (showPinInput) {
                AlertDialog(
                    onDismissRequest = { showPinInput = false },
                    title = { Text("Enter Payment PIN") },
                    text = {
                        Column {
                            Text("Please enter your security PIN to authorize this transaction.", style = MaterialTheme.typography.bodyMedium, color = ShopizzoMidGray)
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = pinValue,
                                onValueChange = { 
                                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                        pinValue = it
                                    }
                                },
                                label = { Text("4-Digit PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = shopizzoFieldColors(),
                                isError = pinValue.isNotEmpty() && pinValue.length < 4,
                                supportingText = {
                                    if (pinValue.isNotEmpty() && pinValue.length < 4) {
                                        Text("PIN must be 4 digits", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )
                        }
                    },
                    confirmButton = {
                        val authState by productViewModel.paymentAuthState.collectAsState()
                        Button(
                            onClick = {
                                val userId = currentUser?.uid
                                if (userId != null) {
                                    productViewModel.verifyPin(userId, pinValue) {
                                        // PIN Verified, Success state handled in LaunchedEffect
                                    }
                                } else {
                                    // For Guest, we might just bypass or use a default
                                    productViewModel.setPaymentAuthState(PaymentAuthState.Success)
                                }
                            },
                            enabled = pinValue.length == 4 && authState !is PaymentAuthState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = ShopizzoBlue)
                        ) {
                            if (authState is PaymentAuthState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Verify PIN")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPinInput = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: Double) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ShopizzoMidGray)
        Text("MK ${"%,.0f".format(amount)}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CourierSelector(selected: Courier, onSelect: (Courier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Courier.values().forEach { courier ->
            val isSelected = selected == courier
            Surface(
                onClick = { onSelect(courier) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) ShopizzoBlue.copy(0.08f) else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ShopizzoBlue else Color(0xFFEEEEEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(if (isSelected) ShopizzoBlue else Color(0xFFF5F5F5), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocalShipping, null, tint = if (isSelected) Color.White else ShopizzoMidGray, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(courier.label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        Text("Est: ${courier.days}", style = MaterialTheme.typography.bodySmall, color = ShopizzoMidGray)
                    }
                    Text("MK ${"%,.0f".format(courier.fee)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    RadioButton(selected = isSelected, onClick = { onSelect(courier) }, colors = RadioButtonDefaults.colors(selectedColor = ShopizzoBlue))
                }
            }
        }
    }
}

@Composable
fun PaymentMethodGrid(selected: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PaymentMethod.values().forEach { method ->
            val isSelected = selected == method
            Surface(
                onClick = { onSelect(method) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) ShopizzoBlue.copy(0.08f) else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ShopizzoBlue else Color(0xFFEEEEEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(if (isSelected) ShopizzoBlue else Color(0xFFF5F5F5), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(method.icon, null, tint = if (isSelected) Color.White else ShopizzoMidGray, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(method.label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    Spacer(Modifier.weight(1f))
                    RadioButton(selected = isSelected, onClick = { onSelect(method) }, colors = RadioButtonDefaults.colors(selectedColor = ShopizzoBlue))
                }
            }
        }
    }
}

@Composable
fun OrderSuccessScreen(order: Order, onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.CheckCircle, null, tint = ShopizzoSuccess, modifier = Modifier.size(100.dp))
            Spacer(Modifier.height(24.dp))
            Text("Order Placed Successfully!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Your order # ${order.id} has been received and is being processed.", color = ShopizzoMidGray, textAlign = TextAlign.Center)
            
            Spacer(Modifier.height(32.dp))
            
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    DetailRow("Payment Method", order.paymentMethod)
                    DetailRow("Delivery Address", order.deliveryAddress)
                    DetailRow("Total Amount", "MK ${"%,.0f".format(order.totalAmount)}")
                    DetailRow("Courier", order.courier)
                }
            }
            
            Spacer(Modifier.height(48.dp))
            
            ShopizzoButton(text = "Back to Shopping", onClick = onDone)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = ShopizzoMidGray, style = MaterialTheme.typography.bodyMedium)
        Text(value, modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
    }
}
