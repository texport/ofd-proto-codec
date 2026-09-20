package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.service

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kz.bfd.proto.v204.OrgRegInfo
import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired

/**
 * Сборщик proto OrgRegInfo из JSON-структуры.
 */
internal class OrgRegInfoBuilder {
    fun build(json: JsonObject): OrgRegInfo {
        // В 203 поле называлось okved, в 204 — oked. Принимаются оба написания.
        val oked = json["oked"]?.jsonPrimitive?.content
            ?: json["okved"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing oked / Отсутствует oked")
        return OrgRegInfo(
            title = json.readStringRequired("title"),
            address = json.readStringRequired("address"),
            iin = json.readStringRequired("inn"),
            address_kz = json.readStringRequired("addressKz"),
            oked = oked
        )
    }
}
