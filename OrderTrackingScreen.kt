package com.shopizzo.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopizzo.ui.components.ShopizzoTopBar
import com.shopizzo.data.model.Order
import com.shopizzo.ui.theme.*
import com.shopizzo.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    orderId: String,
    productViewModel: ProductViewModel,
    onBack: () -> Unit
) {
    val orders by productViewModel.userOrders.collectAsState()
    val order = orders.find { it.id == orderId }

    Scaffold(
        topBar = {
            ShopizzoTopBar(
                title = "Order Tracking",
                showBack = true,
                onBackClick = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (order == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ShopizzoBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Order Info Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Order Number", color = ShopizzoMidGray, fontSize = 12.sp)
                                Text("#${order.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Estimated Delivery", color = ShopizzoMidGray, fontSize = 12.sp)
                                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                val estDate = order.estimatedDeliveryDate?.toDate()?.let { sdf.format(it) } ?: "2-3 Business Days"
                                Text(estDate, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ShopizzoBlue)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Status Timeline
                Text("Order Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                val statuses = listOf(
                    "Pending" to Icons.Default.History,
                    "Confirmed" to Icons.Default.CheckCircle,
                    "Processing" to Icons.Default.Settings,
                    "Packed" to Icons.Default.Inventory,
                    "Shipped" to Icons.Default.LocalShipping,
                    "Out for Delivery" to Icons.Default.DeliveryDining,
                    "Delivered" to Icons.Default.Home
                )

                val currentStatusIndex = statuses.indexOfFirst { it.first == order.orderStatus }.coerceAtLeast(0)

                statuses.forEachIndexed { index, (status, icon) ->
                    val isCompleted = index <= currentStatusIndex
                    val isCurrent = index == currentStatusIndex
                    
                    TimelineItem(
                        status = status,
                        icon = icon,
                        isCompleted = isCompleted,
                        isCurrent = isCurrent,
                        isLast = index == statuses.size - 1,
                        date = if (isCompleted) {
                            if (isCurrent) "Updated: " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(order.lastUpdated.toDate())
                            else ""
                        } else ""
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Courier Info
                Text("Shipping Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).background(ShopizzoBlue.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocalShipping, null, tint = ShopizzoBlue)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(order.courier, fontWeight = FontWeight.Bold)
                            Text("Tracking: ${order.trackingNumber.ifEmpty { "Pending..." }}", fontSize = 12.sp, color = ShopizzoMidGray)
                        }
                        if (order.trackingNumber.isNotEmpty()) {
                            IconButton(onClick = { /* Copy tracking */ }) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(20.dp), tint = ShopizzoBlue)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun TimelineItem(
    status: String,
    icon: ImageVector,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLast: Boolean,
    date: String
) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (isCompleted) ShopizzoBlue else ShopizzoLightGray,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isCompleted) Color.White else ShopizzoMidGray
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(if (isCompleted) ShopizzoBlue else ShopizzoLightGray)
                )
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = status,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCompleted) MaterialTheme.colorScheme.onBackground else ShopizzoMidGray,
                fontSize = 15.sp
            )
            if (date.isNotEmpty()) {
                Text(text = date, fontSize = 12.sp, color = ShopizzoMidGray)
            }
        }
    }
}
