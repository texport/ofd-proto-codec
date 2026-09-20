package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.ticket

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.DomainTypeEnum
import kz.bfd.proto.v204.TicketRequest
import kz.mybrain.ofdcodec.infrastructure.json.readBoolRequired
import kz.mybrain.ofdcodec.infrastructure.json.readObject
import kz.mybrain.ofdcodec.infrastructure.json.readObjectRequired
import kz.mybrain.ofdcodec.infrastructure.json.readString
import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired

/**
 * Отраслевые реквизиты чека.
 *
 * Вид отрасли задаёт, какой подблок обязателен: услуги и гостиницы требуют
 * services, нефтепродукты — gasoil, такси — taxi, стоянки — parking.
 * Торговля не требует ни одного. Подблок допускается ровно один: их набор
 * проверяется получателем, и лишний означает противоречивый чек.
 */
internal class DomainBuilder {
    private val moneyBuilder = kz.mybrain.ofdcodec.ofd.bfd.v204.codec.common.MoneyBuilder()
    private val dateTimeBuilder = kz.mybrain.ofdcodec.ofd.bfd.v204.codec.common.DateTimeBuilder()

    fun build(domainJson: JsonObject?): TicketRequest.Domain? {
        if (domainJson == null) return null
        val name = domainJson.readString("type") ?: return null
        val type = DomainTypeEnum.entries.firstOrNull { it.name == name }
            ?: throw IllegalArgumentException("Unknown domain type " + name)
        return TicketRequest.Domain(
            type = type,
            services = domainJson.readObject("services")?.let {
                TicketRequest.Domain.Services(account_number = it.readStringRequired("accountNumber"))
            },
            gasoil = domainJson.readObject("gasoil")?.let {
                TicketRequest.Domain.GasOil(
                    correction_number = it.readString("correctionNumber"),
                    correction_sum = it.readObject("correctionSum")?.let(moneyBuilder::build),
                    card_number = it.readString("cardNumber")
                )
            },
            taxi = domainJson.readObject("taxi")?.let {
                TicketRequest.Domain.Taxi(
                    car_number = it.readStringRequired("carNumber"),
                    is_order = it.readBoolRequired("isOrder"),
                    current_fee = moneyBuilder.build(it.readObjectRequired("currentFee"))
                )
            },
            parking = domainJson.readObject("parking")?.let {
                TicketRequest.Domain.Parking(
                    begin_time = dateTimeBuilder.build(it, "beginTime"),
                    end_time = dateTimeBuilder.build(it, "endTime")
                )
            }
        )
    }
}
