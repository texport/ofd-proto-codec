package kz.mybrain.ofdcodec.domain.model

/**
 * Формирует ошибки с подстановкой параметров в шаблоны RU/EN.
 */
object ErrorFactory {
    fun error(code: ErrorCode, path: String, params: Map<String, String> = emptyMap()): ValidationError {
        val ru = format(code.ru, params)
        val kk = format(code.kk, params)
        val en = format(code.en, params)
        return ValidationError(code.name, path, ru, kk, en, params)
    }

    private fun format(template: String, params: Map<String, String>): String {
        return params.entries.fold(template) { acc, entry ->
            acc.replace("{${entry.key}}", entry.value)
        }
    }
}
