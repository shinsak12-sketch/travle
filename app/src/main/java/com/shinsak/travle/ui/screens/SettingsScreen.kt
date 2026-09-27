package com.shinsak.travle.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.shinsak.travle.data.CURRENCIES
import com.shinsak.travle.data.CURRENCY_LABEL
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.ui.components.ConfirmDialog
import com.shinsak.travle.ui.components.InsetField
import com.shinsak.travle.ui.components.NeuButton
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.components.RiseIn
import com.shinsak.travle.ui.components.ScreenHeader
import com.shinsak.travle.ui.components.SectionLabel
import com.shinsak.travle.ui.components.Segmented
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.neuInset
import com.shinsak.travle.ui.theme.pressable
import com.shinsak.travle.ui.trimZeros
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(repo: TripRepository, onBack: () -> Unit) {
    val n = Neu
    val ctx = LocalContext.current
    val settings by repo.settings.collectAsStateWithLifecycle()
    val trips by repo.trips.collectAsStateWithLifecycle()
    var askClear by remember { mutableStateOf(false) }
    val fxCodes = CURRENCIES.filter { it != "KRW" }
    val fxText = remember { mutableStateMapOf<String, String>().apply { fxCodes.forEach { put(it, (settings.fxRates[it] ?: 1.0).trimZeros()) } } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(repo.exportJson().toByteArray()) } ?: error("열기 실패") }.isSuccess
        Toast.makeText(ctx, if (ok) "백업 파일 저장함" else "저장 실패", Toast.LENGTH_SHORT).show()
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("열기 실패")
            repo.importJson(text)
        }.onSuccess { count ->
            fxCodes.forEach { fxText[it] = (repo.settings.value.fxRates[it] ?: 1.0).trimZeros() }
            Toast.makeText(ctx, "여행 ${count}개 불러옴", Toast.LENGTH_SHORT).show()
        }.onFailure { Toast.makeText(ctx, "불러오기 실패: ${it.message ?: "형식이 다름"}", Toast.LENGTH_LONG).show() }
    }

    Column(Modifier.fillMaxSize().background(n.bg).statusBarsPadding().imePadding()) {
        ScreenHeader(title = "설정", subtitle = "전부 이 기기에만 저장됨 · 인터넷 안 씀", onBack = onBack)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RiseIn(0) {
                NeuCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(start = 17.dp, end = 17.dp, top = 15.dp, bottom = 14.dp)) {
                    SectionLabel("화면 테마")
                    Spacer(Modifier.height(10.dp))
                    val modes = listOf("system", "light", "dark")
                    Segmented(options = listOf("시스템", "라이트", "다크"), selected = modes.indexOf(settings.themeMode).coerceAtLeast(0), onSelect = { i -> repo.updateSettings { it.copy(themeMode = modes[i]) } }, modifier = Modifier.fillMaxWidth())
                }
            }
            RiseIn(1) {
                NeuCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(start = 17.dp, end = 17.dp, top = 15.dp, bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        SectionLabel("환율")
                        Spacer(Modifier.weight(1f))
                        Text("외화 1 = ? 원 · 지출 입력 때 환산에 씀", color = n.ink2, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(11.dp))
                    fxCodes.forEachIndexed { i, code ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(code, color = n.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(44.dp))
                            Text(CURRENCY_LABEL[code] ?: "", color = n.ink2, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            InsetField(
                                value = fxText[code] ?: "",
                                onValueChange = { s ->
                                    val cleaned = s.filter { it.isDigit() || it == '.' }
                                    fxText[code] = cleaned
                                    cleaned.toDoubleOrNull()?.takeIf { it > 0 }?.let { v -> repo.updateSettings { it.copy(fxRates = it.fxRates + (code to v)) } }
                                },
                                modifier = Modifier.width(150.dp), height = 40.dp, radius = 13.dp,
                                keyboardType = KeyboardType.Decimal, suffix = "원", textAlign = TextAlign.End, fontSize = 14,
                            )
                        }
                        if (i < fxCodes.size - 1) Spacer(Modifier.height(8.dp))
                    }
                }
            }
            RiseIn(2) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeuButton("JSON 내보내기", onClick = { exportLauncher.launch("travle-backup-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.KOREA).format(Date())}.json") }, modifier = Modifier.weight(1f), height = 56.dp, radius = 19.dp, icon = Icons.Rounded.FileDownload, color = n.ink)
                        NeuButton("불러오기", onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }, modifier = Modifier.weight(1f), height = 56.dp, radius = 19.dp, icon = Icons.Rounded.FileUpload, color = n.ink)
                    }
                    Text("여행 ${trips.size}개와 설정이 파일 하나에 들어감. 기기 바꿀 때 이걸로 옮기면 됨.", color = n.ink2, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
                }
            }
            RiseIn(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .pressable { askClear = true }
                        .neuInset(radius = 17.dp, fill = n.bg, dark = n.red.copy(alpha = 0.18f), light = n.shadowLight, offset = 4.dp, blur = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("전체 데이터 삭제", color = n.red, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) }
            }
            Text("Travle v2.0.0 · 오프라인 전용", color = n.ink2, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 4.dp))
        }
    }

    if (askClear) {
        ConfirmDialog(
            title = "전부 지울까요?", message = "여행 ${trips.size}개가 삭제됨. 설정은 남음. 먼저 JSON으로 내보내두는 게 안전함.",
            confirmText = "전체 삭제", destructive = true,
            onConfirm = { askClear = false; repo.clearAll(); Toast.makeText(ctx, "전부 지웠음", Toast.LENGTH_SHORT).show() },
            onDismiss = { askClear = false },
        )
    }
}
