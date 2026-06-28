package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*
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
