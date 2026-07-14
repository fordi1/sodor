package com.example.ui.screens

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
            '۰' -> '0'
            '۱' -> '1'
            '۲' -> '2'
            '۳' -> '3'
            '۴' -> '4'
            '۵' -> '5'
            '۶' -> '6'
            '۷' -> '7'
            '۸' -> '8'
            '۹' -> '9'
            else -> ch
        }
    }
    return out
}
