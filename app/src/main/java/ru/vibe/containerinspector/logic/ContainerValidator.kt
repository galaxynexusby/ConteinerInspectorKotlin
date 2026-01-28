package ru.vibe.containerinspector.logic

import kotlin.math.pow

object ContainerValidator {
    /**
     * Валидация номера контейнера согласно ISO 6346.
     * Формат: 4 буквы + 7 цифр (напр. MSKU1234567).
     */
    fun isValid(number: String?): Boolean {
        if (number == null) return false
        val clean = number.uppercase().replace(Regex("[^A-Z0-9]"), "")
        if (clean.length != 11) return false

        val prefix = clean.substring(0, 4)
        val digits = clean.substring(4)

        if (!prefix.all { it.isLetter() } || !digits.all { it.isDigit() }) return false

        val checkDigit = digits.last().digitToInt()
        val calculatedCheckDigit = calculateCheckDigit(clean.substring(0, 10))

        return checkDigit == calculatedCheckDigit % 10 // ISO 6346 says mod 11, then last digit of that is check digit? Actually mod 11 and result 0-9 is check digit. 10 is 0.
    }

    private fun calculateCheckDigit(base: String): Int {
        var sum = 0
        for (i in 0..9) {
            val char = base[i]
            val value = if (char.isLetter()) {
                val alphaVal = char.code - 'A'.code + 10
                // Letters A=10...Z=38, but some numbers are skipped in ISO 6346: 11, 22, 33
                var adjusted = alphaVal
                if (alphaVal >= 11) adjusted++
                if (alphaVal >= 22) adjusted++
                if (alphaVal >= 33) adjusted++
                adjusted
            } else {
                char.digitToInt()
            }
            sum += value * 2.0.pow(i).toInt()
        }
        val remainder = sum % 11
        return if (remainder == 10) 0 else remainder
    }
}
