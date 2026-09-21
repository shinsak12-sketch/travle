package com.shinsak.travle.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.Category
import com.shinsak.travle.data.Trip
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.ui.components.BottomTabBar
import com.shinsak.travle.ui.components.Gauge
import com.shinsak.travle.ui.components.InsetPanel
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.components.Tab
import com.shinsak.travle.ui.manwon
import com.shinsak.travle.ui.theme.Bricolage
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.neuRaised
import com.shinsak.travle.ui.theme.pressable
import java.util.Locale

/** 표에 넣는 짧은 만원 숫자. 760000 → "76", 7000 → "0.7" */
private fun manNum(v: Long): String = when {
    v <= 0 -> "0"
    v < 10_000 -> String.format(Locale.US, "%.1f", v / 10_000.0)
    else -> ((v + 5_000) / 10_000).toString()
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun CompareScreen(
    repo: TripRepository,
    onBack: () -> Unit,
    onTab: (Tab) -> Unit,
) {
    val n = Neu
    val trips by repo.trips.collectAsStateWithLifecycle()
    val ids by repo.compare.collectAsStateWithLifecycle()
    val selected = trips.filter { it.id in ids }.sortedBy { it.totalKrw }
    val palette = listOf(n.chart[0], n.chart[2], n.chart[4], n.chart[1])

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding()) {
        ScreenHeader(
            title = "나란히 비교",
            subtitle = "${selected.size}개 선택됨 · 최대 4개",
            onBack = onBack,
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 16.dp),
        ) {
            if (trips.isEmpty()) {
                InsetPanel(modifier = Modifier.fillMaxWidth(), radius = 22.dp, padding = PaddingValues(24.dp)) {
                    Text("비교할 견적이 없음", color = n.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text("홈에서 견적을 먼저 만들어주세요.", color = n.ink2, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    trips.forEach { t ->
                        val on = t.id in ids
                        val idx = selected.indexOfFirst { it.id == t.id }
                        val dot = if (idx >= 0) palette[idx % palette.size] else n.line
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .pressable { repo.toggleCompare(t.id) }
                                .then(
                                    if (on) Modifier.neuRaised(radius = 14.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 10.dp)
                                    else Modifier.neuInset(radius = 14.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 8.dp)
                                )
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(3.dp)).background(dot))
                            Text(
                                t.name.ifBlank { "이름 없음" },
                                color = if (on) n.ink else n.ink2,
                                fontSize = 12.sp,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (selected.size < 2) {
                    InsetPanel(modifier = Modifier.fillMaxWidth(), radius = 20.dp, padding = PaddingValues(20.dp)) {
                        Text(
                            "위에서 2개 이상 고르면 나란히 비교됨",
                            color = n.ink2, fontSize = 13.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    RiseIn(0) { TotalsCard(selected, palette) }
                    Spacer(Modifier.height(13.dp))
                    RiseIn(1) { TableCard(selected) }
                    Spacer(Modifier.height(13.dp))
                    RiseIn(2) { SummaryPanel(selected) }
                }
            }
        }

        BottomTabBar(current = Tab.COMPARE, onSelect = onTab, modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun TotalsCard(selected: List<Trip>, palette: List<Color>) {
    val n = Neu
    val max = selected.maxOf { it.totalKrw }.coerceAtLeast(1)
    NeuCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("총액 비교")
        Spacer(Modifier.height(14.dp))
        selected.forEachIndexed { i, t ->
            val c = palette[i % palette.size]
            Row(verticalAlignment = Alignment.Bottom) {
                Text(t.name.ifBlank { "이름 없음" }, color = n.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(t.totalKrw.manwon(), color = c, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(5.dp))
            Gauge(fraction = t.totalKrw.toFloat() / max, color = c, modifier = Modifier.fillMaxWidth(), height = 14.dp, delayMs = 200 + i * 100)
            if (i < selected.size - 1) Spacer(Modifier.height(13.dp))
        }
    }
}

@Composable
private fun TableCard(selected: List<Trip>) {
    val n = Neu
    NeuCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 14.dp, vertical = 15.dp)) {
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
            SectionLabel("항목별")
            Spacer(Modifier.weight(1f))
            Text("단위 만원 · 최저값 강조", color = n.ink2, fontSize = 10.sp)
        }
        Spacer(Modifier.height(10.dp))

        // 헤더
        Row(Modifier.padding(vertical = 6.dp)) {
            Text("항목", color = n.ink2, fontSize = 10.5.sp, modifier = Modifier.weight(1.15f).padding(horizontal = 4.dp))
            selected.forEach { t ->
                Text(
                    t.name.ifBlank { "이름 없음" }, color = n.ink, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End, maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                )
            }
        }

        Category.entries.forEach { c ->
            val values = selected.map { it.costKrw(c) }
            val minV = values.filter { it > 0 }.minOrNull()
            TableRow(label = c.short, values = values, highlight = { v -> minV != null && v == minV && values.count { it == minV } == 1 })
        }

        Divider(thick = true)
        val totals = selected.map { it.totalKrw }
        val minTotal = totals.minOrNull()
        BigRow("총액", totals, highlight = { it == minTotal })
        val perPerson = selected.map { it.perPersonKrw }
        val minPer = perPerson.minOrNull()
        BigRow("1인당", perPerson, highlight = { it == minPer })
    }
}

@Composable
private fun Divider(thick: Boolean = false) {
    val n = Neu
    Box(Modifier.fillMaxWidth().height(if (thick) 2.dp else 1.dp).background(if (thick) n.hint.copy(alpha = 0.5f) else n.line))
}

@Composable
private fun TableRow(label: String, values: List<Long>, highlight: (Long) -> Boolean) {
    val n = Neu
    Divider()
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = n.ink2, fontSize = 11.5.sp, modifier = Modifier.weight(1.15f).padding(horizontal = 4.dp, vertical = 7.dp))
        values.forEach { v ->
            val hi = highlight(v)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .then(if (hi) Modifier.clip(RoundedCornerShape(7.dp)).background(n.accentTint) else Modifier)
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    manNum(v),
                    color = if (hi) n.accent else n.ink,
                    fontSize = 11.5.sp,
                    fontWeight = if (hi) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun BigRow(label: String, values: List<Long>, highlight: (Long) -> Boolean) {
    val n = Neu
    Row(Modifier.padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = n.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.15f).padding(horizontal = 4.dp))
        values.forEach { v ->
            val hi = highlight(v)
            Text(
                manNum(v),
                color = if (hi) n.accent else n.ink,
                fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun SummaryPanel(selected: List<Trip>) {
    val n = Neu
    val cheapest = selected.first()
    val others = selected.drop(1)
    val diffs = others.joinToString(", ") { "${it.name.ifBlank { "이름 없음" }}보다 ${(it.totalKrw - cheapest.totalKrw).manwon()}원" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neuInset(radius = 20.dp, fill = n.accentTint, dark = n.accentShadow.copy(alpha = 0.25f), light = n.shadowLight, offset = 3.dp, blur = 8.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(Icons.Rounded.Star, contentDescription = null, tint = n.accent, modifier = Modifier.size(20.dp))
        Column {
            Text(
                "${cheapest.name.ifBlank { "이름 없음" }}이 가장 저렴함",
                color = n.accentDeep, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text("$diffs 적게 듦", color = n.accentDeep, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
    Spacer(Modifier.width(0.dp))
}
