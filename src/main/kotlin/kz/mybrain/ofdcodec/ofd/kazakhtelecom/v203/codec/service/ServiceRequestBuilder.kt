package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kz.kazakhtelecom.proto.v203.Service
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Построение ServiceRequest для команд, где он обязателен или допускается.
 */
class ServiceRequestBuilder {
    private val dateTimeBuilder = DateTimeBuilder()
    private val kkmRegInfoBuilder = KkmRegInfoBuilder()
    private val orgRegInfoBuilder = OrgRegInfoBuilder()
    private val securityStatsBuilder = SecurityStatsBuilder()

    /**
     * Собирает ServiceRequest из JSON payload.
     */
    fun build(payload: JsonObject): Service.ServiceRequest {
        val serviceJson = payload["service"] as? JsonObject
        val builder = Service.ServiceRequest.newBuilder()

        if (serviceJson != null) {
            builder.setGetRegInfo(readBoolRequired(serviceJson, "getRegInfo"))
            builder.setGetBindedTaxation(false)
            readUInt(serviceJson, "nomenclatureVersion")?.let { builder.setNomenclatureVersion(it) }

            val offline = serviceJson["offlinePeriod"] as? JsonObject
                ?: throw IllegalArgumentException("Missing offlinePeriod")
            val begin = dateTimeBuilder.build(offline, "beginTime")
            val end = dateTimeBuilder.build(offline, "endTime")
            builder.setOfflinePeriod(
                Service.ServiceRequest.OfflinePeriod.newBuilder()
                    .setBeginTime(begin)
                    .setEndTime(end)
                    .build()
            )

            val security = serviceJson["securityStats"] as? JsonObject
                ?: throw IllegalArgumentException("Missing securityStats")
            builder.setSecurityStats(securityStatsBuilder.build(security))

            val regInfo = serviceJson["regInfo"] as? JsonObject
                ?: throw IllegalArgumentException("Missing regInfo")
            val kkm = regInfo["kkm"] as? JsonObject ?: throw IllegalArgumentException("Missing regInfo.kkm")
            val org = regInfo["org"] as? JsonObject ?: throw IllegalArgumentException("Missing regInfo.org")
            builder.setRegInfo(
                Service.ServiceRequest.RegInfo.newBuilder()
                    .setKkm(kkmRegInfoBuilder.build(kkm))
                    .setOrg(orgRegInfoBuilder.build(org))
                    .build()
            )
        }

        builder.clearAuxiliary()
        builder.clearTicketAdInfos()
        return builder.build()
    }

    /**
     * Читает boolean значение, если оно корректно.
     */
    private fun readBool(json: JsonObject, key: String): Boolean? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.booleanOrNull
    }

    /**
     * Читает целое значение, если оно корректно.
     */
    private fun readUInt(json: JsonObject, key: String): Int? {
        val element = json[key] as? JsonPrimitive ?: return null
        return element.intOrNull
    }

    /**
     * Читает обязательный boolean или выбрасывает ошибку.
     */
    private fun readBoolRequired(json: JsonObject, key: String): Boolean {
        val element = json[key] as? JsonPrimitive ?: throw IllegalArgumentException("Missing $key")
        return element.booleanOrNull ?: throw IllegalArgumentException("Invalid type for $key")
    }

}
