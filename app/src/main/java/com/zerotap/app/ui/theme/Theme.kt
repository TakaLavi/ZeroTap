package com.zerotap.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---- Palette -----------------------------------------------------------------

val ZtBg = Color(0xFF05060A)
val ZtBgDeep = Color(0xFF020306)
val ZtBgElevated = Color(0xFF0A0E18)
val ZtInk = Color(0xFFF1FAFF)
val ZtInkDim = Color(0xFFA2B2C6)
val ZtInkFaint = Color(0xFF5E6E84)

val ZtTeal = Color(0xFF35F1CD)
val ZtBlue = Color(0xFF4DA3FF)
val ZtViolet = Color(0xFF8E7BFF)
val ZtAmber = Color(0xFFFFC279)
val ZtRose = Color(0xFFFF6F8B)
val ZtGreen = Color(0xFF49E0A0)

val GlassHi = Color(0x14FFFFFF)
val GlassLo = Color(0x05FFFFFF)
val GlassStroke = Color(0x33FFFFFF)
val GlassStrokeSoft = Color(0x14FFFFFF)
val GlassScrim = Color(0x800A0E18)

// ---- Typography (system font, premium weights) -------------------------------

object ZtType {
    val hero = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    )
    val title = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    )
    val section = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    )
    val body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    )
    val small = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    val label = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp
    )
    val mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    )
}

val ZtShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private val ZtColorScheme = darkColorScheme(
    primary = ZtTeal,
    onPrimary = Color(0xFF00201A),
    secondary = ZtBlue,
    tertiary = ZtViolet,
    background = ZtBg,
    onBackground = ZtInk,
    surface = ZtBgElevated,
    onSurface = ZtInk,
    surfaceVariant = Color(0xFF131826),
    onSurfaceVariant = ZtInkDim,
    error = ZtRose,
    outline = GlassStroke
)

@Composable
fun ZeroTapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZtColorScheme,
        typography = Typography(),
        shapes = ZtShapes,
        content = content
    )
}
