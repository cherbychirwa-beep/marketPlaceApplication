package com.shopizzo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.shopizzo.data.model.Product
import com.shopizzo.data.model.UserProfile
import com.shopizzo.ui.components.*
import com.shopizzo.ui.theme.*
import com.shopizzo.viewmodel.AuthViewModel
import com.shopizzo.viewmodel.DetailUiState
import com.shopizzo.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId        : String,
    productViewModel : ProductViewModel,
    authViewModel    : AuthViewModel,
    cartCount        : Int,
    onBack           : () -> Unit,
    onCartClick      : () -> Unit,
    onFavouriteClick : () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val detailState  by productViewModel.detailState.collectAsState()
    val userProfile  by authViewModel.userProfile.collectAsState()
    val favouriteCount = userProfile?.favouriteIds?.size ?: 0
    val ratingMsg    by productViewModel.snackbarMessage.collectAsState(initial = null)
    val snackbarHost = remember { SnackbarHostState() }
    var pickedStars  by remember { mutableStateOf(0) }
    
    val isLoggedIn = authViewModel.isLoggedIn
    var showAuthPrompt by remember { mutableStateOf(false) }

    fun checkAuthAndExecute(action: () -> Unit) {
        if (isLoggedIn) {
            action()
        } else {
            authViewModel.setPendingAction(action)
            showAuthPrompt = true
        }
    }

    LaunchedEffect(productId) {
        productViewModel.loadProductById(productId)
        pickedStars = 0
    }

    LaunchedEffect(ratingMsg) {
        ratingMsg?.let {
            snackbarHost.showSnackbar(it)
            productViewModel.clearRatingMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            ShopizzoTopBar(
                title       = "Product Details",
                cartCount   = cartCount,
                favouriteCount = favouriteCount,
                showBack    = true,
                isLoggedIn  = isLoggedIn,
                onBackClick = onBack,
                onCartClick = onCartClick,
                onFavouriteClick = { checkAuthAndExecute(onFavouriteClick) },
                onNotificationClick = { /* Navigate in MainActivity */ }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val state = detailState) {
            is DetailUiState.Loading, DetailUiState.Idle -> Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = ShopizzoBlue) }

            is DetailUiState.Error -> Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(state.message, color = ShopizzoError) }

            is DetailUiState.Success -> {
                val product = state.product
                val allImages = listOf(product.imageUrl) + product.additionalImages
                val pagerState = rememberPagerState(pageCount = { allImages.size })

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Image Carousel
                    Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model              = allImages[page],
                                contentDescription = product.name,
                                contentScale       = ContentScale.Crop,
                                placeholder        = painterResource(android.R.drawable.ic_menu_gallery),
                                error              = painterResource(android.R.drawable.ic_dialog_alert),
                                modifier           = Modifier.fillMaxSize()
                            )
                        }
                        
                        // Pager Indicator
                        if (allImages.size > 1) {
                            Row(
                                Modifier
                                    .height(50.dp)
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(allImages.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) ShopizzoBlue else Color.LightGray
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(8.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick  = { checkAuthAndExecute { authViewModel.toggleFavourite(product.id) } },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(50))
                        ) {
                            val isFav = userProfile?.favouriteIds?.contains(product.id) == true
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isFav) ShopizzoError else ShopizzoMidGray
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(20.dp)) {
                        Row {
                            Text(text = product.category, color = ShopizzoBlue, style = MaterialTheme.typography.labelLarge)
                            Text(text = "  •  ${product.brand}", color = ShopizzoMidGray, style = MaterialTheme.typography.labelLarge)
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(text = product.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

                        Spacer(Modifier.height(8.dp))

                        // Enhanced Rating UI
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StarRatingRow(rating = product.rating, ratingCount = product.numberOfReviews)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${product.rating} out of 5",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Based on ${product.numberOfReviews} customer reviews",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShopizzoMidGray
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text  = "MK ${"%,.0f".format(product.price)}",
                            style = MaterialTheme.typography.headlineLarge.copy(color = ShopizzoBlue, fontWeight = FontWeight.ExtraBold)
                        )

                        Text(
                            text  = if (product.stockQuantity > 0) "${product.stockQuantity} in stock" else "Out of stock",
                            color = if (product.stockQuantity > 0) ShopizzoSuccess else ShopizzoError,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(24.dp))

                        // Product Video Section
                        if (!product.productVideoUrl.isNullOrBlank()) {
                            Text(text = "Product Video", style = MaterialTheme.typography.titleMedium, color = ShopizzoBlue)
                            Spacer(Modifier.height(12.dp))
                            VideoPlayer(videoUrl = product.productVideoUrl)
                            Spacer(Modifier.height(24.dp))
                        }

                        Text(text = "Description", style = MaterialTheme.typography.titleMedium, color = ShopizzoBlue)
                        Spacer(Modifier.height(8.dp))
                        Text(text = product.description, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground))

                        // Technical Specifications
                        if (product.specifications.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            Text(text = "Technical Specifications", style = MaterialTheme.typography.titleMedium, color = ShopizzoBlue)
                            Spacer(Modifier.height(8.dp))
                            product.specifications.forEach { (key, value) ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(text = key, modifier = Modifier.weight(1f), color = ShopizzoMidGray, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = value, modifier = Modifier.weight(1.5f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Text(text = "Rate this product", style = MaterialTheme.typography.titleMedium, color = ShopizzoBlue)
                        Spacer(Modifier.height(8.dp))
                        StarRatingPicker(
                            selectedRating = pickedStars,
                            onRatingChange = { stars ->
                                checkAuthAndExecute {
                                    pickedStars = stars
                                    productViewModel.rateProduct(product, userProfile?.uid ?: "GUEST", stars)
                                }
                            }
                        )

                        Spacer(Modifier.height(32.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ShopizzoOutlinedButton(
                                text     = "Add to Cart",
                                onClick  = { checkAuthAndExecute { productViewModel.addToCart(product, userProfile?.uid) } },
                                modifier = Modifier.weight(1f)
                            )
                            ShopizzoButton(
                                text     = "Buy Now",
                                onClick  = {
                                    checkAuthAndExecute {
                                        productViewModel.addToCart(product, userProfile?.uid)
                                        onCartClick()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(videoUrl: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
