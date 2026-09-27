package com.shinsak.travle.data

/** 예약의 항공편을 일정 day1 / 마지막날에 자동 줄로 반영. 기존 auto 줄은 갈아끼움. */
fun Trip.withAutoFlightItems(): Trip {
    val manual = items.filter { !it.auto }
    val auto = mutableListOf<PlanItem>()
    val f = flight
    val air = f.airline.ifBlank { "항공편" }
    f.outbound?.let { leg ->
        auto += PlanItem(
            id = "auto-out", dayIndex = 1, time = leg.dep, name = "출발 → $city · $air",
            category = PlaceCategory.TRANSPORT, auto = true,
            note = listOf(leg.arr.takeIf { it.isNotBlank() }?.let { "$it 도착" }, leg.durationLabel.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · "),
        )
    }
    f.inbound?.let { leg ->
        auto += PlanItem(
            id = "auto-in", dayIndex = days, time = leg.dep, name = "$city → 귀국 · $air",
            category = PlaceCategory.TRANSPORT, auto = true,
            note = listOf(leg.arr.takeIf { it.isNotBlank() }?.let { "$it 도착" }, leg.durationLabel.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · "),
        )
    }
    return copy(items = manual + auto)
}

/** day 안에서 시간순 정렬. 시간 없는 건 뒤로. */
fun List<PlanItem>.forDay(day: Int): List<PlanItem> =
    filter { it.dayIndex == day }.sortedWith(compareBy({ it.time.isBlank() }, { it.time }))

/** 항공권 요금을 '여행준비' 지출로 반영 (같은 id로 덮어씀) */
fun Trip.withFlightExpense(): Trip {
    val others = expenses.filter { it.id != "auto-flight" }
    if (flight.totalPrice <= 0) return copy(expenses = others)
    val e = Expense(
        id = "auto-flight", dayIndex = 0, amount = flight.totalPrice.toDouble(), currency = "KRW", fxRate = 1.0,
        title = "항공권 왕복" + (flight.seller.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
        category = ExpCategory.FLIGHT, method = PayMethod.CARD,
        paidBy = companions.first(), splitWith = companions,
        createdAt = expenses.firstOrNull { it.id == "auto-flight" }?.createdAt ?: System.currentTimeMillis(),
    )
    return copy(expenses = listOf(e) + others)
}

/** 숙소 요금을 '여행준비' 지출로 반영 */
fun Trip.withStayExpense(): Trip {
    val others = expenses.filter { it.id != "auto-stay" }
    if (stay.price <= 0) return copy(expenses = others)
    val e = Expense(
        id = "auto-stay", dayIndex = 0, amount = stay.price.toDouble(), currency = "KRW", fxRate = 1.0,
        title = "숙소" + (stay.name.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
        category = ExpCategory.STAY, method = PayMethod.CARD,
        paidBy = companions.first(), splitWith = companions,
        createdAt = expenses.firstOrNull { it.id == "auto-stay" }?.createdAt ?: System.currentTimeMillis(),
    )
    return copy(expenses = listOf(e) + others)
}

/** 두 사람 기준 "A가 B한테 N원". 셋 이상이면 받을 사람/줄 사람 요약. */
fun Trip.settlementLabel(): String {
    val s = settlement()
    if (s.size < 2 || s.values.all { it == 0L }) return ""
    val receivers = s.filter { it.value > 0 }.toList().sortedByDescending { it.second }
    val payers = s.filter { it.value < 0 }.toList().sortedBy { it.second }
    if (receivers.isEmpty() || payers.isEmpty()) return ""
    if (s.size == 2) {
        val (r, amt) = receivers.first()
        val (p, _) = payers.first()
        return "${p}가 ${r}한테 ${fmt(amt)}원"
    }
    return payers.joinToString(" · ") { (p, v) -> "$p → ${fmt(-v)}원" } + " (받는 사람: " + receivers.joinToString(", ") { it.first } + ")"
}

private fun fmt(v: Long): String = v.toString().reversed().chunked(3).joinToString(",").reversed()
