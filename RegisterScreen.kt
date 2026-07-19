package com.shopizzo.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.shopizzo.ui.components.ShopizzoButton
import com.shopizzo.ui.components.shopizzoFieldColors
import com.shopizzo.ui.theme.ShopizzoBlue
import com.shopizzo.ui.theme.ShopizzoMidGray
import com.shopizzo.ui.screens.CountryInfo
import com.shopizzo.ui.screens.shopizzoCountriesList
import com.shopizzo.viewmodel.AuthUiState
import com.shopizzo.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<CountryInfo>(com.shopizzo.ui.screens.shopizzoCountriesList[0]) }
    var expandedCode by remember { mutableStateOf(false) }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var agreeToTerms by remember { mutableStateOf(false) }
    var countrySearchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(countrySearchQuery) {
        if (countrySearchQuery.isEmpty()) {
            com.shopizzo.ui.screens.shopizzoCountriesList
        } else {
            com.shopizzo.ui.screens.shopizzoCountriesList.filter { country: CountryInfo ->
                country.name.contains(countrySearchQuery, ignoreCase = true) || 
                country.code.contains(countrySearchQuery)
            }
        }
    }

    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val authState by viewModel.authState.collectAsState()

    // Google Sign In
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
    // ----------------------------

    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> {
                snackbarHost.showSnackbar((authState as AuthUiState.Success).message)
                viewModel.resetState()
                onSuccess()
            }
            is AuthUiState.Error -> {
                snackbarHost.showSnackbar((authState as AuthUiState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
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
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = "Get Started",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Experience the future of electronics shopping",
                color = ShopizzoMidGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = shopizzoFieldColors()
            )
            
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = shopizzoFieldColors()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                leadingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(start = 12.dp, end = 8.dp)
                            .clickable { expandedCode = true }
                    ) {
                        Text(
                            text = "${selectedCountry.flag} ${selectedCountry.code}",
                            fontWeight = FontWeight.Bold,
                            color = ShopizzoBlue
                        )
                        Icon(Icons.Filled.ArrowDropDown, null, tint = ShopizzoBlue)
                        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = shopizzoFieldColors()
            )

            if (expandedCode) {
                ModalBottomSheet(
                    onDismissRequest = { expandedCode = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Select Country",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        OutlinedTextField(
                            value = countrySearchQuery,
                            onValueChange = { countrySearchQuery = it },
                            placeholder = { Text("Search country or code") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            shape = RoundedCornerShape(12.dp),
                            colors = shopizzoFieldColors(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            this.items(filteredCountries) { countryItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCountry = countryItem
                                            expandedCode = false
                                            countrySearchQuery = ""
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(countryItem.flag, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                                    Text(countryItem.name, modifier = Modifier.weight(1f), fontSize = 16.sp)
                                    Text(countryItem.code, color = ShopizzoBlue, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }

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

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = shopizzoFieldColors()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreeToTerms,
                    onCheckedChange = { agreeToTerms = it },
                    colors = CheckboxDefaults.colors(checkedColor = ShopizzoBlue)
                )
                Text(
                    text = buildAnnotatedString {
                        append("I agree to the ")
                        withStyle(SpanStyle(color = ShopizzoBlue, fontWeight = FontWeight.Bold)) {
                            append("Terms & Conditions")
                        }
                        append(" and ")
                        withStyle(SpanStyle(color = ShopizzoBlue, fontWeight = FontWeight.Bold)) {
                            append("Privacy Policy")
                        }
                    },
                    fontSize = 12.sp,
                    color = ShopizzoMidGray,
                    modifier = Modifier.clickable { agreeToTerms = !agreeToTerms }
                )
            }

            Spacer(Modifier.height(32.dp))

            ShopizzoButton(
                text = "Create Account",
                enabled = agreeToTerms && fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                onClick = { 
                    if (password == confirmPassword) {
                        viewModel.register(
                            email = email,
                            password = password,
                            fullName = fullName,
                            phoneNumber = "${selectedCountry.code}$phoneNumber",
                            country = selectedCountry.name
                        )
                    } else {
                        // Error logic already handled by validation in a real app
                    }
                },
                isLoading = authState is AuthUiState.Loading
            )

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(" OR ", modifier = Modifier.padding(horizontal = 8.dp), color = ShopizzoMidGray, fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

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
                Text("Already have an account? ", color = ShopizzoMidGray)
                Text(
                    "Sign In",
                    color = ShopizzoBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onLoginClick() }
                )
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

