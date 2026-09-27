package com.shinsak.travle.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.CURRENCY_SYMBOL
import com.shinsak.travle.data.ExpCategory
import com.shinsak.travle.data.Expense
import com.shinsak.travle.data.PayMethod
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.ui.components.AccentButton
import com.shinsak.travle.ui.components.ConfirmDialog
import com.shinsak.travle.ui.components.InsetField
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.NeuIconButton
import com.shinsak.travle.ui.components.NeuToggle
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.components.Segmented
import com.shinsak.travle.ui.formatTyped
import com.shinsak.travle.ui.icon
import com.shinsak.travle.ui.parseAmount
import com.shinsak.travle.ui.theme.Bricolage
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.neuRaised
import com.shinsak.travle.ui.theme.pressable
import com.shinsak.travle.ui.trimZeros
import com.shinsak.travle.ui.won
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseEditScreen(
    repo: TripRepository,
    tripId: String,
    expId: String?,
    initialDay: Int,
    onBack: () -> Unit,
) {
    val n = Neu
    val trips by repo.trips.collectAsStateWithLifecycle()
    val settings by repo.settings.collectAsStateWithLifecycle()
    val trip0 = trips.firstOrNull { it.id == tripId }
    if (trip0 == null) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(n.bg))
        return
    }
    val trip = trip0
    val existing = remember(expId) { trip.expenses.firstOrNull { it.id == expId } }
    val me = trip.companions.first()

    var amount by rememberSaveable { mutableStateOf(existing?.amount?.takeIf { it > 0 }?.let { if (existing.currency == "KRW") formatTyped(it.toLong().toString()) else it.trimZeros() } ?: "") }
    var currency by rememberSaveable { mutableStateOf(existing?.currency ?: trip.currency) }
    var day by rememberSaveable { mutableIntStateOf(existing?.dayIndex ?: initialDay) }
    var method by rememberSaveable { mutableStateOf(existing?.method ?: if (trip.currency == "CNY") PayMethod.ALIPAY else PayMethod.CARD) }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var cat by rememberSaveable { mutableStateOf(existing?.category ?: ExpCategory.FOOD) }
    var paidBy by rememberSaveable { mutableStateOf(existing?.paidBy ?: me) }
    var split by rememberSaveable { mutableStateOf(existing?.splitWith?.isNotEmpty() ?: (trip.people > 1)) }
    var splitWith by remember { mutableStateOf(existing?.splitWith?.ifEmpty { trip.companions } ?: trip.companions) }
    var askDelete by remember { mutableStateOf(false) }

    val rate = if (currency == "KRW") 1.0 else (existing?.takeIf { it.currency == currency }?.fxRate ?: settings.fxRates[currency] ?: 1.0)
    val amt = parseAmount(amount)
    val krw = (amt * rate).roundToLong()
    val share = if (split && splitWith.isNotEmpty()) krw / splitWith.size else krw

    fun save() {
        if (amt <= 0) return
        val e = Expense(
            id = existing?.id ?: Expense().id, dayIndex = day, amount = amt, currency = currency, fxRate = rate,
            title = title.trim(), category = cat, method = method, paidBy = paidBy,
            splitWith = if (split) splitWith else emptyList(),
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        repo.update(tripId) { t -> t.copy(expenses = t.expenses.filter { it.id != e.id } + e) }
        onBack()
    }

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding().imePadding()) {
        ScreenHeader(title = if (existing == null) "비용 추가" else "비용 수정", onBack = onBack)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            RiseIn(0) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .neuInset(radius = 24.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 6.dp, blur = 13.dp)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(trip.currency, "KRW", "USD").distinct()) { code ->
                            val on = code == currency
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .pressable { currency = code; if (code == "KRW") amount = formatTyped(amount) }
                                    .then(if (on) Modifier.neuRaised(radius = 10.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp) else Modifier)
                                    .padding(horizontal = 11.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text("$code ${CURRENCY_SYMBOL[code] ?: ""}", color = if (on) n.accent else n.ink2, fontSize = 11.5.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(CURRENCY_SYMBOL[currency] ?: currency, color = n.hint, fontFamily = Bricolage, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, modifier = Modifier.padding(bottom = 6.dp))
                        BasicTextField(
                            value = amount,
                            onValueChange = { s -> amount = if (currency == "KRW") formatTyped(s) else s.filter { it.isDigit() || it == '.' } },
                            singleLine = true,
                            textStyle = TextStyle(color = n.ink, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, letterSpacing = (-1.6).sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            cursorBrush = SolidColor(n.accent),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (amount.isEmpty()) Text("0", color = n.hint, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp)
                                    inner()
                                }
                            },
                        )
                    }
                    Text(
                        if (currency == "KRW") "원화 입력" else "≈ ${krw.won()}원 · 1 $currency = ${rate.trimZeros()}원 (설정 환율)",
                        color = n.ink2, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            RiseIn(1) {
                Column {
                    SectionLabel("날짜", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                    Segmented(
                        options = listOf("준비") + (1..trip.days).map { "day $it" },
                        selected = day.coerceIn(0, trip.days),
                        onSelect = { day = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            RiseIn(2) {
                Column {
                    SectionLabel("결제", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                    Segmented(options = PayMethod.entries.map { it.label }, selected = PayMethod.entries.indexOf(method), onSelect = { method = PayMethod.entries[it] }, modifier = Modifier.fillMaxWidth())
                }
            }
            RiseIn(3) {
                Column {
                    SectionLabel("항목명", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                    InsetField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = "예) 선양 고궁 입장 ×2", fontSize = 15)
                }
            }
            RiseIn(4) {
                Column {
                    SectionLabel("카테고리", modifier = Modifier.padding(start = 4.dp, bottom = 7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ExpCategory.entries.forEach { c ->
                            val on = c == cat
                            Column(Modifier.weight(1f).pressable { cat = c }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .then(
                                            if (on) Modifier.neuRaised(radius = 15.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 10.dp)
                                            else Modifier.neuInset(radius = 15.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 9.dp)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(c.icon(), contentDescription = c.label, tint = if (on) n.accent else n.ink2, modifier = Modifier.size(18.dp)) }
                                Spacer(Modifier.height(5.dp))
                                Text(c.label, color = if (on) n.accent else n.ink2, fontSize = 10.5.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                }
            }
            if (trip.people > 1) {
                RiseIn(5) {
                    NeuCard(modifier = Modifier.fillMaxWidth(), radius = 20.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("1/N 정산")
                            Spacer(Modifier.weight(1f))
                            Text(if (split) "켬" else "끔", color = n.ink2, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                            NeuToggle(checked = split, onChange = { split = it }, desc = "1/N 정산")
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("낸 사람", color = n.ink2, fontSize = 10.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    trip.companions.forEach { name -> PersonChip(name, name == paidBy) { paidBy = name } }
                                }
                            }
                            if (split) {
                                Column(Modifier.weight(1f)) {
                                    Text("나눌 사람", color = n.ink2, fontSize = 10.5.sp, modifier = Modifier.padding(bottom = 6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        trip.companions.forEach { name ->
                                            PersonChip(name, name in splitWith) {
                                                splitWith = if (name in splitWith) splitWith - name else splitWith + name
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (split && krw > 0 && splitWith.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            val others = splitWith.filter { it != paidBy }
                            Text(
                                if (others.isEmpty()) "→ ${paidBy} 혼자 부담" else "→ ${others.joinToString(", ")}가 ${paidBy}한테 각 ${share.won()}원",
                                color = n.amber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        Row(Modifier.padding(horizontal = 22.dp).padding(bottom = 12.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentButton(text = "완료", onClick = { save() }, modifier = Modifier.weight(1f), height = 54.dp, enabled = amt > 0)
            if (existing != null) NeuIconButton(Icons.Rounded.Delete, contentDescription = "삭제", onClick = { askDelete = true }, size = 54.dp, radius = 19.dp, tint = n.red)
        }
    }

    if (askDelete && existing != null) {
        ConfirmDialog(
            title = "이 지출을 지울까요?", message = "\"${existing.title.ifBlank { existing.category.label }}\" ${existing.krw.won()}원", confirmText = "삭제", destructive = true,
            onConfirm = { askDelete = false; repo.update(tripId) { t -> t.copy(expenses = t.expenses.filter { it.id != existing.id }) }; onBack() },
            onDismiss = { askDelete = false },
        )
    }
}

@Composable
private fun PersonChip(name: String, on: Boolean, onClick: () -> Unit) {
    val n = Neu
    Box(
        modifier = Modifier
            .height(32.dp)
            .pressable(onClick = onClick)
            .then(
                if (on) Modifier.neuRaised(radius = 11.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 8.dp)
                else Modifier.neuInset(radius = 11.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp)
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(name, color = if (on) n.accent else n.ink2, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium) }
}
