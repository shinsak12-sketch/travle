package com.shinsak.travle.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.CURRENCIES
import com.shinsak.travle.data.CURRENCY_LABEL
import com.shinsak.travle.data.Category
import com.shinsak.travle.data.FlightLeg
import com.shinsak.travle.data.ParsedFlight
import com.shinsak.travle.data.Trip
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.ui.ScanBusyDialog
import com.shinsak.travle.ui.ScanResultDialog
import com.shinsak.travle.ui.components.AccentButton
import com.shinsak.travle.ui.components.InsetField
import com.shinsak.travle.ui.components.MoneyText
import com.shinsak.travle.ui.components.NeuButton
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.NeuIconButton
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.components.Stars
import com.shinsak.travle.ui.components.Stepper
import com.shinsak.travle.ui.formatTyped
import com.shinsak.travle.ui.parseAmount
import com.shinsak.travle.ui.rememberScreenshotScanner
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.neuRaised
import com.shinsak.travle.ui.theme.pressable
import com.shinsak.travle.ui.trimZeros
import com.shinsak.travle.ui.won
import kotlin.math.roundToLong

/** "1:25" / "1h 25m" / "85" → 분 */
private fun parseDuration(s: String): Int {
    val t = s.trim()
    if (t.isEmpty()) return 0
    Regex("^(\\d{1,2})\\s*[:h시간]\\s*(\\d{0,2})").find(t)?.let { m ->
        return m.groupValues[1].toInt() * 60 + (m.groupValues[2].toIntOrNull() ?: 0)
    }
    return t.filter { it.isDigit() }.toIntOrNull() ?: 0
}

/** 85 → "1:25" */
private fun durationText(min: Int): String = if (min <= 0) "" else "%d:%02d".format(min / 60, min % 60)

/** 시간 입력 정리: 숫자와 : + 만 남김 */
private fun cleanTime(s: String): String = s.filter { it.isDigit() || it == ':' || it == '+' }.take(8)

@Composable
fun EditScreen(
    repo: TripRepository,
    tripId: String?,
    autoScan: Boolean = false,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val n = Neu
    val ctx = LocalContext.current
    val settings by repo.settings.collectAsStateWithLifecycle()
    val existing = remember(tripId) { repo.trip(tripId) }

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var month by rememberSaveable { mutableStateOf(existing?.month ?: "") }
    var nights by rememberSaveable { mutableIntStateOf(existing?.nights ?: 3) }
    var days by rememberSaveable { mutableIntStateOf(existing?.days ?: 4) }
    var people by rememberSaveable { mutableIntStateOf(existing?.people ?: 2) }
    var currency by rememberSaveable { mutableStateOf(existing?.currency ?: "KRW") }
    var fx by rememberSaveable { mutableStateOf(existing?.fxRate?.trimZeros() ?: "1") }
    var airline by rememberSaveable { mutableStateOf(existing?.airline ?: "") }
    var outDep by rememberSaveable { mutableStateOf(existing?.outbound?.dep ?: "") }
    var outArr by rememberSaveable { mutableStateOf(existing?.outbound?.arr ?: "") }
    var outDur by rememberSaveable { mutableStateOf(durationText(existing?.outbound?.minutes ?: existing?.flightMinutes ?: 0)) }
    var inDep by rememberSaveable { mutableStateOf(existing?.inbound?.dep ?: "") }
    var inArr by rememberSaveable { mutableStateOf(existing?.inbound?.arr ?: "") }
    var inDur by rememberSaveable { mutableStateOf(durationText(existing?.inbound?.minutes ?: 0)) }
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
        p.airline?.let { airline = it }
        p.legs.getOrNull(0)?.let { l ->
            outDep = l.dep
            outArr = l.arr
            outDur = durationText(l.minutes)
        }
        p.legs.getOrNull(1)?.let { l ->
            inDep = l.dep
            inArr = l.arr
            inDur = durationText(l.minutes)
        }
        p.totalPrice?.let { total ->
            val v = if (currency == "KRW") total.toDouble() else total / (rate.takeIf { it > 0 } ?: 1.0)
            costs[Category.FLIGHT] = formatTyped(v.trimZeros())
        }
        val priceBit = listOfNotNull(p.seller, p.pricePerPerson?.let { "${it.won()}원/인" }).joinToString(" ")
        if (priceBit.isNotBlank()) {
            val line = "항공권 $priceBit"
            if (!memo.contains(line)) memo = if (memo.isBlank()) line else "$memo\n$line"
        }
        nameError = false
    }

    fun save() {
        if (name.isBlank()) {
            nameError = true
            return
        }
        val out = FlightLeg(outDep.trim(), outArr.trim(), parseDuration(outDur)).takeIf { !it.isEmpty }
        val inn = FlightLeg(inDep.trim(), inArr.trim(), parseDuration(inDur)).takeIf { !it.isEmpty }
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
            flightMinutes = out?.minutes ?: 0,
            airline = airline.trim(),
            outbound = out,
            inbound = inn,
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
            NeuIconButton(
                Icons.Rounded.DocumentScanner,
                contentDescription = "스크린샷에서 가져오기",
                onClick = { scanner.launch() },
                size = 44.dp, radius = 16.dp, tint = n.accent, iconSize = 20.dp,
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 2.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── 스크린샷 가져오기
            RiseIn(0) {
                NeuButton(
                    "항공권 검색 스크린샷에서 가져오기",
                    onClick = { scanner.launch() },
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp, radius = 17.dp,
                    icon = Icons.Rounded.DocumentScanner,
                )
            }

            // ── 여행지
            RiseIn(1) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FieldLabel("여행지")
                        if (nameError) {
                            Spacer(Modifier.width(8.dp))
                            Text("이름은 꼭 넣어야 함", color = n.red, fontSize = 11.sp, modifier = Modifier.padding(bottom = 7.dp))
                        }
                    }
                    InsetField(
                        value = name,
                        onValueChange = { name = it; nameError = false },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "예) 다낭",
                        fontSize = 16,
                    )
                }
            }

            // ── 날짜 + 인원
            RiseIn(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("날짜")
                        InsetField(
                            value = month,
                            onValueChange = { month = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "10월 9–11일",
                        )
                    }
                    Column(Modifier.width(150.dp)) {
                        FieldLabel("인원")
                        Stepper(
                            value = "${people}인",
                            onMinus = { if (people > 1) people-- },
                            onPlus = { people++ },
                            label = "인원",
                        )
                    }
                }
            }

            // ── 박 / 일
            RiseIn(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("박")
                        Stepper(
                            value = "${nights}박",
                            onMinus = { if (nights > 0) { nights--; if (days <= nights) days = nights + 1 } },
                            onPlus = { nights++; if (days <= nights) days = nights + 1 },
                            label = "박",
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel("일")
                        Stepper(
                            value = "${days}일",
                            onMinus = { if (days > nights + 1) days-- },
                            onPlus = { days++ },
                            label = "일",
                        )
                    }
                }
            }

            // ── 통화 + 환율
            RiseIn(4) {
                Column {
                    FieldLabel("통화")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    .padding(horizontal = 13.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$code ${CURRENCY_LABEL[code] ?: ""}",
                                    color = if (on) n.accent else n.ink2,
                                    fontSize = 12.sp,
                                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    if (currency != "KRW") {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("환율  1 $currency =", color = n.ink2, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            InsetField(
                                value = fx,
                                onValueChange = { fx = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                modifier = Modifier.width(160.dp),
                                height = 42.dp, radius = 14.dp,
                                keyboardType = KeyboardType.Decimal,
                                suffix = "원", textAlign = TextAlign.End, fontSize = 14,
                            )
                        }
                    }
                }
            }

            // ── 항목별 금액
            RiseIn(5) {
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
                            Text(c.label, color = n.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
                            InsetField(
                                value = costs[c] ?: "",
                                onValueChange = { costs[c] = formatTyped(it) },
                                modifier = Modifier.width(150.dp),
                                height = 40.dp, radius = 13.dp,
                                keyboardType = KeyboardType.Decimal,
                                suffix = if (currency == "KRW") "원" else currency,
                                textAlign = TextAlign.End, fontSize = 14, placeholder = "0",
                            )
                        }
                        if (i < Category.entries.size - 1) Spacer(Modifier.height(9.dp))
                    }
                }
            }

            // ── 항공편
            RiseIn(6) {
                NeuCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        SectionLabel("항공편")
                        Spacer(Modifier.weight(1f))
                        Text("24시간제 · 소요 h:mm", color = n.ink2, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(11.dp))
                    InsetField(
                        value = airline,
                        onValueChange = { airline = it },
                        modifier = Modifier.fillMaxWidth(),
                        height = 42.dp, radius = 14.dp,
                        placeholder = "항공사 (예: 대한항공)",
                        fontSize = 14,
                    )
                    Spacer(Modifier.height(12.dp))
                    LegRow(
                        title = "가는편",
                        dep = outDep, onDep = { outDep = cleanTime(it) },
                        arr = outArr, onArr = { outArr = cleanTime(it) },
                        dur = outDur, onDur = { outDur = cleanTime(it) },
                    )
                    Spacer(Modifier.height(10.dp))
                    LegRow(
                        title = "오는편",
                        dep = inDep, onDep = { inDep = cleanTime(it) },
                        arr = inArr, onArr = { inArr = cleanTime(it) },
                        dur = inDur, onDur = { inDur = cleanTime(it) },
                    )
                }
            }

            // ── 선호도
            RiseIn(7) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text("선호도", color = n.ink2, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(12.dp))
                    Stars(rating = rating, size = 22.dp, onChange = { rating = it })
                    Spacer(Modifier.weight(1f))
                    Text("${rating}점", color = n.ink2, fontSize = 12.sp)
                }
            }

            // ── 메모
            RiseIn(8) {
                Column {
                    FieldLabel("메모")
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

        // ── 하단: 합계 + 저장
        Column(Modifier.padding(horizontal = 18.dp).navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .neuInset(radius = 22.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("총액", color = n.ink2, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
                    MoneyText(totalKrw, size = 26, color = n.ink)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("1인당", color = n.ink2, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("${perPerson.won()}원", color = n.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            AccentButton(
                text = if (existing == null) "저장하기" else "수정 내용 저장",
                onClick = { save() },
                modifier = Modifier.fillMaxWidth(),
                height = 54.dp,
            )
            Spacer(Modifier.height(12.dp))
        }
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

@Composable
private fun FieldLabel(text: String) {
    SectionLabel(text, modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
}

/** 가는편/오는편 한 줄: 제목 · 출발 → 도착 · 소요 */
@Composable
private fun LegRow(
    title: String,
    dep: String, onDep: (String) -> Unit,
    arr: String, onArr: (String) -> Unit,
    dur: String, onDur: (String) -> Unit,
) {
    val n = Neu
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = n.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(44.dp), maxLines = 1)
        InsetField(
            value = dep, onValueChange = onDep,
            modifier = Modifier.weight(1f),
            height = 40.dp, radius = 13.dp,
            keyboardType = KeyboardType.Number,
            placeholder = "18:35", textAlign = TextAlign.Center, fontSize = 14,
        )
        Text("→", color = n.ink2, fontSize = 13.sp)
        InsetField(
            value = arr, onValueChange = onArr,
            modifier = Modifier.weight(1f),
            height = 40.dp, radius = 13.dp,
            keyboardType = KeyboardType.Number,
            placeholder = "19:00", textAlign = TextAlign.Center, fontSize = 14,
        )
        InsetField(
            value = dur, onValueChange = onDur,
            modifier = Modifier.width(72.dp),
            height = 40.dp, radius = 13.dp,
            keyboardType = KeyboardType.Number,
            placeholder = "1:25", textAlign = TextAlign.Center, fontSize = 13,
        )
    }
}
