package kz.mybrain.ofdcodec.domain.model

/**
 * Описание ошибки валидации с сообщениями на русском и английском языках.
 */
data class ValidationError(
    val code: String,
    val path: String,
    val messageRu: String,
    val messageEn: String,
    val params: Map<String, String> = emptyMap()
)
