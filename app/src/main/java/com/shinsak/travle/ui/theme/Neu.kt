package com.shinsak.travle.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 카드. 채움색 + 아래로 떨어지는 부드러운 그림자 한 겹 (클린 카드 스타일).
 * light / offset / blur 파라미터는 호환용으로 남겨둠.
 */
fun Modifier.neuRaised(
    radius: Dp = 22.dp,
    fill: Color? = null,
    dark: Color = Color(0x16191F28),
    light: Color = Color(0x00000000),
    offset: Dp = 6.dp,
    blur: Dp = 14.dp,
): Modifier = this.drawBehind {
    val r = radius.toPx()
    val o = (offset.toPx() * 0.5f).coerceAtMost(4.dp.toPx())
    val b = (blur.toPx() * 0.9f).coerceIn(6.dp.toPx(), 16.dp.toPx())
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val fp = paint.asFrameworkPaint()
        fp.isAntiAlias = true
        fp.color = android.graphics.Color.TRANSPARENT
        fp.setShadowLayer(b, 0f, o, dark.toArgb())
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
    }
    if (fill != null) drawRoundRect(color = fill, cornerRadius = CornerRadius(r, r))
}

/** 강조색 버튼: 채움 + 같은 색 그림자 */
fun Modifier.neuRaisedAccent(
    radius: Dp = 19.dp,
    fill: Color,
    shadow: Color,
    light: Color = Color(0x00000000),
    offset: Dp = 7.dp,
    blur: Dp = 18.dp,
): Modifier = this.drawBehind {
    val r = radius.toPx()
    val o = (offset.toPx() * 0.6f).coerceAtMost(6.dp.toPx())
    val b = blur.toPx().coerceIn(8.dp.toPx(), 22.dp.toPx())
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val fp = paint.asFrameworkPaint()
        fp.isAntiAlias = true
        fp.color = android.graphics.Color.TRANSPARENT
        fp.setShadowLayer(b, 0f, o, shadow.toArgb())
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
    }
    drawRoundRect(color = fill, cornerRadius = CornerRadius(r, r))
}

/**
 * 움푹한 영역(입력칸·선택 안 된 칩·트랙). 채움색 + 얇은 외곽선.
 * dark 를 외곽선 색으로 씀.
 */
fun Modifier.neuInset(
    radius: Dp = 17.dp,
    fill: Color? = null,
    dark: Color = Color(0x16191F28),
    light: Color = Color(0x00000000),
    offset: Dp = 5.dp,
    blur: Dp = 11.dp,
): Modifier = this.drawBehind {
    val r = radius.toPx()
    if (fill != null) drawRoundRect(color = fill, cornerRadius = CornerRadius(r, r))
    val sw = 1.dp.toPx()
    drawRoundRect(
        color = dark.copy(alpha = (dark.alpha * 2.2f).coerceAtMost(0.35f)),
        topLeft = Offset(sw / 2, sw / 2),
        size = Size(size.width - sw, size.height - sw),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = sw),
    )
}

/** pressable + 길게 누르기 (삭제 등) */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun Modifier.pressableLong(onLongClick: () -> Unit, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "pressScale")
    val sink by animateFloatAsState(if (pressed) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "pressSink")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale; translationY = sink * 2.dp.toPx() }
        .combinedClickable(interactionSource = src, indication = null, onClick = onClick, onLongClick = onLongClick)
}

/**
 * 누르면 살짝 작아지면서 2dp 내려감. 그림자 modifier보다 *앞에* 붙여야 그림자까지 같이 눌림.
 */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    val sink by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressSink",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = sink * 2.dp.toPx()
        }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}
