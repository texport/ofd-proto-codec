package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kz.kazakhtelecom.proto.v203.*

import kotlinx.serialization.json.JsonObject

import kz.mybrain.ofdcodec.infrastructure.json.readString
import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired

/**
 * Сборщик proto KkmRegInfo из JSON-структуры.
 */
internal class KkmRegInfoBuilder {
    fun build(json: JsonObject): KkmRegInfo {
        return KkmRegInfo(
            point_of_payment_number = json.readString("pointOfPaymentNumber"),
            terminal_number = json.readString("terminalNumber"),
            fns_kkm_id = json.readStringRequired("fnsKkmId"),
            serial_number = json.readStringRequired("serialNumber"),
            kkm_id = json.readStringRequired("kkmId")
        )
    }
}
