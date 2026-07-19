package com.shopizzo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopizzo.ui.components.ShopizzoButton
import com.shopizzo.ui.components.ShopizzoTopBar
import com.shopizzo.ui.components.shopizzoFieldColors
import com.shopizzo.ui.theme.ShopizzoBlue
import com.shopizzo.ui.theme.ShopizzoMidGray
import com.shopizzo.viewmodel.AuthUiState
import com.shopizzo.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()

    Scaffold(
        topBar = {
            ShopizzoTopBar(
                title = "Reset Password",
                showBack = true,
                onBackClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Forgot Your Password?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = "Enter your email address and we'll send you a link to reset your password.",
                color = ShopizzoMidGray,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Email, null, tint = ShopizzoBlue) },
                shape = RoundedCornerShape(12.dp),
                colors = shopizzoFieldColors()
            )

            when (authState) {
                is AuthUiState.Error -> {
                    Text(
                        text = (authState as AuthUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                is AuthUiState.Success -> {
                    Text(
                        text = (authState as AuthUiState.Success).message,
                        color = ShopizzoBlue,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                else -> {}
            }

            Spacer(Modifier.height(32.dp))

            ShopizzoButton(
                text = "Send Reset Link",
                isLoading = authState is AuthUiState.Loading,
                onClick = { authViewModel.sendPasswordReset(email) }
            )
        }
    }
}
