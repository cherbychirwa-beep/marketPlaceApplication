package com.shopizzo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopizzo.ui.components.ShopizzoTopBar
import com.shopizzo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            ShopizzoTopBar(
                title = "About Shopizzo",
                showBack = true,
                onBackClick = onBack,
                onNotificationClick = {}
            )
        },
        containerColor = ShopizzoBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(ShopizzoBlue, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("S", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Shopizzo",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ShopizzoWhite
            )

            Text(
                text = "v1.0.0",
                color = ShopizzoMidGray,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Shopizzo is your premium destination for high-quality electronics.",
                textAlign = TextAlign.Center,
                color = ShopizzoWhite,
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(48.dp))

            AboutSection("Developer Information", "Developed by Shopizzo Tech Team")
            AboutSection("Contact Information", "support@shopizzo.com")

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun AboutSection(title: String, content: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(text = title, color = ShopizzoBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(text = content, color = ShopizzoWhite, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = ShopizzoDarkGray)
    }
}
