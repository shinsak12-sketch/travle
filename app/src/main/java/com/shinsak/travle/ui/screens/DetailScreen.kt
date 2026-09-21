package com.shinsak.travle.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.Category
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.ui.components.AccentButton
import com.shinsak.travle.ui.components.ConfirmDialog
import com.shinsak.travle.ui.components.Ease
import com.shinsak.travle.ui.components.Gauge
import com.shinsak.travle.ui.components.InsetPanel
import com.shinsak.travle.ui.components.MoneyText
import com.shinsak.travle.ui.components.NeuButton
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.NeuIconButton
import com.shinsak.travle.ui.components.Pill
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.manwon
import com.shinsak.travle.ui.theme.Bricolage
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.won
import kotlin.math.roundToInt

@Composable
fun DetailScreen(
    repo: TripRepository,
    tripId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCompare: () -> Unit,
) {
    val n = Neu
    val trips by repo.trips.collectAsStateWithLifecycle()
    val settings by repo.settings.collectAsStateWithLifecycle()
    val trip = trips.firstOrNull { it.id == tripId }
    var askDelete by remember { mutableStateOf(false) }

    if (trip == null) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(n.bg))
        return
    }

    val total = trip.totalKrw
    val cheapest = trips.size > 1 && total == trips.minOf { it.totalKrw }
    val overCap = total > settings.budgetCap
    val fractions = Category.entries.map { trip.costKrw(it) / total.toFloat().coerceAtLeast(1f) }
    val capFraction = if (settings.budgetCap > 0) total.toFloat() / settings.budgetCap else 0f
    val capPct = (capFraction * 100).roundToInt()

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding()) {
        val meta = buildList {
            add(trip.periodLabel)
            add("${trip.people}인")
            if (trip.month.isNotBlank()) add(trip.month)
        }.joinToString(" · ")
        ScreenHeader(title = trip.name.ifBlank { "이름 없음" }, subtitle = meta, onBack = onBack) {
            NeuButton("수정", onClick = onEdit, height = 40.dp, radius = 15.dp)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            RiseIn(0) {
                NeuCard(modifier = Modifier.fillMaxWidth(), radius = 25.dp, padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                SectionLabel("총액")
                                if (cheapest) Pill("최저가", n.amber)
                                if (overCap) Pill("예산 초과", n.red)
                            }
                            Spacer(Modifier.height(4.dp))
                            MoneyText(total, size = 32, color = if (overCap) n.red else n.ink)
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            SectionLabel("1인당")
                            Spacer(Modifier.height(5.dp))
                            Text("${trip.perPersonKrw.won()}원", color = n.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (trip.currency != "KRW") {
                        Spacer(Modifier.height(10.dp))
                        Text("${trip.currency} 기준 · 환율 ${trip.fxRate}", color = n.ink2, fontSize = 11.5.sp)
                    }
                }
            }

            val hasFlight = trip.airline.isNotBlank() || trip.outbound != null || trip.inbound != null
            if (hasFlight) {
                RiseIn(1) {
                    NeuCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            SectionLabel("항공편")
                            Spacer(Modifier.weight(1f))
                            if (trip.airline.isNotBlank()) {
                                Text(trip.airline, color = n.accent, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        listOf("가는편" to trip.outbound, "오는편" to trip.inbound).forEach { (title, leg) ->
                            if (leg != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(title, color = n.ink2, fontSize = 12.sp, modifier = Modifier.width(52.dp))
                                    Text(leg.timeLabel.ifBlank { "—" }, color = n.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = (-0.5).sp)
                                    Spacer(Modifier.weight(1f))
                                    if (leg.durationLabel.isNotBlank()) {
                                        Text(leg.durationLabel, color = n.ink2, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            RiseIn(2) {
                NeuCard(modifier = Modifier.fillMaxWidth(), radius = 25.dp, padding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp)) {
                    SectionLabel("항목별 비중")
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Donut(fractions = fractions, colors = n.chart, track = n.well, center = total.manwon(), sub = "${fractions.count { it > 0f }}개 항목")
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Category.entries.forEachIndexed { i, c ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(n.chart[i]))
                                    Text(c.short, color = n.ink, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                                    Text("${(fractions[i] * 100).roundToInt()}%", color = n.ink, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Category.entries.forEachIndexed { i, c ->
                        val v = trip.costKrw(c)
                        if (v > 0) {
                            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(c.label, color = n.ink2, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text("${v.won()}원", color = n.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            RiseIn(3) {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        SectionLabel("예산 상한 대비")
                        Spacer(Modifier.weight(1f))
                        val gap = settings.budgetCap - total
                        Text(
                            if (gap >= 0) "$capPct% · ${gap.manwon()}원 남음" else "$capPct% · ${(-gap).manwon()}원 초과",
                            color = if (gap >= 0) n.accent else n.red, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(11.dp))
                    Gauge(fraction = capFraction, color = if (overCap) n.red else n.accent, modifier = Modifier.fillMaxWidth(), height = 20.dp)
                    Spacer(Modifier.height(7.dp))
                    Row {
                        Text("0", color = n.ink2, fontSize = 10.sp)
                        Spacer(Modifier.weight(1f))
                        Text("상한 ${settings.budgetCap.won()}원", color = n.ink2, fontSize = 10.sp)
                    }
                }
            }

            if (trip.memo.isNotBlank()) {
                RiseIn(4) {
                    InsetPanel(modifier = Modifier.fillMaxWidth(), radius = 20.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                        SectionLabel("메모", color = n.ink2)
                        Spacer(Modifier.height(6.dp))
                        Text(trip.memo, color = n.ink, fontSize = 12.5.sp, lineHeight = 19.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 14.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccentButton("비교에 추가", onClick = onCompare, modifier = Modifier.weight(1f), icon = Icons.Rounded.BarChart)
            NeuIconButton(Icons.Rounded.Delete, contentDescription = "삭제", onClick = { askDelete = true }, size = 52.dp, radius = 19.dp, tint = n.red)
        }
    }

    if (askDelete) {
        ConfirmDialog(
            title = "이 견적을 지울까요?",
            message = "\"${trip.name.ifBlank { "이름 없음" }}\" 견적이 삭제됨. 되돌릴 수 없음.",
            confirmText = "삭제",
            destructive = true,
            onConfirm = {
                askDelete = false
                repo.delete(trip.id)
            },
            onDismiss = { askDelete = false },
        )
    }
}

@Composable
private fun Donut(fractions: List<Float>, colors: List<Color>, track: Color, center: String, sub: String) {
    val n = Neu
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(900, delayMillis = 150, easing = Ease)) }
    Box(
        modifier = Modifier
            .size(152.dp)
            .graphicsLayer {
                val p = progress.value
                rotationZ = -40f * (1f - p)
                scaleX = 0.82f + 0.18f * p
                scaleY = 0.82f + 0.18f * p
                alpha = p
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 23.dp.toPx()
            val d = size.minDimension - stroke
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val sz = Size(d, d)
            drawArc(track, 0f, 360f, false, topLeft, sz, style = Stroke(stroke))
            var start = -90f
            fractions.forEachIndexed { i, f ->
                val sweep = 360f * f
                if (sweep > 0f) {
                    val gap = if (sweep > 3f) 1.2f else 0f
                    drawArc(
                        color = colors[i % colors.size],
                        startAngle = start + gap,
                        sweepAngle = (sweep - gap * 2).coerceAtLeast(0.6f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = sz,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(center, color = n.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = (-0.8).sp)
            Spacer(Modifier.height(1.dp))
            Text(sub, color = n.ink2, fontSize = 10.sp)
        }
    }
    Spacer(Modifier.width(0.dp))
}
