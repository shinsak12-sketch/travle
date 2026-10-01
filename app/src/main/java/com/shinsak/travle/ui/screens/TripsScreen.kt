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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.CITY_PRESETS
import com.shinsak.travle.data.Trip
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.ui.components.ConfirmDialog
import com.shinsak.travle.ui.components.InsetPanel
import com.shinsak.travle.ui.components.NeuIconButton
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.manwon
import com.shinsak.travle.ui.theme.Bricolage
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.neuRaised
import com.shinsak.travle.ui.theme.neuRaisedAccent
import com.shinsak.travle.ui.theme.pressable
import com.shinsak.travle.ui.theme.pressableLong
import kotlin.math.min

@Composable
fun TripsScreen(
    repo: TripRepository,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit,
) {
    val n = Neu
    val trips by repo.trips.collectAsStateWithLifecycle()
    val upcoming = trips.filter { !it.isPast }.sortedBy { it.start }
    val past = trips.filter { it.isPast }.sortedByDescending { it.start }
    val featured = upcoming.firstOrNull()
    val rest = upcoming.drop(1)
    var deleting by remember { mutableStateOf<Trip?>(null) }

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding()) {
        Row(Modifier.padding(horizontal = 22.dp).padding(top = 22.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TRAVLE", color = n.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.4.sp)
                Spacer(Modifier.height(6.dp))
                Text("내 여행", color = n.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
            }
            NeuIconButton(Icons.Rounded.Tune, contentDescription = "설정", onClick = onSettings, size = 46.dp, radius = 17.dp)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (featured != null) {
                item { RiseIn(0) { FeaturedCard(featured, onLong = { deleting = featured }) { onOpen(featured.id) } } }
            }
            item {
                RiseIn(1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable(onClick = onNew)
                            .neuRaised(radius = 22.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier.size(46.dp).neuInset(radius = 16.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Add, contentDescription = null, tint = n.accent, modifier = Modifier.size(24.dp)) }
                        Column {
                            Text("여행 일정 만들기", color = n.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("도시 고르고 날짜만 정하면 됨", color = n.ink2, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            if (trips.isEmpty()) {
                item {
                    RiseIn(2) {
                        InsetPanel(modifier = Modifier.fillMaxWidth(), radius = 22.dp, padding = PaddingValues(24.dp)) {
                            Text("아직 여행이 없음", color = n.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            Text("위에서 첫 여행을 만들어보세요.", color = n.ink2, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            if (rest.isNotEmpty()) {
                item { SectionLabel("다가오는 여행 · ${rest.size}", modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
                items(rest, key = { it.id }) { t -> TripRow(t, onLong = { deleting = t }) { onOpen(t.id) } }
            }
            if (past.isNotEmpty()) {
                item { SectionLabel("지난 여행 · ${past.size}", modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
                items(past, key = { it.id }) { t -> TripRow(t, onLong = { deleting = t }) { onOpen(t.id) } }
            }
            item { Text("카드를 길게 누르면 삭제", color = n.hint, fontSize = 10.5.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) }
        }

        Text(
            "전부 이 기기에만 저장됨 · 설정에서 JSON 백업",
            color = n.hint, fontSize = 10.5.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 14.dp),
        )
    }

    deleting?.let { t ->
        ConfirmDialog(
            title = "이 여행을 지울까요?",
            message = "\"${t.title.ifBlank { "${t.city} 여행" }}\"의 일정·가계부·체크리스트가 전부 삭제됨. 되돌릴 수 없음.",
            confirmText = "삭제", destructive = true,
            onConfirm = { repo.delete(t.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun FeaturedCard(t: Trip, onLong: () -> Unit, onClick: () -> Unit) {
    val n = Neu
    val d = t.dDay
    val dLabel = when {
        d > 0 -> "D-$d"
        d == 0L -> "D-DAY"
        else -> "여행 중"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressableLong(onLongClick = onLong, onClick = onClick)
            .neuRaisedAccent(radius = 25.dp, fill = n.accent, shadow = n.accentShadow, light = n.shadowLight, offset = 8.dp, blur = 24.dp)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("다가오는 여행", color = n.onAccentSoft, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp)
            Spacer(Modifier.weight(1f))
            Text(
                dLabel, color = n.onAccent, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(t.title.ifBlank { "${t.city} 여행" }, color = n.onAccent, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
        Spacer(Modifier.height(4.dp))
        Text("${t.periodLabel} · ${t.nights}박 ${t.days}일 · ${t.people}명", color = n.onAccentSoft, fontSize = 12.5.sp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val f = t.flight
            Chip(if (f.outbound != null) "✈ ${f.outbound.dep}" else "항공 미정")
            Chip(if (!t.stay.isEmpty) "숙소 ✓" else "숙소 미정")
            Chip("체크 ${t.checksDone}/${t.checks.size}")
            Chip("지출 ${t.totalKrw.manwon()}")
        }
    }
}

@Composable
private fun Chip(text: String) {
    val n = Neu
    Text(
        text, color = n.onAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun TripRow(t: Trip, onLong: () -> Unit, onClick: () -> Unit) {
    val n = Neu
    val code = CITY_PRESETS.firstOrNull { it.name == t.city }?.code ?: t.city.take(3).uppercase()
    val color = n.chart[(t.id.hashCode().let { if (it < 0) -it else it }) % n.chart.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressableLong(onLongClick = onLong, onClick = onClick)
            .neuRaised(radius = 20.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 5.dp, blur = 13.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Text(code, color = Color.White, fontFamily = Bricolage, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 0.5.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(t.title.ifBlank { "${t.city} 여행" }, color = n.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${t.periodLabel} · ${t.people}명 · 지출 ${t.totalKrw.manwon()}", color = n.ink2, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = n.hint, modifier = Modifier.size(18.dp))
    }
}
