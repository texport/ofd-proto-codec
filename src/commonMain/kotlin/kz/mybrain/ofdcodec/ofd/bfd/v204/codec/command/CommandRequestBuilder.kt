package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.command

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.Request

/**
 * Построение payload для конкретной команды в виде proto Request.
 */
internal fun interface CommandRequestBuilder {
    /**
     * Строит proto Request на основе JSON payload.
     */
    fun build(json: JsonObject): Request
}
