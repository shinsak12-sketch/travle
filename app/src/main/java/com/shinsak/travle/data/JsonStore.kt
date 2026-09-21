package com.shinsak.travle.data

import org.json.JSONArray
import org.json.JSONObject

/** 저장 파일 / 백업 파일 공용 JSON 변환. 형식은 둘이 완전히 같음. */
object JsonStore {
    private const val VERSION = 1

    fun exportAll(trips: List<Trip>, settings: Settings): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("settings", settingsToJson(settings))
        val arr = JSONArray()
        trips.forEach { arr.put(tripToJson(it)) }
        root.put("trips", arr)
        return root.toString(2)
    }

    /** 실패하면 예외. 호출부에서 runCatching. */
    fun parseAll(text: String): Pair<List<Trip>, Settings?> {
        val root = JSONObject(text)
        val trips = mutableListOf<Trip>()
        val arr = root.optJSONArray("trips") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            runCatching { tripFromJson(obj) }.getOrNull()?.let { trips.add(it) }
        }
        val settings = root.optJSONObject("settings")?.let { settingsFromJson(it) }
        return trips to settings
    }

    fun tripToJson(t: Trip): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("name", t.name)
        put("month", t.month)
        put("nights", t.nights)
        put("days", t.days)
        put("people", t.people)
        put("currency", t.currency)
        put("fxRate", t.fxRate)
        val costs = JSONObject()
        t.costs.forEach { (c, v) -> costs.put(c.name, v) }
        put("costs", costs)
        put("flightMinutes", t.flightMinutes)
        put("rating", t.rating)
        put("memo", t.memo)
        put("createdAt", t.createdAt)
        put("updatedAt", t.updatedAt)
    }

    fun tripFromJson(o: JSONObject): Trip {
        val costsObj = o.optJSONObject("costs") ?: JSONObject()
        val costs = mutableMapOf<Category, Double>()
        Category.entries.forEach { c ->
            if (costsObj.has(c.name)) costs[c] = costsObj.optDouble(c.name, 0.0)
        }
        val created = o.optLong("createdAt", System.currentTimeMillis())
        return Trip(
            id = o.optString("id").ifBlank { Trip().id },
            name = o.optString("name"),
            month = o.optString("month"),
            nights = o.optInt("nights", 3),
            days = o.optInt("days", 4),
            people = o.optInt("people", 2).coerceAtLeast(1),
            currency = o.optString("currency", "KRW").ifBlank { "KRW" },
            fxRate = o.optDouble("fxRate", 1.0),
            costs = costs,
            flightMinutes = o.optInt("flightMinutes", 0),
            rating = o.optInt("rating", 3).coerceIn(0, 5),
            memo = o.optString("memo"),
            createdAt = created,
            updatedAt = o.optLong("updatedAt", created),
        )
    }

    fun settingsToJson(s: Settings): JSONObject = JSONObject().apply {
        put("budgetCap", s.budgetCap)
        put("themeMode", s.themeMode)
        put("perPersonFirst", s.perPersonFirst)
        val fx = JSONObject()
        s.fxRates.forEach { (k, v) -> fx.put(k, v) }
        put("fxRates", fx)
    }

    fun settingsFromJson(o: JSONObject): Settings {
        val fxObj = o.optJSONObject("fxRates")
        val fx = DEFAULT_FX.toMutableMap()
        if (fxObj != null) {
            val keys = fxObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                fx[k] = fxObj.optDouble(k, fx[k] ?: 1.0)
            }
        }
        return Settings(
            budgetCap = o.optLong("budgetCap", 2_300_000),
            themeMode = o.optString("themeMode", "system").ifBlank { "system" },
            perPersonFirst = o.optBoolean("perPersonFirst", false),
            fxRates = fx,
        )
    }
}
