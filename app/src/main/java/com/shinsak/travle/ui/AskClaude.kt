package com.shinsak.travle.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

private const val CLAUDE_PKG = "com.anthropic.claude"

/**
 * 질문 텍스트를 들고 클로드 앱을 연다 (공유 인텐트). 앱 자체는 인터넷을 안 쓰고, 답은 클로드 앱에서 봄.
 * 클로드 앱이 없으면 일반 공유 시트.
 */
fun askClaude(ctx: Context, question: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, question)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val direct = Intent(send).setPackage(CLAUDE_PKG)
    val ok = runCatching { ctx.startActivity(direct); true }.getOrDefault(false)
    if (!ok) {
        runCatching { ctx.startActivity(Intent.createChooser(send, "클로드에게 물어보기").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Toast.makeText(ctx, "공유할 앱이 없음", Toast.LENGTH_SHORT).show() }
    }
}

/** 지도 링크나 주소를 외부 지도앱으로. 링크가 아니면 geo 검색으로 넘김. */
fun openMap(ctx: Context, linkOrAddress: String) {
    val s = linkOrAddress.trim()
    if (s.isEmpty()) return
    val uri = if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("geo:")) Uri.parse(s)
    else Uri.parse("geo:0,0?q=" + Uri.encode(s))
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(intent) }
        .onFailure { Toast.makeText(ctx, "열 수 있는 앱이 없음", Toast.LENGTH_SHORT).show() }
}
