package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.Service
import kz.mybrain.ofdcodec.infrastructure.json.readBoolRequired
import kz.mybrain.ofdcodec.infrastructure.json.readInt
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder

/**
 * Построение ServiceRequest для команд, где он обязателен или допускается.
 */
internal class ServiceRequestBuilder {
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
            builder.setGetRegInfo(serviceJson.readBoolRequired("getRegInfo"))
            builder.setGetBindedTaxation(false)
            serviceJson.readInt("nomenclatureVersion")?.let { builder.setNomenclatureVersion(it) }

            val offline = serviceJson["offlinePeriod"] as? JsonObject
                ?: throw IllegalArgumentException("Missing offlinePeriod / Отсутствует offlinePeriod / offlinePeriod өрісі жетіспейді")
            val begin = dateTimeBuilder.build(offline, "beginTime")
            val end = dateTimeBuilder.build(offline, "endTime")
            builder.setOfflinePeriod(
                Service.ServiceRequest.OfflinePeriod.newBuilder()
                    .setBeginTime(begin)
                    .setEndTime(end)
                    .build()
            )

            val security = serviceJson["securityStats"] as? JsonObject
                ?: throw IllegalArgumentException("Missing securityStats / Отсутствует securityStats / securityStats өрісі жетіспейді")
            builder.setSecurityStats(securityStatsBuilder.build(security))

            val regInfo = serviceJson["regInfo"] as? JsonObject
                ?: throw IllegalArgumentException("Missing regInfo / Отсутствует regInfo / regInfo өрісі жетіспейді")
            val kkm = regInfo["kkm"] as? JsonObject ?: throw IllegalArgumentException("Missing regInfo.kkm / Отсутствует regInfo.kkm / regInfo.kkm өрісі жетіспейді")
            val org = regInfo["org"] as? JsonObject ?: throw IllegalArgumentException("Missing regInfo.org / Отсутствует regInfo.org / regInfo.org өрісі жетіспейді")
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
}
