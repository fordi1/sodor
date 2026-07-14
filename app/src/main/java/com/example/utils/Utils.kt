package com.example.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.dao.InvoiceWithRelations
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

fun String.toPersianDigits(): String {
    var out = ""
    for (ch in this) {
        out += when (ch) {
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
    }
    return out
}

fun String.toEnglishDigits(): String {
    var out = ""
    for (ch in this) {
        out += when (ch) {
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
    }
    return out
}

fun formatPrice(amount: Double): String {
    val formatter = java.text.DecimalFormat("#,###")
    return formatter.format(amount).toPersianDigits()
}

class ThousandSeparatorVisualTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return androidx.compose.ui.text.input.TransformedText(text, androidx.compose.ui.text.input.OffsetMapping.Identity)
        }

        val formattedText = try {
            val num = originalText.toLong()
            java.text.DecimalFormat("#,###").format(num).toPersianDigits()
        } catch (e: Exception) {
            originalText.toPersianDigits()
        }

        val offsetMapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (originalText.isBlank()) return 0
                val textBeforeCursor = originalText.substring(0, offset.coerceAtMost(originalText.length))
                return try {
                    val formattedTextBeforeCursor = java.text.DecimalFormat("#,###").format(textBeforeCursor.toLong()).toPersianDigits()
                    formattedTextBeforeCursor.length
                } catch (e: Exception) {
                    offset
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (formattedText.isBlank()) return 0
                val textBeforeCursor = formattedText.substring(0, offset.coerceAtMost(formattedText.length))
                val onlyDigits = textBeforeCursor.replace(",", "").replace("،", "").replace("٫", "")
                return onlyDigits.length
            }
        }

        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(formattedText), offsetMapping)
    }
}

fun String.removeCommas(): String {
    return this.replace(",", "").replace("،", "").replace("٫", "").toEnglishDigits()
}


object JalaliCalendar {
    fun getTodayString(): String {
        val pCal = android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))
        val jy = pCal.get(android.icu.util.Calendar.YEAR)
        val jm = pCal.get(android.icu.util.Calendar.MONTH) + 1
        val jd = pCal.get(android.icu.util.Calendar.DAY_OF_MONTH)
        return JalaliDate(jy, jm, jd).toString()
    }

    fun fromTimestamp(millis: Long): String {
        val pCal = android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))
        pCal.timeInMillis = millis
        val jy = pCal.get(android.icu.util.Calendar.YEAR)
        val jm = pCal.get(android.icu.util.Calendar.MONTH) + 1
        val jd = pCal.get(android.icu.util.Calendar.DAY_OF_MONTH)
        return JalaliDate(jy, jm, jd).toString()
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gCal = java.util.Calendar.getInstance()
        gCal.set(gy, gm - 1, gd, 12, 0, 0)
        
        val pCal = android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))
        pCal.timeInMillis = gCal.timeInMillis
        val jy = pCal.get(android.icu.util.Calendar.YEAR)
        val jm = pCal.get(android.icu.util.Calendar.MONTH) + 1
        val jd = pCal.get(android.icu.util.Calendar.DAY_OF_MONTH)
        return JalaliDate(jy, jm, jd)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Calendar {
        val pCal = android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))
        pCal.clear()
        pCal.set(android.icu.util.Calendar.YEAR, jy)
        pCal.set(android.icu.util.Calendar.MONTH, jm - 1)
        pCal.set(android.icu.util.Calendar.DAY_OF_MONTH, jd)
        pCal.set(android.icu.util.Calendar.HOUR_OF_DAY, 12)
        pCal.set(android.icu.util.Calendar.MINUTE, 0)
        pCal.set(android.icu.util.Calendar.SECOND, 0)
        pCal.set(android.icu.util.Calendar.MILLISECOND, 0)

        val gCal = Calendar.getInstance()
        gCal.timeInMillis = pCal.timeInMillis
        return gCal
    }

    data class JalaliDate(val year: Int, val month: Int, val day: Int) {
        override fun toString(): String {
            val mStr = if (month < 10) "۰${month.toString().toPersianDigits()}" else month.toString().toPersianDigits()
            val dStr = if (day < 10) "۰${day.toString().toPersianDigits()}" else day.toString().toPersianDigits()
            return "${year.toString().toPersianDigits()}/$mStr/$dStr"
        }
    }
}

fun String.shapePersian(): String {
    if (this.isBlank()) return this
    
    val charTable = mapOf(
        'آ' to charArrayOf('ﺁ', 'ﺁ', 'ﺂ', 'ﺂ'),
        'ا' to charArrayOf('ﺍ', 'ﺍ', 'ﺎ', 'ﺎ'),
        'ب' to charArrayOf('ﺏ', 'ﺑ', 'ﺒ', 'ﺐ'),
        'پ' to charArrayOf('ﭖ', 'ﭘ', 'ﭙ', 'ﭗ'),
        'ت' to charArrayOf('ﺕ', 'ﺗ', 'ﺘ', 'ﺖ'),
        'ث' to charArrayOf('ﺙ', 'ﺛ', 'ﺜ', 'ﺚ'),
        'ج' to charArrayOf('ﺝ', 'ﺟ', 'ﺠ', 'ﺞ'),
        'چ' to charArrayOf('ﭺ', 'ﭼ', 'ﭽ', 'ﭻ'),
        'ح' to charArrayOf('ﺡ', 'ﺣ', 'ﺤ', 'ﺢ'),
        'خ' to charArrayOf('ﺥ', 'ﺧ', 'ﺨ', 'ﺦ'),
        'د' to charArrayOf('ﺩ', 'ﺩ', 'ﺪ', 'ﺪ'),
        'ذ' to charArrayOf('ﺫ', 'ﺫ', 'ﺬ', 'ﺬ'),
        'ر' to charArrayOf('ﺭ', 'ﺭ', 'ﺮ', 'ﺮ'),
        'ز' to charArrayOf('ﺯ', 'ﺯ', 'ﺰ', 'ﺰ'),
        'ژ' to charArrayOf('ﮊ', 'ﮊ', 'ﮋ', 'ﮋ'),
        'س' to charArrayOf('ﺱ', 'ﺳ', 'ﺴ', 'ﺲ'),
        'ش' to charArrayOf('ﺵ', 'ﺷ', 'ﺸ', 'ﺶ'),
        'ص' to charArrayOf('ﺹ', 'ﺻ', 'ﺼ', 'ﺺ'),
        'ض' to charArrayOf('ﺽ', 'ﺿ', 'ﻀ', 'ﺾ'),
        'ط' to charArrayOf('ﻁ', 'ﻃ', 'ﻄ', 'ﻂ'),
        'ظ' to charArrayOf('ﻅ', 'ﻅ', 'ﻈ', 'ﻆ'),
        'ع' to charArrayOf('ﻉ', 'ﻋ', 'ﻌ', 'ﻊ'),
        'غ' to charArrayOf('ﻍ', 'ﻏ', 'ﻐ', 'ﻎ'),
        'ف' to charArrayOf('ﻑ', 'ﻓ', 'ﻔ', 'ﻒ'),
        'ق' to charArrayOf('ﻕ', 'ﻗ', 'ﻘ', 'ﻖ'),
        'ک' to charArrayOf('ﻙ', 'ﻛ', 'ﻜ', 'ﻚ'),
        'گ' to charArrayOf('ﮒ', 'ﮔ', 'ﮕ', 'ﮓ'),
        'ل' to charArrayOf('ﻝ', 'ﻟ', 'ﻠ', 'ﻞ'),
        'م' to charArrayOf('ﻡ', 'ﻣ', 'ﻤ', 'ﻢ'),
        'ن' to charArrayOf('ﻥ', 'ﻧ', 'ﻨ', 'ﻦ'),
        'و' to charArrayOf('ﻭ', 'ﻭ', 'ﻮ', 'ﻮ'),
        'ه' to charArrayOf('ﻩ', 'ﻫ', 'ﻬ', 'ﻪ'),
        'ی' to charArrayOf('ﻯ', 'ﻳ', 'ﻴ', 'ﻰ'),
        'ي' to charArrayOf('ﻱ', 'ﻳ', 'ﻴ', 'ﻲ'),
        'ئ' to charArrayOf('ﺋ', 'ﺌ', 'ﺌ', 'ﺊ'),
        'ة' to charArrayOf('ﺓ', 'ﺓ', 'ﺔ', 'ﺔ')
    )

    fun canLeftConnect(c: Char): Boolean {
        return c in charTable && c !in setOf('ا', 'آ', 'د', 'ذ', 'ر', 'ز', 'ژ', 'و', 'أ', 'إ', 'ة')
    }

    val sb = java.lang.StringBuilder()
    for (i in indices) {
        val curr = this[i]
        val mappings = charTable[curr]
        if (mappings != null) {
            val rightConnected = i > 0 && this[i - 1] in charTable && canLeftConnect(this[i - 1])
            val leftConnected = i < length - 1 && this[i + 1] in charTable && canLeftConnect(curr)
            
            val mappedChar = when {
                leftConnected && rightConnected -> mappings[2] // Medial
                leftConnected && !rightConnected -> mappings[1] // Initial
                !leftConnected && rightConnected -> mappings[3] // Final
                else -> mappings[0] // Isolated
            }
            sb.append(mappedChar)
        } else {
            sb.append(curr)
        }
    }
    val shapedText = sb.toString()

    try {
        val bidi = android.icu.text.Bidi(shapedText, android.icu.text.Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT)
        return bidi.writeReordered(android.icu.text.Bidi.DO_MIRRORING.toInt())
    } catch (e: Exception) {
        return shapedText
    }
}


fun formatCardNumber(raw: String): String {
    if (raw.length != 16) return raw.toPersianDigits()
    return "${raw.substring(0, 4)} - ${raw.substring(4, 8)} - ${raw.substring(8, 12)} - ${raw.substring(12, 16)}".toPersianDigits()
}

fun formatShebaNumber(raw: String): String {
    if (raw.isBlank()) return ""
    val clean = raw.removePrefix("IR")
    return "IR$clean".toPersianDigits()
}

object NumberToWordsConverter {
    private val yekan = arrayOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
    private val dahgan = arrayOf("", "ده", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    private val dahYek = arrayOf("", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده")
    private val sadgan = arrayOf("", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
    private val steps = arrayOf("", "هزار", "میلیون", "میلیارد", "تریلیون")

    fun convert(amount: Double): String {
        val num = amount.toLong()
        if (num == 0L) return "صفر"
        
        var tempNum = num
        val groups = ArrayList<String>()
        var stepCount = 0
        
        while (tempNum > 0) {
            val part = (tempNum % 1000).toInt()
            if (part > 0) {
                val partWords = convertThreeDigits(part)
                val stepWord = steps[stepCount]
                if (stepWord.isNotEmpty()) {
                    groups.add(0, "$partWords $stepWord")
                } else {
                    groups.add(0, partWords)
                }
            }
            tempNum /= 1000
            stepCount++
        }
        
        return groups.joinToString(" و ")
    }

    private fun convertThreeDigits(part: Int): String {
        val s = part / 100
        val d = (part % 100) / 10
        val y = part % 10
        
        val list = ArrayList<String>()
        
        if (s > 0) {
            list.add(sadgan[s])
        }
        
        if (part % 100 in 11..19) {
            list.add(dahYek[part % 100 - 10])
        } else {
            if (d > 0) {
                list.add(dahgan[d])
            }
            if (y > 0) {
                list.add(yekan[y])
            }
        }
        
        return list.joinToString(" و ")
    }
}


