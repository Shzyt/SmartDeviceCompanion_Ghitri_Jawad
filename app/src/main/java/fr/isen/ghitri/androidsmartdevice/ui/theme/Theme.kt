package fr.isen.ghitri.androidsmartdevice.ui.theme

import fr.isen.ghitri.androidsmartdevice.ui.theme.Pink40
import fr.isen.ghitri.androidsmartdevice.ui.theme.Pink80
import fr.isen.ghitri.androidsmartdevice.ui.theme.Purple40
import fr.isen.ghitri.androidsmartdevice.ui.theme.Purple80
import fr.isen.ghitri.androidsmartdevice.ui.theme.PurpleGrey40
import fr.isen.ghitri.androidsmartdevice.ui.theme.PurpleGrey80


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun AndroidSmartDeviceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
