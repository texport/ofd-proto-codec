package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Common
import kz.mybrain.ofdcodec.infrastructure.json.readIntRequired
import kz.mybrain.ofdcodec.infrastructure.json.readString

/**
 * Сборщик Operator из JSON-структуры.
 */
class OperatorBuilder {
    /**
     * Строит Operator из JSON-объекта.
     */
    fun build(operatorJson: JsonObject): Common.Operator {
        val builder = Common.Operator.newBuilder()
        builder.setCode(operatorJson.readIntRequired("code"))
        operatorJson.readString("name")?.let { builder.setName(it) }
        return builder.build()
    }
}
