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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.shinsak.travle.data.Flight
import com.shinsak.travle.data.FlightLeg
import com.shinsak.travle.data.ParsedFlight
import com.shinsak.travle.data.Stay
import com.shinsak.travle.data.Trip
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.data.withAutoFlightItems
import com.shinsak.travle.data.withFlightExpense
import com.shinsak.travle.data.withStayExpense
import com.shinsak.travle.ui.ScanBusyDialog
import com.shinsak.travle.ui.ScanResultDialog
import com.shinsak.travle.ui.components.AccentButton
import com.shinsak.travle.ui.components.BottomTabBar
import com.shinsak.travle.ui.components.InsetField
import com.shinsak.travle.ui.components.NeuButton
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.components.Tab
import com.shinsak.travle.ui.formatTyped
import com.shinsak.travle.ui.openMap
import com.shinsak.travle.ui.parseAmount
import com.shinsak.travle.ui.rememberScreenshotScanner
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.won
import kotlin.math.roundToLong

private fun parseDuration(s: String): Int {
    val t = s.trim()
    if (t.isEmpty()) return 0
    Regex("^(\\d{1,2})\\s*[:h시간]\\s*(\\d{0,2})").find(t)?.let { m -> return m.groupValues[1].toInt() * 60 + (m.groupValues[2].toIntOrNull() ?: 0) }
    return t.filter { it.isDigit() }.toIntOrNull() ?: 0
}

private fun durationText(min: Int): String = if (min <= 0) "" else "%d:%02d".format(min / 60, min % 60)
private fun cleanTime(s: String): String = s.filter { it.isDigit() || it == ':' || it == '+' }.take(8)

@Composable
fun BookingScreen(
    repo: TripRepository,
    tripId: String,
    onBack: () -> Unit,
    onTab: (Tab) -> Unit,
) {
    val n = Neu
    val ctx = LocalContext.current
    val trips by repo.trips.collectAsStateWithLifecycle()
    val trip = trips.firstOrNull { it.id == tripId }

    if (trip == null) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(n.bg))
        return
    }

    // 항공
    var airline by remember(trip.id) { mutableStateOf(trip.flight.airline) }
    var outDep by remember(trip.id) { mutableStateOf(trip.flight.outbound?.dep ?: "") }
    var outArr by remember(trip.id) { mutableStateOf(trip.flight.outbound?.arr ?: "") }
    var outDur by remember(trip.id) { mutableStateOf(durationText(trip.flight.outbound?.minutes ?: 0)) }
    var inDep by remember(trip.id) { mutableStateOf(trip.flight.inbound?.dep ?: "") }
    var inArr by remember(trip.id) { mutableStateOf(trip.flight.inbound?.arr ?: "") }
    var inDur by remember(trip.id) { mutableStateOf(durationText(trip.flight.inbound?.minutes ?: 0)) }
    var seller by remember(trip.id) { mutableStateOf(trip.flight.seller) }
    var fPrice by remember(trip.id) { mutableStateOf(trip.flight.totalPrice.takeIf { it > 0 }?.let { formatTyped(it.toString()) } ?: "") }
    var fNote by remember(trip.id) { mutableStateOf(trip.flight.note) }
    // 숙소
    var sName by remember(trip.id) { mutableStateOf(trip.stay.name) }
    var sIn by remember(trip.id) { mutableStateOf(trip.stay.checkIn) }
    var sOut by remember(trip.id) { mutableStateOf(trip.stay.checkOut) }
    var sAddr by remember(trip.id) { mutableStateOf(trip.stay.address) }
    var sLink by remember(trip.id) { mutableStateOf(trip.stay.mapLink) }
    var sNo by remember(trip.id) { mutableStateOf(trip.stay.bookingNo) }
    var sPrice by remember(trip.id) { mutableStateOf(trip.stay.price.takeIf { it > 0 }?.let { formatTyped(it.toString()) } ?: "") }
    var sNote by remember(trip.id) { mutableStateOf(trip.stay.note) }

    var scanResult by remember { mutableStateOf<ParsedFlight?>(null) }
    val scanner = rememberScreenshotScanner { parsed ->
        if (parsed == null || parsed.isEmpty) Toast.makeText(ctx, "스크린샷에서 항공권 정보를 못 찾았음", Toast.LENGTH_SHORT).show()
        else scanResult = parsed
    }

    fun saveFlight() {
        val f = Flight(
            airline = airline.trim(),
            outbound = FlightLeg(outDep.trim(), outArr.trim(), parseDuration(outDur)).takeIf { !it.isEmpty },
            inbound = FlightLeg(inDep.trim(), inArr.trim(), parseDuration(inDur)).takeIf { !it.isEmpty },
            seller = seller.trim(),
            totalPrice = parseAmount(fPrice).roundToLong(),
            pricePerPerson = (parseAmount(fPrice) / trip.people).roundToLong(),
            note = fNote.trim(),
        )
        repo.update(trip.id) { it.copy(flight = f).withAutoFlightItems().withFlightExpense() }
        Toast.makeText(ctx, "항공편 저장 · 일정과 가계부에 반영됨", Toast.LENGTH_SHORT).show()
    }

    fun saveStay() {
        val s = Stay(sName.trim(), sIn.trim(), sOut.trim(), sAddr.trim(), sLink.trim(), sNo.trim(), parseAmount(sPrice).roundToLong(), sNote.trim())
        repo.update(trip.id) { it.copy(stay = s).withStayExpense() }
        Toast.makeText(ctx, "숙소 저장 · 가계부에 반영됨", Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding().imePadding()) {
        ScreenHeader(title = "예약", subtitle = "${trip.title} · 저장하면 일정·가계부에 자동 반영")

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RiseIn(0) {
                NeuCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("항공편")
                        Spacer(Modifier.weight(1f))
                        NeuButton("스크린샷에서 가져오기", onClick = { scanner.launch() }, height = 38.dp, radius = 13.dp, icon = Icons.Rounded.DocumentScanner)
                    }
                    Spacer(Modifier.height(12.dp))
                    InsetField(value = airline, onValueChange = { airline = it }, modifier = Modifier.fillMaxWidth(), height = 44.dp, radius = 14.dp, placeholder = "항공사 (예: 중국남방항공)", fontSize = 14)
                    Spacer(Modifier.height(10.dp))
                    LegRow("가는편", outDep, { outDep = cleanTime(it) }, outArr, { outArr = cleanTime(it) }, outDur, { outDur = cleanTime(it) })
                    Spacer(Modifier.height(8.dp))
                    LegRow("오는편", inDep, { inDep = cleanTime(it) }, inArr, { inArr = cleanTime(it) }, inDur, { inDur = cleanTime(it) })
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InsetField(value = seller, onValueChange = { seller = it }, modifier = Modifier.weight(1f), height = 44.dp, radius = 14.dp, placeholder = "판매처", fontSize = 13)
                        InsetField(value = fPrice, onValueChange = { fPrice = formatTyped(it) }, modifier = Modifier.width(150.dp), height = 44.dp, radius = 14.dp, keyboardType = KeyboardType.Number, suffix = "원 총액", textAlign = TextAlign.End, fontSize = 14, placeholder = "0")
                    }
                    Spacer(Modifier.height(8.dp))
                    InsetField(value = fNote, onValueChange = { fNote = it }, modifier = Modifier.fillMaxWidth(), height = 44.dp, radius = 14.dp, placeholder = "예약번호 · 수하물 · 메모", fontSize = 13)
                    Spacer(Modifier.height(12.dp))
                    AccentButton("항공편 저장", onClick = { saveFlight() }, modifier = Modifier.fillMaxWidth(), height = 48.dp, radius = 17.dp)
                    Text("24시간제 · 소요는 h:mm · 저장하면 day 1과 마지막 날 일정에 자동으로 들어감", color = n.ink2, fontSize = 10.5.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                }
            }

            RiseIn(1) {
                NeuCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
                    SectionLabel("숙소")
                    Spacer(Modifier.height(12.dp))
                    InsetField(value = sName, onValueChange = { sName = it }, modifier = Modifier.fillMaxWidth(), height = 44.dp, radius = 14.dp, placeholder = "숙소 이름", fontSize = 14)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InsetField(value = sIn, onValueChange = { sIn = it }, modifier = Modifier.weight(1f), height = 44.dp, radius = 14.dp, placeholder = "체크인 (10.9 15:00)", fontSize = 13)
                        InsetField(value = sOut, onValueChange = { sOut = it }, modifier = Modifier.weight(1f), height = 44.dp, radius = 14.dp, placeholder = "체크아웃", fontSize = 13)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        InsetField(value = sLink, onValueChange = { sLink = it }, modifier = Modifier.weight(1f), height = 44.dp, radius = 14.dp, placeholder = "지도 링크", fontSize = 13)
                        NeuButton("열기", onClick = { openMap(ctx, sLink.ifBlank { sAddr }) }, height = 44.dp, radius = 14.dp, enabled = sLink.isNotBlank() || sAddr.isNotBlank())
                    }
                    Spacer(Modifier.height(8.dp))
                    InsetField(value = sAddr, onValueChange = { sAddr = it }, modifier = Modifier.fillMaxWidth(), height = 44.dp, radius = 14.dp, placeholder = "주소", fontSize = 13)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InsetField(value = sNo, onValueChange = { sNo = it }, modifier = Modifier.weight(1f), height = 44.dp, radius = 14.dp, placeholder = "예약번호", fontSize = 13)
                        InsetField(value = sPrice, onValueChange = { sPrice = formatTyped(it) }, modifier = Modifier.width(150.dp), height = 44.dp, radius = 14.dp, keyboardType = KeyboardType.Number, suffix = "원 총액", textAlign = TextAlign.End, fontSize = 14, placeholder = "0")
                    }
                    Spacer(Modifier.height(8.dp))
                    InsetField(value = sNote, onValueChange = { sNote = it }, modifier = Modifier.fillMaxWidth(), height = 44.dp, radius = 14.dp, placeholder = "조식 포함 · 와이파이 · 메모", fontSize = 13)
                    Spacer(Modifier.height(12.dp))
                    AccentButton("숙소 저장", onClick = { saveStay() }, modifier = Modifier.fillMaxWidth(), height = 48.dp, radius = 17.dp)
                }
            }
        }

        BottomTabBar(current = Tab.BOOKING, onSelect = onTab, modifier = Modifier.navigationBarsPadding())
    }

    if (scanner.busy.value) ScanBusyDialog()
    scanResult?.let { p ->
        ScanResultDialog(
            parsed = p,
            onApply = {
                p.airline?.let { airline = it }
                p.legs.getOrNull(0)?.let { outDep = it.dep; outArr = it.arr; outDur = durationText(it.minutes) }
                p.legs.getOrNull(1)?.let { inDep = it.dep; inArr = it.arr; inDur = durationText(it.minutes) }
                p.seller?.let { seller = it }
                p.totalPrice?.let { fPrice = formatTyped(it.toString()) }
                scanResult = null
            },
            onDismiss = { scanResult = null },
        )
    }
}

@Composable
private fun LegRow(title: String, dep: String, onDep: (String) -> Unit, arr: String, onArr: (String) -> Unit, dur: String, onDur: (String) -> Unit) {
    val n = Neu
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = n.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(44.dp), maxLines = 1)
        InsetField(value = dep, onValueChange = onDep, modifier = Modifier.weight(1f), height = 40.dp, radius = 13.dp, keyboardType = KeyboardType.Number, placeholder = "21:50", textAlign = TextAlign.Center, fontSize = 14)
        Text("→", color = n.ink2, fontSize = 13.sp)
        InsetField(value = arr, onValueChange = onArr, modifier = Modifier.weight(1f), height = 40.dp, radius = 13.dp, keyboardType = KeyboardType.Number, placeholder = "22:50", textAlign = TextAlign.Center, fontSize = 14)
        InsetField(value = dur, onValueChange = onDur, modifier = Modifier.width(72.dp), height = 40.dp, radius = 13.dp, keyboardType = KeyboardType.Number, placeholder = "2:00", textAlign = TextAlign.Center, fontSize = 13)
    }
}
