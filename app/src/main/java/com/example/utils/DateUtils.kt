package com.example.utils

import java.util.Calendar

object PersianDigitConverter {
    fun toPersian(input: String): String {
        return input.map { ch ->
            when (ch) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                else -> ch
            }
        }.joinToString("")
    }

    fun toEnglish(input: String): String {
        return input.map { ch ->
            when (ch) {
                '۰', '٠' -> '0'
                '۱', '١' -> '1'
                '۲', '٢' -> '2'
                '۳', '٣' -> '3'
                '۴', '٤' -> '4'
                '۵', '٥' -> '5'
                '۶', '٦' -> '6'
                '۷', '٧' -> '7'
                '۸', '٨' -> '8'
                '۹', '٩' -> '9'
                else -> ch
            }
        }.joinToString("")
    }

    fun normalizeBarcode(barcode: String?): String {
        if (barcode == null) return ""
        val englishDigits = toEnglish(barcode)
        val basic = englishDigits.trim()
            .filter { !it.isWhitespace() && it.code >= 32 }
            .lowercase()
        return basic.replaceFirst("^0+".toRegex(), "")
    }

    fun matchBarcodes(barcode1: String?, barcode2: String?): Boolean {
        val norm1 = normalizeBarcode(barcode1)
        val norm2 = normalizeBarcode(barcode2)
        if (norm1.isBlank() || norm2.isBlank()) return false
        if (norm1 == norm2) return true
        if (norm1.length > 5 && norm2.length > 5 && (norm1.endsWith(norm2) || norm2.endsWith(norm1))) return true
        return false
    }
}

object DeviceDateProvider {
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    fun getCurrentJalaliDate(): JalaliCalendar.JalaliDate {
        val pCal = android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))
        val jy = pCal.get(android.icu.util.Calendar.YEAR)
        val jm = pCal.get(android.icu.util.Calendar.MONTH) + 1
        val jd = pCal.get(android.icu.util.Calendar.DAY_OF_MONTH)
        return JalaliCalendar.JalaliDate(jy, jm, jd)
    }

    fun getCurrentJalaliString(): String {
        val jalali = getCurrentJalaliDate()
        return JalaliDateFormatter.format(jalali.year, jalali.month, jalali.day)
    }
}

object JalaliDateFormatter {
    fun format(timestamp: Long): String {
        val pCal = android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))
        pCal.timeInMillis = timestamp
        val jy = pCal.get(android.icu.util.Calendar.YEAR)
        val jm = pCal.get(android.icu.util.Calendar.MONTH) + 1
        val jd = pCal.get(android.icu.util.Calendar.DAY_OF_MONTH)
        return format(jy, jm, jd)
    }

    fun format(year: Int, month: Int, day: Int): String {
        val mStr = if (month < 10) "۰$month" else month.toString()
        val dStr = if (day < 10) "۰$day" else day.toString()
        val yStr = year.toString()
        return PersianDigitConverter.toPersian("$yStr/$mStr/$dStr")
    }

    fun isLeapYear(year: Int): Boolean {
        val pCal = android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))
        pCal.clear()
        pCal.set(android.icu.util.Calendar.YEAR, year)
        pCal.set(android.icu.util.Calendar.MONTH, 11) // Esfand/Last month
        pCal.set(android.icu.util.Calendar.DAY_OF_MONTH, 1)
        return pCal.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH) == 30
    }

    fun getMaxDaysInMonth(year: Int, month: Int): Int {
        val pCal = android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))
        pCal.clear()
        pCal.set(android.icu.util.Calendar.YEAR, year)
        pCal.set(android.icu.util.Calendar.MONTH, month - 1)
        pCal.set(android.icu.util.Calendar.DAY_OF_MONTH, 1)
        return pCal.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH)
    }

    fun parseToTimestamp(jalaliStr: String): Long? {
        try {
            val engStr = PersianDigitConverter.toEnglish(jalaliStr).trim()
            val parts = engStr.split("/")
            if (parts.size != 3) return null
            
            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            val day = parts[2].toIntOrNull() ?: return null

            if (year < 1300 || year > 1500) return null
            if (month < 1 || month > 12) return null
            val maxDays = getMaxDaysInMonth(year, month)
            if (day < 1 || day > maxDays) return null

            val pCal = android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))
            pCal.clear()
            pCal.set(android.icu.util.Calendar.YEAR, year)
            pCal.set(android.icu.util.Calendar.MONTH, month - 1)
            pCal.set(android.icu.util.Calendar.DAY_OF_MONTH, day)
            pCal.set(android.icu.util.Calendar.HOUR_OF_DAY, 12)
            pCal.set(android.icu.util.Calendar.MINUTE, 0)
            pCal.set(android.icu.util.Calendar.SECOND, 0)
            pCal.set(android.icu.util.Calendar.MILLISECOND, 0)
            return pCal.timeInMillis
        } catch (e: Exception) {
            return null
        }
    }
}
