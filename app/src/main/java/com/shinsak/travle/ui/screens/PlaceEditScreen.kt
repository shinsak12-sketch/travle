package com.shinsak.travle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Link
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.CURRENCY_SYMBOL
import com.shinsak.travle.data.PlaceCategory
import com.shinsak.travle.data.PlanItem
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.ui.askClaude
import com.shinsak.travle.ui.components.AccentButton
import com.shinsak.travle.ui.components.ChipRow
import com.shinsak.travle.ui.components.ConfirmDialog
import com.shinsak.travle.ui.components.InsetField
import com.shinsak.travle.ui.components.NeuButton
import com.shinsak.travle.ui.components.NeuIconButton
import com.shinsak.travle.ui.components.PasteButton
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.components.Segmented
import com.shinsak.travle.ui.formatTime
import com.shinsak.travle.ui.openMap
import com.shinsak.travle.ui.parseAmount
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.neuRaised
import com.shinsak.travle.ui.theme.pressable
import com.shinsak.travle.ui.trimZeros
import com.shinsak.travle.ui.won
import kotlin.math.roundToLong

private val TIME_CHIPS = listOf("08:00", "09:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaceEditScreen(
    repo: TripRepository,
    tripId: String,
    itemId: String?,
    initialDay: Int,
    onBack: () -> Unit,
) {
    val n = Neu
    val ctx = LocalContext.current
    val trips by repo.trips.collectAsStateWithLifecycle()
    val settings by repo.settings.collectAsStateWithLifecycle()
    val trip0 = trips.firstOrNull { it.id == tripId }
    if (trip0 == null) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(n.bg))
        return
    }
    val trip = trip0
    val existing = remember(itemId) { trip.items.firstOrNull { it.id == itemId } }

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var isMemo by rememberSaveable { mutableStateOf(existing?.isMemo ?: false) }
    var day by rememberSaveable { mutableIntStateOf(existing?.dayIndex ?: initialDay) }
    var time by rememberSaveable { mutableStateOf(existing?.time ?: "") }
    var cat by rememberSaveable { mutableStateOf(existing?.category ?: PlaceCategory.SIGHT) }
    var mapLink by rememberSaveable { mutableStateOf(existing?.mapLink ?: "") }
    var address by rememberSaveable { mutableStateOf(existing?.address ?: "") }
    var cost by rememberSaveable { mutableStateOf(existing?.cost?.takeIf { it > 0 }?.trimZeros() ?: "") }
    var costCur by rememberSaveable { mutableStateOf(existing?.costCurrency ?: trip.currency) }
    var hours by rememberSaveable { mutableStateOf(existing?.hours?.takeIf { it > 0 }?.trimZeros() ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var askDelete by remember { mutableStateOf(false) }

    val rate = if (costCur == "KRW") 1.0 else (settings.fxRates[costCur] ?: 1.0)
    val costKrw = (parseAmount(cost) * rate).roundToLong()

    fun save() {
        if (name.isBlank()) return
        val item = PlanItem(
            id = existing?.id ?: PlanItem().id, dayIndex = day, isMemo = isMemo, time = time.trim(), name = name.trim(),
            category = cat, address = address.trim(), mapLink = mapLink.trim(), cost = parseAmount(cost), costCurrency = costCur,
            hours = parseAmount(hours), note = note.trim(),
        )
        repo.update(tripId) { t -> t.copy(items = t.items.filter { it.id != item.id } + item) }
        onBack()
    }

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding().imePadding()) {
        ScreenHeader(title = if (existing == null) "장소 추가" else "장소 수정", subtitle = "day $day · ${trip.dateOf(day).let { "${it.monthValue}.${it.dayOfMonth}" }}", onBack = onBack) {
            NeuIconButton(
                Icons.Rounded.AutoAwesome, contentDescription = "클로드에게 물어보기", tint = n.amber,
                onClick = {
                    val q = if (name.isBlank()) "${trip.city}에서 day $day 에 갈 만한 ${cat.label} 추천해줘. 이동 동선이랑 예상 비용도." else "${trip.city} \"$name\" 운영시간, 입장료, 가는 법, 소요시간 알려줘."
                    askClaude(ctx, q)
                },
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            RiseIn(0) {
                Column {
                    SectionLabel("이름", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                    InsetField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), placeholder = if (isMemo) "메모 내용" else "예) 선양 고궁", fontSize = 16)
                }
            }
            RiseIn(1) {
                Column {
                    SectionLabel("종류", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                    Segmented(options = listOf("장소", "메모"), selected = if (isMemo) 1 else 0, onSelect = { isMemo = it == 1 }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (!isMemo) {
                RiseIn(2) {
                    Column {
                        SectionLabel("카테고리", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PlaceCategory.entries.forEach { c ->
                                val on = c == cat
                                Box(
                                    modifier = Modifier
                                        .height(38.dp)
                                        .pressable { cat = c }
                                        .then(
                                            if (on) Modifier.neuRaised(radius = 13.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 8.dp)
                                            else Modifier.neuInset(radius = 13.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp)
                                        )
                                        .padding(horizontal = 13.dp),
                                    contentAlignment = Alignment.Center,
                                ) { Text(c.label, color = if (on) n.accent else n.ink2, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium) }
                            }
                        }
                    }
                }
            }
            RiseIn(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("날짜", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                        Segmented(options = (1..trip.days).map { "day $it" }, selected = (day - 1).coerceIn(0, trip.days - 1), onSelect = { day = it + 1 }, modifier = Modifier.fillMaxWidth())
                    }
                    Column(Modifier.width(110.dp)) {
                        SectionLabel("시간", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                        InsetField(value = time, onValueChange = { time = formatTime(it) }, modifier = Modifier.fillMaxWidth(), height = 48.dp, placeholder = "09:30", keyboardType = KeyboardType.Number, textAlign = TextAlign.Center, fontSize = 15)
                    }
                }
            }
            RiseIn(3) {
                ChipRow(options = TIME_CHIPS, selected = TIME_CHIPS.indexOf(time), onSelect = { time = TIME_CHIPS[it] }, height = 34.dp)
            }
            if (!isMemo) {
                RiseIn(4) {
                    Column {
                        SectionLabel("지도 링크 · 주소", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            InsetField(
                                value = mapLink, onValueChange = { mapLink = it }, modifier = Modifier.weight(1f), height = 48.dp,
                                placeholder = "구글맵·바이두맵 링크 붙여넣기", fontSize = 13,
                                leading = { Icon(Icons.Rounded.Link, contentDescription = null, tint = n.hint, modifier = Modifier.size(16.dp)) },
                            )
                            if (mapLink.isBlank()) PasteButton(onPaste = { mapLink = it }, height = 48.dp)
                            else NeuButton("열기", onClick = { openMap(ctx, mapLink.ifBlank { address }) }, height = 48.dp, radius = 15.dp)
                        }
                        Spacer(Modifier.height(8.dp))
                        InsetField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth(), height = 44.dp, placeholder = "주소 (링크 없으면 주소로 검색됨)", fontSize = 13)
                        Text("앱 자체는 인터넷 안 씀. '열기'는 폰에 깔린 지도앱으로 넘김.", color = n.ink2, fontSize = 10.5.sp, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
                    }
                }
                RiseIn(5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            SectionLabel("예상 비용 (1인)", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                            InsetField(
                                value = cost, onValueChange = { cost = it.filter { c -> c.isDigit() || c == '.' } }, modifier = Modifier.fillMaxWidth(), height = 46.dp,
                                keyboardType = KeyboardType.Decimal, textAlign = TextAlign.End, fontSize = 14, placeholder = "0",
                                leading = {
                                    Text(costCur, color = n.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.pressable {
                                        val list = listOf(trip.currency, "KRW").distinct()
                                        costCur = list[(list.indexOf(costCur) + 1) % list.size]
                                    })
                                },
                                suffix = if (costCur != "KRW" && costKrw > 0) "≈ ${costKrw.won()}원" else CURRENCY_SYMBOL[costCur] ?: "",
                            )
                        }
                        Column(Modifier.width(110.dp)) {
                            SectionLabel("머무는 시간", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                            InsetField(value = hours, onValueChange = { hours = it.filter { c -> c.isDigit() || c == '.' } }, modifier = Modifier.fillMaxWidth(), height = 46.dp, keyboardType = KeyboardType.Decimal, textAlign = TextAlign.End, fontSize = 14, suffix = "시간", placeholder = "0")
                        }
                    }
                }
            }
            RiseIn(6) {
                Column {
                    SectionLabel("메모", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                    InsetField(value = note, onValueChange = { note = it }, modifier = Modifier.fillMaxWidth(), placeholder = "휴관일, 결제 방법, 팁 같은 것", singleLine = false, fontSize = 13)
                }
            }
        }

        Row(Modifier.padding(horizontal = 22.dp).padding(bottom = 12.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentButton(text = "day ${day}에 넣기", onClick = { save() }, modifier = Modifier.weight(1f), height = 54.dp, enabled = name.isNotBlank())
            if (existing != null) {
                NeuIconButton(Icons.Rounded.Delete, contentDescription = "삭제", onClick = { askDelete = true }, size = 54.dp, radius = 19.dp, tint = n.red)
            }
        }
    }

    if (askDelete && existing != null) {
        ConfirmDialog(
            title = "이 항목을 지울까요?", message = "\"${existing.name}\"이 일정에서 빠짐.", confirmText = "삭제", destructive = true,
            onConfirm = { askDelete = false; repo.update(tripId) { t -> t.copy(items = t.items.filter { it.id != existing.id }) }; onBack() },
            onDismiss = { askDelete = false },
        )
    }
}
