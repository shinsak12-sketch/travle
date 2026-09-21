package com.shinsak.travle.data

import java.util.UUID

/** 비용 항목. 순서가 곧 차트 색 순서. */
enum class Category(val label: String, val short: String) {
    FLIGHT("항공·교통", "항공"),
    STAY("숙박", "숙박"),
    FOOD("식비", "식비"),
    LOCAL("현지교통", "교통"),
    ACTIVITY("액티비티·입장료", "액티비티"),
    ETC("기타 (비자·보험)", "기타"),
}

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

data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val month: String = "",
    val nights: Int = 3,
    val days: Int = 4,
    val people: Int = 2,
    /** 금액을 입력한 통화 코드. KRW면 fxRate = 1 */
    val currency: String = "KRW",
    /** 외화 1단위 = fxRate 원 */
    val fxRate: Double = 1.0,
    /** 입력 통화 기준 금액 */
    val costs: Map<Category, Double> = emptyMap(),
    val flightMinutes: Int = 0,
    val airline: String = "",
    val outbound: FlightLeg? = null,
    val inbound: FlightLeg? = null,
    val rating: Int = 3,
    val memo: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
) {
    fun costKrw(c: Category): Long = Math.round((costs[c] ?: 0.0) * fxRate)

    val totalKrw: Long
        get() = Category.entries.sumOf { costKrw(it) }

    val perPersonKrw: Long
        get() = if (people > 0) totalKrw / people else totalKrw

    val flightLabel: String
        get() {
            if (flightMinutes <= 0) return ""
            val h = flightMinutes / 60
            val m = flightMinutes % 60
            return buildString {
                if (h > 0) append("${h}h")
                if (m > 0) {
                    if (h > 0) append(' ')
                    append("${m}m")
                }
            }
        }

    val periodLabel: String
        get() = "${nights}박 ${days}일"

    /** 카드용 한 줄: "대한항공 · 18:35 → 19:00" */
    val flightSummary: String
        get() = listOf(airline.takeIf { it.isNotBlank() }, outbound?.timeLabel?.takeIf { it.isNotBlank() })
            .filterNotNull().joinToString(" · ")
}

data class Settings(
    val budgetCap: Long = 2_300_000,
    /** "system" | "light" | "dark" */
    val themeMode: String = "system",
    /** 홈 카드에서 1인당 금액을 크게 보여줄지 */
    val perPersonFirst: Boolean = false,
    /** 새 견적 만들 때 기본으로 채워주는 환율 (외화 1단위 = 원) */
    val fxRates: Map<String, Double> = DEFAULT_FX,
)

val CURRENCIES = listOf("KRW", "USD", "JPY", "VND", "EUR", "TWD", "THB", "CNY", "PHP", "MYR", "SGD", "HKD")

val CURRENCY_LABEL = mapOf(
    "KRW" to "원", "USD" to "달러", "JPY" to "엔", "VND" to "동", "EUR" to "유로",
    "TWD" to "대만달러", "THB" to "바트", "CNY" to "위안", "PHP" to "페소",
    "MYR" to "링깃", "SGD" to "싱가포르달러", "HKD" to "홍콩달러",
)

val DEFAULT_FX = mapOf(
    "USD" to 1384.0, "JPY" to 9.12, "VND" to 0.0545, "EUR" to 1520.0,
    "TWD" to 43.0, "THB" to 40.0, "CNY" to 192.0, "PHP" to 24.0,
    "MYR" to 310.0, "SGD" to 1050.0, "HKD" to 178.0,
)
