package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*

/**
 * Построение payload для конкретной команды в виде proto Request.
 */
internal fun interface CommandRequestBuilder {
    /**
     * Строит proto Request на основе JSON payload.
     */
    fun build(json: JsonObject): Request
}
