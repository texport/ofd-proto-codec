package kz.mybrain.ofdcodec.domain.model

/**
 * Исключение библиотеки, содержащее список ошибок на трех языках (RU, KK, EN).
 */
class OfdCodecException(
    val errors: List<ValidationError>
) : RuntimeException(
    if (errors.isEmpty()) {
        "OFD codec error / Ошибка кодека ОФД / ОФД кодек қателігі"
    } else {
        errors.joinToString(separator = "\n") { error ->
            """
            Error: ${error.code} at path ${error.path}
            RU: ${error.messageRu}
            KK: ${error.messageKk}
            EN: ${error.messageEn}
            """.trimIndent()
        }
    }
)
