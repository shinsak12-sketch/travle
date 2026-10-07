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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.PlanItem
import com.shinsak.travle.data.Trip
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.data.forDay
import com.shinsak.travle.ui.askClaude
import com.shinsak.travle.ui.components.AccentButton
import com.shinsak.travle.ui.components.BackButton
import com.shinsak.travle.ui.components.BottomTabBar
import com.shinsak.travle.ui.components.ConfirmDialog
import com.shinsak.travle.ui.components.Gauge
import com.shinsak.travle.ui.components.InsetField
import com.shinsak.travle.ui.components.NeuButton
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.NeuIconButton
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.components.Tab
import com.shinsak.travle.ui.formatTime
import com.shinsak.travle.ui.formatTyped
import com.shinsak.travle.ui.color
import com.shinsak.travle.ui.icon
import com.shinsak.travle.ui.trimZeros
import com.shinsak.travle.data.CURRENCY_SYMBOL
import com.shinsak.travle.ui.manwon
import com.shinsak.travle.ui.openMap
import com.shinsak.travle.ui.parseAmount
import com.shinsak.travle.ui.theme.Bricolage
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.neuRaised
import com.shinsak.travle.ui.theme.pressable
import com.shinsak.travle.ui.theme.pressableLong
import com.shinsak.travle.ui.won
import kotlin.math.min
import kotlin.math.roundToLong

@Composable
fun TripHomeScreen(
    repo: TripRepository,
    tripId: String,
    onBack: () -> Unit,
    onTab: (Tab) -> Unit,
    onAddPlace: (day: Int) -> Unit,
    onEditPlace: (itemId: String) -> Unit,
) {
    val n = Neu
    val ctx = LocalContext.current
    val trips by repo.trips.collectAsStateWithLifecycle()
    val trip = trips.firstOrNull { it.id == tripId }
    var editTitle by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var memoDay by remember { mutableStateOf<Int?>(null) }
    var deletingItem by remember { mutableStateOf<PlanItem?>(null) }

    if (trip == null) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(n.bg))
        return
    }

    val d = trip.dDay
    val dLabel = when {
        d > 0 -> "D-$d"
        d == 0L -> "D-DAY"
        trip.isPast -> "다녀옴"
        else -> "여행 중"
    }
    val capFrac = if (trip.budgetCap > 0) trip.totalKrw.toFloat() / trip.budgetCap else 0f

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 22.dp).padding(top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(onBack)
            Column(Modifier.weight(1f).pressable { editTitle = true }) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(trip.title.ifBlank { "${trip.city} 여행" }, color = n.ink, fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    Text("편집", color = n.accent, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text("${trip.periodLabel} · ${trip.nights}박 ${trip.days}일 · ${trip.people}명 · $dLabel", color = n.ink2, fontSize = 11.5.sp, maxLines = 1)
            }
            NeuIconButton(
                Icons.Rounded.AutoAwesome, contentDescription = "클로드에게 물어보기", tint = n.amber,
                onClick = { askClaude(ctx, "${trip.city} ${trip.nights}박 ${trip.days}일 여행 일정 추천해줘. 날짜는 ${trip.periodLabel}, ${trip.people}명이야. 동선 위주로 day별로 정리해줘.") },
            )
        }

        Row(Modifier.padding(horizontal = 22.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            StatChip("항공", trip.flight.outbound?.dep?.let { "$it ✓" } ?: "+ 추가", trip.flight.outbound != null, Modifier.weight(1f)) { onTab(Tab.BOOKING) }
            StatChip("숙소", if (trip.stay.isEmpty) "+ 추가" else "✓", !trip.stay.isEmpty, Modifier.weight(1f)) { onTab(Tab.BOOKING) }
            StatChip("체크", "${trip.checksDone} / ${trip.checks.size}", trip.checks.isNotEmpty() && trip.checksDone == trip.checks.size, Modifier.weight(1f)) { onTab(Tab.CHECK) }
            StatChip("지출", trip.totalKrw.manwon(), false, Modifier.weight(1f)) { onTab(Tab.LEDGER) }
        }

        Column(Modifier.padding(horizontal = 26.dp).padding(top = 10.dp)) {
            Row {
                Text("예산 ${trip.budgetCap.manwon()}", color = n.ink2, fontSize = 10.5.sp)
                Spacer(Modifier.weight(1f))
                val left = trip.budgetCap - trip.totalKrw
                Text(
                    if (left >= 0) "${(capFrac * 100).roundToLong()}% 씀 · ${left.manwon()} 남음" else "${(-left).manwon()} 초과",
                    color = if (left >= 0) n.accent else n.red, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(5.dp))
            Gauge(fraction = capFrac, color = if (capFrac > 1f) n.red else n.accent, modifier = Modifier.fillMaxWidth(), height = 10.dp, delayMs = 150)
        }

        // Day 스위처
        var day by rememberSaveable(trip.id) { mutableIntStateOf(initialDay(trip)) }
        Row(Modifier.padding(horizontal = 22.dp).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (d in 1..trip.days) {
                val on = d == day
                val date = trip.dateOf(d)
                val cnt = trip.items.count { it.dayIndex == d }
                Column(
                    modifier = Modifier.weight(1f)
                        .pressable { day = d }
                        .neuRaised(radius = 13.dp, fill = if (on) n.ink else n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 2.dp, blur = 7.dp)
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${date.monthValue}.${date.dayOfMonth} ${Trip.dow(date)}", color = if (on) n.surface.copy(alpha = 0.75f) else n.ink2, fontSize = 9.5.sp, maxLines = 1)
                    Text("Day $d", color = if (on) n.surface else n.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1)
                    if (cnt > 0) Text("$cnt", color = if (on) n.surface.copy(alpha = 0.75f) else n.hint, fontSize = 9.sp)
                }
            }
        }

        val items = trip.items.forDay(day)
        val dayCost = items.filter { !it.isMemo && it.cost > 0 }.sumOf { it.cost * repo.fxRate(it.costCurrency) } * trip.people
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 12.dp),
        ) {
            item(key = "head$day") {
                Row(Modifier.padding(start = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
                    Text("${items.count { !it.isMemo }}곳 · 메모 ${items.count { it.isMemo }}", color = n.ink2, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    if (dayCost > 0) Text("예상 ${dayCost.roundToLong().manwon()} (${trip.people}명)", color = n.ink2, fontSize = 11.sp)
                }
            }
            itemsIndexed(items, key = { _, it -> it.id }) { i, it ->
                RiseIn(min(i, 6)) {
                    TimelineRow(
                        item = it, isLast = i == items.lastIndex,
                        onEdit = { if (!it.auto) onEditPlace(it.id) },
                        onLong = { if (!it.auto) deletingItem = it },
                        onOpenMap = { openMap(ctx, it.mapLink.ifBlank { it.address }) },
                        onAsk = { askClaude(ctx, "${trip.city} \"${it.name}\" 지금 운영시간, 가격, 가는 방법, 주의할 점 알려줘.") },
                    )
                }
            }
            item(key = "add$day") {
                RiseIn(min(items.size, 6)) {
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AddRow("장소 추가", n.accent, Modifier.weight(1f)) { onAddPlace(day) }
                        AddRow("메모 추가", n.ink2, Modifier.weight(1f)) { memoDay = day }
                    }
                }
            }
            item {
                Text(
                    "여행 삭제", color = n.red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().pressable { askDelete = true }.padding(top = 18.dp, bottom = 6.dp),
                )
            }
        }

        BottomTabBar(current = Tab.PLAN, onSelect = onTab, modifier = Modifier.navigationBarsPadding())
    }

    if (editTitle) {
        EditTripDialog(trip = trip, onDismiss = { editTitle = false }) { title, cap ->
            repo.update(trip.id) { it.copy(title = title, budgetCap = cap) }
            editTitle = false
        }
    }
    memoDay?.let { day ->
        MemoDialog(dayLabel = "day $day", onDismiss = { memoDay = null }) { time, text ->
            repo.update(trip.id) { it.copy(items = it.items + PlanItem(dayIndex = day, isMemo = true, time = time, name = text)) }
            memoDay = null
        }
    }
    deletingItem?.let { item ->
        ConfirmDialog(
            title = "이 항목을 지울까요?", message = "\"${item.name}\"이 day ${item.dayIndex}에서 빠짐.", confirmText = "삭제", destructive = true,
            onConfirm = { repo.update(trip.id) { t -> t.copy(items = t.items.filter { it.id != item.id }) }; deletingItem = null },
            onDismiss = { deletingItem = null },
        )
    }
    if (askDelete) {
        ConfirmDialog(
            title = "이 여행을 지울까요?",
            message = "\"${trip.title}\"의 일정·가계부·체크리스트가 전부 삭제됨. 되돌릴 수 없음.",
            confirmText = "삭제", destructive = true,
            onConfirm = { askDelete = false; repo.delete(trip.id); onBack() },
            onDismiss = { askDelete = false },
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, good: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val n = Neu
    Column(
        modifier = modifier
            .pressable(onClick = onClick)
            .neuRaised(radius = 15.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 11.dp)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = n.ink2, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = if (good) n.accent else if (value.startsWith("+")) n.amber else n.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** 오늘이 여행 중이면 그 날, 아니면 Day 1 */
private fun initialDay(trip: Trip): Int {
    val today = java.time.LocalDate.now()
    for (d in 1..trip.days) if (trip.dateOf(d) == today) return d
    return 1
}

@Composable
private fun TimelineRow(item: PlanItem, isLast: Boolean, onEdit: () -> Unit, onLong: () -> Unit, onOpenMap: () -> Unit, onAsk: () -> Unit) {
    val n = Neu
    val dot = if (item.isMemo) n.hint else if (item.auto) n.chart[1] else item.category.color(n)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // 선 + 점
        Box(Modifier.width(22.dp).fillMaxHeight()) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 22.dp).width(2.dp).fillMaxHeight().background(if (isLast) androidx.compose.ui.graphics.Color.Transparent else n.line))
            Box(Modifier.align(Alignment.TopCenter).padding(top = 16.dp).size(12.dp).clip(RoundedCornerShape(6.dp)).background(n.bg).padding(2.dp).clip(RoundedCornerShape(5.dp)).background(dot))
        }
        Column(Modifier.weight(1f).padding(start = 6.dp, bottom = if (item.transit.isNotBlank()) 0.dp else 10.dp)) {
            if (item.isMemo) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .pressableLong(onLongClick = onLong, onClick = onEdit)
                        .neuInset(radius = 14.dp, fill = n.surface.copy(alpha = 0.55f), dark = n.shadowDark, light = n.shadowLight)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(item.time.ifBlank { "—" }, color = n.hint, fontFamily = Bricolage, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(40.dp))
                    Text(item.name, color = n.ink2, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .pressableLong(onLongClick = onLong, onClick = onEdit)
                        .neuRaised(radius = 16.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 12.dp)
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(item.time.ifBlank { "—" }, color = n.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.width(40.dp))
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(dot.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(item.category.icon(), contentDescription = null, tint = dot, modifier = Modifier.size(15.dp))
                        }
                        Text(item.name, color = n.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                        Text(if (item.auto) "항공" else item.category.label, color = dot, fontSize = 9.5.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(dot.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    // 정보 줄: 요금 · 소요 · 운영시간/메모 · 주소
                    val facts = buildList {
                        if (item.cost > 0) add(if (item.costCurrency == "KRW") "${item.cost.roundToLong().won()}원" else "${CURRENCY_SYMBOL[item.costCurrency] ?: ""}${item.cost.trimZeros()}")
                        if (item.hours > 0) add("${item.hours.trimZeros()}시간")
                    }
                    if (facts.isNotEmpty() || item.note.isNotBlank() || item.address.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Column(Modifier.padding(start = 50.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (facts.isNotEmpty()) Text(facts.joinToString("  ·  "), color = n.ink, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            if (item.note.isNotBlank()) Text(item.note, color = n.ink2, fontSize = 11.5.sp, lineHeight = 16.sp, maxLines = 4)
                            if (item.address.isNotBlank()) Text("📍 ${item.address}", color = n.hint, fontSize = 10.5.sp, maxLines = 1)
                        }
                    }
                    if (!item.auto) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.padding(start = 50.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (item.mapLink.isNotBlank() || item.address.isNotBlank()) MiniAction("지도", Icons.Rounded.Map, onOpenMap)
                            MiniAction("물어보기", Icons.Rounded.AutoAwesome, onAsk)
                        }
                    }
                }
            }
            if (item.transit.isNotBlank() && !isLast) {
                Text("↓ ${item.transit}", color = n.ink2, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 8.dp))
            } else if (item.transit.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun MiniAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val n = Neu
    Row(
        modifier = Modifier.height(26.dp).pressable(onClick = onClick).clip(RoundedCornerShape(8.dp)).background(n.well).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = n.ink2, modifier = Modifier.size(12.dp))
        Text(text, color = n.ink2, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AddRow(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    val n = Neu
    Row(
        modifier = modifier
            .height(42.dp)
            .pressable(onClick = onClick)
            .neuInset(radius = 14.dp, fill = n.surface.copy(alpha = 0.6f), dark = n.shadowDark, light = n.shadowLight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = color, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EditTripDialog(trip: Trip, onDismiss: () -> Unit, onSave: (String, Long) -> Unit) {
    val n = Neu
    var title by remember { mutableStateOf(trip.title) }
    var cap by remember { mutableStateOf(formatTyped(trip.budgetCap.toString())) }
    Dialog(onDismissRequest = onDismiss) {
        NeuCard(radius = 26.dp, padding = PaddingValues(22.dp)) {
            Text("여행 정보", color = n.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            SectionLabel("제목", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
            InsetField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), fontSize = 15)
            Spacer(Modifier.height(12.dp))
            SectionLabel("예산 상한", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
            InsetField(value = cap, onValueChange = { cap = formatTyped(it) }, modifier = Modifier.fillMaxWidth(), keyboardType = KeyboardType.Number, suffix = "원", textAlign = TextAlign.End, fontSize = 15)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeuButton("취소", onClick = onDismiss, modifier = Modifier.weight(1f), color = n.ink2)
                AccentButton("저장", onClick = { onSave(title.trim(), parseAmount(cap).roundToLong().coerceAtLeast(0)) }, modifier = Modifier.weight(1f), height = 46.dp, radius = 17.dp)
            }
        }
    }
}

@Composable
private fun MemoDialog(dayLabel: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val n = Neu
    var time by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        NeuCard(radius = 26.dp, padding = PaddingValues(22.dp)) {
            Text("$dayLabel 메모", color = n.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InsetField(value = time, onValueChange = { time = formatTime(it) }, modifier = Modifier.width(92.dp), placeholder = "09:30", keyboardType = KeyboardType.Number, textAlign = TextAlign.Center, fontSize = 14)
                InsetField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), placeholder = "예) 디디 앱 미리 깔기", fontSize = 14)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeuButton("취소", onClick = onDismiss, modifier = Modifier.weight(1f), color = n.ink2)
                AccentButton("추가", onClick = { if (text.isNotBlank()) onSave(time, text.trim()) }, modifier = Modifier.weight(1f), height = 46.dp, radius = 17.dp, enabled = text.isNotBlank())
            }
        }
    }
}
