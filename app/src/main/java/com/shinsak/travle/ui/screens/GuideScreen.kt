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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shinsak.travle.data.CURRENCY_SYMBOL
import com.shinsak.travle.data.PlaceCategory
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.data.guides.GuidePlace
import com.shinsak.travle.data.guides.Guides
import com.shinsak.travle.ui.askClaude
import com.shinsak.travle.ui.components.ChipRow
import com.shinsak.travle.ui.components.InsetField
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.icon
import com.shinsak.travle.ui.openMap
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuRaised
import com.shinsak.travle.ui.theme.pressable
import com.shinsak.travle.ui.trimZeros
import kotlin.math.min

private val FILTERS = listOf("전체" to null, "관광" to PlaceCategory.SIGHT, "맛집" to PlaceCategory.FOOD, "카페" to PlaceCategory.CAFE, "쇼핑" to PlaceCategory.SHOPPING, "골프" to PlaceCategory.GOLF, "교통·기타" to null)

@Composable
fun GuideScreen(
    repo: TripRepository,
    tripId: String,
    onBack: () -> Unit,
    onPick: (GuidePlace) -> Unit,
) {
    val n = Neu
    val ctx = LocalContext.current
    val trips by repo.trips.collectAsStateWithLifecycle()
    val trip = trips.firstOrNull { it.id == tripId }
    val guide = trip?.let { Guides.forCity(it.city) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }

    if (trip == null || guide == null) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(n.bg))
        return
    }

    val list = guide.places.filter { p ->
        val f = FILTERS[filter]
        val catOk = when {
            filter == 0 -> true
            f.second != null -> p.category == f.second
            else -> p.category == PlaceCategory.TRANSPORT || p.category == PlaceCategory.ETC || p.category == PlaceCategory.STAY
        }
        catOk && (query.isBlank() || p.name.contains(query, true) || p.local.contains(query, true) || p.area.contains(query, true) || p.tip.contains(query, true))
    }

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding().imePadding()) {
        ScreenHeader(title = guide.title, subtitle = "${guide.places.size}곳 · 탭하면 장소 추가에 채워짐", onBack = onBack)

        InsetField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.padding(horizontal = 22.dp).fillMaxWidth(), height = 46.dp,
            placeholder = "이름·동네·팁 검색", fontSize = 14,
            leading = { Icon(Icons.Rounded.Search, contentDescription = null, tint = n.hint, modifier = Modifier.size(17.dp)) },
        )
        Spacer(Modifier.height(10.dp))
        ChipRow(options = FILTERS.map { it.first }, selected = filter, onSelect = { filter = it }, modifier = Modifier.padding(horizontal = 22.dp), height = 34.dp)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(guide.note, color = n.ink2, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
            items(list.withIndex().toList(), key = { it.value.local + it.value.name }) { (i, p) ->
                RiseIn(min(i, 6)) {
                    GuideCard(
                        p = p, currency = guide.currency,
                        onPick = { onPick(p) },
                        onMap = { openMap(ctx, p.mapQuery) },
                        onAsk = { askClaude(ctx, "${trip.city} \"${p.name}(${p.local})\" 지금 운영시간, 입장료·가격, 가는 방법, 주변에 같이 갈 만한 곳 알려줘.") },
                    )
                }
            }
            if (list.isEmpty()) {
                item { Text("검색 결과 없음", color = n.hint, fontSize = 13.sp, modifier = Modifier.padding(16.dp)) }
            }
        }
    }
}

@Composable
private fun GuideCard(p: GuidePlace, currency: String, onPick: () -> Unit, onMap: () -> Unit, onAsk: () -> Unit) {
    val n = Neu
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onPick)
            .neuRaised(radius = 20.dp, fill = n.surface, dark = n.shadowDark, light = n.shadowLight, offset = 5.dp, blur = 13.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(n.well), contentAlignment = Alignment.Center) {
                Icon(p.category.icon(), contentDescription = p.category.label, tint = n.amber, modifier = Modifier.size(17.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(p.name, color = n.ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(listOf(p.local, p.area).filter { it.isNotBlank() }.joinToString(" · "), color = n.ink2, fontSize = 11.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (p.cost > 0) Text("${CURRENCY_SYMBOL[currency] ?: ""}${p.cost.trimZeros()}", color = n.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                else if (p.costNote.contains("무료")) Text("무료", color = n.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (p.hoursNeeded > 0) Text("${p.hoursNeeded.trimZeros()}시간", color = n.ink2, fontSize = 10.5.sp)
            }
        }
        if (p.hours.isNotBlank() || p.costNote.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(listOf(p.hours, p.costNote).filter { it.isNotBlank() }.joinToString(" · "), color = n.ink2, fontSize = 11.sp)
        }
        if (p.tip.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(p.tip, color = n.ink, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallAction("지도", Icons.Rounded.Map, onMap)
            SmallAction("클로드에 물어보기", Icons.Rounded.AutoAwesome, onAsk)
            Spacer(Modifier.weight(1f))
            Text("+ 일정에 넣기", color = n.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun SmallAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val n = Neu
    Row(
        modifier = Modifier
            .height(30.dp)
            .pressable(onClick = onClick)
            .neuRaised(radius = 10.dp, fill = n.bg, dark = n.shadowDark, light = n.shadowLight, offset = 3.dp, blur = 7.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = n.ink2, modifier = Modifier.size(13.dp))
        Text(text, color = n.ink2, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
