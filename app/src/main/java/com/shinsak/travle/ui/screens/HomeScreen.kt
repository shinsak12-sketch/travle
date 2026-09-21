package com.shinsak.travle.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.Category
import com.shinsak.travle.data.Trip
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.ui.components.BottomTabBar
import com.shinsak.travle.ui.components.Fab
import com.shinsak.travle.ui.components.InsetPanel
import com.shinsak.travle.ui.components.MoneyText
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.Pill
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.Segmented
import com.shinsak.travle.ui.components.StackedBar
import com.shinsak.travle.ui.components.Stars
import com.shinsak.travle.ui.components.Tab
import com.shinsak.travle.ui.manwon
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.won
import kotlin.math.min

@Composable
fun HomeScreen(
    repo: TripRepository,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onTab: (Tab) -> Unit,
) {
    val n = Neu
    val trips by repo.trips.collectAsStateWithLifecycle()
    val settings by repo.settings.collectAsStateWithLifecycle()
    var sort by rememberSaveable { mutableIntStateOf(0) }

    val sorted = remember(trips, sort) {
        when (sort) {
            0 -> trips.sortedBy { it.totalKrw }
            1 -> trips.sortedByDescending { it.updatedAt }
            else -> trips.sortedWith(compareByDescending<Trip> { it.rating }.thenBy { it.totalKrw })
        }
    }
    val cheapest = trips.minOfOrNull { it.totalKrw }

    Box(Modifier.fillMaxSize().background(n.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {

            Column(Modifier.padding(horizontal = 22.dp).padding(top = 22.dp, bottom = 12.dp)) {
                Text("TRIP BUDGET", color = n.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.4.sp)
                Spacer(Modifier.height(6.dp))
                Text("어디로 갈까", color = n.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "저장한 견적 ${trips.size}개 · 예산 상한 ${settings.budgetCap.manwon()}원",
                    color = n.ink2, fontSize = 13.sp,
                )
            }

            Segmented(
                options = listOf("최저가순", "최신순", "선호도순"),
                selected = sort,
                onSelect = { sort = it },
                modifier = Modifier.padding(horizontal = 22.dp).fillMaxWidth(),
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                if (sorted.isEmpty()) {
                    item {
                        RiseIn {
                            InsetPanel(modifier = Modifier.fillMaxWidth(), radius = 22.dp, padding = PaddingValues(24.dp)) {
                                Text("아직 견적이 없음", color = n.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                Spacer(Modifier.height(6.dp))
                                Text("오른쪽 아래 + 눌러서 첫 여행지를 넣어보세요.", color = n.ink2, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                itemsIndexed(sorted, key = { _, t -> t.id }) { i, trip ->
                    RiseIn(index = min(i, 6)) {
                        TripCard(
                            trip = trip,
                            isCheapest = trips.size > 1 && trip.totalKrw == cheapest,
                            overCap = trip.totalKrw > settings.budgetCap,
                            capGap = trip.totalKrw - settings.budgetCap,
                            perPersonFirst = settings.perPersonFirst,
                            delayMs = 200 + min(i, 6) * 60,
                            onClick = { onOpen(trip.id) },
                        )
                    }
                }
                if (sorted.isNotEmpty()) {
                    item { Legend() }
                }
            }

            BottomTabBar(current = Tab.HOME, onSelect = onTab, modifier = Modifier.navigationBarsPadding())
        }

        Fab(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 24.dp, bottom = 100.dp),
        )
    }
}

@Composable
private fun TripCard(
    trip: Trip,
    isCheapest: Boolean,
    overCap: Boolean,
    capGap: Long,
    perPersonFirst: Boolean,
    delayMs: Int,
    onClick: () -> Unit,
) {
    val n = Neu
    val big = if (perPersonFirst) trip.perPersonKrw else trip.totalKrw
    val small = if (perPersonFirst) trip.totalKrw else trip.perPersonKrw
    val bigColor = if (overCap) n.red else n.ink

    NeuCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(trip.name.ifBlank { "이름 없음" }, color = n.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
            if (isCheapest) Pill("최저가", n.amber)
            if (overCap) Pill("예산 초과", n.red)
            Spacer(Modifier.weight(1f))
            Stars(trip.rating)
        }
        Spacer(Modifier.height(5.dp))
        val meta = buildList {
            add(trip.periodLabel)
            add("${trip.people}인")
            if (trip.month.isNotBlank()) add(trip.month)
            if (trip.flightLabel.isNotEmpty()) add("비행 ${trip.flightLabel}")
        }.joinToString(" · ")
        Text(meta, color = n.ink2, fontSize = 12.sp)
        Spacer(Modifier.height(13.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            MoneyText(big, size = 28, color = bigColor)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                if (overCap) {
                    Text("상한 대비", color = n.ink2, fontSize = 10.sp)
                    Text("+${capGap.won()}원", color = n.red, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(if (perPersonFirst) "총액" else "1인당", color = n.ink2, fontSize = 10.sp)
                    Text("${small.won()}원", color = n.accent, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val total = trip.totalKrw.toFloat().coerceAtLeast(1f)
        val fractions = Category.entries.map { trip.costKrw(it) / total }
        StackedBar(fractions = fractions, colors = n.chart, modifier = Modifier.fillMaxWidth(), delayMs = delayMs)
    }
}

@Composable
private fun Legend() {
    val n = Neu
    Row(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Category.entries.forEachIndexed { i, c ->
            Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(n.chart[i]))
            Text(c.short, color = n.ink2, fontSize = 10.5.sp)
            if (i < Category.entries.size - 1) Spacer(Modifier.width(2.dp))
        }
    }
}
