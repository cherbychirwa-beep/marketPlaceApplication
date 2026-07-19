package com.shopizzo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import coil.compose.AsyncImage
import com.shopizzo.data.model.Order
import com.shopizzo.data.model.UserProfile
import com.shopizzo.ui.components.ShopizzoButton
import com.shopizzo.ui.components.ShopizzoTopBar
import com.shopizzo.ui.components.shopizzoFieldColors
import com.shopizzo.ui.theme.*
import com.shopizzo.viewmodel.AuthUiState
import com.shopizzo.viewmodel.AuthViewModel
import com.shopizzo.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    productViewModel: ProductViewModel,
    onLoggedOut: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onAboutClick: () -> Unit,
    onViewAllOrders: () -> Unit = {},
    onOrderClick: (String) -> Unit = {}
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    val favouriteCount = userProfile?.favouriteIds?.size ?: 0
    val authState by authViewModel.authState.collectAsState()
    val appTheme by authViewModel.appTheme.collectAsState()
    val biometricEnabled by authViewModel.biometricEnabled.collectAsState()
    val userOrders by productViewModel.userOrders.collectAsState()
    
    val snackbarHost = remember { SnackbarHostState() }
    val isLoggedIn = authViewModel.isLoggedIn
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }

    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedPhone by remember { mutableStateOf("") }
    var editedCountry by remember { mutableStateOf("") }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            editedName = it.fullName
            editedPhone = it.phoneNumber
            editedCountry = it.country
            productViewModel.observeUserOrders(it.uid)
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            snackbarHost.showSnackbar((authState as AuthUiState.Success).message)
            authViewModel.resetState()
            isEditing = false
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout Confirmation", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of your Shopizzo account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.logout()
                        onLoggedOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShopizzoError)
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = ShopizzoMidGray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ShopizzoTopBar(
                title = "Profile",
                isLoggedIn = isLoggedIn,
                favouriteCount = favouriteCount,
                onCartClick = {}, // Handled in MainActivity
                onFavouriteClick = {}
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                title = { Text("Set Payment PIN") },
                text = {
                    Column {
                        Text("Create a 4-digit security PIN for your payments.", color = ShopizzoMidGray)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { 
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    newPin = it
                                }
                            },
                            label = { Text("4-Digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = shopizzoFieldColors(),
                            isError = newPin.isNotEmpty() && newPin.length < 4,
                            supportingText = {
                                if (newPin.isNotEmpty() && newPin.length < 4) {
                                    Text("PIN must be 4 digits", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            authViewModel.updatePaymentPin(newPin)
                            showPinDialog = false
                            newPin = ""
                        },
                        enabled = newPin.length == 4
                    ) { Text("Save PIN") }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoggedIn) {
                // ─── Header Section ──────────────────────────────────────────
                ProfileHeader(userProfile)

                Spacer(Modifier.height(24.dp))

                if (isEditing) {
                    EditProfileSection(
                        editedName = editedName,
                        onNameChange = { editedName = it },
                        editedPhone = editedPhone,
                        onPhoneChange = { editedPhone = it },
                        editedCountry = editedCountry,
                        onCountryChange = { editedCountry = it },
                        onCancel = { isEditing = false },
                        onSave = {
                            userProfile?.let {
                                authViewModel.updateProfile(it.copy(
                                    fullName = editedName,
                                    phoneNumber = editedPhone,
                                    country = editedCountry
                                ))
                            }
                        },
                        isLoading = authState is AuthUiState.Loading
                    )
                } else {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        // ─── Profile Info ──────────────────────────────────────
                        ProfileInfoCard(userProfile) { isEditing = true }

                        Spacer(Modifier.height(24.dp))

                        // ─── My Orders Section ──────────────────────────────────
                        OrdersSection(userOrders, onViewAllOrders, onOrderClick)

                        Spacer(Modifier.height(24.dp))

                        // ─── Connected Accounts ─────────────────────────────────
                        ConnectedAccountsSection()

                        Spacer(Modifier.height(24.dp))

                        // ─── Appearance & Settings ──────────────────────────────
                        SettingsSection(
                            appTheme = appTheme,
                            biometricEnabled = biometricEnabled,
                            preferredAuth = userProfile?.preferredAuthMethod ?: "PIN",
                            onThemeChange = { authViewModel.saveThemePreference(it) },
                            onBiometricChange = { authViewModel.saveBiometricPreference(it) },
                            onAuthMethodChange = { authViewModel.updatePreferredAuthMethod(it) },
                            onAboutClick = onAboutClick,
                            onSetPin = { showPinDialog = true }
                        )

                        Spacer(Modifier.height(24.dp))

                        // ─── Privacy & Security ─────────────────────────────────
                        PrivacySecuritySection(
                            onLogoutDevices = { /* Handle logout from other devices */ }
                        )

                        Spacer(Modifier.height(32.dp))

                        // ─── Logout Button ──────────────────────────────────────
                        Button(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ShopizzoError)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Logout, contentDescription = null)
                                Spacer(Modifier.width(12.dp))
                                Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                GuestProfilePlaceholder(onNavigateToLogin, appTheme, authViewModel, onAboutClick)
            }
        }
    }
}

@Composable
fun ProfileHeader(userProfile: UserProfile?) {
    val name = userProfile?.fullName ?: "User"
    val email = userProfile?.email ?: ""
    val initial = if (name.isNotBlank()) name.first().uppercase() else if (email.isNotBlank()) email.first().uppercase() else "U"
    
    val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val memberSince = userProfile?.createdAt?.toDate()?.let { sdf.format(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ShopizzoBlue, MaterialTheme.colorScheme.background)
                )
            )
            .padding(top = 32.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(ShopizzoBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial.toString(),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(text = name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = email, color = ShopizzoMidGray, fontSize = 14.sp)
        if (memberSince != null) {
            Text(
                text = "Member since $memberSince",
                color = ShopizzoBlue.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProfileInfoCard(userProfile: UserProfile?, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Personal Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ShopizzoBlue)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = ShopizzoBlue, modifier = Modifier.size(20.dp))
                }
            }
            
            InfoRow(Icons.Default.Person, "Full Name", userProfile?.fullName ?: "N/A")
            InfoRow(Icons.Default.Email, "Email Address", userProfile?.email ?: "N/A")
            InfoRow(Icons.Default.Phone, "Phone Number", userProfile?.phoneNumber ?: "N/A")
            InfoRow(Icons.Default.LocationOn, "Delivery Country", userProfile?.country ?: "N/A")
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = ShopizzoMidGray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = ShopizzoMidGray)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun OrdersSection(orders: List<Order>, onViewAll: () -> Unit, onOrderClick: (String) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("My Orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ShopizzoBlue)
            if (orders.size > 3) {
                TextButton(onClick = onViewAll) {
                    Text("View All", color = ShopizzoBlue)
                }
            }
        }
        
        if (orders.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ShoppingBag, null, tint = ShopizzoLightGray, modifier = Modifier.size(48.dp))
                    Text("No orders placed yet", color = ShopizzoMidGray, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            orders.take(3).forEach { order ->
                OrderCard(order) { onOrderClick(order.id) }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun OrderCard(order: Order, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val date = sdf.format(order.orderDate.toDate())
    
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(ShopizzoBlue.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Receipt, null, tint = ShopizzoBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(order.id, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(date, fontSize = 12.sp, color = ShopizzoMidGray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("MK ${"%,.0f".format(order.totalAmount)}", fontWeight = FontWeight.ExtraBold, color = ShopizzoBlue)
                Surface(
                    color = when(order.orderStatus) {
                        "Delivered" -> ShopizzoSuccess.copy(0.1f)
                        "Cancelled" -> ShopizzoError.copy(0.1f)
                        else -> ShopizzoWarning.copy(0.1f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = order.orderStatus,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when(order.orderStatus) {
                            "Delivered" -> ShopizzoSuccess
                            "Cancelled" -> ShopizzoError
                            else -> Color(0xFFE65100) // Darker warning
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectedAccountsSection() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Connected Accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ShopizzoBlue)
            Spacer(Modifier.height(12.dp))
            
            AccountProviderRow("Google", true)
            AccountProviderRow("Email & Password", true)
            AccountProviderRow("Facebook", false)
        }
    }
}

@Composable
fun AccountProviderRow(provider: String, isConnected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when(provider) {
                "Google" -> Icons.Default.GTranslate // Placeholder for Google
                "Email & Password" -> Icons.Default.VpnKey
                else -> Icons.Default.Facebook
            },
            contentDescription = null,
            tint = if (isConnected) ShopizzoBlue else ShopizzoMidGray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(provider, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        if (isConnected) {
            Icon(Icons.Default.CheckCircle, null, tint = ShopizzoSuccess, modifier = Modifier.size(16.dp))
            Text(" Connected", color = ShopizzoSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        } else {
            Text("Not Connected", color = ShopizzoMidGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun SettingsSection(
    appTheme: String,
    biometricEnabled: Boolean,
    preferredAuth: String,
    onThemeChange: (String) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onAuthMethodChange: (String) -> Unit,
    onAboutClick: () -> Unit,
    onSetPin: () -> Unit
) {
    Column {
        Text("Preferences & Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ShopizzoBlue)
        Spacer(Modifier.height(8.dp))
        
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                ProfileOption(
                    icon = Icons.Default.Palette,
                    title = "Theme Mode",
                    trailing = { ThemeSelector(currentTheme = appTheme) { onThemeChange(it) } }
                ) {}
                
                ProfileOption(
                    icon = Icons.Default.Security,
                    title = "Payment Verification",
                    description = "Preferred: $preferredAuth",
                    trailing = { AuthMethodSelector(preferredAuth, onAuthMethodChange) }
                ) {}

                ProfileOption(
                    icon = Icons.Default.Lock,
                    title = "Change Payment PIN",
                    description = "Update your 4-digit security PIN"
                ) { onSetPin() }

                ProfileOption(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Login",
                    description = if (biometricEnabled) "Biometric login is enabled on this device." 
                                  else "Enable fingerprint, face unlock, or device authentication for faster login.",
                    trailing = {
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { onBiometricChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                                checkedTrackColor = ShopizzoBlue,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    },
                    onClick = { onBiometricChange(!biometricEnabled) }
                )
                
                ProfileOption(icon = Icons.Default.Info, title = "About Shopizzo") { onAboutClick() }
            }
        }
    }
}

@Composable
fun AuthMethodSelector(current: String, onSelected: (String) -> Unit) {
    Row {
        FilterChip(
            selected = current == "PIN",
            onClick = { onSelected("PIN") },
            label = { Text("PIN", fontSize = 10.sp) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ShopizzoBlue.copy(0.1f))
        )
        Spacer(Modifier.width(4.dp))
        FilterChip(
            selected = current == "BIOMETRIC",
            onClick = { onSelected("BIOMETRIC") },
            label = { Text("Biometric", fontSize = 10.sp) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ShopizzoBlue.copy(0.1f))
        )
    }
}

@Composable
fun PrivacySecuritySection(
    onLogoutDevices: () -> Unit
) {
    Column {
        Text("Privacy & Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ShopizzoBlue)
        Spacer(Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SecurityItem(Icons.Default.Lock, "Secure Account Protection", "Shopizzo protects your account using secure authentication methods to prevent unauthorized access.")
                SecurityItem(Icons.Default.EnhancedEncryption, "Data Encryption", "Your personal information and account data are protected using secure encryption methods.")
                SecurityItem(Icons.Default.VerifiedUser, "Personal Information Control", "You control your profile information, delivery addresses, and account preferences.")
                SecurityItem(Icons.Default.CreditCard, "Secure Payments", "Payment information is processed securely. Shopizzo does not store sensitive payment credentials.")
                SecurityItem(Icons.Default.LocationOn, "Location & Delivery Privacy", "Your delivery information is only used to complete and manage your orders.")
                SecurityItem(Icons.Default.Shield, "Device Security", "Biometric authentication helps provide an additional layer of security on supported devices.")
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                
                DataManagementRow("Logout from devices", onClick = onLogoutDevices)
            }
        }
    }
}

@Composable
fun SecurityItem(icon: ImageVector, title: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(description, fontSize = 13.sp, color = ShopizzoMidGray, lineHeight = 18.sp)
        }
    }
}

@Composable
fun DataManagementRow(label: String, isCritical: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (isCritical) ShopizzoError else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        Icon(Icons.Default.ChevronRight, null, tint = ShopizzoMidGray, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun EditProfileSection(
    editedName: String, onNameChange: (String) -> Unit,
    editedPhone: String, onPhoneChange: (String) -> Unit,
    editedCountry: String, onCountryChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    isLoading: Boolean
) {
    Column(modifier = Modifier.padding(20.dp)) {
        OutlinedTextField(
            value = editedName,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = shopizzoFieldColors()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = editedPhone,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = shopizzoFieldColors()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = editedCountry,
            onValueChange = onCountryChange,
            label = { Text("Country") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = shopizzoFieldColors()
        )
        Spacer(Modifier.height(32.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel") }
            ShopizzoButton(
                text = "Save Changes",
                onClick = onSave,
                modifier = Modifier.weight(1f),
                isLoading = isLoading
            )
        }
    }
}

@Composable
fun GuestProfilePlaceholder(
    onNavigateToLogin: () -> Unit,
    appTheme: String,
    authViewModel: AuthViewModel,
    onAboutClick: () -> Unit
) {
    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = ShopizzoLightGray) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(64.dp), tint = ShopizzoMidGray)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("You are not logged in", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Log in to access your profile, track orders, and manage your account.", color = ShopizzoMidGray, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
        Spacer(Modifier.height(32.dp))
        ShopizzoButton(text = "Log In", onClick = onNavigateToLogin)
        
        Spacer(Modifier.height(48.dp))
        SettingsSection(
            appTheme = appTheme,
            biometricEnabled = false,
            preferredAuth = "PIN",
            onThemeChange = { authViewModel.saveThemePreference(it) },
            onBiometricChange = { onNavigateToLogin() },
            onAuthMethodChange = { onNavigateToLogin() },
            onAboutClick = onAboutClick,
            onSetPin = { onNavigateToLogin() }
        )
    }
}

@Composable
fun ProfileOption(
    icon: ImageVector, 
    title: String, 
    description: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (description != null) {
                Text(text = description, color = ShopizzoMidGray, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(Icons.Default.ChevronRight, null, tint = ShopizzoMidGray)
        }
    }
}

@Composable
fun ThemeSelector(currentTheme: String, onThemeSelected: (String) -> Unit) {
    val options = listOf("LIGHT", "DARK", "SYSTEM")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = currentTheme == option,
                onClick = { onThemeSelected(option) },
                label = { Text(option.lowercase().capitalize(), fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ShopizzoBlue,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
