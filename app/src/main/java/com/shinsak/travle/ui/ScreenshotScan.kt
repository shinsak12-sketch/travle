package com.shinsak.travle.ui

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.shinsak.travle.data.ParsedFlight
import com.shinsak.travle.data.ScreenshotParser
import com.shinsak.travle.ui.components.AccentButton
import com.shinsak.travle.ui.components.NeuButton
import com.shinsak.travle.ui.components.NeuCard
import com.shinsak.travle.ui.theme.Neu

/** 사진 고르기 → ML Kit 한국어 OCR(온디바이스, 모델 번들) → 파싱 */
class ScreenshotScanner(val busy: State<Boolean>, private val launcher: () -> Unit) {
    fun launch() = launcher()
}

@Composable
fun rememberScreenshotScanner(onResult: (ParsedFlight?) -> Unit): ScreenshotScanner {
    val ctx = LocalContext.current
    val busy = remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy.value = true
        recognize(ctx, uri) { parsed ->
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

private fun recognize(ctx: Context, uri: Uri, done: (ParsedFlight?) -> Unit) {
    val image = try {
        InputImage.fromFilePath(ctx, uri)
    } catch (e: Exception) {
        done(null)
        return
    }
    val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    recognizer.process(image)
        .addOnSuccessListener { text ->
            // 블록 → 줄로 펼치고 위→아래, 왼→오른쪽 순으로
            val lines = text.textBlocks
                .flatMap { it.lines }
                .sortedWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
                .map { it.text }
            done(ScreenshotParser.parse(lines))
        }
        .addOnFailureListener { done(null) }
        .addOnCompleteListener { recognizer.close() }
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
    Dialog(onDismissRequest = onDismiss) {
        NeuCard(radius = 26.dp, padding = PaddingValues(22.dp)) {
            Text("스크린샷 인식 결과", color = n.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("맞으면 적용, 틀린 건 적용 후에 직접 고치면 됨", color = n.ink2, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
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
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeuButton("취소", onClick = onDismiss, modifier = Modifier.weight(1f), color = n.ink2)
                AccentButton("적용", onClick = onApply, modifier = Modifier.weight(1f), height = 46.dp, radius = 17.dp)
            }
        }
    }
}
