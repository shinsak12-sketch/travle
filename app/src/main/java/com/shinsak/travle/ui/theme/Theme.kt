package com.shinsak.travle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shinsak.travle.R

/** 숫자·금액 전용 */
val Manrope = FontFamily(
    Font(R.font.manrope_bold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

/** 예전 이름 호환 */
val Bricolage: FontFamily get() = Manrope

/** 한글 본문 */
val PlexKR = FontFamily(
    Font(R.font.ibm_plex_sans_kr_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_kr_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_kr_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_kr_bold, FontWeight.Bold),
)

@Immutable
data class NeuColors(
    val bg: Color,
    val surface: Color,
    val well: Color,
    val ink: Color,
    val ink2: Color,
    val hint: Color,
    val line: Color,
    val accent: Color,
    val accentDeep: Color,
    val onAccent: Color,
    val onAccentSoft: Color,
    val accentTint: Color,
    val amber: Color,
    val red: Color,
    val shadowDark: Color,
    val shadowLight: Color,
    val accentShadow: Color,
    val chart: List<Color>,
    val isDark: Boolean,
)

val LightNeu = NeuColors(
    bg = Color(0xFFF2F4F6),
    surface = Color(0xFFFFFFFF),
    well = Color(0xFFE9ECF1),
    ink = Color(0xFF191F28),
    ink2 = Color(0xFF6B7684),
    hint = Color(0xFF9AA3AF),
    line = Color(0xFFE5E8EC),
    accent = Color(0xFF2F6BFF),
    accentDeep = Color(0xFF1E4FD6),
    onAccent = Color(0xFFFFFFFF),
    onAccentSoft = Color(0xFF9DB8FF),
    accentTint = Color(0xFFE8F0FF),
    amber = Color(0xFFF59E0B),
    red = Color(0xFFF03E3E),
    shadowDark = Color(0x16191F28),
    shadowLight = Color(0x00000000),
    accentShadow = Color(0x4D2F6BFF),
    // ExpCategory 순서: STAY FLIGHT TRANSPORT FOOD SHOPPING SIGHT GOLF ETC
    chart = listOf(
        Color(0xFF14B8A6), Color(0xFF8B5CF6), Color(0xFF64748B), Color(0xFFF59E0B),
        Color(0xFFEC4899), Color(0xFF2F6BFF), Color(0xFF22A06B), Color(0xFF9AA3AF),
    ),
    isDark = false,
)

val DarkNeu = NeuColors(
    bg = Color(0xFF0F1115),
    surface = Color(0xFF1A1D24),
    well = Color(0xFF242830),
    ink = Color(0xFFF2F4F7),
    ink2 = Color(0xFF9AA3AF),
    hint = Color(0xFF6B7280),
    line = Color(0xFF2A2F3A),
    accent = Color(0xFF5B8CFF),
    accentDeep = Color(0xFF9DB8FF),
    onAccent = Color(0xFF0B1220),
    onAccentSoft = Color(0xFF2A3F6E),
    accentTint = Color(0xFF1D2A4A),
    amber = Color(0xFFF5B04A),
    red = Color(0xFFF26B6B),
    shadowDark = Color(0x66000000),
    shadowLight = Color(0x00000000),
    accentShadow = Color(0x405B8CFF),
    chart = listOf(
        Color(0xFF2DD4BF), Color(0xFFA78BFA), Color(0xFF94A3B8), Color(0xFFFBBF24),
        Color(0xFFF472B6), Color(0xFF5B8CFF), Color(0xFF34D399), Color(0xFF9AA3AF),
    ),
    isDark = true,
)

val LocalNeu = staticCompositionLocalOf { LightNeu }

/** 현재 테마 색. 컴포저블 안에서 `Neu.accent` 처럼 씀. */
val Neu: NeuColors
    @Composable @ReadOnlyComposable get() = LocalNeu.current

private val body = TextStyle(fontFamily = PlexKR, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)

val TravleTypography = Typography(
    displayLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp),
    headlineLarge = TextStyle(fontFamily = PlexKR, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = PlexKR, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = PlexKR, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = PlexKR, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = body,
    bodyMedium = body.copy(fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = body.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = PlexKR, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = PlexKR, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.4.sp),
    labelSmall = TextStyle(fontFamily = PlexKR, fontWeight = FontWeight.Medium, fontSize = 10.sp),
)

@Composable
fun TravleTheme(themeMode: String, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val neu = if (dark) DarkNeu else LightNeu
    val scheme = if (dark) {
        darkColorScheme(
            primary = neu.accent, onPrimary = neu.onAccent,
            background = neu.bg, onBackground = neu.ink,
            surface = neu.surface, onSurface = neu.ink,
            surfaceVariant = neu.well, onSurfaceVariant = neu.ink2,
            outline = neu.line, error = neu.red,
        )
    } else {
        lightColorScheme(
            primary = neu.accent, onPrimary = neu.onAccent,
            background = neu.bg, onBackground = neu.ink,
            surface = neu.surface, onSurface = neu.ink,
            surfaceVariant = neu.well, onSurfaceVariant = neu.ink2,
            outline = neu.line, error = neu.red,
        )
    }
    CompositionLocalProvider(LocalNeu provides neu) {
        MaterialTheme(colorScheme = scheme, typography = TravleTypography, content = content)
    }
}
