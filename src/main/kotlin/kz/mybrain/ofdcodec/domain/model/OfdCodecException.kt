package kz.mybrain.ofdcodec.domain.model

/**
 * Исключение библиотеки, содержащее список ошибок (RU/EN).
 */
class OfdCodecException(
    val errors: List<ValidationError>,
    message: String = "OFD codec error"
) : RuntimeException(message)
