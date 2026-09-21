package com.shinsak.travle.data

import java.util.Calendar

/** 항공권 검색 결과 스크린샷(스카이스캐너류)에서 뽑아낸 값. 못 찾은 건 null. */
data class ParsedFlight(
    val origin: String? = null,
    val destination: String? = null,
    val month: Int? = null,
    val startDay: Int? = null,
    val endDay: Int? = null,
    val nights: Int? = null,
    val days: Int? = null,
    val people: Int? = null,
    val airline: String? = null,
    /** 가는편부터. 시간은 24시간 "18:35" 로 정규화 */
    val legs: List<FlightLeg> = emptyList(),
    val outboundMinutes: Int? = null,
    val pricePerPerson: Long? = null,
    val totalPrice: Long? = null,
    val seller: String? = null,
    val rawLines: List<String> = emptyList(),
) {
    val isEmpty: Boolean
        get() = destination == null && pricePerPerson == null && totalPrice == null && airline == null

    /** "10월 9–11일" */
    val dateLabel: String?
        get() {
            val m = month ?: return null
            val s = startDay ?: return "${m}월"
            val e = endDay
            return if (e != null && e != s) "${m}월 ${s}–${e}일" else "${m}월 ${s}일"
        }
}

object ScreenshotParser {
    private val DASH = "[–—\\-~]"

    private val ROUTE = Regex("^(.{1,20}?)\\s*$DASH\\s*(.{1,20}?)\\s*행\\s*$")
    /** "9–11 10월", "9-11 10월" */
    private val DATE_RANGE_A = Regex("(\\d{1,2})\\s*$DASH\\s*(\\d{1,2})\\s*(\\d{1,2})\\s*월")
    /** "10월 9일 – 10월 11일", "10월 9 – 11일", "9월 30일 – 10월 3일" */
    private val DATE_RANGE_B = Regex("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일?\\s*$DASH\\s*(?:(\\d{1,2})\\s*월\\s*)?(\\d{1,2})\\s*일?")
    /** "10월 9일 (편도)" 같이 하루만 있을 때 */
    private val DATE_SINGLE = Regex("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일")
    private val PEOPLE = Regex("(?:여행객|승객|성인|인원)\\s*(\\d{1,2})\\s*명?")
    /** 오전/오후 표기. OCR이 "오 후", "오휴"로 읽는 경우 포함 */
    private const val AMPM = "(?:오\\s*[전후휴호]|[AaPp][Mm])"
    /** 시간 토큰 하나. 시:분 구분자는 콜론이 아닌 아무 기호여도 되고, 숫자를 글자로 읽은 것도 허용 (뒤에서 보정) */
    private val TIME_TOKEN = Regex("($AMPM)?\\s*(?<![0-9])([0-9OoQDlIgq]{1,2})\\s*[^0-9A-Za-z가-힣\\s]{0,2}\\s*([0-9OoQDlISBgq]{2})(?![0-9])(?:\\s*\\+\\s*(\\d))?")
    private val TIME_RANGE = Regex("((?:오전|오후)\\s*\\d{1,2}[:.;：∶]\\d{2})\\s*$DASH\\s*((?:오전|오후)\\s*\\d{1,2}[:.;：∶]\\d{2})")
    private val TIME_RANGE_24 = Regex("(\\d{1,2}[:.;：∶]\\d{2})\\s*$DASH\\s*(\\d{1,2}[:.;：∶]\\d{2})")
    private val DURATION = Regex("(?:(\\d{1,2})\\s*시간)?\\s*(?:(\\d{1,2})\\s*분)?")
    private val DURATION_STRICT = Regex("(\\d{1,2})\\s*시간(?:\\s*(\\d{1,2})\\s*분)?|(\\d{1,3})\\s*분")
    private val PRICE = Regex("[₩W￦]\\s*(\\d{1,3}(?:,\\d{3})+|\\d{5,})")
    private val TOTAL_PRICE = Regex("(?:여행객|승객|성인|총)\\s*\\d*\\s*명?\\s*[₩W￦]?\\s*(\\d{1,3}(?:,\\d{3})+|\\d{5,})")
    private val SELLER = Regex("([A-Za-z][A-Za-z0-9]+\\.com)|(트립닷컴|마이리얼트립|인터파크|익스피디아|노랑풍선|하나투어|모두투어|와이페이모어|카약|아고다|부킹닷컴|스카이스캐너|네이버항공권|웹투어|투어비스|온라인투어|땡처리닷컴|여행박사|참좋은여행|고투게이트|키위닷컴|Kiwi)")

    private val AIRLINES = listOf(
        "대한항공", "아시아나항공", "아시아나", "제주항공", "진에어", "티웨이항공", "티웨이", "에어부산", "에어서울",
        "이스타항공", "이스타", "에어프레미아", "에어로케이", "플라이강원", "파라타항공",
        "베트남항공", "비엣젯", "뱀부항공", "필리핀항공", "세부퍼시픽", "에어아시아", "타이항공", "타이에어아시아",
        "싱가포르항공", "스쿠트", "말레이시아항공", "가루다", "라이온에어", "바틱에어",
        "중국동방항공", "중국남방항공", "중국국제항공", "에어차이나", "춘추항공", "샤먼항공", "산동항공", "하이난항공", "길상항공",
        "일본항공", "JAL", "전일본공수", "ANA", "피치", "젯스타", "스카이마크", "스타플라이어",
        "캐세이퍼시픽", "캐세이", "홍콩익스프레스", "에바항공", "중화항공", "스타럭스", "타이거에어",
        "델타항공", "델타", "유나이티드", "아메리칸항공", "에어캐나다", "하와이안항공",
        "에미레이트", "카타르항공", "에티하드", "터키항공", "루프트한자", "에어프랑스", "KLM", "영국항공", "핀에어",
        "콴타스", "에어뉴질랜드", "몽골항공", "우즈베키스탄항공", "에어아스타나",
    )

    private val KNOWN_CITIES = listOf(
        "다롄", "다낭", "오사카", "도쿄", "후쿠오카", "삿포로", "오키나와", "나고야", "타이베이", "가오슝", "홍콩", "마카오",
        "방콕", "치앙마이", "푸껫", "하노이", "호치민", "나트랑", "달랏", "푸꾸옥", "세부", "마닐라", "보라카이", "싱가포르",
        "쿠알라룸푸르", "코타키나발루", "발리", "자카르타", "괌", "사이판", "상하이", "베이징", "칭다오", "청도", "장가계",
        "파리", "런던", "로마", "바르셀로나", "프라하", "빈", "취리히", "암스테르담", "뉴욕", "LA", "로스앤젤레스", "하와이",
        "시드니", "멜버른", "두바이", "이스탄불", "울란바토르", "블라디보스토크", "타슈켄트", "알마티",
    )

    fun parse(lines: List<String>): ParsedFlight {
        val clean = lines.map { it.trim() }.filter { it.isNotEmpty() }
        var origin: String? = null
        var destination: String? = null

        // 1) 노선: "서울 – 다롄행" — 맨 위 매치를 씀 (구간 라인 "인천 국제 – 다롄행"보다 위에 있음)
        for (l in clean) {
            val m = ROUTE.find(l) ?: continue
            val o = m.groupValues[1].trim()
            val d = m.groupValues[2].trim()
            if (o.any { it.isDigit() } || d.any { it.isDigit() }) continue
            origin = o
            destination = d
            break
        }
        if (destination == null) {
            // 노선 라인이 없으면 알려진 도시명이 있는 줄에서
            for (l in clean) {
                val hit = KNOWN_CITIES.firstOrNull { l.contains(it) } ?: continue
                destination = hit
                break
            }
        }

        // 2) 날짜
        var month: Int? = null
        var start: Int? = null
        var end: Int? = null
        var nights: Int? = null
        run {
            for (l in clean) {
                DATE_RANGE_A.find(l)?.let { m ->
                    start = m.groupValues[1].toInt()
                    end = m.groupValues[2].toInt()
                    month = m.groupValues[3].toInt()
                    nights = (end!! - start!!).takeIf { it >= 0 }
                    return@run
                }
            }
            for (l in clean) {
                DATE_RANGE_B.find(l)?.let { m ->
                    month = m.groupValues[1].toInt()
                    start = m.groupValues[2].toInt()
                    val endMonth = m.groupValues[3].toIntOrNull() ?: month!!
                    end = m.groupValues[4].toInt()
                    nights = daysBetween(month!!, start!!, endMonth, end!!)
                    return@run
                }
            }
            for (l in clean) {
                DATE_SINGLE.find(l)?.let { m ->
                    month = m.groupValues[1].toInt()
                    start = m.groupValues[2].toInt()
                    return@run
                }
            }
        }

        // 3) 인원
        val people = clean.firstNotNullOfOrNull { PEOPLE.find(it)?.groupValues?.get(1)?.toIntOrNull() }

        // 4) 항공사
        val airline = clean.firstNotNullOfOrNull { l ->
            AIRLINES.firstOrNull { l.contains(it) }
                ?: Regex("([가-힣A-Za-z]{2,12}항공)").find(l)?.groupValues?.get(1)
        }

        // 5) 구간 시간 · 소요시간
        // 행(row) 단위로 본다: 같은 행의 시간 둘 = 출발·도착. 행에 하나만 있으면 앞에 대시가 있는지로 출발/도착 판단.
        // 다른 행끼리 억지로 짝짓지 않음. 상태바 시계는 노선/날짜 행보다 위라 제외.
        val headerIdx = clean.indexOfFirst { ROUTE.containsMatchIn(it) || DATE_RANGE_A.containsMatchIn(it) || DATE_RANGE_B.containsMatchIn(it) || PEOPLE.containsMatchIn(it) }
        val body = if (headerIdx >= 0) clean.drop(headerIdx) else clean
        val times = body.indices.mapNotNull { i ->
            val row = body[i]
            if (PRICE.containsMatchIn(row) || row.contains("여행객") || row.contains("월")) return@mapNotNull null
            val near = listOfNotNull(body.getOrNull(i - 1), body.getOrNull(i + 1)).joinToString(" ")
            rowTimes(row, near)
        }
        val durations = clean.mapNotNull { l ->
            // 시간 범위가 같이 있는 줄은 소요시간이 아님
            if (rowTimes(l) != null) return@mapNotNull null
            val m = DURATION_STRICT.find(l) ?: return@mapNotNull null
            val h = m.groupValues[1].toIntOrNull()
            val mm = m.groupValues[2].toIntOrNull()
            val onlyMin = m.groupValues[3].toIntOrNull()
            when {
                h != null -> h * 60 + (mm ?: 0)
                onlyMin != null && onlyMin >= 20 -> onlyMin
                else -> null
            }
        }
        val legs = times.mapIndexed { i, t ->
            FlightLeg(dep = t.first, arr = t.second, minutes = durations.getOrNull(i) ?: 0)
        }
        val outbound = durations.firstOrNull()

        // 6) 가격
        val prices = clean.flatMap { l -> PRICE.findAll(l).map { it.groupValues[1].replace(",", "").toLong() }.toList() }
        val totalFromLine = clean.firstNotNullOfOrNull { TOTAL_PRICE.find(it)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() }
        val perPerson = prices.firstOrNull { p -> totalFromLine == null || p != totalFromLine } ?: prices.firstOrNull()
        val total = totalFromLine ?: perPerson?.let { p -> if (people != null && people > 1) p * people else p }

        // 7) 판매처
        val seller = clean.firstNotNullOfOrNull { l ->
            SELLER.find(l)?.let { m -> m.groupValues[1].ifEmpty { m.groupValues[2] } }
        }

        return ParsedFlight(
            origin = origin,
            destination = destination,
            month = month,
            startDay = start,
            endDay = end,
            nights = nights,
            days = nights?.let { it + 1 },
            people = people,
            airline = airline,
            legs = legs,
            outboundMinutes = outbound,
            pricePerPerson = perPerson,
            totalPrice = total,
            seller = seller,
            rawLines = clean,
        )
    }

    private fun daysBetween(m1: Int, d1: Int, m2: Int, d2: Int): Int? {
        if (m1 !in 1..12 || m2 !in 1..12) return null
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val a = Calendar.getInstance().apply { clear(); set(year, m1 - 1, d1) }
        val y2 = if (m2 < m1) year + 1 else year
        val b = Calendar.getInstance().apply { clear(); set(y2, m2 - 1, d2) }
        val diff = ((b.timeInMillis - a.timeInMillis) / 86_400_000L).toInt()
        return diff.takeIf { it in 0..120 }
    }

    private fun fixDigits(t: String): String = t.map {
        when (it) {
            'O', 'o', 'Q', 'D' -> '0'
            'l', 'I', '|' -> '1'
            'S' -> '5'
            'B' -> '8'
            'g', 'q' -> '9'
            else -> it
        }
    }.joinToString("")

    private fun isPm(s: String) = Regex("오\\s*[후휴호]|[Pp][Mm]").containsMatchIn(s)
    private fun isAm(s: String) = Regex("오\\s*[전잔]|[Aa][Mm]").containsMatchIn(s)

    /** 한 행에서 출발/도착 뽑기. 없으면 null. */
    private fun rowTimes(row: String, near: String = ""): Pair<String, String>? {
        val ms = TIME_TOKEN.findAll(row).toList()
        if (ms.isEmpty()) return null
        // 행 안에 오전/오후가 없으면 바로 위·아래 행에서 찾음 (OCR이 "오후"를 딴 행으로 보낼 때)
        val rowPm = isPm(row) || (!isAm(row) && isPm(near))
        val rowAm = isAm(row) || (!isPm(row) && isAm(near))
        val toks = ms.mapNotNull { m ->
            var h = fixDigits(m.groupValues[2]).toIntOrNull() ?: return@mapNotNull null
            val min = fixDigits(m.groupValues[3])
            if ((min.toIntOrNull() ?: 99) > 59) return@mapNotNull null
            val own = m.groupValues[1]
            val pm = if (own.isNotEmpty()) isPm(own) else rowPm
            val am = if (own.isNotEmpty()) isAm(own) else rowAm
            if ((pm || am) && h > 12) return@mapNotNull null
            if (!pm && !am && h > 23) return@mapNotNull null
            if (pm && h < 12) h += 12
            if (am && h == 12) h = 0
            val plus = m.groupValues[4]
            Triple(m.range.first, "%02d:%s".format(h, min), plus)
        }
        if (toks.isEmpty()) return null
        if (toks.size >= 2) {
            val a = toks[0]
            val b = toks[1]
            return a.second to (b.second + if (b.third.isNotEmpty()) "+${b.third}" else "")
        }
        // 하나만: 바로 앞 몇 글자 안에 대시가 있으면 도착, 아니면 출발
        val t = toks[0]
        val before = row.substring(0, t.first).takeLast(8)
        val isArr = Regex(DASH).containsMatchIn(before)
        return if (isArr) "" to t.second else t.second to ""
    }

    /** "오후 6:35" → "18:35", "오전 12:10" → "00:10", "오후 12:20" → "12:20" */
    fun to24(t: String): String {
        val m = Regex("(오전|오후)\\s*(\\d{1,2}):(\\d{2})").find(t) ?: return t.trim()
        var h = m.groupValues[2].toInt()
        val min = m.groupValues[3]
        if (m.groupValues[1] == "오후" && h < 12) h += 12
        if (m.groupValues[1] == "오전" && h == 12) h = 0
        return "%02d:%s".format(h, min)
    }

    fun minutesLabel(min: Int): String {
        val h = min / 60
        val m = min % 60
        return when {
            h > 0 && m > 0 -> "${h}시간 ${m}분"
            h > 0 -> "${h}시간"
            else -> "${m}분"
        }
    }
}
