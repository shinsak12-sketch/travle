package com.shinsak.travle.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/** 지출 카테고리. 순서가 차트 색 순서. */
enum class ExpCategory(val label: String) {
    STAY("숙소"), FLIGHT("항공"), TRANSPORT("교통"), FOOD("식비"), SHOPPING("쇼핑"), SIGHT("관광"), GOLF("골프"), ETC("기타"),
}

/** 일정 항목 카테고리 */
enum class PlaceCategory(val label: String) {
    SIGHT("관광"), FOOD("식당"), CAFE("카페"), SHOPPING("쇼핑"), GOLF("골프"), STAY("숙소"), TRANSPORT("교통"), ETC("기타"),
}

enum class PayMethod(val label: String) { CARD("카드"), CASH("현금"), ALIPAY("알리페이"), OTHER("기타") }

/** 항공 구간 하나. 시간은 "18:35" 같은 24시간 문자열, 도착에 "+1" 붙을 수 있음. */
data class FlightLeg(
    val dep: String = "",
    val arr: String = "",
    val minutes: Int = 0,
) {
    val isEmpty: Boolean get() = dep.isBlank() && arr.isBlank() && minutes <= 0

    val timeLabel: String
        get() = listOf(dep, arr).filter { it.isNotBlank() }.joinToString(" → ")

    val durationLabel: String
        get() = if (minutes <= 0) "" else {
            val h = minutes / 60
            val m = minutes % 60
            when {
                h > 0 && m > 0 -> "${h}시간 ${m}분"
                h > 0 -> "${h}시간"
                else -> "${m}분"
            }
        }

    val label: String
        get() = listOf(timeLabel, durationLabel.takeIf { it.isNotBlank() }?.let { "($it)" }).filterNotNull().joinToString(" ")
}

/** 일정 한 줄. 장소 또는 메모. dayIndex는 1부터. */
data class PlanItem(
    val id: String = UUID.randomUUID().toString(),
    val dayIndex: Int = 1,
    val isMemo: Boolean = false,
    val time: String = "",
    val name: String = "",
    val category: PlaceCategory = PlaceCategory.SIGHT,
    val address: String = "",
    val mapLink: String = "",
    val cost: Double = 0.0,
    val costCurrency: String = "KRW",
    val hours: Double = 0.0,
    val note: String = "",
    /** 다음 장소까지 이동 메모 (예: 택시 15분). 타임라인 연결선에 표시 */
    val transit: String = "",
    /** 예약에서 자동 생성된 항공편 줄 (편집 불가, 예약 바꾸면 갱신) */
    val auto: Boolean = false,
)

/** 지출. dayIndex 0 = 여행준비. */
data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val dayIndex: Int = 0,
    val amount: Double = 0.0,
    val currency: String = "KRW",
    /** 입력 시점 환율 스냅샷. 외화 1단위 = fxRate 원 */
    val fxRate: Double = 1.0,
    val title: String = "",
    val category: ExpCategory = ExpCategory.ETC,
    val method: PayMethod = PayMethod.CARD,
    val paidBy: String = "나",
    /** 나눌 사람. 비어 있으면 1/N 안 함 (낸 사람 혼자 부담) */
    val splitWith: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    val krw: Long get() = Math.round(amount * fxRate)
}

data class Flight(
    val airline: String = "",
    val outbound: FlightLeg? = null,
    val inbound: FlightLeg? = null,
    val seller: String = "",
    val pricePerPerson: Long = 0,
    val totalPrice: Long = 0,
    val note: String = "",
) {
    val isEmpty: Boolean get() = airline.isBlank() && outbound == null && inbound == null && totalPrice <= 0
}

data class Stay(
    val name: String = "",
    val checkIn: String = "",
    val checkOut: String = "",
    val address: String = "",
    val mapLink: String = "",
    val bookingNo: String = "",
    val price: Long = 0,
    val note: String = "",
) {
    val isEmpty: Boolean get() = name.isBlank() && bookingNo.isBlank() && price <= 0
}

data class CheckItem(
    val id: String = UUID.randomUUID().toString(),
    val group: String = "출발 전",
    val text: String = "",
    val done: Boolean = false,
)

data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val city: String = "",
    /** ISO yyyy-MM-dd */
    val startDate: String = LocalDate.now().toString(),
    val endDate: String = LocalDate.now().plusDays(2).toString(),
    /** 첫 번째가 "나" */
    val companions: List<String> = listOf("나"),
    /** 현지 통화 코드 */
    val currency: String = "KRW",
    val budgetCap: Long = 1_500_000,
    val items: List<PlanItem> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val flight: Flight = Flight(),
    val stay: Stay = Stay(),
    val checks: List<CheckItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
) {
    val start: LocalDate get() = runCatching { LocalDate.parse(startDate) }.getOrDefault(LocalDate.now())
    val end: LocalDate get() = runCatching { LocalDate.parse(endDate) }.getOrDefault(start)
    val nights: Int get() = ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(0)
    val days: Int get() = nights + 1
    val people: Int get() = companions.size.coerceAtLeast(1)

    fun dateOf(dayIndex: Int): LocalDate = start.plusDays((dayIndex - 1).toLong())

    /** 오늘 기준 D-day. 음수면 지난 여행 */
    val dDay: Long get() = ChronoUnit.DAYS.between(LocalDate.now(), start)
    val isPast: Boolean get() = end.isBefore(LocalDate.now())

    val totalKrw: Long get() = expenses.sumOf { it.krw }
    val perPersonKrw: Long get() = totalKrw / people

    val checksDone: Int get() = checks.count { it.done }

    /** "2026.10.9 금 – 10.11 일" */
    val periodLabel: String
        get() {
            val s = start
            val e = end
            val sd = "${s.year}.${s.monthValue}.${s.dayOfMonth} ${dow(s)}"
            val ed = if (e.year == s.year) "${e.monthValue}.${e.dayOfMonth} ${dow(e)}" else "${e.year}.${e.monthValue}.${e.dayOfMonth} ${dow(e)}"
            return if (nights == 0) sd else "$sd – $ed"
        }

    companion object {
        fun dow(d: LocalDate): String = listOf("월", "화", "수", "목", "금", "토", "일")[d.dayOfWeek.value - 1]
    }
}

/** 1/N 정산 결과: 이름 → 받을 돈(+) / 줄 돈(−), 원 */
fun Trip.settlement(): Map<String, Long> {
    val balance = companions.associateWith { 0L }.toMutableMap()
    expenses.forEach { e ->
        val payer = e.paidBy.ifBlank { companions.first() }
        val sharers = e.splitWith.filter { it in balance }.ifEmpty { listOf(payer) }
        val each = e.krw / sharers.size
        balance[payer] = (balance[payer] ?: 0L) + e.krw
        sharers.forEach { balance[it] = (balance[it] ?: 0L) - each }
    }
    return balance
}

data class Settings(
    /** "system" | "light" | "dark" */
    val themeMode: String = "system",
    /** 외화 1단위 = 원 */
    val fxRates: Map<String, Double> = DEFAULT_FX,
)

val CURRENCIES = listOf("KRW", "CNY", "JPY", "USD", "VND", "TWD", "THB", "EUR", "PHP", "MYR", "SGD", "HKD")

val CURRENCY_SYMBOL = mapOf(
    "KRW" to "₩", "CNY" to "¥", "JPY" to "¥", "USD" to "$", "VND" to "₫", "TWD" to "NT$",
    "THB" to "฿", "EUR" to "€", "PHP" to "₱", "MYR" to "RM", "SGD" to "S$", "HKD" to "HK$",
)

val CURRENCY_LABEL = mapOf(
    "KRW" to "원", "CNY" to "위안", "JPY" to "엔", "USD" to "달러", "VND" to "동", "TWD" to "대만달러",
    "THB" to "바트", "EUR" to "유로", "PHP" to "페소", "MYR" to "링깃", "SGD" to "싱가포르달러", "HKD" to "홍콩달러",
)

val DEFAULT_FX = mapOf(
    "CNY" to 192.0, "JPY" to 9.12, "USD" to 1384.0, "VND" to 0.0545, "TWD" to 43.0, "THB" to 40.0,
    "EUR" to 1520.0, "PHP" to 24.0, "MYR" to 310.0, "SGD" to 1050.0, "HKD" to 178.0,
)

/** 도시 프리셋: 이름, 코드, 현지 통화, 한 줄 소개 */
data class CityPreset(val name: String, val code: String, val currency: String, val region: String, val hint: String)

val CITY_PRESETS = listOf(
    CityPreset("선양 (심양)", "SHE", "CNY", "중국", "고궁 · 중가 · 시타 · 북릉"),
    CityPreset("다롄", "DLC", "CNY", "중국", "싱하이광장 · 러시아거리"),
    CityPreset("하얼빈", "HRB", "CNY", "중국", "중앙대가 · 성소피아"),
    CityPreset("베이징", "PEK", "CNY", "중국", "자금성 · 만리장성"),
    CityPreset("상하이", "PVG", "CNY", "중국", "와이탄 · 디즈니"),
    CityPreset("칭다오", "TAO", "CNY", "중국", "잔교 · 맥주박물관"),
    CityPreset("장자제", "DYG", "CNY", "중국", "천문산 · 유리다리"),
    CityPreset("도쿄", "NRT", "JPY", "일본", "시부야 · 아사쿠사"),
    CityPreset("오사카", "KIX", "JPY", "일본", "도톤보리 · 교토 · 나라"),
    CityPreset("후쿠오카", "FUK", "JPY", "일본", "유후인 · 벳푸"),
    CityPreset("삿포로", "CTS", "JPY", "일본", "오타루 · 비에이"),
    CityPreset("오키나와", "OKA", "JPY", "일본", "츄라우미 · 미국촌"),
    CityPreset("타이베이", "TPE", "TWD", "대만·홍콩", "지우펀 · 스린야시장"),
    CityPreset("홍콩", "HKG", "HKD", "대만·홍콩", "빅토리아피크 · 침사추이"),
    CityPreset("마카오", "MFM", "HKD", "대만·홍콩", "세나도광장 · 카지노"),
    CityPreset("방콕", "BKK", "THB", "동남아", "왓포 · 카오산"),
    CityPreset("치앙마이", "CNX", "THB", "동남아", "올드시티 · 도이수텝"),
    CityPreset("다낭", "DAD", "VND", "동남아", "바나힐 · 호이안"),
    CityPreset("나트랑", "CXR", "VND", "동남아", "빈펄 · 머드온천"),
    CityPreset("하노이", "HAN", "VND", "동남아", "호안끼엠 · 하롱베이"),
    CityPreset("세부", "CEB", "PHP", "동남아", "막탄 · 오슬롭"),
    CityPreset("싱가포르", "SIN", "SGD", "동남아", "마리나베이 · 센토사"),
    CityPreset("쿠알라룸푸르", "KUL", "MYR", "동남아", "페트로나스 · 바투동굴"),
    CityPreset("발리", "DPS", "USD", "동남아", "우붓 · 스미냑"),
)

val CITY_REGIONS = listOf("전체", "중국", "일본", "대만·홍콩", "동남아")

/** 중국행 기본 체크리스트 */
fun defaultChecks(city: String, golf: Boolean = true): List<CheckItem> {
    val china = CITY_PRESETS.firstOrNull { it.name == city }?.region == "중국" || city.contains("중국") || city.contains("선양") || city.contains("심양")
    val docs = mutableListOf(
        "여권 유효기간 6개월 이상",
        "여권 사본·증명사진 폰에 저장",
        "항공권 예약 · e티켓 스크린샷",
        "숙소 예약 · 바우처 저장 (현지어 주소)",
        "여행자보험 가입 · 긴급콜 번호 저장",
        "공항 가는 교통 · 주차 예약",
    )
    if (china) docs.add(2, "무비자 입국 조건 확인 (기간·왕복권)")
    if (golf) { docs.add("골프 티타임 예약 확인 (카트·캐디 포함 여부)"); docs.add("항공사에 골프백 위탁 사전 신청") }

    val apps = mutableListOf("eSIM 또는 로밍 개통", "번역앱 오프라인 언어팩 다운로드", "현지 통화 소액 환전", "해외 결제 되는 카드 2장 이상", "트립닷컴 앱 · 예약 내역 확인")
    if (china) {
        apps.addAll(0, listOf("알리페이 설치 · 해외카드 연동 · 실명인증", "위챗 설치 (QR 주문·위챗페이)", "디디추싱 가입 또는 알리페이 안 디디 확인", "고덕지도(高德) 또는 바이두지도 설치", "VPN 설치 · 켜지는지 테스트"))
    }

    val pack = listOf(
        "경량패딩 · 바람막이", "긴팔 · 긴바지 (일수+1)", "편한 운동화", "속옷·양말 (일수+1)", "세면도구 · 면도기",
        "상비약 (소화제·지사제·감기약·진통제·밴드)", "멀티 어댑터", "보조배터리 (기내 반입)", "충전 케이블 · 충전기",
        "휴지 · 물티슈 (식당 휴지 유료)", "손소독제 · 마스크", "선글라스 · 모자", "접이식 우산", "립밤 · 핸드크림", "작은 자물쇠 · 지퍼백",
    )

    val golfPack = listOf(
        "골프백 · 항공 커버", "클럽 개수 확인 (14개 이내)", "골프화", "장갑 2켤레", "골프공 1더즌 이상", "티 · 볼마커 · 그린보수기",
        "거리측정기 (충전)", "골프웨어 2세트 (칼라 셔츠)", "니트 조끼 · 바람막이", "레인웨어", "골프 모자", "썬크림",
        "핫팩 (아침 추위)", "캐디팁 현금 (¥100–200)", "에너지바 · 물", "라운드 후 갈아입을 옷",
    )

    val local = mutableListOf("호텔 체크인 · 호텔 명함 받기 (현지어 주소)", "유심·eSIM 작동 확인", "택시 호출 1회 테스트", "환율 앱 확인 · 가계부 환율 입력", "생수 사두기", "영사관 번호 저장")
    if (china) { local.add(2, "알리페이 결제 1회 테스트"); local.add(0, "입국카드 작성 · 지문 등록") }

    val back = listOf("체크아웃 · 보증금 환불 확인", "충전기·세면도구 두고 온 것 확인", "기념품 세관 한도 확인", "남은 현지 통화 처리", "국제선 2.5시간 전 공항 도착") +
        (if (golf) listOf("골프백 위탁 · 수하물 무게 확인") else emptyList())

    return docs.map { CheckItem(group = "출발 전", text = it) } +
        apps.map { CheckItem(group = "앱·결제", text = it) } +
        pack.map { CheckItem(group = "짐", text = it) } +
        (if (golf) golfPack.map { CheckItem(group = "골프", text = it) } else emptyList()) +
        local.map { CheckItem(group = "현지", text = it) } +
        back.map { CheckItem(group = "귀국 전", text = it) }
}
