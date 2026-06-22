package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kz.kazakhtelecom.proto.v203.Reginfo
import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired

/**
 * Сборщик proto OrgRegInfo из JSON-структуры.
 */
internal class OrgRegInfoBuilder {
    /**
     * Строит OrgRegInfo из JSON-объекта.
     */
    fun build(json: JsonObject): Reginfo.OrgRegInfo {
        val builder = Reginfo.OrgRegInfo.newBuilder()
            .setTitle(json.readStringRequired("title"))
            .setAddress(json.readStringRequired("address"))
            .setInn(json.readStringRequired("inn"))
            .setAddressKz(json.readStringRequired("addressKz"))

        val okved = json["okved"]?.jsonPrimitive?.content
        if (okved != null) {
            builder.setOkved(okved)
        }
        return builder.build()
    }
}
