package com.shinsak.travle.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/** 앱 상태의 단일 출처. 파일 하나(travle.json)에 통째로 저장. */
class TripRepository(context: Context) {
    private val file = File(context.filesDir, "travle.json")
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings

    init {
        if (file.exists()) {
            runCatching {
                val (t, s) = JsonStore.parseAll(file.readText())
                _trips.value = t.sortedByDescending { it.updatedAt }
                if (s != null) _settings.value = s
            }
        }
    }

    private fun persist() {
        val snapshot = JsonStore.exportAll(_trips.value, _settings.value)
        io.launch {
            runCatching {
                val tmp = File(file.parentFile, "travle.json.tmp")
                tmp.writeText(snapshot)
                if (!tmp.renameTo(file)) {
                    file.writeText(snapshot)
                    tmp.delete()
                }
            }
        }
    }

    fun trip(id: String?): Trip? = id?.let { key -> _trips.value.firstOrNull { it.id == key } }

    fun upsert(trip: Trip) {
        val stamped = trip.copy(updatedAt = System.currentTimeMillis())
        _trips.value = listOf(stamped) + _trips.value.filter { it.id != trip.id }
        persist()
    }

    /** 특정 여행만 변환해서 저장 */
    fun update(id: String, transform: (Trip) -> Trip) {
        val cur = trip(id) ?: return
        upsert(transform(cur))
    }

    fun delete(id: String) {
        _trips.value = _trips.value.filter { it.id != id }
        persist()
    }

    fun clearAll() {
        _trips.value = emptyList()
        persist()
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        _settings.value = transform(_settings.value)
        persist()
    }

    fun fxRate(currency: String): Double = if (currency == "KRW") 1.0 else (_settings.value.fxRates[currency] ?: 1.0)

    fun exportJson(): String = JsonStore.exportAll(_trips.value, _settings.value)

    /** 같은 id는 덮어쓰고 나머지는 추가. 들여온 여행 수를 돌려줌. */
    fun importJson(text: String): Int {
        val (incoming, settings) = JsonStore.parseAll(text)
        if (incoming.isEmpty() && settings == null) throw IllegalArgumentException("읽을 수 있는 여행이 없음")
        val byId = _trips.value.associateBy { it.id }.toMutableMap()
        incoming.forEach { byId[it.id] = it }
        _trips.value = byId.values.sortedByDescending { it.updatedAt }
        if (settings != null) _settings.value = settings
        persist()
        return incoming.size
    }
}
