package com.shopizzo.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shopizzo.data.model.Product
import com.shopizzo.data.model.UserProfile
import com.shopizzo.ui.components.AuthPrompt
import com.shopizzo.ui.components.BrandSlider
import com.shopizzo.ui.components.CategorySelector
import com.shopizzo.ui.components.ProductCard
import com.shopizzo.ui.components.SectionHeader
import com.shopizzo.ui.components.ShopizzoSearchBar
import com.shopizzo.ui.components.ShopizzoTopBar
import com.shopizzo.ui.theme.ShopizzoBlue
import com.shopizzo.ui.theme.ShopizzoMidGray
import com.shopizzo.viewmodel.AuthViewModel
import com.shopizzo.viewmodel.ProductViewModel
import com.shopizzo.viewmodel.ProductsUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel    : AuthViewModel,
    productViewModel : ProductViewModel,
    cartCount        : Int,
    onProductClick   : (String) -> Unit,
    onCartClick      : () -> Unit,
    onFavouriteClick : () -> Unit,
    onSeeAllProducts : () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val featuredState by productViewModel.featuredState.collectAsState()
    val productsState by productViewModel.productsState.collectAsState()
    val selectedCategory by productViewModel.selectedCategory.collectAsState()
    val searchQuery by productViewModel.searchQuery.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    
    val favouriteCount = userProfile?.favouriteIds?.size ?: 0

    val scope         = rememberCoroutineScope()
    val isLoggedIn = authViewModel.isLoggedIn

    var isRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()

    var showAuthPrompt by remember { mutableStateOf(false) }

    fun checkAuthAndExecute(action: () -> Unit) {
        if (isLoggedIn) action() else {
            authViewModel.setPendingAction(action)
            showAuthPrompt = true
        }
    }

    val categories = listOf("All", "Smartphones", "Laptops", "Tablets", "Smart Watches", "Headphones", "Gaming Accessories")
    val brands = listOf("Samsung", "Apple", "Sony", "Dell", "LG", "HP", "Asus", "Xiaomi", "Logitech")

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                ShopizzoTopBar(
                    title = "Shopizzo",
                    cartCount = cartCount,
                    favouriteCount = favouriteCount,
                    isLoggedIn = isLoggedIn,
                    onCartClick = onCartClick,
                    onFavouriteClick = { checkAuthAndExecute(onFavouriteClick) },
                    onNotificationClick = { checkAuthAndExecute(onNotificationClick) }
                )
                
                ShopizzoSearchBar(
                    query = searchQuery,
                    onQueryChange = { productViewModel.updateSearchQuery(it) }
                )

                if (isLoggedIn) {
                    val nameToShow = authViewModel.currentUserName ?: "User"
                    val firstName = nameToShow.split(" ").firstOrNull() ?: ""
                    Text("Hi, $firstName 👋", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = ShopizzoBlue))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = refreshState,
            onRefresh = { scope.launch { isRefreshing = true; delay(1500); isRefreshing = false } },
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // 1. Featured Auto-Slider
                if (searchQuery.isEmpty()) {
                    item {
                        FeaturedAutoSlider(featuredState, onProductClick)
                    }
                }

                // 2. Category Selector
                item {
                    CategorySelector(categories, selectedCategory) { productViewModel.updateCategory(it) }
                }

                // 3. Featured Brands
                if (searchQuery.isEmpty()) {
                    item {
                        BrandSlider(brands) { brand -> productViewModel.updateSearchQuery(brand) }
                    }
                }

                // 4. Dynamic Products by Category
                when (val state = productsState) {
                    is ProductsUiState.Loading -> item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ShopizzoBlue) } }
                    is ProductsUiState.Error -> item { Text(state.message, modifier = Modifier.padding(16.dp), color = Color.Red) }
                    is ProductsUiState.Success -> {
                        val filtered = productViewModel.filteredProducts()
                        
                        if (selectedCategory == null || (selectedCategory == "All")) {
                            // Group and show all categories
                            val groups = filtered.groupBy { it.category }
                            groups.forEach { (catName, categoryProducts) ->
                                item { SectionHeader(title = catName) }
                                items(categoryProducts.chunked(2)) { row ->
                                    ProductRow(
                                        row = row,
                                        favouriteIds = userProfile?.favouriteIds ?: emptyList(),
                                        authViewModel = authViewModel,
                                        productViewModel = productViewModel,
                                        onProductClick = onProductClick,
                                        checkAuthAndExecute = ::checkAuthAndExecute
                                    )
                                }
                            }
                        } else {
                            // Show single selected category
                            item { SectionHeader(title = selectedCategory!!) }
                            items(filtered.chunked(2)) { row ->
                                ProductRow(
                                    row = row,
                                    favouriteIds = userProfile?.favouriteIds ?: emptyList(),
                                    authViewModel = authViewModel,
                                    productViewModel = productViewModel,
                                    onProductClick = onProductClick,
                                    checkAuthAndExecute = ::checkAuthAndExecute
                                )
                            }
                        }
                    }
                }
            }
        }



        if (showAuthPrompt) {
            AuthPrompt(
                show = showAuthPrompt,
                onDismiss = { showAuthPrompt = false },
                onSignIn = {
                    showAuthPrompt = false
                    onNavigateToLogin()
                },
                onRegister = {
                    showAuthPrompt = false
                    onNavigateToLogin() // Users can click register from Login screen
                }
            )
        }
    }
}

@Composable
fun ProductRow(
    row: List<Product>,
    favouriteIds: List<String>,
    authViewModel: AuthViewModel,
    productViewModel: ProductViewModel,
    onProductClick: (String) -> Unit,
    checkAuthAndExecute: (() -> Unit) -> Unit
) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        row.forEach { product ->
            ProductCard(
                product     = product,
                isFavourite = favouriteIds.contains(product.id),
                onCardClick = { onProductClick(product.id) },
                onLikeClick = { checkAuthAndExecute { authViewModel.toggleFavourite(product.id) } },
                onAddToCart = { productViewModel.addToCart(product, authViewModel.userProfile.value?.uid) },
                modifier    = Modifier.weight(1f)
            )
        }
        if (row.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
fun FeaturedAutoSlider(
    state: ProductsUiState,
    onProductClick: (String) -> Unit
) {
    if (state !is ProductsUiState.Success) return
    val featured = state.products
    if (featured.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { featured.size })
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (!isPressed) {
            while (true) {
                delay(4000)
                val nextPage = (pagerState.currentPage + 1) % featured.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { page ->
            val product = featured[page]
            FeaturedHeroCard(product, onProductClick, interactionSource)
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Page Indicators
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(featured.size) { i ->
                val color = if (pagerState.currentPage == i) ShopizzoBlue else Color.LightGray
                Box(modifier = Modifier.padding(2.dp).size(6.dp).background(color, CircleShape))
            }
        }
    }
}

@Composable
fun FeaturedHeroCard(
    product: Product,
    onProductClick: (String) -> Unit,
    interactionSource: MutableInteractionSource
) {
    Card(
        modifier = Modifier.fillMaxSize().clickable(interactionSource = interactionSource, indication = null) { onProductClick(product.id) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ShopizzoBlue)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                error = painterResource(android.R.drawable.ic_dialog_alert),
                modifier = Modifier.fillMaxSize().alpha(0.3f)
            )
            
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1.2f)) {
                    if (product.discountPercentage != null) {
                        Surface(color = Color.Red, shape = RoundedCornerShape(4.dp)) {
                            Text("${product.discountPercentage}% OFF", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(product.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                    Text(product.description, color = Color.White.copy(0.8f), fontSize = 11.sp, maxLines = 1)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onProductClick(product.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ShopizzoBlue), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                        Text("Shop Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(modifier = Modifier.weight(0.8f).padding(8.dp), contentAlignment = Alignment.Center) {
                    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                            error = painterResource(android.R.drawable.ic_dialog_alert),
                            modifier = Modifier.size(90.dp)
                        )
                    }
                }
            }
        }
    }
}
