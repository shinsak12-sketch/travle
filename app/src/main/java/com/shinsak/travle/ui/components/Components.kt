package com.shinsak.travle.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shinsak.travle.ui.theme.Bricolage
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.neuRaised
import com.shinsak.travle.ui.theme.neuRaisedAccent
import com.shinsak.travle.ui.theme.pressable

/** 시안의 공통 곡선 cubic-bezier(.22, 1, .36, 1) */
val Ease = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

// ---------------------------------------------------------------- 텍스트

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Neu.ink2) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
    )
}

/** 큰 금액 숫자. Bricolage ExtraBold. */
@Composable
fun MoneyText(
    amount: Long,
    modifier: Modifier = Modifier,
    size: Int = 28,
    color: Color = Neu.ink,
    suffix: String = "원",
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = amount.toString().reversed().chunked(3).joinToString(",").reversed(),
            color = color,
            fontFamily = Bricolage,
            fontWeight = FontWeight.ExtraBold,
            fontSize = size.sp,
            letterSpacing = (-size * 0.04f).sp,
            lineHeight = (size * 1.05f).sp,
        )
        if (suffix.isNotEmpty()) {
            Spacer(Modifier.width(3.dp))
            Text(
                text = suffix,
                color = color,
                fontSize = (size * 0.46f).sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = (size * 0.12f).dp),
            )
        }
    }
}

// ---------------------------------------------------------------- 카드 · 버튼

@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    radius: Dp = 23.dp,
    padding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val n = Neu
    val base = if (onClick != null) modifier.pressable(onClick = onClick) else modifier
    Column(
        modifier = base
            .neuRaised(radius = radius, fill = n.surface, dark = n.shadowDark, light = n.shadowLight)
            .padding(padding),
        content = content,
    )
}

/** 오목한 판. 입력칸·트랙·메모 배경 */
@Composable
fun InsetPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 17.dp,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val n = Neu
    Column(
        modifier = modifier
            .neuInset(radius = radius, fill = n.bg, dark = n.shadowDark, light = n.shadowLight)
            .padding(padding),
        content = content,
    )
}

/** 볼록 버튼 (보조) */
@Composable
fun NeuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
    radius: Dp = 17.dp,
    icon: ImageVector? = null,
    color: Color = Neu.accent,
    enabled: Boolean = true,
) {
    val n = Neu
    Row(
        modifier = modifier
            .height(height)
            .pressable(enabled = enabled, onClick = onClick)
            .neuRaised(radius = radius, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 5.dp, blur = 12.dp)
            .padding(horizontal = 18.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 강조색 버튼 (주 동작) */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    radius: Dp = 19.dp,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val n = Neu
    Row(
        modifier = modifier
            .height(height)
            .pressable(enabled = enabled, onClick = onClick)
            .neuRaisedAccent(radius = radius, fill = n.accent, shadow = n.accentShadow, light = n.shadowLight)
            .padding(horizontal = 20.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = n.onAccent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = n.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun NeuIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    radius: Dp = 16.dp,
    tint: Color = Neu.ink2,
    iconSize: Dp = 18.dp,
) {
    val n = Neu
    Box(
        modifier = modifier
            .size(size)
            .pressable(onClick = onClick)
            .neuRaised(radius = radius, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 5.dp, blur = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    NeuIconButton(Icons.Rounded.ArrowBackIosNew, contentDescription = "뒤로", onClick = onClick, iconSize = 16.dp)
}

/** 동그란 강조 FAB */
@Composable
fun Fab(onClick: () -> Unit, modifier: Modifier = Modifier, desc: String = "견적 추가") {
    val n = Neu
    Box(
        modifier = modifier
            .size(62.dp)
            .pressable(onClick = onClick)
            .neuRaisedAccent(radius = 23.dp, fill = n.accent, shadow = n.accentShadow, light = n.shadowLight, offset = 8.dp, blur = 22.dp)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = n.onAccent, modifier = Modifier.size(26.dp))
    }
}

// ---------------------------------------------------------------- 입력

/** 오목한 텍스트 입력칸 */
@Composable
fun InsetField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    height: Dp = 50.dp,
    radius: Dp = 17.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String? = null,
    textAlign: TextAlign = TextAlign.Start,
    fontSize: Int = 15,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val n = Neu
    Row(
        modifier = modifier
            .then(if (singleLine) Modifier.height(height) else Modifier)
            .neuInset(radius = radius, fill = n.bg, dark = n.shadowDark, light = n.shadowLight)
            .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            enabled = enabled,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = if (enabled) n.ink else n.hint,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = textAlign,
                lineHeight = (fontSize * 1.45f).sp,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(n.accent),
            decorationBox = { inner ->
                Box(contentAlignment = if (textAlign == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = n.hint, fontSize = fontSize.sp, fontWeight = FontWeight.Medium)
                    }
                    inner()
                }
            },
        )
        if (suffix != null) {
            Spacer(Modifier.width(6.dp))
            Text(suffix, color = n.ink2, fontSize = 11.sp)
        }
    }
}

/** 오목한 트랙 위에 볼록한 선택 칩 */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp,
) {
    val n = Neu
    Row(
        modifier = modifier
            .neuInset(radius = 19.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .pressable { onSelect(i) }
                    .then(
                        if (on) Modifier.neuRaised(radius = 15.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 8.dp)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (on) n.accent else n.ink2,
                    fontSize = 12.5.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

/** − 값 + */
@Composable
fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    height: Dp = 50.dp,
) {
    val n = Neu
    Row(
        modifier = modifier
            .height(height)
            .neuInset(radius = 17.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StepButton(Icons.Rounded.Remove, "$label 줄이기", n.ink2, onMinus)
        Text(
            value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = n.ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
        )
        StepButton(Icons.Rounded.Add, "$label 늘리기", n.accent, onPlus)
    }
}

@Composable
private fun StepButton(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    val n = Neu
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(38.dp)
            .pressable(onClick = onClick)
            .neuRaised(radius = 13.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(15.dp))
    }
}

@Composable
fun NeuToggle(checked: Boolean, onChange: (Boolean) -> Unit, desc: String) {
    val n = Neu
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "knob",
    )
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(30.dp)
            .pressable { onChange(!checked) }
            .then(
                if (checked) Modifier.clip(RoundedCornerShape(999.dp)).background(n.accent)
                else Modifier.neuInset(radius = 15.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 9.dp)
            )
            .padding(3.dp)
            .semantics { contentDescription = desc },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = knobOffset)
                .size(24.dp)
                .neuRaised(radius = 12.dp, fill = if (checked) Color.White else n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 2.dp, blur = 6.dp),
        )
    }
}

// ---------------------------------------------------------------- 표시

@Composable
fun Pill(text: String, color: Color, textColor: Color = Color.White) {
    Text(
        text,
        color = textColor,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun Stars(rating: Int, size: Dp = 13.dp, onChange: ((Int) -> Unit)? = null) {
    val n = Neu
    val off = if (n.isDark) Color(0xFF4A453D) else Color(0xFFCFC6B4)
    Row(
        horizontalArrangement = Arrangement.spacedBy(if (onChange != null) 4.dp else 1.dp),
        modifier = Modifier.semantics { contentDescription = "선호도 ${rating}점" },
    ) {
        for (i in 1..5) {
            val m = if (onChange != null) Modifier.size(size + 14.dp).pressable { onChange(i) } else Modifier.size(size)
            Box(m, contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Star,
                    contentDescription = null,
                    tint = if (i <= rating) n.amber else off,
                    modifier = Modifier.size(size),
                )
            }
        }
    }
}

/** 항목 비중 미니 막대. 왼쪽부터 차오름. */
@Composable
fun StackedBar(fractions: List<Float>, colors: List<Color>, modifier: Modifier = Modifier, height: Dp = 8.dp, delayMs: Int = 200) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1050, delayMillis = delayMs, easing = Ease)) }
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .graphicsLayer {
                scaleX = progress.value
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
            },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        fractions.forEachIndexed { i, f ->
            if (f > 0f) {
                Box(
                    Modifier
                        .weight(f)
                        .fillMaxHeight()
                        .background(colors[i % colors.size]),
                )
            }
        }
    }
}

/** 가로 게이지 (오목 트랙 + 볼록 채움) */
@Composable
fun Gauge(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 16.dp, delayMs: Int = 250) {
    val n = Neu
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fraction) { progress.animateTo(fraction.coerceIn(0f, 1f), tween(1050, delayMillis = delayMs, easing = Ease)) }
    Box(
        modifier = modifier
            .height(height)
            .neuInset(radius = height / 2, fill = n.well, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value.coerceAtLeast(0.001f))
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

/** 아래에서 올라오며 등장. index마다 70ms씩 늦게. */
@Composable
fun RiseIn(index: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(420, delayMillis = index * 70, easing = Ease)) +
            slideInVertically(tween(560, delayMillis = index * 70, easing = Ease)) { it / 5 },
        exit = fadeOut(tween(120)),
    ) { content() }
}

// ---------------------------------------------------------------- 구조

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val n = Neu
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) BackButton(onBack)
        Column(Modifier.weight(1f)) {
            Text(title, color = n.ink, fontSize = if (onBack != null) 22.sp else 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, maxLines = 1)
            if (subtitle != null) {
                Text(subtitle, color = n.ink2, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        trailing()
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    PLAN("일정", Icons.Rounded.CalendarMonth),
    LEDGER("가계부", Icons.Rounded.ReceiptLong),
    CHECK("체크", Icons.Rounded.Checklist),
    BOOKING("예약", Icons.Rounded.ConfirmationNumber),
}

@Composable
fun BottomTabBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val n = Neu
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 14.dp, top = 8.dp)
            .neuInset(radius = 27.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Tab.entries.forEach { tab ->
            val on = tab == current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .pressable { onSelect(tab) }
                    .then(
                        if (on) Modifier.neuRaised(radius = 22.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 9.dp)
                        else Modifier
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = if (on) n.accent else n.ink2, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(2.dp))
                Text(tab.label, color = if (on) n.accent else n.ink2, fontSize = 10.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium)
            }
        }
    }
}

/** 확인 다이얼로그. 카드 스타일. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    val n = Neu
    Dialog(onDismissRequest = onDismiss) {
        NeuCard(radius = 26.dp, padding = PaddingValues(22.dp)) {
            Text(title, color = n.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = n.ink2, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeuButton("취소", onClick = onDismiss, modifier = Modifier.weight(1f), color = n.ink2)
                if (destructive) {
                    NeuButton(confirmText, onClick = onConfirm, modifier = Modifier.weight(1f), color = n.red)
                } else {
                    AccentButton(confirmText, onClick = onConfirm, modifier = Modifier.weight(1f), height = 46.dp, radius = 17.dp)
                }
            }
        }
    }
}
