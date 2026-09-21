package com.shinsak.travle.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text as MlText
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.shinsak.travle.data.ParsedFlight
import com.shinsak.travle.data.ScreenshotParser
import com.shinsak.travle.ui.components.AccentButton
import com.shinsak.travle.ui.components.InsetPanel
import com.shinsak.travle.ui.components.NeuButton
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.theme.Neu
import com.shinsak.travle.ui.theme.pressable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** 사진 고르기 → ML Kit 한국어 OCR(온디바이스, 모델 번들) → 파싱 */
class ScreenshotScanner(val busy: State<Boolean>, private val launcher: () -> Unit) {
    fun launch() = launcher()
}

@Composable
fun rememberScreenshotScanner(onResult: (ParsedFlight?) -> Unit): ScreenshotScanner {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val busy = remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy.value = true
        scope.launch {
            val parsed = runCatching { recognize(ctx, uri) }.getOrNull()
            busy.value = false
            onResult(parsed)
        }
    }
    return remember(picker) {
        ScreenshotScanner(busy) {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

private class RowData(val text: String, val rect: Rect, val words: List<Pair<String, Rect>>)

private val TIME_LIKE = Regex("[0-9OoQDlIgq]{1,4}\\s*[:.;：∶]\\s*[0-9OoQDlISBgq]{2}")
private val AMPM_WORD = Regex("오\\s*[전후휴호]|[AaPp][Mm]")

private suspend fun recognize(ctx: Context, uri: Uri): ParsedFlight? = withContext(Dispatchers.Default) {
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { decoder, _, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = false
    }
    val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    try {
        val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        val rows = rebuildRows(text)
        val fixed = rows.map { row -> repairAmPm(recognizer, bitmap, row) }
        ScreenshotParser.parse(fixed)
    } finally {
        recognizer.close()
    }
}

/**
 * 굵은 "오후"가 숫자/글자로 오인식되는 행("239:50- 2210:50")은 그 행만 3배 확대해서 다시 읽는다.
 * 확대해도 안 나오면 시간 단어 왼쪽(오전/오후 자리)만 잘라 한 번 더.
 */
private suspend fun repairAmPm(recognizer: TextRecognizer, bitmap: Bitmap, row: RowData): String {
    if (!TIME_LIKE.containsMatchIn(row.text)) return row.text
    if (AMPM_WORD.containsMatchIn(row.text)) return row.text
    if (row.text.contains("월") || row.text.contains("여행객") || row.text.contains("₩")) return row.text

    // 1) 행 전체 확대
    ocrCrop(recognizer, bitmap, row.rect, 3f)?.let { again ->
        if (AMPM_WORD.containsMatchIn(again) && TIME_LIKE.findAll(again).count() >= TIME_LIKE.findAll(row.text).count()) {
            return again
        }
        if (AMPM_WORD.containsMatchIn(again)) return "${AMPM_WORD.find(again)!!.value} ${row.text}"
    }
    // 2) 시간 단어마다 왼쪽 절반(오전/오후 자리)만 확대
    val hints = row.words.filter { TIME_LIKE.containsMatchIn(it.first) }.mapNotNull { (_, r) ->
        val left = Rect(r.left - r.height() / 2, r.top, r.left + (r.width() * 0.55f).toInt(), r.bottom)
        ocrCrop(recognizer, bitmap, left, 4f)?.let { t -> AMPM_WORD.find(t)?.value }
    }
    if (hints.isNotEmpty()) {
        var idx = 0
        return TIME_LIKE.replace(row.text) { m ->
            val h = hints.getOrNull(idx) ?: hints.last()
            idx++
            "$h ${m.value}"
        }
    }
    return row.text
}

private suspend fun ocrCrop(recognizer: TextRecognizer, src: Bitmap, rect: Rect, scale: Float): String? {
    val pad = (rect.height() * 0.35f).toInt().coerceAtLeast(6)
    val l = (rect.left - pad).coerceAtLeast(0)
    val t = (rect.top - pad).coerceAtLeast(0)
    val r = (rect.right + pad).coerceAtMost(src.width)
    val b = (rect.bottom + pad).coerceAtMost(src.height)
    if (r - l < 8 || b - t < 8) return null
    return runCatching {
        val crop = Bitmap.createBitmap(src, l, t, r - l, b - t)
        val big = Bitmap.createScaledBitmap(crop, ((r - l) * scale).toInt(), ((b - t) * scale).toInt(), true)
        val res = recognizer.process(InputImage.fromBitmap(big, 0)).await()
        rebuildRows(res).joinToString(" ") { it.text }
    }.getOrNull()
}

/**
 * ML Kit이 주는 "줄"은 대시나 열 간격에서 멋대로 끊기므로, 단어(Element) 좌표로 행을 다시 묶는다.
 * 세로 중심이 비슷한 단어끼리 한 행, 행 안에서는 왼→오른쪽.
 */
private fun rebuildRows(text: MlText): List<RowData> {
    data class W(val s: String, val r: Rect)
    val words = text.textBlocks.flatMap { b -> b.lines.flatMap { l -> l.elements } }
        .mapNotNull { e -> e.boundingBox?.let { W(e.text, Rect(it)) } }
        .sortedBy { it.r.centerY() }
    if (words.isEmpty()) {
        return text.textBlocks.flatMap { it.lines }.map { RowData(it.text, it.boundingBox ?: Rect(), emptyList()) }
    }
    val medianH = words.map { it.r.height() }.sorted()[words.size / 2].coerceAtLeast(1)
    val tol = (medianH * 0.6f).toInt().coerceAtLeast(6)
    val rows = mutableListOf<MutableList<W>>()
    for (w in words) {
        val row = rows.lastOrNull()
        if (row != null && kotlin.math.abs(row.map { it.r.centerY() }.average().toInt() - w.r.centerY()) <= tol) row.add(w) else rows.add(mutableListOf(w))
    }
    return rows.map { r ->
        val sorted = r.sortedBy { it.r.centerX() }
        val union = Rect(sorted.first().r)
        sorted.forEach { union.union(it.r) }
        RowData(sorted.joinToString(" ") { it.s }, union, sorted.map { it.s to it.r })
    }
}

@Composable
fun ScanBusyDialog() {
    val n = Neu
    Dialog(onDismissRequest = {}) {
        NeuCard(radius = 24.dp, padding = PaddingValues(horizontal = 26.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CircularProgressIndicator(color = n.accent, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                Text("스크린샷 읽는 중…", color = n.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** 인식 결과 확인. 적용 누르면 폼에 채움. */
@Composable
fun ScanResultDialog(parsed: ParsedFlight, onApply: () -> Unit, onDismiss: () -> Unit) {
    val n = Neu
    val rows = listOf(
        "여행지" to (parsed.destination?.let { d -> parsed.origin?.let { o -> "$o → $d" } ?: d }),
        "날짜" to parsed.dateLabel?.let { d -> parsed.nights?.let { "$d · ${it}박 ${it + 1}일" } ?: d },
        "인원" to parsed.people?.let { "${it}명" },
        "항공사" to parsed.airline,
        "가는편" to parsed.legs.getOrNull(0)?.label,
        "오는편" to parsed.legs.getOrNull(1)?.label,
        "1인 요금" to parsed.pricePerPerson?.let { "${it.won()}원" },
        "항공권 총액" to parsed.totalPrice?.let { "${it.won()}원" },
        "판매처" to parsed.seller,
    )
    var showRaw by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    Dialog(onDismissRequest = onDismiss) {
        NeuCard(radius = 26.dp, padding = PaddingValues(22.dp)) {
            Text("스크린샷 인식 결과", color = n.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("맞으면 적용, 틀린 건 적용 후에 직접 고치면 됨", color = n.ink2, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
            ) {
                rows.forEach { (label, value) ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(label, color = n.ink2, fontSize = 12.5.sp, modifier = Modifier.width(76.dp).padding(top = 1.dp))
                        Text(
                            value ?: "못 찾음",
                            color = if (value != null) n.ink else n.hint,
                            fontSize = 13.sp,
                            fontWeight = if (value != null) FontWeight.SemiBold else FontWeight.Normal,
                            lineHeight = 18.sp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (showRaw) "원문 접기" else "인식된 원문 보기 (${parsed.rawLines.size}줄)",
                    color = n.accent, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.pressable { showRaw = !showRaw }.padding(vertical = 4.dp),
                )
                if (showRaw) {
                    InsetPanel(modifier = Modifier.fillMaxWidth(), radius = 14.dp, padding = PaddingValues(12.dp)) {
                        parsed.rawLines.forEachIndexed { i, l ->
                            Text("${i + 1}. $l", color = n.ink2, fontSize = 10.5.sp, lineHeight = 15.sp)
                        }
                    }
                    Text(
                        "원문 복사",
                        color = n.accent, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .pressable { clipboard.setText(AnnotatedString(parsed.rawLines.joinToString("\n"))) }
                            .padding(vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeuButton("취소", onClick = onDismiss, modifier = Modifier.weight(1f), color = n.ink2)
                AccentButton("적용", onClick = onApply, modifier = Modifier.weight(1f), height = 46.dp, radius = 17.dp)
            }
        }
    }
}
