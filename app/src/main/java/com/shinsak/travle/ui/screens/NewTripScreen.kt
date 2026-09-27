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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinsak.travle.data.CITY_PRESETS
import com.shinsak.travle.data.CITY_REGIONS
import com.shinsak.travle.data.CityPreset
import com.shinsak.travle.data.Trip
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.data.defaultChecks
import com.shinsak.travle.ui.components.AccentButton
import com.shinsak.travle.ui.components.InsetField
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.NeuIconButton
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.theme.Bricolage
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.neuRaised
import com.shinsak.travle.ui.theme.neuRaisedAccent
import com.shinsak.travle.ui.theme.pressable
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// ──────────────────────────────────────────────────────────── 1/2 도시

@Composable
fun CityScreen(onBack: () -> Unit, onNext: (city: String, currency: String) -> Unit) {
    val n = Neu
    var query by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf("") }

    val list = CITY_PRESETS.filter { c ->
        (region == 0 || c.region == CITY_REGIONS[region]) &&
            (query.isBlank() || c.name.contains(query, ignoreCase = true) || c.code.contains(query, ignoreCase = true))
    }
    val customName = query.trim().takeIf { it.isNotBlank() && CITY_PRESETS.none { c -> c.name.equals(it, true) } }
    val chosenName = selected.ifBlank { customName ?: "" }
    val chosenCurrency = CITY_PRESETS.firstOrNull { it.name == chosenName }?.currency ?: "KRW"

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding().imePadding()) {
        ScreenHeader(title = "어디로 떠나시나요?", subtitle = "1 / 2", onBack = onBack)

        Row(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .fillMaxWidth()
                .height(50.dp)
                .neuInset(radius = 17.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = n.hint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            InsetFieldBare(value = query, onValueChange = { query = it; selected = "" }, placeholder = "도시 이름 · 없으면 그 이름으로 만들어짐")
        }

        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            contentPadding = PaddingValues(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(CITY_REGIONS.indices.toList()) { i ->
                val on = i == region
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .pressable { region = i }
                        .then(
                            if (on) Modifier.neuRaised(radius = 13.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 8.dp)
                            else Modifier.neuInset(radius = 13.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp)
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(CITY_REGIONS[i], color = if (on) n.accent else n.ink2, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium) }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (customName != null) {
                item {
                    CityRow(
                        preset = CityPreset(customName, customName.take(3).uppercase(), "KRW", "직접 입력", "목록에 없는 도시 · 통화는 다음 화면에서"),
                        selected = selected == customName,
                        onSelect = { selected = customName },
                    )
                }
            }
            items(list, key = { it.name }) { c ->
                CityRow(preset = c, selected = selected == c.name, onSelect = { selected = c.name })
            }
        }

        Box(Modifier.padding(horizontal = 22.dp).padding(bottom = 12.dp).navigationBarsPadding()) {
            AccentButton(
                text = if (chosenName.isBlank()) "도시를 골라주세요" else "$chosenName · 날짜 정하기",
                onClick = { if (chosenName.isNotBlank()) onNext(chosenName, chosenCurrency) },
                modifier = Modifier.fillMaxWidth(),
                height = 54.dp,
                enabled = chosenName.isNotBlank(),
            )
        }
    }
}

@Composable
private fun CityRow(preset: CityPreset, selected: Boolean, onSelect: () -> Unit) {
    val n = Neu
    val color = n.chart[(preset.name.hashCode().let { if (it < 0) -it else it }) % n.chart.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onSelect)
            .neuRaised(radius = 20.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 5.dp, blur = 13.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(if (selected) n.accent else color), contentAlignment = Alignment.Center) {
            Text(preset.code, color = Color.White, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(preset.name, color = n.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(preset.hint, color = n.ink2, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
        }
        if (selected) {
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .neuRaisedAccent(radius = 13.dp, fill = n.accent, shadow = n.accentShadow, light = n.shadowLight, offset = 4.dp, blur = 10.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = n.onAccent, modifier = Modifier.size(14.dp))
                Text("선택됨", color = n.onAccent, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .neuRaised(radius = 13.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 10.dp)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("선택", color = n.accent, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/** 배경 없는 텍스트 입력 (검색창처럼 부모가 판을 그릴 때) */
@Composable
private fun InsetFieldBare(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val n = Neu
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = n.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(n.accent),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = n.hint, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                inner()
            }
        },
    )
}

// ──────────────────────────────────────────────────────────── 2/2 날짜 · 인원

@Composable
fun DatesScreen(
    repo: TripRepository,
    city: String,
    currency: String,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
) {
    val n = Neu
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var start by remember { mutableStateOf<String?>(null) }
    var end by remember { mutableStateOf<String?>(null) }
    var companions by remember { mutableStateOf(listOf("나")) }
    var newName by rememberSaveable { mutableStateOf("") }
    var cur by rememberSaveable { mutableStateOf(currency) }

    val s = start?.let { LocalDate.parse(it) }
    val e = end?.let { LocalDate.parse(it) }
    val nights = if (s != null && e != null) ChronoUnit.DAYS.between(s, e).toInt() else 0

    fun pick(d: LocalDate) {
        val ss = s
        if (ss == null || e != null) {
            start = d.toString(); end = null
        } else if (d.isBefore(ss)) {
            start = d.toString(); end = null
        } else {
            end = d.toString()
        }
    }

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding().imePadding()) {
        ScreenHeader(title = "언제 다녀오시나요?", subtitle = "2 / 2 · $city", onBack = onBack)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RiseIn(0) {
                NeuCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)) {
                    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        NeuIconButton(Icons.Rounded.ArrowBackIosNew, contentDescription = "이전 달", onClick = { month = month.minusMonths(1) }, size = 36.dp, radius = 12.dp, iconSize = 14.dp)
                        Spacer(Modifier.weight(1f))
                        Text("${month.year}년 ${month.monthValue}월", color = n.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        NeuIconButton(Icons.Rounded.ArrowForwardIos, contentDescription = "다음 달", onClick = { month = month.plusMonths(1) }, size = 36.dp, radius = 12.dp, iconSize = 14.dp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Calendar(month = month, start = s, end = e, today = today, onPick = { pick(it) })
                }
            }

            RiseIn(1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neuInset(radius = 22.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        SectionLabel("기간")
                        Spacer(Modifier.height(4.dp))
                        if (s == null) {
                            Text("출발일을 눌러주세요", color = n.hint, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            val endTxt = e?.let { "${it.monthValue}.${it.dayOfMonth} ${Trip.dow(it)}" } ?: "도착일?"
                            Text("${s.monthValue}.${s.dayOfMonth} ${Trip.dow(s)} – $endTxt", color = n.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = (-0.5).sp)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        SectionLabel("일수")
                        Spacer(Modifier.height(4.dp))
                        Text(if (e != null) "${nights}박 ${nights + 1}일" else "—", color = n.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            RiseIn(2) {
                Column {
                    SectionLabel("동행 · 1/N 정산용", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neuInset(radius = 17.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            companions.forEachIndexed { i, name ->
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(n.accentTint).padding(start = 10.dp, end = if (i == 0) 10.dp else 6.dp, top = 5.dp, bottom = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(name, color = n.accentDeep, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    if (i > 0) {
                                        Spacer(Modifier.width(4.dp))
                                        Box(Modifier.size(18.dp).pressable { companions = companions - name }, contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Close, contentDescription = "$name 빼기", tint = n.accentDeep, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        InsetField(
                            value = newName, onValueChange = { newName = it }, modifier = Modifier.weight(1f),
                            placeholder = "동행 이름 추가 (예: 민수)", height = 44.dp, radius = 15.dp, fontSize = 13,
                        )
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .pressable {
                                    val nm = newName.trim()
                                    if (nm.isNotEmpty() && nm !in companions) companions = companions + nm
                                    newName = ""
                                }
                                .neuRaised(radius = 15.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 10.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("추가", color = n.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            RiseIn(3) {
                Column {
                    SectionLabel("현지 통화", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(com.shinsak.travle.data.CURRENCIES) { code ->
                            val on = code == cur
                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .pressable { cur = code }
                                    .then(
                                        if (on) Modifier.neuRaised(radius = 13.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 8.dp)
                                        else Modifier.neuInset(radius = 13.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp)
                                    )
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text("$code ${com.shinsak.travle.data.CURRENCY_LABEL[code] ?: ""}", color = if (on) n.accent else n.ink2, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1) }
                        }
                    }
                }
            }
        }

        Box(Modifier.padding(horizontal = 22.dp).padding(bottom = 12.dp).navigationBarsPadding()) {
            AccentButton(
                text = if (e == null) "날짜를 골라주세요" else "$city 여행 만들기",
                onClick = {
                    if (s != null && e != null) {
                        val trip = Trip(
                            title = "$city 여행", city = city, startDate = s.toString(), endDate = e.toString(),
                            companions = companions, currency = cur, checks = defaultChecks(city),
                        )
                        repo.upsert(trip)
                        onCreated(trip.id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                height = 54.dp,
                enabled = e != null,
            )
        }
    }
}

/** 한 달 달력. 범위 선택. */
@Composable
fun Calendar(month: YearMonth, start: LocalDate?, end: LocalDate?, today: LocalDate, onPick: (LocalDate) -> Unit) {
    val n = Neu
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value % 7 // 일요일 시작
    val total = month.lengthOfMonth()
    val cells = (0 until lead).map { null } + (1..total).map { month.atDay(it) }
    val rows = cells.chunked(7)
    Column {
        Row {
            listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { i, d ->
                Text(d, color = if (i == 0 || i == 6) n.red else n.hint, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(vertical = 4.dp))
            }
        }
        rows.forEach { week ->
            Row {
                week.forEach { d ->
                    if (d == null) {
                        Spacer(Modifier.weight(1f).height(40.dp))
                    } else {
                        val isStart = d == start
                        val isEnd = d == end
                        val inRange = start != null && end != null && d.isAfter(start) && d.isBefore(end)
                        val weekend = d.dayOfWeek.value >= 6
                        val shape = when {
                            isStart && end != null && end != start -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                            isEnd && start != null && end != start -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                            else -> RoundedCornerShape(12.dp)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .then(if (inRange) Modifier.background(n.accentTint) else Modifier)
                                .then(if (isStart || isEnd) Modifier.clip(shape).background(n.accent) else Modifier)
                                .pressable { onPick(d) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                d.dayOfMonth.toString(),
                                color = when {
                                    isStart || isEnd -> n.onAccent
                                    d.isBefore(today) -> n.hint
                                    weekend -> n.red
                                    else -> n.ink
                                },
                                fontFamily = Bricolage, fontWeight = if (isStart || isEnd) FontWeight.ExtraBold else FontWeight.SemiBold, fontSize = 14.sp,
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f).height(40.dp)) }
            }
        }
    }
}
