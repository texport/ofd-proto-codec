package kz.mybrain.ofdcodec.infrastructure.util

/**
 * Утилиты преобразования версии протокола между текстовым и числовым видом.
 */
internal object ProtocolVersion {
    private val numericPattern = Regex("^\\d+$")

    /**
     * Парсит числовую версию протокола, например "203".
     */
    fun parseNumeric(text: String): Int? {
        if (!numericPattern.matches(text)) return null
        return text.toIntOrNull()
    }

    /**
     * Возвращает строковое представление версии без точек, например "203".
     */
    fun toNumericString(version: Int): String = version.toString()

    fun isValidNumericVersion(version: Int): Boolean {
        return version > 0
    }
}
