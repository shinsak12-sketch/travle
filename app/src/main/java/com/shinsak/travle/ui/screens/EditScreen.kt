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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.CURRENCIES
import com.shinsak.travle.data.CURRENCY_LABEL
import com.shinsak.travle.data.Category
import com.shinsak.travle.data.ParsedFlight
import com.shinsak.travle.data.Trip
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.ui.ScanBusyDialog
import com.shinsak.travle.ui.ScanResultDialog
import com.shinsak.travle.ui.rememberScreenshotScanner
import com.shinsak.travle.ui.components.AccentButton
import com.shinsak.travle.ui.components.InsetField
import com.shinsak.travle.ui.components.NeuButton
import com.shinsak.travle.ui.components.NeuIconButton
import com.shinsak.travle.ui.components.MoneyText
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.components.Stars
import com.shinsak.travle.ui.components.Stepper
import com.shinsak.travle.ui.formatTyped
import com.shinsak.travle.ui.parseAmount
import com.shinsak.travle.ui.parseIntSafe
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.neuRaised
import com.shinsak.travle.ui.theme.neuRaisedAccent
import com.shinsak.travle.ui.theme.pressable
import com.shinsak.travle.ui.trimZeros
import com.shinsak.travle.ui.won
import kotlin.math.roundToLong

@Composable
fun EditScreen(
    repo: TripRepository,
    tripId: String?,
    autoScan: Boolean = false,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val n = Neu
    val settings by repo.settings.collectAsStateWithLifecycle()
    val existing = remember(tripId) { repo.trip(tripId) }

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var month by rememberSaveable { mutableStateOf(existing?.month ?: "") }
    var nights by rememberSaveable { mutableIntStateOf(existing?.nights ?: 3) }
    var days by rememberSaveable { mutableIntStateOf(existing?.days ?: 4) }
    var people by rememberSaveable { mutableIntStateOf(existing?.people ?: 2) }
    var currency by rememberSaveable { mutableStateOf(existing?.currency ?: "KRW") }
    var fx by rememberSaveable { mutableStateOf(existing?.fxRate?.trimZeros() ?: "1") }
    var flightH by rememberSaveable { mutableStateOf(existing?.let { (it.flightMinutes / 60).takeIf { h -> h > 0 }?.toString() } ?: "") }
    var flightM by rememberSaveable { mutableStateOf(existing?.let { (it.flightMinutes % 60).takeIf { m -> m > 0 }?.toString() } ?: "") }
    var rating by rememberSaveable { mutableIntStateOf(existing?.rating ?: 3) }
    var memo by rememberSaveable { mutableStateOf(existing?.memo ?: "") }
    var nameError by remember { mutableStateOf(false) }

    val costs = remember {
        mutableStateMapOf<Category, String>().apply {
            Category.entries.forEach { c ->
                val v = existing?.costs?.get(c)
                put(c, if (v != null && v > 0) formatTyped(v.trimZeros()) else "")
            }
        }
    }

    val ctx = LocalContext.current
    var scanResult by remember { mutableStateOf<ParsedFlight?>(null) }
    val scanner = rememberScreenshotScanner { parsed ->
        if (parsed == null || parsed.isEmpty) {
            Toast.makeText(ctx, "스크린샷에서 항공권 정보를 못 찾았음", Toast.LENGTH_SHORT).show()
        } else {
            scanResult = parsed
        }
    }
    LaunchedEffect(Unit) { if (autoScan) scanner.launch() }

    val rate = if (currency == "KRW") 1.0 else parseAmount(fx)
    val sumForeign = Category.entries.sumOf { parseAmount(costs[it] ?: "") }
    val totalKrw = (sumForeign * rate).roundToLong()
    val perPerson = if (people > 0) totalKrw / people else totalKrw

    fun applyParsed(p: ParsedFlight) {
        p.destination?.let { name = it }
        p.dateLabel?.let { month = it }
        p.nights?.let { nn ->
            nights = nn
            days = p.days ?: (nn + 1)
        }
        p.people?.let { people = it }
        p.totalPrice?.let { total ->
            val v = if (currency == "KRW") total.toDouble() else total / (rate.takeIf { it > 0 } ?: 1.0)
            costs[Category.FLIGHT] = formatTyped(v.trimZeros())
        }
        p.outboundMinutes?.let {
            flightH = (it / 60).takeIf { h -> h > 0 }?.toString() ?: ""
            flightM = (it % 60).takeIf { m -> m > 0 }?.toString() ?: ""
        }
        val note = buildList {
            p.airline?.let { add(it) }
            p.legs.getOrNull(0)?.let { add("가는편 $it") }
            p.legs.getOrNull(1)?.let { add("오는편 $it") }
            val priceBit = listOfNotNull(p.seller, p.pricePerPerson?.let { "${it.won()}원/인" }).joinToString(" ")
            if (priceBit.isNotBlank()) add(priceBit)
        }.joinToString(" · ")
        if (note.isNotBlank()) {
            memo = if (memo.isBlank()) "항공: $note" else "$memo\n항공: $note"
        }
        nameError = false
    }

    fun save() {
        if (name.isBlank()) {
            nameError = true
            return
        }
        val trip = Trip(
            id = existing?.id ?: Trip().id,
            name = name.trim(),
            month = month.trim(),
            nights = nights,
            days = days,
            people = people,
            currency = currency,
            fxRate = rate.takeIf { it > 0 } ?: 1.0,
            costs = Category.entries.associateWith { parseAmount(costs[it] ?: "") }.filterValues { it > 0 },
            flightMinutes = parseIntSafe(flightH) * 60 + parseIntSafe(flightM),
            rating = rating,
            memo = memo.trim(),
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        repo.upsert(trip)
        onSaved()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(n.bg)
            .statusBarsPadding()
            .imePadding(),
    ) {
        ScreenHeader(title = if (existing == null) "견적 입력" else "견적 수정", onBack = onBack) {
            NeuIconButton(Icons.Rounded.DocumentScanner, contentDescription = "스크린샷에서 가져오기", onClick = { scanner.launch() }, size = 40.dp, radius = 15.dp, tint = n.accent, iconSize = 19.dp)
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .pressable { save() }
                    .neuRaisedAccent(radius = 15.dp, fill = n.accent, shadow = n.accentShadow, light = n.shadowLight, offset = 5.dp, blur = 14.dp)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("저장", color = n.onAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RiseIn(0) {
                NeuButton(
                    "항공권 검색 스크린샷에서 가져오기",
                    onClick = { scanner.launch() },
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp, radius = 17.dp,
                    icon = Icons.Rounded.DocumentScanner,
                )
            }

            RiseIn(0) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("여행지", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                        if (nameError) {
                            Spacer(Modifier.width(8.dp))
                            Text("이름은 꼭 넣어야 함", color = n.red, fontSize = 11.sp, modifier = Modifier.padding(bottom = 7.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InsetField(
                            value = name,
                            onValueChange = { name = it; nameError = false },
                            modifier = Modifier.weight(1f),
                            placeholder = "예) 다낭",
                            fontSize = 16,
                        )
                        InsetField(
                            value = month,
                            onValueChange = { month = it },
                            modifier = Modifier.width(96.dp),
                            placeholder = "12월",
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            RiseIn(1) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("박", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                        Stepper(
                            value = "${nights}박",
                            onMinus = { if (nights > 0) { nights--; if (days <= nights) days = nights + 1 } },
                            onPlus = { nights++; if (days <= nights) days = nights + 1 },
                            label = "박",
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        SectionLabel("일", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                        Stepper(
                            value = "${days}일",
                            onMinus = { if (days > nights + 1) days-- },
                            onPlus = { days++ },
                            label = "일",
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        SectionLabel("인원", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                        Stepper(
                            value = "${people}인",
                            onMinus = { if (people > 1) people-- },
                            onPlus = { people++ },
                            label = "인원",
                        )
                    }
                }
            }

            RiseIn(2) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        SectionLabel("통화", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                        Spacer(Modifier.weight(1f))
                        if (currency != "KRW") {
                            Text("1 $currency = ", color = n.ink2, fontSize = 11.sp, modifier = Modifier.padding(bottom = 7.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(CURRENCIES) { code ->
                                val on = code == currency
                                Box(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .pressable {
                                            currency = code
                                            fx = if (code == "KRW") "1" else (settings.fxRates[code] ?: 1.0).trimZeros()
                                        }
                                        .then(
                                            if (on) Modifier.neuRaised(radius = 14.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 8.dp)
                                            else Modifier.neuInset(radius = 14.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp)
                                        )
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "$code ${CURRENCY_LABEL[code] ?: ""}",
                                        color = if (on) n.accent else n.ink2,
                                        fontSize = 12.sp,
                                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                                    )
                                }
                            }
                        }
                        InsetField(
                            value = fx,
                            onValueChange = { fx = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            modifier = Modifier.width(118.dp),
                            height = 40.dp,
                            radius = 14.dp,
                            keyboardType = KeyboardType.Decimal,
                            suffix = "원",
                            textAlign = TextAlign.End,
                            fontSize = 14,
                            enabled = currency != "KRW",
                        )
                    }
                }
            }

            RiseIn(3) {
                NeuCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        SectionLabel("항목별 금액")
                        Spacer(Modifier.weight(1f))
                        Text(if (currency == "KRW") "원 단위" else "$currency 단위로 입력", color = n.ink2, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(11.dp))
                    Category.entries.forEachIndexed { i, c ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.width(9.dp).height(26.dp).clip(RoundedCornerShape(4.dp)).background(n.chart[i]))
                            Text(c.label, color = n.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            InsetField(
                                value = costs[c] ?: "",
                                onValueChange = { costs[c] = formatTyped(it) },
                                modifier = Modifier.width(150.dp),
                                height = 40.dp,
                                radius = 13.dp,
                                keyboardType = KeyboardType.Decimal,
                                suffix = if (currency == "KRW") "원" else currency,
                                textAlign = TextAlign.End,
                                fontSize = 14,
                                placeholder = "0",
                            )
                        }
                        if (i < Category.entries.size - 1) Spacer(Modifier.height(9.dp))
                    }
                }
            }

            RiseIn(4) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text("선호도", color = n.ink2, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(10.dp))
                    Stars(rating = rating, size = 22.dp, onChange = { rating = it })
                    Spacer(Modifier.weight(1f))
                    Text("비행", color = n.ink2, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    InsetField(
                        value = flightH,
                        onValueChange = { flightH = it.filter { ch -> ch.isDigit() }.take(2) },
                        modifier = Modifier.width(58.dp),
                        height = 38.dp, radius = 13.dp,
                        keyboardType = KeyboardType.Number,
                        suffix = "h", textAlign = TextAlign.End, fontSize = 13, placeholder = "0",
                    )
                    Spacer(Modifier.width(6.dp))
                    InsetField(
                        value = flightM,
                        onValueChange = { flightM = it.filter { ch -> ch.isDigit() }.take(2) },
                        modifier = Modifier.width(58.dp),
                        height = 38.dp, radius = 13.dp,
                        keyboardType = KeyboardType.Number,
                        suffix = "m", textAlign = TextAlign.End, fontSize = 13, placeholder = "0",
                    )
                }
            }

            RiseIn(5) {
                Column {
                    SectionLabel("메모", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                    InsetField(
                        value = memo,
                        onValueChange = { memo = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "특가 기준, 호텔 등급, 환율 근거 같은 것",
                        singleLine = false,
                        fontSize = 13,
                    )
                }
            }
        }

        RiseIn(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 6.dp)
                    .neuRaisedAccent(radius = 25.dp, fill = n.accent, shadow = n.accentShadow, light = n.shadowLight, offset = 8.dp, blur = 24.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("총액", color = n.onAccentSoft, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(2.dp))
                    MoneyText(totalKrw, size = 29, color = n.onAccent)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("1인당", color = n.onAccentSoft, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${perPerson.won()}원", color = n.onAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        AccentButton(
            text = if (existing == null) "저장하기" else "수정 내용 저장",
            onClick = { save() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 6.dp),
        )
        Spacer(Modifier.navigationBarsPadding())
    }

    if (scanner.busy.value) ScanBusyDialog()
    scanResult?.let { parsed ->
        ScanResultDialog(
            parsed = parsed,
            onApply = {
                applyParsed(parsed)
                scanResult = null
            },
            onDismiss = { scanResult = null },
        )
    }
}
