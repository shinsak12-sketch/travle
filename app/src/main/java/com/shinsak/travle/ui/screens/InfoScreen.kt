package com.shinsak.travle.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.GolfCourse
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Wifi
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.data.guides.Guides
import com.shinsak.travle.data.guides.Phrase
import com.shinsak.travle.data.guides.Phrasebooks
import com.shinsak.travle.data.guides.TipKind
import com.shinsak.travle.data.guides.TipSection
import com.shinsak.travle.ui.TtsController
import com.shinsak.travle.ui.askClaude
import com.shinsak.travle.ui.components.BottomTabBar
import com.shinsak.travle.ui.components.ChipRow
import com.shinsak.travle.ui.components.InsetField
import com.shinsak.travle.ui.components.NeuButton
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.NeuIconButton
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.components.Segmented
import com.shinsak.travle.ui.components.Tab
import com.shinsak.travle.ui.openTtsInstall
import com.shinsak.travle.ui.rememberTts
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.neuRaised
import com.shinsak.travle.ui.theme.neuRaisedAccent
import com.shinsak.travle.ui.theme.pressable
import kotlin.math.min

private fun TipKind.icon(): ImageVector = when (this) {
    TipKind.PAY -> Icons.Rounded.Payments
    TipKind.TAXI -> Icons.Rounded.LocalTaxi
    TipKind.NET -> Icons.Rounded.Wifi
    TipKind.ENTRY -> Icons.Rounded.Flight
    TipKind.FOOD -> Icons.Rounded.Restaurant
    TipKind.SIGHT -> Icons.Rounded.Museum
    TipKind.GOLF -> Icons.Rounded.GolfCourse
    TipKind.SAFETY -> Icons.Rounded.LocalHospital
    TipKind.SHOP -> Icons.Rounded.ShoppingBag
    TipKind.MANNER -> Icons.Rounded.VolunteerActivism
    TipKind.WEATHER -> Icons.Rounded.Thermostat
}

@Composable
fun InfoScreen(
    repo: TripRepository,
    tripId: String,
    onBack: () -> Unit,
    onTab: (Tab) -> Unit,
    onGuide: () -> Unit,
) {
    val n = Neu
    val ctx = LocalContext.current
    val trips by repo.trips.collectAsStateWithLifecycle()
    val trip = trips.firstOrNull { it.id == tripId }
    var seg by rememberSaveable { mutableIntStateOf(0) }

    if (trip == null) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(n.bg))
        return
    }
    val guide = remember(trip.city) { Guides.forCity(trip.city) }
    val book = remember(guide) { guide?.language?.let { Phrasebooks.forLanguage(it) } }

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding().imePadding()) {
        ScreenHeader(title = "정보", subtitle = guide?.title ?: "${trip.city} · 가이드 없음") {
            NeuIconButton(
                Icons.Rounded.AutoAwesome, contentDescription = "클로드에게 물어보기", tint = n.amber, size = 40.dp, radius = 14.dp, iconSize = 18.dp,
                onClick = { askClaude(ctx, "${trip.city} ${trip.periodLabel} 여행인데 현지에서 꼭 알아야 할 팁(결제·교통·입국·매너)이랑 최근 바뀐 거 있으면 알려줘.") },
            )
        }

        if (guide == null) {
            Column(Modifier.weight(1f).padding(22.dp)) {
                NeuCard(modifier = Modifier.fillMaxWidth()) {
                    Text("이 도시 가이드가 아직 없음", color = n.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("선양처럼 도시별 가이드 파일을 추가하면 꿀팁·회화·장소가 여기 뜸. 위 ✨ 버튼으로 클로드에 바로 물어볼 수 있음.", color = n.ink2, fontSize = 12.5.sp, lineHeight = 18.sp)
                }
            }
        } else {
            Segmented(options = listOf("꿀팁", "회화", "장소"), selected = seg, onSelect = { seg = it }, modifier = Modifier.padding(horizontal = 22.dp).fillMaxWidth(), height = 38.dp)
            Spacer(Modifier.height(10.dp))
            when (seg) {
                0 -> TipsPane(guide.tips, Modifier.weight(1f))
                1 -> if (book != null) PhrasePane(book.groups, book.localeTag, Modifier.weight(1f)) else Box(Modifier.weight(1f))
                else -> Column(Modifier.weight(1f).padding(horizontal = 22.dp)) {
                    val by = guide.places.groupingBy { it.category.label }.eachCount()
                    NeuCard(modifier = Modifier.fillMaxWidth().pressable(onClick = onGuide)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(n.accentTint), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.TravelExplore, contentDescription = null, tint = n.accent, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${guide.title} ${guide.places.size}곳", color = n.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(by.entries.joinToString(" · ") { "${it.key} ${it.value}" }, color = n.ink2, fontSize = 11.sp)
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = n.hint)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("각 장소 카드에 운영시간·요금·팁 있고, 탭하면 바로 일정에 들어감. 지도 버튼은 현지명으로 지도앱 검색.", color = n.ink2, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }

        BottomTabBar(current = Tab.INFO, onSelect = onTab, modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun TipsPane(sections: List<TipSection>, modifier: Modifier) {
    val n = Neu
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sections.forEachIndexed { i, s ->
            item(key = s.title) {
                val isOpen = open == s.title
                RiseIn(min(i, 6)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .neuRaised(radius = 20.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 5.dp, blur = 13.dp)
                            .pressable { open = if (isOpen) null else s.title }
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(if (isOpen) n.accentTint else n.well), contentAlignment = Alignment.Center) {
                                Icon(s.kind.icon(), contentDescription = null, tint = if (isOpen) n.accent else n.amber, modifier = Modifier.size(19.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.title, color = n.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(s.summary, color = n.ink2, fontSize = 11.5.sp, lineHeight = 15.sp)
                            }
                            Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = n.hint, modifier = Modifier.rotate(if (isOpen) 180f else 0f))
                        }
                        AnimatedVisibility(visible = isOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column(Modifier.padding(top = 12.dp)) {
                                s.tips.forEachIndexed { ti, t ->
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .neuInset(radius = 14.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                    ) {
                                        Text(t.title, color = n.accentDeep, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(3.dp))
                                        Text(t.body, color = n.ink, fontSize = 12.5.sp, lineHeight = 18.sp)
                                    }
                                    if (ti < s.tips.lastIndex) Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhrasePane(groups: List<com.shinsak.travle.data.guides.PhraseGroup>, localeTag: String, modifier: Modifier) {
    val n = Neu
    val ctx = LocalContext.current
    val tts = rememberTts(localeTag)
    var gi by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val list: List<Phrase> = if (query.isBlank()) groups[gi].phrases
    else groups.flatMap { it.phrases }.filter { it.ko.contains(query, true) || it.local.contains(query) || it.roman.contains(query, true) || it.read.contains(query) }

    Column(modifier) {
        InsetField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.padding(horizontal = 22.dp).fillMaxWidth(), height = 44.dp,
            placeholder = "한국어로 검색 (예: 화장실, 계산)", fontSize = 13,
            leading = { Icon(Icons.Rounded.Search, contentDescription = null, tint = n.hint, modifier = Modifier.size(16.dp)) },
        )
        Spacer(Modifier.height(8.dp))
        if (query.isBlank()) ChipRow(options = groups.map { it.title }, selected = gi, onSelect = { gi = it }, modifier = Modifier.padding(horizontal = 22.dp), height = 34.dp)

        if (tts.state == TtsController.State.MISSING || tts.state == TtsController.State.FAILED) {
            Row(
                Modifier.padding(horizontal = 22.dp, vertical = 8.dp).fillMaxWidth()
                    .neuInset(radius = 14.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("중국어 음성 데이터가 없어서 소리 안 남. 한 번만 설치하면 오프라인에서도 됨.", color = n.ink2, fontSize = 11.5.sp, lineHeight = 15.sp, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                NeuButton("설치", onClick = { openTtsInstall(ctx) }, height = 34.dp, radius = 12.dp)
            }
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (list.isEmpty()) item { Text("검색 결과 없음", color = n.hint, fontSize = 13.sp, modifier = Modifier.padding(16.dp)) }
            list.forEachIndexed { i, p ->
                item(key = "$i-${p.local}") {
                    RiseIn(min(i, 6)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .neuRaised(radius = 18.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 4.dp, blur = 11.dp)
                                .pressable { tts.speak(p.local) }
                                .padding(start = 14.dp, end = 10.dp, top = 11.dp, bottom = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.ko, color = n.ink2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text(p.local, color = n.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("${p.roman} · ${p.read}", color = n.hint, fontSize = 11.5.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier.size(42.dp)
                                    .neuRaisedAccent(radius = 14.dp, fill = n.accent, shadow = n.accentShadow, light = n.shadowLight, offset = 3.dp, blur = 8.dp)
                                    .pressable { tts.speak(p.local) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.VolumeUp, contentDescription = "듣기", tint = n.onAccent, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }
}
