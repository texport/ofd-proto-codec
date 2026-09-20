package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.common

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.Operator
import kz.mybrain.ofdcodec.infrastructure.json.readIntRequired
import kz.mybrain.ofdcodec.infrastructure.json.readString

/**
 * Сборщик Operator из JSON-структуры.
 */
internal class OperatorBuilder {
    /**
     * Строит Operator из JSON-объекта.
     */
    fun build(operatorJson: JsonObject): Operator {
        val code = operatorJson.readIntRequired("code")
        val name = operatorJson.readString("name")
        return Operator(code = code, name = name)
    }
}
