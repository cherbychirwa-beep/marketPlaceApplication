package com.shopizzo.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shopizzo.data.model.Product
import com.shopizzo.ui.theme.*

// ─── Product Card ─────────────────────────────────────────────────────────────

@Composable
fun ProductCard(
    product     : Product,
    isFavourite : Boolean,
    onCardClick : () -> Unit,
    onLikeClick : () -> Unit,
    onAddToCart : () -> Unit,
    onBuyNow    : () -> Unit = {},
    modifier    : Modifier = Modifier
) {
    Card(
        modifier  = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                AsyncImage(
                    model              = product.imageUrl,
                    contentDescription = product.name,
                    contentScale       = ContentScale.Crop,
                    placeholder        = painterResource(android.R.drawable.ic_menu_gallery),
                    error              = painterResource(android.R.drawable.ic_dialog_alert),
                    modifier           = Modifier.fillMaxSize().clip(
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                )
                
                // Badge Column (Discount + New)
                Column(modifier = Modifier.align(Alignment.TopStart)) {
                    // Discount Badge
                    if (product.discountPercentage != null) {
                        Surface(
                            color = Color.Red,
                            shape = RoundedCornerShape(bottomEnd = 8.dp)
                        ) {
                            Text(
                                text = "-${product.discountPercentage}%",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // New Badge
                    val isNew = remember(product.createdAt) {
                        product.createdAt?.let {
                            val threshold = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                            it.toDate().time > threshold
                        } ?: false
                    }
                    if (isNew) {
                        Surface(
                            color = ShopizzoBlue,
                            shape = RoundedCornerShape(bottomEnd = 8.dp, topEnd = if (product.discountPercentage != null) 0.dp else 0.dp),
                            modifier = Modifier.padding(top = if (product.discountPercentage != null) 2.dp else 0.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick  = onLikeClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavourite) ShopizzoError else ShopizzoMidGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text     = product.name,
                    style    = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = product.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = ShopizzoMidGray
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = ShopizzoStar, modifier = Modifier.size(12.dp))
                    Text(
                        text = " ${product.rating} (${product.numberOfReviews})",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = ShopizzoMidGray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text  = "MK ${"%,.0f".format(product.price)}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color      = ShopizzoBlue,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        if (product.oldPrice != null) {
                            Text(
                                text = "MK ${"%,.0f".format(product.oldPrice)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                ),
                                color = ShopizzoMidGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onAddToCart,
                        modifier = Modifier.size(32.dp).background(ShopizzoBlue, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Filled.AddShoppingCart, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ─── Star Rating Row ──────────────────────────────────────────────────────────

@Composable
fun StarRatingRow(
    rating      : Double,
    ratingCount : Int,
    modifier    : Modifier = Modifier
) {
    Row(
        modifier          = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            Icon(
                imageVector = when {
                    index < rating.toInt()                            -> Icons.Filled.Star
                    index < rating.toInt() + 1 && rating % 1 >= 0.5 -> Icons.Filled.StarHalf
                    else                                              -> Icons.Filled.StarOutline
                },
                contentDescription = null,
                tint     = ShopizzoStar,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text  = "($ratingCount)",
            style = MaterialTheme.typography.bodySmall.copy(color = ShopizzoMidGray, fontSize = 10.sp)
        )
    }
}

// ─── Interactive Star Rating Picker ───────────────────────────────────────────

@Composable
fun StarRatingPicker(
    selectedRating : Int,
    onRatingChange : (Int) -> Unit,
    modifier       : Modifier = Modifier
) {
    Row(modifier = modifier) {
        (1..5).forEach { star ->
            Icon(
                imageVector = if (star <= selectedRating) Icons.Filled.Star else Icons.Filled.StarOutline,
                contentDescription = "$star stars",
                tint     = ShopizzoStar,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onRatingChange(star) }
                    .padding(2.dp)
            )
        }
    }
}

// ─── Primary Blue Button ──────────────────────────────────────────────────────

@Composable
fun ShopizzoButton(
    text     : String,
    onClick  : () -> Unit,
    modifier : Modifier  = Modifier,
    enabled  : Boolean   = true,
    isLoading: Boolean   = false
) {
    Button(
        onClick  = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled  = enabled && !isLoading,
        shape    = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ShopizzoBlue,
            contentColor   = Color.White
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color    = Color.White,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text  = text,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)
            )
        }
    }
}

// ─── Outlined Secondary Button ────────────────────────────────────────────────

@Composable
fun ShopizzoOutlinedButton(
    text    : String,
    onClick : () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick  = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = ButtonDefaults.outlinedButtonColors(contentColor = ShopizzoBlue),
        border   = androidx.compose.foundation.BorderStroke(1.5.dp, ShopizzoBlue)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp))
    }
}

// ─── Top App Bar ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopizzoTopBar(
    title          : String,
    cartCount      : Int    = 0,
    favouriteCount : Int    = 0,
    showBack       : Boolean = false,
    isLoggedIn     : Boolean = false,
    onBackClick    : () -> Unit = {},
    onCartClick    : () -> Unit = {},
    onFavouriteClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    scrollBehavior : TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Shopizzo Brand Logo Icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = ShopizzoBlue,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text  = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            // Favourites badge (Visible to all, but logic handled in screens)
            BadgedBox(
                badge = {
                    if (isLoggedIn && favouriteCount > 0) Badge(containerColor = ShopizzoError, contentColor = Color.White) { 
                        Text(favouriteCount.toString()) 
                    }
                }
            ) {
                IconButton(onClick = onFavouriteClick) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Favourites", tint = ShopizzoError)
                }
            }

            IconButton(onClick = onNotificationClick) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = ShopizzoBlue)
            }

            // Cart icon with badge
            BadgedBox(
                badge = {
                    if (cartCount > 0) Badge(containerColor = ShopizzoError, contentColor = Color.White) {
                        Text(cartCount.toString()) 
                    }
                },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                IconButton(onClick = onCartClick) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", tint = ShopizzoBlue)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor    = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = ShopizzoBlue
        ),
        scrollBehavior = scrollBehavior
    )
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title    : String,
    onSeeAll : (() -> Unit)? = null,
    modifier : Modifier      = Modifier
) {
    Row(
        modifier              = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        if (onSeeAll != null) {
            Text(
                text     = "See All",
                style    = MaterialTheme.typography.labelLarge.copy(color = ShopizzoBlue),
                modifier = Modifier.clickable { onSeeAll() }
            )
        }
    }
}

// ─── Brand Badge ──────────────────────────────────────────────────────────────

@Composable
fun BrandBadge(brand: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = brand,
            style = MaterialTheme.typography.labelLarge.copy(color = ShopizzoBlue)
        )
    }
}

@Composable
fun CategorySelector(categories: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        items(categories) { category ->
            val isSelected = (category == "All" && selected == null) || (selected == category)
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onSelect(if (category == "All") null else category) },
                color = if (isSelected) ShopizzoBlue else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ShopizzoBlue.copy(0.2f))
            ) {
                Text(category, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun shopizzoFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = ShopizzoBlue,
    unfocusedBorderColor    = Color(0xFFBDBDBD),
    focusedLabelColor       = ShopizzoBlue,
    cursorColor             = ShopizzoBlue,
    focusedContainerColor   = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedTextColor        = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor      = MaterialTheme.colorScheme.onSurface
)

@Composable
fun ShopizzoSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search products...", color = ShopizzoMidGray, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ShopizzoBlue) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = null, tint = ShopizzoMidGray)
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ShopizzoBlue,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            cursorColor = ShopizzoBlue
        )
    )
}

@Composable
fun BrandSlider(brands: List<String>, onBrandClick: (String) -> Unit) {
    Column {
        SectionHeader(title = "Featured Brands")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(brands) { brand ->
                Card(
                    modifier = Modifier
                        .size(width = 100.dp, height = 60.dp)
                        .clickable { onBrandClick(brand) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = brand,
                            fontWeight = FontWeight.Bold,
                            color = ShopizzoBlue,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

suspend fun SnackbarHostState.showInfo(message: String) {
    showSnackbar(message = message, duration = SnackbarDuration.Short)
}

// ─── Authentication Prompt ───────────────────────────────────────────────────

@Composable
fun AuthPrompt(
    show        : Boolean,
    onDismiss   : () -> Unit,
    onSignIn    : () -> Unit,
    onRegister  : () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ShopizzoBlue)
                    Spacer(Modifier.width(12.dp))
                    Text("Sign in required", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Please sign in or create an account to continue with this feature.", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = onSignIn,
                    colors = ButtonDefaults.buttonColors(containerColor = ShopizzoBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                TextButton(onClick = onRegister) {
                    Text("Create Account", color = ShopizzoBlue)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
