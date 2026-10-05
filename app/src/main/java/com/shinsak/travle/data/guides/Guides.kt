package com.shinsak.travle.data.guides

import com.shinsak.travle.data.PlaceCategory

/**
 * 도시 가이드 한 항목. 현지명(local)은 지도앱 검색어로 씀.
 * cost는 1인 현지 통화 기준 대략값, 0이면 무료 또는 미정.
 */
data class GuidePlace(
    val name: String,
    val local: String,
    val category: PlaceCategory,
    val area: String,
    val hours: String = "",
    val cost: Double = 0.0,
    val costNote: String = "",
    val tip: String = "",
    val hoursNeeded: Double = 0.0,
) {
    val mapQuery: String get() = local.ifBlank { name }
}

data class CityGuide(
    /** 여행 도시명에 이 중 하나라도 포함되면 매칭 */
    val cityKeys: List<String>,
    val title: String,
    val currency: String,
    val note: String,
    val places: List<GuidePlace>,
)

object Guides {
    private val all: List<CityGuide> = listOf(ShenyangGuide.guide)

    fun forCity(city: String): CityGuide? = all.firstOrNull { g -> g.cityKeys.any { city.contains(it, ignoreCase = true) } }
}
