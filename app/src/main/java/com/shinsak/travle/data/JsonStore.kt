package com.shinsak.travle.data

import org.json.JSONArray
import org.json.JSONObject

/** 저장 파일 / 백업 파일 공용 JSON 변환. */
object JsonStore {
    private const val VERSION = 2

    fun exportAll(trips: List<Trip>, settings: Settings): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("settings", settingsToJson(settings))
        root.put("trips", JSONArray().apply { trips.forEach { put(tripToJson(it)) } })
        return root.toString(2)
    }

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

    private fun strList(a: JSONArray?): List<String> = a?.let { arr -> (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() } } ?: emptyList()

    fun tripToJson(t: Trip): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("city", t.city)
        put("startDate", t.startDate)
        put("endDate", t.endDate)
        put("companions", JSONArray(t.companions))
        put("currency", t.currency)
        put("budgetCap", t.budgetCap)
        put("items", JSONArray().apply { t.items.forEach { put(itemToJson(it)) } })
        put("expenses", JSONArray().apply { t.expenses.forEach { put(expenseToJson(it)) } })
        put("flight", flightToJson(t.flight))
        put("stay", stayToJson(t.stay))
        put("checks", JSONArray().apply { t.checks.forEach { c -> put(JSONObject().apply { put("id", c.id); put("group", c.group); put("text", c.text); put("done", c.done) }) } })
        put("createdAt", t.createdAt)
        put("updatedAt", t.updatedAt)
    }

    fun tripFromJson(o: JSONObject): Trip {
        val created = o.optLong("createdAt", System.currentTimeMillis())
        val items = o.optJSONArray("items")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) }.map { itemFromJson(it) } } ?: emptyList()
        val expenses = o.optJSONArray("expenses")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) }.map { expenseFromJson(it) } } ?: emptyList()
        val checks = o.optJSONArray("checks")?.let { a ->
            (0 until a.length()).mapNotNull { a.optJSONObject(it) }.map { c ->
                CheckItem(id = c.optString("id").ifBlank { CheckItem().id }, group = c.optString("group", "출발 전"), text = c.optString("text"), done = c.optBoolean("done", false))
            }
        } ?: emptyList()
        return Trip(
            id = o.optString("id").ifBlank { Trip().id },
            title = o.optString("title"),
            city = o.optString("city"),
            startDate = o.optString("startDate").ifBlank { Trip().startDate },
            endDate = o.optString("endDate").ifBlank { Trip().endDate },
            companions = strList(o.optJSONArray("companions")).ifEmpty { listOf("나") },
            currency = o.optString("currency", "KRW").ifBlank { "KRW" },
            budgetCap = o.optLong("budgetCap", 1_500_000),
            items = items,
            expenses = expenses,
            flight = o.optJSONObject("flight")?.let { flightFromJson(it) } ?: Flight(),
            stay = o.optJSONObject("stay")?.let { stayFromJson(it) } ?: Stay(),
            checks = checks,
            createdAt = created,
            updatedAt = o.optLong("updatedAt", created),
        )
    }

    private fun itemToJson(p: PlanItem) = JSONObject().apply {
        put("id", p.id); put("dayIndex", p.dayIndex); put("isMemo", p.isMemo); put("time", p.time); put("name", p.name)
        put("category", p.category.name); put("address", p.address); put("mapLink", p.mapLink); put("cost", p.cost)
        put("costCurrency", p.costCurrency); put("hours", p.hours); put("note", p.note); put("auto", p.auto)
    }

    private fun itemFromJson(o: JSONObject) = PlanItem(
        id = o.optString("id").ifBlank { PlanItem().id },
        dayIndex = o.optInt("dayIndex", 1),
        isMemo = o.optBoolean("isMemo", false),
        time = o.optString("time"),
        name = o.optString("name"),
        category = runCatching { PlaceCategory.valueOf(o.optString("category")) }.getOrDefault(PlaceCategory.SIGHT),
        address = o.optString("address"),
        mapLink = o.optString("mapLink"),
        cost = o.optDouble("cost", 0.0),
        costCurrency = o.optString("costCurrency", "KRW").ifBlank { "KRW" },
        hours = o.optDouble("hours", 0.0),
        note = o.optString("note"),
        auto = o.optBoolean("auto", false),
    )

    private fun expenseToJson(e: Expense) = JSONObject().apply {
        put("id", e.id); put("dayIndex", e.dayIndex); put("amount", e.amount); put("currency", e.currency); put("fxRate", e.fxRate)
        put("title", e.title); put("category", e.category.name); put("method", e.method.name); put("paidBy", e.paidBy)
        put("splitWith", JSONArray(e.splitWith)); put("createdAt", e.createdAt)
    }

    private fun expenseFromJson(o: JSONObject) = Expense(
        id = o.optString("id").ifBlank { Expense().id },
        dayIndex = o.optInt("dayIndex", 0),
        amount = o.optDouble("amount", 0.0),
        currency = o.optString("currency", "KRW").ifBlank { "KRW" },
        fxRate = o.optDouble("fxRate", 1.0),
        title = o.optString("title"),
        category = runCatching { ExpCategory.valueOf(o.optString("category")) }.getOrDefault(ExpCategory.ETC),
        method = runCatching { PayMethod.valueOf(o.optString("method")) }.getOrDefault(PayMethod.CARD),
        paidBy = o.optString("paidBy", "나").ifBlank { "나" },
        splitWith = strList(o.optJSONArray("splitWith")),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
    )

    private fun legToJson(l: FlightLeg) = JSONObject().apply { put("dep", l.dep); put("arr", l.arr); put("minutes", l.minutes) }
    private fun legFromJson(o: JSONObject): FlightLeg? =
        FlightLeg(dep = o.optString("dep"), arr = o.optString("arr"), minutes = o.optInt("minutes", 0)).takeIf { !it.isEmpty }

    private fun flightToJson(f: Flight) = JSONObject().apply {
        put("airline", f.airline)
        f.outbound?.let { put("outbound", legToJson(it)) }
        f.inbound?.let { put("inbound", legToJson(it)) }
        put("seller", f.seller); put("pricePerPerson", f.pricePerPerson); put("totalPrice", f.totalPrice); put("note", f.note)
    }

    private fun flightFromJson(o: JSONObject) = Flight(
        airline = o.optString("airline"),
        outbound = o.optJSONObject("outbound")?.let { legFromJson(it) },
        inbound = o.optJSONObject("inbound")?.let { legFromJson(it) },
        seller = o.optString("seller"),
        pricePerPerson = o.optLong("pricePerPerson", 0),
        totalPrice = o.optLong("totalPrice", 0),
        note = o.optString("note"),
    )

    private fun stayToJson(s: Stay) = JSONObject().apply {
        put("name", s.name); put("checkIn", s.checkIn); put("checkOut", s.checkOut); put("address", s.address)
        put("mapLink", s.mapLink); put("bookingNo", s.bookingNo); put("price", s.price); put("note", s.note)
    }

    private fun stayFromJson(o: JSONObject) = Stay(
        name = o.optString("name"), checkIn = o.optString("checkIn"), checkOut = o.optString("checkOut"),
        address = o.optString("address"), mapLink = o.optString("mapLink"), bookingNo = o.optString("bookingNo"),
        price = o.optLong("price", 0), note = o.optString("note"),
    )

    fun settingsToJson(s: Settings): JSONObject = JSONObject().apply {
        put("themeMode", s.themeMode)
        put("fxRates", JSONObject().apply { s.fxRates.forEach { (k, v) -> put(k, v) } })
    }

    fun settingsFromJson(o: JSONObject): Settings {
        val fx = DEFAULT_FX.toMutableMap()
        o.optJSONObject("fxRates")?.let { fxObj ->
            val keys = fxObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                fx[k] = fxObj.optDouble(k, fx[k] ?: 1.0)
            }
        }
        return Settings(themeMode = o.optString("themeMode", "system").ifBlank { "system" }, fxRates = fx)
    }
}
