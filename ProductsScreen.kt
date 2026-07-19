package com.shopizzo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shopizzo.data.model.Product
import com.shopizzo.ui.components.*
import com.shopizzo.ui.theme.ShopizzoBlue
import com.shopizzo.ui.theme.ShopizzoError
import com.shopizzo.viewmodel.AuthViewModel
import com.shopizzo.viewmodel.ProductViewModel
import com.shopizzo.viewmodel.ProductsUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    productViewModel : ProductViewModel,
    authViewModel    : AuthViewModel,
    cartCount        : Int,
    onBack           : () -> Unit,
    onProductClick   : (String) -> Unit,
    onCartClick      : () -> Unit,
    onFavouriteClick : () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val productsState by productViewModel.productsState.collectAsState()
    val selectedCategory by productViewModel.selectedCategory.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    val favouriteCount = userProfile?.favouriteIds?.size ?: 0
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
    
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

    val categories = listOf("All", "Smartphones", "Laptops", "Tablets", "Smart Watches", "Headphones", "Gaming Accessories")

    Scaffold(
        topBar = {
            Column {
                ShopizzoTopBar(
                    title       = "All Products",
                    cartCount   = cartCount,
                    favouriteCount = favouriteCount,
                    showBack    = true,
                    isLoggedIn  = isLoggedIn,
                    onBackClick = onBack,
                    onCartClick = onCartClick,
                    onFavouriteClick = { checkAuthAndExecute(onFavouriteClick) },
                    onNotificationClick = { }
                )
                CategorySelector(
                    categories = categories,
                    selected = selectedCategory,
                    onSelect = { productViewModel.updateCategory(it) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = refreshState,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(padding)
        ) {
            when (val state = productsState) {
                is ProductsUiState.Loading -> Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = ShopizzoBlue) }

                is ProductsUiState.Error -> Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text(state.message, color = ShopizzoError) }

                is ProductsUiState.Success -> {
                    val filtered: List<Product> = productViewModel.filteredProducts()
                    
                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No products available", color = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        val groups: Map<String, List<Product>> = if (selectedCategory == null || selectedCategory == "All") {
                            filtered.groupBy { it.category }
                        } else {
                            mapOf(selectedCategory!! to filtered)
                        }

                        LazyVerticalGrid(
                            columns             = GridCells.Fixed(2),
                            modifier            = Modifier.fillMaxSize(),
                            contentPadding      = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement   = Arrangement.spacedBy(12.dp)
                        ) {
                            groups.forEach { (catName, productsList) ->
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = ShopizzoBlue
                                    )
                                }
                                items(productsList, key = { it.id }) { product ->
                                    ProductCard(
                                        product     = product,
                                        isFavourite = authViewModel.isFavourite(product.id),
                                        onCardClick = { onProductClick(product.id) },
                                        onLikeClick = { checkAuthAndExecute { authViewModel.toggleFavourite(product.id) } },
                                        onAddToCart = { checkAuthAndExecute { productViewModel.addToCart(product, userProfile?.uid) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    AuthPrompt(
        show = showAuthPrompt,
        onDismiss = { showAuthPrompt = false },
        onSignIn = {
            showAuthPrompt = false
            onNavigateToLogin()
        },
        onRegister = {
            showAuthPrompt = false
            onNavigateToLogin()
        }
    )
}
