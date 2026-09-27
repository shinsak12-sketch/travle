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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.CheckItem
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.ui.askClaude
import com.shinsak.travle.ui.components.BottomTabBar
import com.shinsak.travle.ui.components.Gauge
import com.shinsak.travle.ui.components.InsetField
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.NeuIconButton
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.components.Segmented
import com.shinsak.travle.ui.components.Tab
import com.shinsak.travle.ui.theme.Bricolage
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.neuRaisedAccent
import com.shinsak.travle.ui.theme.pressable

private val GROUPS = listOf("출발 전", "짐", "현지")

@Composable
fun ChecklistScreen(
    repo: TripRepository,
    tripId: String,
    onBack: () -> Unit,
    onTab: (Tab) -> Unit,
) {
    val n = Neu
    val ctx = LocalContext.current
    val trips by repo.trips.collectAsStateWithLifecycle()
    val trip = trips.firstOrNull { it.id == tripId }
    var newText by remember { mutableStateOf("") }
    var newGroup by remember { mutableStateOf(0) }

    if (trip == null) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(n.bg))
        return
    }
    val done = trip.checksDone
    val total = trip.checks.size

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding().imePadding()) {
        ScreenHeader(title = "체크리스트", subtitle = "${trip.title} · ${trip.city}행 기본 항목에서 시작") {
            Text("$done", color = n.accent, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text("/$total", color = n.hint, fontFamily = Bricolage, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            NeuIconButton(
                Icons.Rounded.AutoAwesome, contentDescription = "클로드에게 물어보기", tint = n.amber, size = 40.dp, radius = 14.dp, iconSize = 18.dp,
                onClick = { askClaude(ctx, "${trip.city} ${trip.periodLabel} 여행 가는데 준비물이랑 출발 전 체크할 것 정리해줘. 날씨에 맞는 옷, 결제 수단, 입국 서류 포함해서.") },
            )
        }

        Gauge(fraction = if (total > 0) done.toFloat() / total else 0f, color = n.accent, modifier = Modifier.padding(horizontal = 22.dp).fillMaxWidth(), height = 10.dp, delayMs = 150)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val groups = (GROUPS + trip.checks.map { it.group }).distinct().filter { g -> trip.checks.any { it.group == g } || g in GROUPS.take(2) }
            groups.forEachIndexed { gi, g ->
                val list = trip.checks.filter { it.group == g }
                item(key = "g$g") {
                    RiseIn(gi) {
                        NeuCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp)) {
                            Row(Modifier.padding(horizontal = 4.dp, vertical = 0.dp).padding(bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
                                SectionLabel(g)
                                Spacer(Modifier.weight(1f))
                                Text("${list.count { it.done }} / ${list.size}", color = n.ink2, fontSize = 11.sp)
                            }
                            if (list.isEmpty()) Text("항목 없음", color = n.hint, fontSize = 12.5.sp, modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 8.dp))
                            list.forEach { c ->
                                CheckRow(
                                    c,
                                    onToggle = { repo.update(tripId) { t -> t.copy(checks = t.checks.map { if (it.id == c.id) it.copy(done = !it.done) else it }) } },
                                    onRemove = { repo.update(tripId) { t -> t.copy(checks = t.checks.filter { it.id != c.id }) } },
                                )
                            }
                        }
                    }
                }
            }
            item {
                RiseIn(3) {
                    Column {
                        Segmented(options = GROUPS, selected = newGroup, onSelect = { newGroup = it }, modifier = Modifier.fillMaxWidth(), height = 34.dp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            InsetField(value = newText, onValueChange = { newText = it }, modifier = Modifier.weight(1f), placeholder = "항목 추가…", height = 44.dp, radius = 15.dp, fontSize = 13)
                            NeuIconButton(
                                Icons.Rounded.Add, contentDescription = "추가", size = 44.dp, radius = 15.dp, tint = n.accent,
                                onClick = {
                                    val t = newText.trim()
                                    if (t.isNotEmpty()) {
                                        repo.update(tripId) { tr -> tr.copy(checks = tr.checks + CheckItem(group = GROUPS[newGroup], text = t)) }
                                        newText = ""
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        BottomTabBar(current = Tab.CHECK, onSelect = onTab, modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun CheckRow(c: CheckItem, onToggle: () -> Unit, onRemove: () -> Unit) {
    val n = Neu
    Row(
        modifier = Modifier.fillMaxWidth().height(46.dp).pressable(onClick = onToggle).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .then(
                    if (c.done) Modifier.neuRaisedAccent(radius = 9.dp, fill = n.accent, shadow = n.accentShadow, light = n.shadowLight, offset = 3.dp, blur = 8.dp)
                    else Modifier.neuInset(radius = 9.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp)
                ),
            contentAlignment = Alignment.Center,
        ) { if (c.done) Icon(Icons.Rounded.Check, contentDescription = null, tint = n.onAccent, modifier = Modifier.size(15.dp)) }
        Text(
            c.text,
            color = if (c.done) n.hint else n.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium,
            textDecoration = if (c.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.size(28.dp).pressable(onClick = onRemove), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, contentDescription = "삭제", tint = n.hint, modifier = Modifier.size(14.dp))
        }
    }
}
