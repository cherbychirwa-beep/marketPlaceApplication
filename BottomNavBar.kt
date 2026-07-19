package com.shopizzo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shopizzo.ui.Screen
import com.shopizzo.ui.theme.ShopizzoBlue
import com.shopizzo.ui.theme.ShopizzoError
import com.shopizzo.ui.theme.ShopizzoMidGray

private data class BottomNavItem(
    val route        : String,
    val label        : String,
    val filledIcon   : ImageVector,
    val outlinedIcon : ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.HOME,       "Home",       Icons.Filled.Home,      Icons.Outlined.Home),
    BottomNavItem(Screen.PRODUCTS,   "Products",   Icons.Filled.GridView,  Icons.Outlined.GridView),
    BottomNavItem(Screen.CART,       "Cart",       Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    BottomNavItem(Screen.FAVOURITES, "Favourites", Icons.Filled.Favorite,  Icons.Outlined.FavoriteBorder),
    BottomNavItem(Screen.PROFILE,    "Profile",    Icons.Filled.Person,    Icons.Outlined.Person)
)

@Composable
fun ShopizzoBottomBar(
    currentRoute   : String?,
    isLoggedIn     : Boolean,
    favouriteCount : Int = 0,
    cartCount      : Int = 0,
    onNavigate     : (String) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        bottomNavItems.forEach { item ->
            val isVisible = when (item.route) {
                Screen.FAVOURITES -> isLoggedIn
                else -> true
            }

            if (isVisible) {
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick  = { onNavigate(item.route) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (item.route == Screen.FAVOURITES && favouriteCount > 0) {
                                    Badge(containerColor = ShopizzoError, contentColor = Color.White) {
                                        Text(favouriteCount.toString())
                                    }
                                } else if (item.route == Screen.CART && cartCount > 0) {
                                    Badge(
                                        modifier = Modifier.size(8.dp),
                                        containerColor = ShopizzoError
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selected) item.filledIcon else item.outlinedIcon,
                                contentDescription = item.label
                            )
                        }
                    },
                    label  = { Text(item.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = ShopizzoBlue,
                        selectedTextColor   = ShopizzoBlue,
                        unselectedIconColor = ShopizzoMidGray,
                        unselectedTextColor = ShopizzoMidGray,
                        indicatorColor      = ShopizzoBlue.copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}
