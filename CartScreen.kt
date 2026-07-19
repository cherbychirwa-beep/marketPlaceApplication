package com.shopizzo.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shopizzo.data.model.CartItem
import com.shopizzo.data.model.Product
import com.shopizzo.ui.components.ShopizzoButton
import com.shopizzo.ui.components.ShopizzoTopBar
import com.shopizzo.ui.theme.*
import com.shopizzo.viewmodel.AuthViewModel
import com.shopizzo.viewmodel.ProductViewModel
import androidx.compose.runtime.collectAsState

/**
 * Shopping Cart Screen — lists cart items with quantity steppers,
 * shows the live total, and proceeds to checkout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    productViewModel : ProductViewModel,
    authViewModel    : AuthViewModel,
    onBack           : () -> Unit,
    onFavouriteClick : () -> Unit,
    onCheckout       : () -> Unit
) {
    val cartItems by productViewModel.cartItems.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    val favouriteCount = userProfile?.favouriteIds?.size ?: 0
    val total = cartItems.sumOf { it.lineTotal }

    Scaffold(
        topBar = {
            ShopizzoTopBar(
                title = "My Cart",
                cartCount = cartItems.sumOf { it.quantity },
                favouriteCount = favouriteCount,
                showBack = true,
                isLoggedIn = authViewModel.isLoggedIn,
                onBackClick = onBack,
                onCartClick = {},
                onFavouriteClick = onFavouriteClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text  = "MK ${"%,.0f".format(total)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color      = ShopizzoBlue,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        ShopizzoButton(text = "Proceed to Checkout", onClick = onCheckout)
                    }
                }
            }
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        tint     = ShopizzoLightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Your cart is empty", color = ShopizzoMidGray)
                }
            }
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentPadding  = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cartItems, key = { it.product.id }) { item ->
                    CartItemRow(
                        item        = item,
                        onIncrease  = { productViewModel.addToCart(item.product, userProfile?.uid) },
                        onDecrease  = { productViewModel.removeFromCart(item.product.id, userProfile?.uid) },
                        onDelete    = { productViewModel.deleteFromCart(item.product.id, userProfile?.uid) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item       : CartItem,
    onIncrease : () -> Unit,
    onDecrease : () -> Unit,
    onDelete   : () -> Unit
) {
    Card(
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model              = item.product.imageUrl,
                contentDescription = item.product.name,
                contentScale       = ContentScale.Crop,
                placeholder        = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                error              = androidx.compose.ui.res.painterResource(android.R.drawable.ic_dialog_alert),
                modifier           = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = item.product.name,
                    style    = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "MK ${"%,.0f".format(item.product.price)}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = ShopizzoBlue)
                )
            }
            // ── Quantity stepper ────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick  = onDecrease,
                    modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) { Icon(Icons.Filled.Remove, "Decrease", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }

                Text(
                    text     = "${item.quantity}",
                    modifier = Modifier.padding(horizontal = 10.dp),
                    style    = MaterialTheme.typography.titleMedium
                )

                IconButton(
                    onClick  = onIncrease,
                    modifier = Modifier.size(28.dp).background(ShopizzoBlue, CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Add, "Increase",
                        tint     = ShopizzoWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.DeleteOutline, "Remove", tint = ShopizzoError)
            }
        }
    }
}
