package com.shinsak.travle.data

/** 숙소 예약 확인 스크린샷(아고다·부킹닷컴·트립닷컴류)에서 뽑은 값. 못 찾은 건 null. */
data class ParsedStay(
    val name: String? = null,
    val checkIn: String? = null,
    val checkOut: String? = null,
    val address: String? = null,
    val bookingNo: String? = null,
    val priceKrw: Long? = null,
    val priceForeign: Pair<String, Double>? = null,
    val rawLines: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = name == null && checkIn == null && priceKrw == null && priceForeign == null && bookingNo == null
}

object StayParser {
    private val DASH = "[–—\\-~]"
    /** "10월 9일", "10.9", "10/9", "2026-10-09", "2026.10.09" */
    private val DATE = Regex("(?:(\\d{4})[.\\-/]\\s*)?(\\d{1,2})\\s*[월.\\-/]\\s*(\\d{1,2})\\s*일?")
    private val TIME = Regex("(\\d{1,2}):(\\d{2})")
    private val KRW = Regex("[₩W￦]\\s*(\\d{1,3}(?:,\\d{3})+|\\d{5,})|(\\d{1,3}(?:,\\d{3})+|\\d{5,})\\s*원")
    private val FOREIGN = Regex("(CNY|RMB|JPY|USD|THB|VND|TWD|HKD|SGD|MYR|EUR|PHP|¥|\\$|€|฿|₫|NT\\$|HK\\$|S\\$)\\s*(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?|\\d+(?:\\.\\d+)?)")
    private val BOOKING_NO = Regex("(?:예약\\s*번호|예약번호|확인\\s*번호|Booking\\s*(?:ID|No\\.?|number)|Confirmation\\s*(?:No\\.?|number|code)|Reservation\\s*(?:ID|No\\.?))\\s*[:：]?\\s*([A-Z0-9][A-Z0-9\\-.]{5,})", RegexOption.IGNORE_CASE)
    private val BARE_NO = Regex("\\b(\\d{8,12})\\b")
    private val HOTEL_WORDS = listOf("호텔", "Hotel", "HOTEL", "리조트", "Resort", "호스텔", "Hostel", "게스트하우스", "인 ", "Inn", "스위트", "Suites", "레지던스", "아파트", "酒店", "宾馆", "民宿", "旅馆", "客栈")
    private val ADDR_HINT = Regex("(주소|Address|[路街区号巷道])")
    private val NOISE = Regex("(예약|확인|취소|무료|조식|체크|check|총액|결제|합계|요금|가격|price|total|night|박|룸|room|객실)", RegexOption.IGNORE_CASE)
    private val SYMBOL_TO_CODE = mapOf("¥" to "CNY", "$" to "USD", "€" to "EUR", "฿" to "THB", "₫" to "VND", "NT$" to "TWD", "HK$" to "HKD", "S$" to "SGD", "RMB" to "CNY")

    fun parse(lines: List<String>): ParsedStay {
        val clean = lines.map { it.trim() }.filter { it.isNotEmpty() }

        // 이름: 호텔 단어가 들어간 줄 중 첫 번째, 없으면 상단 5줄 중 가장 긴 한글/영문 줄
        val name = clean.firstOrNull { l -> HOTEL_WORDS.any { l.contains(it) } && !NOISE.containsMatchIn(l.replace(Regex("체크|check", RegexOption.IGNORE_CASE), "")) }
            ?.replace(Regex("^\\d+\\.\\s*"), "")
            ?: clean.drop(1).take(5).filter { it.length in 4..40 && !it.any { c -> c.isDigit() } }.maxByOrNull { it.length }

        // 날짜: 체크인/체크아웃 단어가 있는 줄 우선, 없으면 순서대로 두 개
        fun dateIn(l: String): String? = DATE.find(l)?.let { m ->
            val mo = m.groupValues[2].toIntOrNull() ?: return null
            val d = m.groupValues[3].toIntOrNull() ?: return null
            if (mo !in 1..12 || d !in 1..31) return null
            val t = TIME.find(l.substring(m.range.last + 1))?.let { " ${it.groupValues[1].padStart(2, '0')}:${it.groupValues[2]}" } ?: ""
            "$mo.$d$t"
        }
        val inLine = clean.firstOrNull { Regex("체크\\s*인|check.?in", RegexOption.IGNORE_CASE).containsMatchIn(it) }
        val outLine = clean.firstOrNull { Regex("체크\\s*아웃|check.?out", RegexOption.IGNORE_CASE).containsMatchIn(it) }
        var checkIn = inLine?.let { dateIn(it) }
        var checkOut = outLine?.let { dateIn(it) }
        if (checkIn == null || checkOut == null) {
            val all = clean.mapNotNull { dateIn(it) }.distinct()
            if (checkIn == null) checkIn = all.getOrNull(0)
            if (checkOut == null) checkOut = all.firstOrNull { it != checkIn }
        }
        // 라벨 줄에 시간이 다음 줄로 넘어간 경우 보정
        fun attachTime(base: String?, line: String?): String? {
            if (base == null || line == null || TIME.containsMatchIn(base)) return base
            val idx = clean.indexOf(line)
            val next = clean.getOrNull(idx + 1) ?: return base
            return TIME.find(next)?.let { "$base ${it.groupValues[1].padStart(2, '0')}:${it.groupValues[2]}" } ?: base
        }
        checkIn = attachTime(checkIn, inLine)
        checkOut = attachTime(checkOut, outLine)

        // 가격: 원화 최댓값 (총액이 제일 큼), 없으면 외화 최댓값
        val krws = clean.flatMap { l -> KRW.findAll(l).map { m -> (m.groupValues[1].ifEmpty { m.groupValues[2] }).replace(",", "").toLongOrNull() }.toList() }.filterNotNull()
        val priceKrw = krws.maxOrNull()
        val foreign = clean.flatMap { l ->
            FOREIGN.findAll(l).mapNotNull { m ->
                val code = SYMBOL_TO_CODE[m.groupValues[1]] ?: m.groupValues[1]
                m.groupValues[2].replace(",", "").toDoubleOrNull()?.let { code to it }
            }.toList()
        }
        val priceForeign = if (priceKrw == null) foreign.maxByOrNull { it.second } else null

        // 예약번호
        val bookingNo = clean.firstNotNullOfOrNull { BOOKING_NO.find(it)?.groupValues?.get(1) }
            ?: clean.firstOrNull { Regex("예약\\s*번호|Booking|Confirmation", RegexOption.IGNORE_CASE).containsMatchIn(it) }?.let { l ->
                val idx = clean.indexOf(l)
                BARE_NO.find(l)?.groupValues?.get(1) ?: clean.getOrNull(idx + 1)?.let { BARE_NO.find(it)?.groupValues?.get(1) }
            }

        // 주소
        val address = clean.firstOrNull { l -> ADDR_HINT.containsMatchIn(l) && l.length > 8 && l != name }
            ?.replace(Regex("^(주소|Address)\\s*[:：]?\\s*", RegexOption.IGNORE_CASE), "")

        return ParsedStay(name = name, checkIn = checkIn, checkOut = checkOut, address = address, bookingNo = bookingNo, priceKrw = priceKrw, priceForeign = priceForeign, rawLines = clean)
    }
}
