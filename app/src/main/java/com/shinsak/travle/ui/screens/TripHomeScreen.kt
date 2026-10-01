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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
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
import com.shinsak.travle.ui.icon
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

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            for (day in 1..trip.days) {
                item(key = "day$day") {
                    RiseIn(min(day - 1, 6)) {
                        DaySection(
                            trip = trip, day = day, fx = { repo.fxRate(it) },
                            onEdit = onEditPlace,
                            onLong = { item -> if (!item.auto) deletingItem = item },
                            onAddPlace = { onAddPlace(day) },
                            onAddMemo = { memoDay = day },
                            onOpenMap = { openMap(ctx, it) },
                        )
                    }
                }
            }
            item {
                Text(
                    "여행 삭제", color = n.red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().pressable { askDelete = true }.padding(vertical = 10.dp),
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

@Composable
private fun DaySection(
    trip: Trip, day: Int, fx: (String) -> Double,
    onEdit: (String) -> Unit, onLong: (PlanItem) -> Unit, onAddPlace: () -> Unit, onAddMemo: () -> Unit, onOpenMap: (String) -> Unit,
) {
    val n = Neu
    val date = trip.dateOf(day)
    val items = trip.items.forDay(day)
    Column {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 0.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("day $day", color = n.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            Text("${date.monthValue}.${date.dayOfMonth} ${Trip.dow(date)}", color = n.ink2, fontSize = 12.sp, modifier = Modifier.padding(bottom = 1.dp))
            Spacer(Modifier.weight(1f))
            val dayCost = items.filter { !it.isMemo && it.cost > 0 }.sumOf { it.cost * fx(it.costCurrency) } * trip.people
            if (dayCost > 0) Text("예상 ${dayCost.roundToLong().manwon()}", color = n.ink2, fontSize = 10.5.sp, modifier = Modifier.padding(bottom = 1.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { it ->
                if (it.isMemo) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressableLong(onLongClick = { onLong(it) }) { onEdit(it.id) }
                            .neuInset(radius = 18.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 9.dp)
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(it.time.ifBlank { "—" }, color = n.hint, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.width(46.dp))
                        Text(it.name, color = n.ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("메모", color = n.hint, fontSize = 10.5.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressableLong(onLongClick = { onLong(it) }) { if (!it.auto) onEdit(it.id) }
                            .neuRaised(radius = 18.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 5.dp, blur = 13.dp)
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(it.time.ifBlank { "—" }, color = n.accent, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.width(46.dp))
                        Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(if (it.auto) n.accentTint else n.well), contentAlignment = Alignment.Center) {
                            Icon(it.category.icon(), contentDescription = null, tint = if (it.auto) n.accent else n.amber, modifier = Modifier.size(15.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(it.name, color = n.ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            val sub = buildList {
                                if (!it.auto) add(it.category.label)
                                if (it.cost > 0) add("${com.shinsak.travle.data.CURRENCY_SYMBOL[it.costCurrency] ?: ""}${it.cost.let { c -> if (c == Math.floor(c)) c.toLong().toString() else c.toString() }}")
                                if (it.hours > 0) add("${it.hours.let { h -> if (h == Math.floor(h)) h.toLong().toString() else h.toString() }}시간")
                                if (it.note.isNotBlank()) add(it.note)
                            }.joinToString(" · ")
                            if (sub.isNotBlank()) Text(sub, color = n.ink2, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 1.dp))
                        }
                        if (it.mapLink.isNotBlank() || it.address.isNotBlank()) {
                            Box(Modifier.size(32.dp).pressable { onOpenMap(it.mapLink.ifBlank { it.address }) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Map, contentDescription = "지도 열기", tint = n.hint, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AddRow("장소 추가", n.accent, Modifier.weight(1f), onAddPlace)
                AddRow("메모 추가", n.ink2, Modifier.weight(1f), onAddMemo)
            }
        }
    }
}

@Composable
private fun AddRow(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    val n = Neu
    Row(
        modifier = modifier
            .height(42.dp)
            .pressable(onClick = onClick)
            .neuInset(radius = 15.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 9.dp),
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
