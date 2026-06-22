package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Reginfo
import kz.mybrain.ofdcodec.infrastructure.json.readString
import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired

/**
 * Сборщик proto KkmRegInfo из JSON-структуры.
 */
internal class KkmRegInfoBuilder {
    /**
     * Строит KkmRegInfo из JSON-объекта.
     */
    fun build(json: JsonObject): Reginfo.KkmRegInfo {
        val builder = Reginfo.KkmRegInfo.newBuilder()

        json.readString("pointOfPaymentNumber")?.let { builder.setPointOfPaymentNumber(it) }
        json.readString("terminalNumber")?.let { builder.setTerminalNumber(it) }
        builder.setFnsKkmId(json.readStringRequired("fnsKkmId"))
        builder.setSerialNumber(json.readStringRequired("serialNumber"))
        builder.setKkmId(json.readStringRequired("kkmId"))

        return builder.build()
    }
}
