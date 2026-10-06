package com.shinsak.travle.ui

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** 오프라인 TTS. 기기에 해당 언어 음성 데이터가 있어야 함 (구글 TTS 중국어 팩 등). */
class TtsController(ctx: Context, private val locale: Locale) {
    enum class State { INIT, READY, MISSING, FAILED }

    var state by mutableStateOf(State.INIT)
        private set
    private var engine: TextToSpeech? = null

    init {
        engine = TextToSpeech(ctx.applicationContext) { status ->
            Handler(Looper.getMainLooper()).post { configure(status) }
        }
    }

    private fun configure(status: Int) {
        val e = engine
        if (status != TextToSpeech.SUCCESS || e == null) { state = State.FAILED; return }
        val r = runCatching { e.setLanguage(locale) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        state = if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) State.MISSING else State.READY
        if (state == State.READY) e.setSpeechRate(0.85f)
    }

    fun speak(text: String) {
        val e = engine ?: return
        if (state != State.READY) return
        e.speak(text, TextToSpeech.QUEUE_FLUSH, null, "p${text.hashCode()}")
    }

    fun shutdown() {
        runCatching { engine?.stop(); engine?.shutdown() }
        engine = null
    }
}

@Composable
fun rememberTts(localeTag: String): TtsController {
    val ctx = LocalContext.current
    val c = remember(localeTag) { TtsController(ctx, Locale.forLanguageTag(localeTag)) }
    DisposableEffect(c) { onDispose { c.shutdown() } }
    return c
}

/** 음성 데이터 설치 화면 또는 TTS 설정으로 */
fun openTtsInstall(ctx: Context) {
    val install = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (runCatching { ctx.startActivity(install); true }.getOrDefault(false)) return
    val settings = Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(settings) }
        .onFailure { Toast.makeText(ctx, "설정 > 일반 > 텍스트 음성 변환에서 중국어 음성 설치", Toast.LENGTH_LONG).show() }
}
