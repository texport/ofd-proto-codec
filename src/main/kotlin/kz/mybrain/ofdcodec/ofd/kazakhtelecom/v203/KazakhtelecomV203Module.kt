package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203

import kz.mybrain.ofdcodec.domain.registry.OfdProtocolHandler
import kz.mybrain.ofdcodec.domain.registry.OfdRegistry
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.KazakhtelecomV203RequestSerializer
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.KazakhtelecomV203ResponseDeserializer
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.CommandAuthRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.CommandCloseShiftRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.CommandInfoRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.CommandMoneyPlacementRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.CommandNomenclatureRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.CommandReportRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.CommandSystemRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.CommandTicketRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.CommandValidatorRegistry
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.RequestValidatorAuth
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.RequestValidatorCloseShift
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.RequestValidatorInfo
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.RequestValidatorMoneyPlacement
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.RequestValidatorNomenclature
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.RequestValidatorReport
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.RequestValidatorReserved
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.RequestValidatorSystem
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.RequestValidatorTicket
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.ResponseValidatorAuth
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.ResponseValidatorCloseShift
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.ResponseValidatorInfo
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.ResponseValidatorMoneyPlacement
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.ResponseValidatorNomenclature
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.ResponseValidatorReport
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.ResponseValidatorReserved
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.ResponseValidatorSystem
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.ResponseValidatorTicket

/**
 * Регистрация обработчика для ОФД Казахтелеком, версия протокола 203.
 * Реальные валидаторы/сериализаторы подключаются отдельно.
 */
object KazakhtelecomV203Module {
    const val OFD_ID = "kazakhtelecom"
    const val PROTOCOL_VERSION = "203"

    fun register(registry: OfdRegistry, ofdId: String = OFD_ID) {
        registry.register(defaultHandler(ofdId))
    }

    fun defaultHandler(ofdId: String = OFD_ID): OfdProtocolHandler {
        val requestRegistry = CommandValidatorRegistry(
            mapOf(
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_SYSTEM to RequestValidatorSystem(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_TICKET to RequestValidatorTicket(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_CLOSE_SHIFT to RequestValidatorCloseShift(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_REPORT to RequestValidatorReport(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_NOMENCLATURE to RequestValidatorNomenclature(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_INFO to RequestValidatorInfo(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_MONEY_PLACEMENT to RequestValidatorMoneyPlacement(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_AUTH to RequestValidatorAuth(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_RESERVED to RequestValidatorReserved()
            )
        )
        val responseRegistry = CommandValidatorRegistry(
            mapOf(
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_SYSTEM to ResponseValidatorSystem(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_TICKET to ResponseValidatorTicket(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_CLOSE_SHIFT to ResponseValidatorCloseShift(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_REPORT to ResponseValidatorReport(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_NOMENCLATURE to ResponseValidatorNomenclature(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_INFO to ResponseValidatorInfo(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_MONEY_PLACEMENT to ResponseValidatorMoneyPlacement(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_AUTH to ResponseValidatorAuth(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_RESERVED to ResponseValidatorReserved()
            )
        )
        val requestSerializer = KazakhtelecomV203RequestSerializer(
            mapOf(
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_SYSTEM to CommandSystemRequestBuilder(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_TICKET to CommandTicketRequestBuilder(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_CLOSE_SHIFT to CommandCloseShiftRequestBuilder(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_INFO to CommandInfoRequestBuilder(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_MONEY_PLACEMENT to CommandMoneyPlacementRequestBuilder(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_NOMENCLATURE to CommandNomenclatureRequestBuilder(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_REPORT to CommandReportRequestBuilder(),
                kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_AUTH to CommandAuthRequestBuilder()
            )
        )
        val responseDeserializer = KazakhtelecomV203ResponseDeserializer()
        return OfdProtocolHandler(
            ofdId = ofdId,
            protocolVersion = PROTOCOL_VERSION,
            requestValidator = requestRegistry,
            requestSerializer = requestSerializer,
            responseValidator = responseRegistry,
            responseDeserializer = responseDeserializer
        )
    }
}
