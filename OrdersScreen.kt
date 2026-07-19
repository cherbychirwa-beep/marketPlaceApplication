package com.shopizzo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopizzo.data.model.Order
import com.shopizzo.ui.components.ShopizzoTopBar
import com.shopizzo.ui.theme.*
import com.shopizzo.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    productViewModel: ProductViewModel,
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit
) {
    val orders by productViewModel.userOrders.collectAsState()

    Scaffold(
        topBar = {
            ShopizzoTopBar(
                title = "My Orders",
                showBack = true,
                onBackClick = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(80.dp), tint = ShopizzoLightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("No orders yet", color = ShopizzoMidGray, fontSize = 18.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    OrderListItem(order = order) { onOrderClick(order.id) }
                }
            }
        }
    }
}

@Composable
fun OrderListItem(order: Order, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val date = sdf.format(order.orderDate.toDate())

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = "Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = date, fontSize = 12.sp, color = ShopizzoMidGray)
                }
                StatusBadge(status = order.orderStatus)
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${order.products.sumOf { it.quantity }} items", fontSize = 14.sp, color = ShopizzoMidGray)
                Text(
                    text = "MK ${"%,.0f".format(order.totalAmount)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = ShopizzoBlue,
                    fontSize = 16.sp
                )
            }
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalShipping, null, tint = ShopizzoBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = order.courier, fontSize = 13.sp, color = ShopizzoBlue, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null, tint = ShopizzoMidGray)
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "Delivered" -> ShopizzoSuccess
        "Cancelled", "Refunded" -> ShopizzoError
        "Pending" -> ShopizzoWarning
        "Confirmed", "Processing", "Packed" -> ShopizzoBlue
        "Shipped", "Out for Delivery" -> Color(0xFF9C27B0) // Purple
        else -> ShopizzoMidGray
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
