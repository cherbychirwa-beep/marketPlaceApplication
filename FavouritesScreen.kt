package com.shopizzo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shopizzo.data.model.Product
import com.shopizzo.ui.components.ProductCard
import com.shopizzo.ui.components.ShopizzoTopBar
import com.shopizzo.ui.theme.ShopizzoLightGray
import com.shopizzo.ui.theme.ShopizzoMidGray
import com.shopizzo.viewmodel.AuthViewModel
import com.shopizzo.viewmodel.ProductViewModel
import com.shopizzo.viewmodel.ProductsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    authViewModel    : AuthViewModel,
    productViewModel : ProductViewModel,
    cartCount        : Int,
    onBack           : () -> Unit,
    onProductClick   : (String) -> Unit,
    onCartClick      : () -> Unit
) {
    val userProfile   by authViewModel.userProfile.collectAsState()
    val productsState by productViewModel.productsState.collectAsState()

    val favouriteProducts: List<Product> = remember(userProfile, productsState) {
        val favIds = userProfile?.favouriteIds ?: emptyList()
        val all    = (productsState as? ProductsUiState.Success)?.products ?: emptyList()
        all.filter { favIds.contains(it.id) }
    }

    Scaffold(
        topBar = {
            ShopizzoTopBar(
                title       = "My Favourites",
                cartCount   = cartCount,
                favouriteCount = favouriteProducts.size,
                showBack    = true,
                isLoggedIn  = true,
                onBackClick = onBack,
                onCartClick = onCartClick,
                onFavouriteClick = {},
                onNotificationClick = {}
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (favouriteProducts.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint     = ShopizzoLightGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No favourites yet", color = ShopizzoMidGray)
                }
            }
        } else {
            LazyVerticalGrid(
                columns               = GridCells.Fixed(2),
                modifier              = Modifier.fillMaxSize().padding(padding),
                contentPadding        = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp)
            ) {
                items(favouriteProducts) { product ->
                    ProductCard(
                        product     = product,
                        isFavourite = true,
                        onCardClick = { onProductClick(product.id) },
                        onLikeClick = { authViewModel.toggleFavourite(product.id) },
                        onAddToCart = { productViewModel.addToCart(product) }
                    )
                }
            }
        }
    }
}
