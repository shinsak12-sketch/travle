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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 양각(볼록). 오른쪽 아래 어두운 그림자 + 왼쪽 위 밝은 그림자.
 * setShadowLayer는 API 28부터 하드웨어 캔버스에서 도형에도 먹음 → minSdk 28.
 */
fun Modifier.neuRaised(
    radius: Dp = 22.dp,
    fill: Color? = null,
    dark: Color = Color(0x4D786C58),
    light: Color = Color(0xF0FFFFFF),
    offset: Dp = 6.dp,
    blur: Dp = 14.dp,
): Modifier = this.drawBehind {
    val r = radius.toPx()
    val o = offset.toPx()
    val b = blur.toPx()
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val fp = paint.asFrameworkPaint()
        fp.isAntiAlias = true
        fp.color = android.graphics.Color.TRANSPARENT
        fp.setShadowLayer(b, o, o, dark.toArgb())
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        fp.setShadowLayer(b, -o, -o, light.toArgb())
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
    }
    if (fill != null) {
        drawRoundRect(color = fill, cornerRadius = CornerRadius(r, r))
    }
}

/** 강조색 버튼용 양각: 진한 색 그림자 하나만 아래로 */
fun Modifier.neuRaisedAccent(
    radius: Dp = 19.dp,
    fill: Color,
    shadow: Color,
    light: Color = Color(0x8CFFFFFF),
    offset: Dp = 7.dp,
    blur: Dp = 18.dp,
): Modifier = this.drawBehind {
    val r = radius.toPx()
    val o = offset.toPx()
    val b = blur.toPx()
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val fp = paint.asFrameworkPaint()
        fp.isAntiAlias = true
        fp.color = android.graphics.Color.TRANSPARENT
        fp.setShadowLayer(b, o, o + 2f, shadow.toArgb())
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        fp.setShadowLayer(b * 0.6f, -o * 0.6f, -o * 0.6f, light.toArgb())
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
    }
    drawRoundRect(color = fill, cornerRadius = CornerRadius(r, r))
}

/**
 * 음각(오목). 도형 안쪽으로 그림자가 떨어짐.
 * 큰 사각형에서 도형을 뺀 '링'을 그리고 그 그림자를 도형 안으로 클립.
 */
fun Modifier.neuInset(
    radius: Dp = 17.dp,
    fill: Color? = null,
    dark: Color = Color(0x52786C58),
    light: Color = Color(0xEBFFFFFF),
    offset: Dp = 5.dp,
    blur: Dp = 11.dp,
): Modifier = this.drawBehind {
    val r = radius.toPx()
    val o = offset.toPx()
    val b = blur.toPx()
    val outline = RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r))
    if (fill != null) {
        drawRoundRect(color = fill, cornerRadius = CornerRadius(r, r))
    }
    val clip = Path().apply { addRoundRect(outline) }
    val ring = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(-size.width - b * 2, -size.height - b * 2, size.width * 2 + b * 2, size.height * 2 + b * 2))
        addRoundRect(outline)
    }
    clipPath(clip) {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            val fp = paint.asFrameworkPaint()
            fp.isAntiAlias = true
            fp.color = android.graphics.Color.TRANSPARENT
            fp.setShadowLayer(b, o, o, dark.toArgb())
            canvas.drawPath(ring, paint)
            fp.setShadowLayer(b, -o, -o, light.toArgb())
            canvas.drawPath(ring, paint)
        }
    }
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
