package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*
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
    fun build(payload: JsonObject): ServiceRequest {
        val serviceJson = payload["service"] as? JsonObject

        var getRegInfo: Boolean? = null
        var getBindedTaxation: Boolean? = null
        var nomenclatureVersion: Int? = null
        var offlinePeriod: ServiceRequest.OfflinePeriod? = null
        var securityStats: ServiceRequest.SecurityStats? = null
        var regInfo: ServiceRequest.RegInfo? = null

        if (serviceJson != null) {
            getRegInfo = serviceJson.readBoolRequired("getRegInfo")
            getBindedTaxation = false
            nomenclatureVersion = serviceJson.readInt("nomenclatureVersion")

            val offline = serviceJson["offlinePeriod"] as? JsonObject
                ?: throw IllegalArgumentException("Missing offlinePeriod / Отсутствует offlinePeriod / offlinePeriod өрісі жетіспейді")
            val begin = dateTimeBuilder.build(offline, "beginTime")
            val end = dateTimeBuilder.build(offline, "endTime")
            offlinePeriod = ServiceRequest.OfflinePeriod(
                begin_time = begin,
                end_time = end
            )

            val security = serviceJson["securityStats"] as? JsonObject
                ?: throw IllegalArgumentException("Missing securityStats / Отсутствует securityStats / securityStats өрісі жетіспейді")
            securityStats = securityStatsBuilder.build(security)

            val reg = serviceJson["regInfo"] as? JsonObject
                ?: throw IllegalArgumentException("Missing regInfo / Отсутствует regInfo / regInfo өрісі жетіспейді")
            val kkm = reg["kkm"] as? JsonObject ?: throw IllegalArgumentException("Missing regInfo.kkm / Отсутствует regInfo.kkm / regInfo.kkm өрісі жетіспейді")
            val org = reg["org"] as? JsonObject ?: throw IllegalArgumentException("Missing regInfo.org / Отсутствует regInfo.org / regInfo.org өрісі жетіспейді")
            regInfo = ServiceRequest.RegInfo(
                kkm = kkmRegInfoBuilder.build(kkm),
                org = orgRegInfoBuilder.build(org)
            )
        }

        return ServiceRequest(
            get_reg_info = getRegInfo,
            get_binded_taxation = getBindedTaxation,
            nomenclature_version = nomenclatureVersion,
            offline_period = offlinePeriod,
            security_stats = securityStats,
            reg_info = regInfo,
            ticket_ad_infos = emptyList(),
            auxiliary = emptyList()
        )
    }
}
