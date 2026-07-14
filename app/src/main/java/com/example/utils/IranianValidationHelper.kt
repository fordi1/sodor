package com.example.utils

object IranianValidationHelper {
    fun isValidPhoneNumber(phone: String): Boolean {
        val clean = phone.toEnglishDigits().trim()
        if (clean.isEmpty()) return true // Assume optional if called this way, or handle appropriately in UI
        return clean.matches(Regex("^09\\d{9}$"))
    }

    fun isValidCardNumber(card: String): Boolean {
        val clean = card.removeCommas().replace(" ", "").replace("-", "")
        if (clean.isEmpty()) return true
        if (clean.length != 16) return false
        return clean.all { it.isDigit() }
    }

    fun isValidSheba(sheba: String): Boolean {
        val clean = sheba.toEnglishDigits().trim().uppercase()
        if (clean.isEmpty()) return true
        if (!clean.startsWith("IR")) return false
        val digits = clean.substring(2)
        if (digits.length != 24) return false
        return digits.all { it.isDigit() }
    }
}
