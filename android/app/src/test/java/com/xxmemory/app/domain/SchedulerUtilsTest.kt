package com.xxmemory.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * 单元测试：验证 SchedulerUtils 的时间槽解析与最近时间点调整逻辑。
 * 仅使用 java.util.Calendar，不依赖 Android 框架，可在 JVM 上直接运行。
 */
class SchedulerUtilsTest {

    @Test
    fun `parseFocusedSlots parses comma separated slots`() {
        val result = SchedulerUtils.parseFocusedSlots("08:00,12:30,18:00")
        assertEquals(listOf("08:00", "12:30", "18:00"), result)
    }

    @Test
    fun `parseFocusedSlots parses semicolon separated slots`() {
        val result = SchedulerUtils.parseFocusedSlots("08:00;12:30;18:00")
        assertEquals(listOf("08:00", "12:30", "18:00"), result)
    }

    @Test
    fun `parseFocusedSlots parses chinese semicolon separated slots`() {
        val result = SchedulerUtils.parseFocusedSlots("08:00；12:30；18:00")
        assertEquals(listOf("08:00", "12:30", "18:00"), result)
    }

    @Test
    fun `parseFocusedSlots parses space separated slots`() {
        val result = SchedulerUtils.parseFocusedSlots("08:00 12:30 18:00")
        assertEquals(listOf("08:00", "12:30", "18:00"), result)
    }

    @Test
    fun `parseFocusedSlots parses mixed separators`() {
        val result = SchedulerUtils.parseFocusedSlots("08:00,12:30;18:00 21:00")
        assertEquals(listOf("08:00", "12:30", "18:00", "21:00"), result)
    }

    @Test
    fun `parseFocusedSlots filters invalid and keeps valid values`() {
        val result = SchedulerUtils.parseFocusedSlots(
            "08:00,  invalid, 12:30, 8:5, 18:00, abc:de, "
        )
        assertEquals(listOf("08:00", "12:30", "18:00"), result)
    }

    @Test
    fun `parseFocusedSlots removes duplicates`() {
        val result = SchedulerUtils.parseFocusedSlots("08:00,08:00,12:30,08:00")
        assertEquals(listOf("08:00", "12:30"), result)
    }

    @Test
    fun `parseFocusedSlots returns empty list for empty input`() {
        val result = SchedulerUtils.parseFocusedSlots("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `adjustToFocusedSlot picks nearest earlier slot`() {
        val base = timestampAt(10, 0)
        val result = SchedulerUtils.adjustToFocusedSlot(
            base,
            listOf("08:00", "20:00")
        )
        assertEquals(timestampAt(8, 0), result)
    }

    @Test
    fun `adjustToFocusedSlot picks nearest later slot`() {
        val base = timestampAt(15, 0)
        val result = SchedulerUtils.adjustToFocusedSlot(
            base,
            listOf("08:00", "20:00")
        )
        assertEquals(timestampAt(20, 0), result)
    }

    @Test
    fun `adjustToFocusedSlot picks exact match when on slot`() {
        val base = timestampAt(20, 0)
        val result = SchedulerUtils.adjustToFocusedSlot(
            base,
            listOf("08:00", "20:00")
        )
        assertEquals(timestampAt(20, 0), result)
    }

    @Test
    fun `adjustToFocusedSlot returns original timestamp when slots empty`() {
        val base = timestampAt(10, 30)
        val result = SchedulerUtils.adjustToFocusedSlot(base, emptyList())
        assertEquals(base, result)
    }

    @Test
    fun `adjustToFocusedSlot returns original timestamp when all slots invalid`() {
        val base = timestampAt(10, 30)
        val result = SchedulerUtils.adjustToFocusedSlot(
            base,
            listOf("25:00", "abc:de", "99:99")
        )
        assertEquals(base, result)
    }

    @Test
    fun `adjustToFocusedSlot ignores invalid slots and uses valid one`() {
        val base = timestampAt(10, 0)
        val result = SchedulerUtils.adjustToFocusedSlot(
            base,
            listOf("25:00", "abc", "08:00", "99:99")
        )
        assertEquals(timestampAt(8, 0), result)
    }

    @Test
    fun `adjustToFocusedSlot preserves date components`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 30)
            set(Calendar.MILLISECOND, 123)
        }
        val base = cal.timeInMillis
        val result = SchedulerUtils.adjustToFocusedSlot(base, listOf("08:00"))

        val resultCal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(2026, resultCal.get(Calendar.YEAR))
        assertEquals(Calendar.JULY, resultCal.get(Calendar.MONTH))
        assertEquals(10, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
        assertEquals(0, resultCal.get(Calendar.MILLISECOND))
    }

    private fun timestampAt(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
