package com.shinsak.travle.ui

import java.text.NumberFormat
import java.util.Locale

private val krFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale.KOREA)

/** 1860000 → "1,860,000" */
fun Long.won(): String = krFormat.format(this)

/** 1860000 → "186만", 8000 → "8,000" */
fun Long.manwon(): String {
    if (this < 10_000) return won()
    val man = (this + 5_000) / 10_000
    return "${krFormat.format(man)}만"
}

/** 2300000 → "230만원", 1234567 → "123.5만원" (소수 한 자리) */
fun Long.manwonPrecise(): String {
    if (this < 10_000) return "${won()}원"
    val v = this / 10_000.0
    val s = String.format(Locale.KOREA, "%.1f", v).removeSuffix(".0")
    return "${s}만원"
}

/** 입력 중인 숫자 문자열에 콤마 붙임. "1234567.5" → "1,234,567.5" */
fun formatTyped(raw: String): String {
    val cleaned = raw.filter { it.isDigit() || it == '.' }
    if (cleaned.isEmpty()) return ""
    val dot = cleaned.indexOf('.')
    val intPart = if (dot >= 0) cleaned.substring(0, dot) else cleaned
    val fracPart = if (dot >= 0) cleaned.substring(dot + 1).replace(".", "") else null
    val intFormatted = intPart.trimStart('0').ifEmpty { "0" }
        .reversed().chunked(3).joinToString(",").reversed()
    return if (fracPart != null) "$intFormatted.$fracPart" else intFormatted
}

fun parseAmount(s: String): Double = s.replace(",", "").trim().toDoubleOrNull() ?: 0.0

fun parseIntSafe(s: String): Int = s.replace(",", "").trim().toIntOrNull() ?: 0

/** 환율처럼 소수점 많은 값 → 불필요한 0 제거 */
fun Double.trimZeros(): String {
    if (this == 0.0) return "0"
    val s = String.format(Locale.US, "%.4f", this)
    return s.trimEnd('0').trimEnd('.')
}
