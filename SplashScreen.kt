package com.shopizzo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.shopizzo.viewmodel.AuthViewModel
import com.shopizzo.ui.theme.ShopizzoBlue
import kotlinx.coroutines.delay


// screen shown briefly at app launch.


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplashScreen(
    isLoggedIn : Boolean,
    viewModel  : AuthViewModel,
    onFinished : () -> Unit
) {
    //  Animation state
    val scale by animateFloatAsState(
        targetValue  = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fade in the logo
        alpha.animateTo(
            targetValue   = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        // Hold on screen to reach total ~3 seconds
        delay(2200)
        // Navigate away
        onFinished()
    }

    // ── Full-screen themed background ───────────────────────────────────────
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier          = Modifier
                .alpha(alpha.value)
                .scale(scale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── App icon area ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = ShopizzoBlue,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("S", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── App name: "Welcome To Shopizzo" ─────────────────────────────
            Text(
                text = buildAnnotatedString {
                    if (isLoggedIn) {
                        val nameToShow = viewModel.currentUserName ?: ""
                        val firstName = nameToShow.split(" ").firstOrNull() ?: ""
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)) {
                            append("Welcome back, ")
                        }
                        if (firstName.isNotEmpty()) {
                            withStyle(SpanStyle(color = ShopizzoBlue, fontWeight = FontWeight.ExtraBold)) {
                                append(firstName)
                            }
                        } else {
                            withStyle(SpanStyle(color = ShopizzoBlue, fontWeight = FontWeight.ExtraBold)) {
                                append("User")
                            }
                        }
                    } else {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)) {
                            append("Welcome To ")
                        }
                        withStyle(SpanStyle(color = ShopizzoBlue, fontWeight = FontWeight.ExtraBold)) {
                            append("Shopizzo")
                        }
                    }
                },
                fontSize = 30.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Tagline ──────────────────────────────────────────────────────
            Text(
                text       = "Your Trusted Plug for Electronic Products",
                fontSize   = 14.sp,
                color      = ShopizzoBlue,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Loading indicator ────────────────────────────────────────────
            CircularProgressIndicator(
                color    = ShopizzoBlue,
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp
            )
        }
    }
}
