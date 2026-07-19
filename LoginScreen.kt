package com.shopizzo.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.shopizzo.ui.components.ShopizzoButton
import com.shopizzo.ui.components.shopizzoFieldColors
import com.shopizzo.ui.theme.*
import com.shopizzo.viewmodel.AuthUiState
import com.shopizzo.viewmodel.AuthViewModel

enum class LoginType { EMAIL, PHONE, OTP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotClick: () -> Unit
) {
    var loginType by remember { mutableStateOf(LoginType.EMAIL) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val authState by viewModel.authState.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    var showBiometricDialog by remember { mutableStateOf(false) }

    // --- Google Sign In Logic ---
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1093346426970-qdibirjh7gusmerda6ran7qb0lmfbacg.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    viewModel.signInWithGoogle(token)
                }
            } catch (e: ApiException) {
                // Handle error
            }
        }
    }


    // Biometric logic
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val biometricPrompt = remember {
        BiometricPrompt(
            context as FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.login("demo@shopizzo.app", "biometric_pass") // Simulated for now
                }
            }
        )
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric Login")
        .setSubtitle("Log in using your biometric credential")
        .setNegativeButtonText("Cancel")
        .build()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> {
                snackbarHost.showSnackbar((authState as AuthUiState.Success).message)
                if (!biometricEnabled && loginType == LoginType.EMAIL) {
                    showBiometricDialog = true
                } else {
                    viewModel.resetState()
                    onLoginSuccess()
                }
            }
            is AuthUiState.Error -> {
                snackbarHost.showSnackbar((authState as AuthUiState.Error).message)
                viewModel.resetState()
            }
            is AuthUiState.OtpSent -> {
                loginType = LoginType.OTP
                viewModel.resetState()
            }
            else -> {}
        }
    }

    if (showBiometricDialog) {
        AlertDialog(
            onDismissRequest = { 
                showBiometricDialog = false
                viewModel.resetState()
                onLoginSuccess()
            },
            title = { Text("Enable Biometric Login", fontWeight = FontWeight.Bold) },
            text = { Text("Would you like to enable fingerprint or face unlock for faster sign-in on this device?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveBiometricPreference(true)
                        showBiometricDialog = false
                        viewModel.resetState()
                        onLoginSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShopizzoBlue)
                ) {
                    Text("Enable", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBiometricDialog = false
                        viewModel.resetState()
                        onLoginSuccess()
                    }
                ) {
                    Text("Not Now", color = ShopizzoMidGray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            
            // Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = ShopizzoBlue,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("S", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Welcome Back",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Discover the latest in premium electronics",
                fontSize = 14.sp,
                color = ShopizzoMidGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(32.dp))

            // Switch to code login link
            if (loginType == LoginType.EMAIL) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "Sign in with a code",
                        color = ShopizzoBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { loginType = LoginType.PHONE }
                    )
                }
            } else if (loginType == LoginType.PHONE) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "Sign in with email",
                        color = ShopizzoBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { loginType = LoginType.EMAIL }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when (loginType) {
                LoginType.EMAIL -> {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = shopizzoFieldColors()
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = shopizzoFieldColors()
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            "Forgot password?",
                            color = ShopizzoBlue,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .clickable { onForgotClick() }
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    ShopizzoButton(
                        text = "Continue",
                        onClick = { viewModel.login(email, password) },
                        isLoading = authState is AuthUiState.Loading
                    )
                }
                LoginType.PHONE -> {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone number") },
                        placeholder = { Text("+265...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = shopizzoFieldColors()
                    )
                    Spacer(Modifier.height(32.dp))
                    ShopizzoButton(
                        text = "Get Verification Code",
                        onClick = { viewModel.startPhoneLogin(phoneNumber, context as Activity) },
                        isLoading = authState is AuthUiState.Loading
                    )
                }
                LoginType.OTP -> {
                    Text(
                        "Enter the 6-digit code sent to $phoneNumber",
                        textAlign = TextAlign.Center,
                        color = ShopizzoMidGray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it },
                        label = { Text("Verification Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = shopizzoFieldColors()
                    )
                    Spacer(Modifier.height(32.dp))
                    ShopizzoButton(
                        text = "Verify & Login",
                        onClick = { viewModel.verifyOtp(otpCode) },
                        isLoading = authState is AuthUiState.Loading
                    )
                    TextButton(onClick = { loginType = LoginType.PHONE }) {
                        Text("Change phone number", color = ShopizzoBlue)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Biometric Option
            if (biometricEnabled && loginType == LoginType.EMAIL) {
                OutlinedButton(
                    onClick = { biometricPrompt.authenticate(promptInfo) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ShopizzoBlue)
                ) {
                    Icon(Icons.Filled.Fingerprint, null, tint = ShopizzoBlue)
                    Spacer(Modifier.width(8.dp))
                    Text("Use Fingerprint / Face Unlock", color = ShopizzoBlue)
                }
                Spacer(Modifier.height(24.dp))
            }

            // OR Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(" OR ", modifier = Modifier.padding(horizontal = 8.dp), color = ShopizzoMidGray, fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Social Logins (Google Sign In)
            OutlinedButton(
                onClick = { launcher.launch(googleSignInClient.signInIntent) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(com.google.android.gms.base.R.drawable.googleg_standard_color_18),
                        contentDescription = "Google",
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Continue with Google", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(40.dp))

            Row {
                Text("New to Shopizzo? ", color = ShopizzoMidGray)
                Text(
                    "Create an account",
                    color = ShopizzoBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onRegisterClick() }
                )
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SocialIcon(painter: androidx.compose.ui.graphics.painter.Painter, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
        modifier = Modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(painter, null, modifier = Modifier.size(24.dp))
        }
    }
}
