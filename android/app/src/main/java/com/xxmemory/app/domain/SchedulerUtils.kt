package com.xxmemory.app.domain

import java.util.Calendar

object SchedulerUtils {

    /**
     * 将算法给出的某一天 0 点迁就到最近的集中模式时间点。
     *
     * @param dayTimestamp 目标日期 0 点（由算法决定）。
     * @param slots 用户指定的时间点列表，格式 "HH:mm"，按当天分钟数排序。
     * @return 目标日期加上最近时间点的毫秒时间戳。
     */
    fun adjustToFocusedSlot(dayTimestamp: Long, slots: List<String>): Long {
        if (slots.isEmpty()) return dayTimestamp

        val slotMinutes = slots.mapNotNull { parseSlot(it) }.sorted()
        if (slotMinutes.isEmpty()) return dayTimestamp

        val cal = Calendar.getInstance().apply { timeInMillis = dayTimestamp }
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val chosenMinutes = slotMinutes.minByOrNull { kotlin.math.abs(it - currentMinutes) }
            ?: slotMinutes.first()

        cal.set(Calendar.HOUR_OF_DAY, chosenMinutes / 60)
        cal.set(Calendar.MINUTE, chosenMinutes % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun parseFocusedSlots(slotsString: String): List<String> {
        return slotsString.split(",", "；", ";", " ")
            .map { it.trim() }
            .filter { it.matches(Regex("^\\d{1,2}:\\d{2}$")) }
            .distinct()
    }

    private fun parseSlot(slot: String): Int? {
        val parts = slot.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfDay(timestamp: Long): Long = getStartOfDay(timestamp) + 24 * 60 * 60 * 1000

    fun getDayOffset(timestamp: Long, days: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }
}
