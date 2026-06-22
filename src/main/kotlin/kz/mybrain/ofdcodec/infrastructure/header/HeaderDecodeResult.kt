package kz.mybrain.ofdcodec.infrastructure.header

import kz.mybrain.ofdcodec.domain.model.MessageHeader
import kz.mybrain.ofdcodec.domain.model.ValidationError

/**
 * Результат чтения заголовка.
 */
internal sealed class HeaderDecodeResult {
    data class Success(val header: MessageHeader) : HeaderDecodeResult()
    data class Errors(val errors: List<ValidationError>) : HeaderDecodeResult()
}
