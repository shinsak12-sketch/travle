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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.CURRENCY_SYMBOL
import com.shinsak.travle.data.ExpCategory
import com.shinsak.travle.data.Expense
import com.shinsak.travle.data.Trip
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.data.settlementLabel
import com.shinsak.travle.ui.components.BottomTabBar
import com.shinsak.travle.ui.components.ConfirmDialog
import com.shinsak.travle.ui.components.MoneyText
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.components.StackedBar
import com.shinsak.travle.ui.components.Tab
import com.shinsak.travle.ui.icon
import com.shinsak.travle.ui.manwon
import com.shinsak.travle.ui.theme.Bricolage
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.neuRaised
import com.shinsak.travle.ui.theme.pressable
import com.shinsak.travle.ui.theme.pressableLong
import com.shinsak.travle.ui.trimZeros
import com.shinsak.travle.ui.won
import kotlin.math.min

@Composable
fun LedgerScreen(
    repo: TripRepository,
    tripId: String,
    onBack: () -> Unit,
    onTab: (Tab) -> Unit,
    onAdd: (day: Int) -> Unit,
    onEdit: (expId: String) -> Unit,
) {
    val n = Neu
    val trips by repo.trips.collectAsStateWithLifecycle()
    val trip = trips.firstOrNull { it.id == tripId }
    var filter by rememberSaveable { mutableIntStateOf(0) } // 0 전체, 1 내가 낸 것, 2 카테고리별
    var deleting by remember { mutableStateOf<Expense?>(null) }

    if (trip == null) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(n.bg))
        return
    }
    val me = trip.companions.first()
    val shown = if (filter == 1) trip.expenses.filter { it.paidBy == me } else trip.expenses
    val fxLabel = if (trip.currency != "KRW") " · ${trip.currency} 1 = ${repo.fxRate(trip.currency).trimZeros()}원" else ""

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding()) {
        ScreenHeader(title = "가계부", subtitle = "${trip.title} · ${trip.people}명$fxLabel")

        Row(Modifier.padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("기간 전체", "내가 낸 것", "카테고리").forEachIndexed { i, label ->
                val on = i == filter
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .pressable { filter = i }
                        .then(
                            if (on) Modifier.neuRaised(radius = 12.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 8.dp)
                            else Modifier.neuInset(radius = 12.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp)
                        )
                        .padding(horizontal = 13.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(label, color = if (on) n.accent else n.ink2, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium) }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (filter == 2) {
                item {
                    RiseIn(0) {
                        NeuCard(modifier = Modifier.fillMaxWidth()) {
                            SectionLabel("카테고리별")
                            Spacer(Modifier.height(10.dp))
                            val total = trip.totalKrw.coerceAtLeast(1)
                            ExpCategory.entries.forEachIndexed { i, c ->
                                val v = trip.expenses.filter { it.category == c }.sumOf { it.krw }
                                if (v > 0) {
                                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(n.chart[i % n.chart.size]))
                                        Spacer(Modifier.width(8.dp))
                                        Text(c.label, color = n.ink, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                                        Text("${(v * 100 / total)}%", color = n.ink2, fontSize = 11.sp)
                                        Spacer(Modifier.width(10.dp))
                                        Text("${v.won()}원", color = n.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            if (trip.expenses.isEmpty()) Text("아직 지출이 없음", color = n.hint, fontSize = 12.5.sp)
                        }
                    }
                }
            } else {
                for (day in 0..trip.days) {
                    val list = shown.filter { it.dayIndex == day }.sortedBy { it.createdAt }
                    item(key = "d$day") {
                        RiseIn(min(day, 6)) {
                            Column {
                                Row(Modifier.padding(horizontal = 4.dp, vertical = 0.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (day == 0) {
                                        Text("여행준비", color = n.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        val date = trip.dateOf(day)
                                        Text("day $day", color = n.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                        Text("${date.monthValue}.${date.dayOfMonth} ${Trip.dow(date)}", color = n.ink2, fontSize = 12.sp, modifier = Modifier.padding(bottom = 1.dp))
                                    }
                                    val sum = list.sumOf { it.krw }
                                    if (sum > 0) Text("· ${sum.manwon()}", color = n.ink2, fontSize = 11.5.sp, modifier = Modifier.padding(bottom = 1.dp))
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    list.forEach { e -> ExpenseRow(e, onLong = { deleting = e }) { onEdit(e.id) } }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .pressable { onAdd(day) }
                                            .neuInset(radius = 15.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, tint = n.accent, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("비용 추가", color = n.accent, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 22.dp).padding(bottom = 8.dp)) {
            NeuCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column {
                        SectionLabel("총 지출")
                        Spacer(Modifier.height(2.dp))
                        MoneyText(trip.totalKrw, size = 26)
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        SectionLabel("1인당")
                        Spacer(Modifier.height(4.dp))
                        Text("${trip.perPersonKrw.won()}원", color = n.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                val total = trip.totalKrw.toFloat().coerceAtLeast(1f)
                val fractions = ExpCategory.entries.map { c -> trip.expenses.filter { it.category == c }.sumOf { it.krw } / total }
                if (trip.totalKrw > 0) {
                    Spacer(Modifier.height(10.dp))
                    StackedBar(fractions = fractions, colors = n.chart, modifier = Modifier.fillMaxWidth(), height = 10.dp, delayMs = 150)
                }
                val settle = trip.settlementLabel()
                if (settle.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("정산: $settle", color = n.amber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        BottomTabBar(current = Tab.LEDGER, onSelect = onTab, modifier = Modifier.navigationBarsPadding())
    }

    deleting?.let { e ->
        ConfirmDialog(
            title = "이 지출을 지울까요?", message = "\"${e.title.ifBlank { e.category.label }}\" ${e.krw.won()}원", confirmText = "삭제", destructive = true,
            onConfirm = { repo.update(tripId) { t -> t.copy(expenses = t.expenses.filter { it.id != e.id }) }; deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun ExpenseRow(e: Expense, onLong: () -> Unit, onClick: () -> Unit) {
    val n = Neu
    val ci = ExpCategory.entries.indexOf(e.category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressableLong(onLongClick = onLong, onClick = onClick)
            .neuRaised(radius = 18.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 5.dp, blur = 13.dp)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(n.chart[ci % n.chart.size].copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(e.category.icon(), contentDescription = null, tint = n.chart[ci % n.chart.size], modifier = Modifier.size(15.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(e.title.ifBlank { e.category.label }, color = n.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            val split = if (e.splitWith.size > 1) "${e.splitWith.size}명 나눔" else if (e.splitWith.size == 1 && e.splitWith.first() != e.paidBy) "${e.splitWith.first()} 몫" else "안 나눔"
            Text("${e.category.label} · ${e.method.label} · ${e.paidBy} · $split", color = n.ink2, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            if (e.currency == "KRW") {
                Text(e.krw.won(), color = n.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            } else {
                Text("${CURRENCY_SYMBOL[e.currency] ?: e.currency}${e.amount.trimZeros()}", color = n.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("${e.krw.won()}원", color = n.ink2, fontSize = 10.5.sp)
            }
        }
    }
}
