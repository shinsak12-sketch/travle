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

/** 숫자·금액 전용 (가변 폰트, wght 축 사용) */
val Bricolage = FontFamily(
    Font(R.font.bricolage_grotesque, FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque, FontWeight.ExtraBold),
)

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
    bg = Color(0xFFE9E5DD),
    surface = Color(0xFFF1EEE7),
    well = Color(0xFFE3DFD6),
    ink = Color(0xFF22201B),
    ink2 = Color(0xFF575044),
    hint = Color(0xFF8C8474),
    line = Color(0xFFDED9CE),
    accent = Color(0xFF12695E),
    accentDeep = Color(0xFF0C4F47),
    onAccent = Color(0xFFFFFFFF),
    onAccentSoft = Color(0xFFA9D3CC),
    accentTint = Color(0xFFE4EDE9),
    amber = Color(0xFFA9540F),
    red = Color(0xFFB03A2E),
    shadowDark = Color(0x4D786C58),
    shadowLight = Color(0xF0FFFFFF),
    accentShadow = Color(0x6612695E),
    chart = listOf(
        Color(0xFF12695E), Color(0xFF3F8F82), Color(0xFFA9540F),
        Color(0xFFC98A3F), Color(0xFF6E7F6A), Color(0xFF5B7DA8), Color(0xFF9A8C72),
    ),
    isDark = false,
)

val DarkNeu = NeuColors(
    bg = Color(0xFF1F1D1A),
    surface = Color(0xFF292622),
    well = Color(0xFF191715),
    ink = Color(0xFFEFEAE1),
    ink2 = Color(0xFFA8A090),
    hint = Color(0xFF7A7365),
    line = Color(0xFF3A3630),
    accent = Color(0xFF3FA391),
    accentDeep = Color(0xFF8AD6C7),
    onAccent = Color(0xFF0B2622),
    onAccentSoft = Color(0xFF1E4A43),
    accentTint = Color(0xFF243532),
    amber = Color(0xFFD9823B),
    red = Color(0xFFD8574A),
    shadowDark = Color(0xA6000000),
    shadowLight = Color(0x17FFFFFF),
    accentShadow = Color(0x553FA391),
    chart = listOf(
        Color(0xFF3FA391), Color(0xFF6BBFAF), Color(0xFFD9823B),
        Color(0xFFE0A863), Color(0xFF8FA58A), Color(0xFF86A6D1), Color(0xFFB3A48C),
    ),
    isDark = true,
)

val LocalNeu = staticCompositionLocalOf { LightNeu }

/** 현재 테마 색. 컴포저블 안에서 `Neu.accent` 처럼 씀. */
val Neu: NeuColors
    @Composable @ReadOnlyComposable get() = LocalNeu.current

private val body = TextStyle(fontFamily = PlexKR, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)

val TravleTypography = Typography(
    displayLarge = TextStyle(fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp),
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
