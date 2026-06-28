package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kz.kazakhtelecom.proto.v203.*

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired

/**
 * Сборщик proto OrgRegInfo из JSON-структуры.
 */
internal class OrgRegInfoBuilder {
    fun build(json: JsonObject): OrgRegInfo {
        val okved = json["okved"]?.jsonPrimitive?.content
        return OrgRegInfo(
            title = json.readStringRequired("title"),
            address = json.readStringRequired("address"),
            inn = json.readStringRequired("inn"),
            address_kz = json.readStringRequired("addressKz"),
            okved = okved
        )
    }
}
