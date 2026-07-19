package com.shopizzo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ShopizzoBlue,
    onPrimary = ShopizzoWhite,
    secondary = ShopizzoWhite,
    onSecondary = ShopizzoBlack,
    background = ShopizzoBlack,
    onBackground = ShopizzoWhite,
    surface = ShopizzoDarkGray,
    onSurface = ShopizzoWhite,
    error = ShopizzoError,
    onError = ShopizzoWhite
)

private val LightColorScheme = lightColorScheme(
    primary = ShopizzoBlue,
    onPrimary = ShopizzoWhite,
    secondary = ShopizzoBlack,
    onSecondary = ShopizzoWhite,
    background = ShopizzoWhite,
    onBackground = ShopizzoBlack,
    surface = ShopizzoWhite,
    onSurface = ShopizzoBlack,
    error = ShopizzoError,
    onError = ShopizzoWhite
)

@Composable
fun ShopizzoTheme(
    appTheme: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShopizzoTypography,
        content = content
    )
}
