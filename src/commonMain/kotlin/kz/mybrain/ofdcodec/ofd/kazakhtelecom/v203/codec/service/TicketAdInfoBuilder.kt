package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.TicketAdInfo
import kz.kazakhtelecom.proto.v203.TicketAdTypeEnum
import kz.mybrain.ofdcodec.infrastructure.json.readLongRequired
import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired

/**
 * Версии рекламных текстов, которые уже есть у кассы.
 *
 * Касса присылает по строке на каждый вид, сервер отдаёт только те тексты,
 * что новее. Пока список собирался пустым, сравнивать было не с чем, и
 * реклама не доходила ни до одной кассы.
 */
internal class TicketAdInfoBuilder {

    /**
     * Собирает список из JSON-массива `ticketAdInfos`.
     *
     * @param infos массив объектов `{type, version}` либо `null`.
     * @return список для протокола; пустой, если касса ничего не прислала.
     */
    fun build(infos: JsonArray?): List<TicketAdInfo> {
        if (infos == null) return emptyList()
        return infos.map { element ->
            val info = element as? JsonObject
                ?: throw IllegalArgumentException(
                    "ticketAdInfos element is not an object / " +
                        "Элемент ticketAdInfos не объект / ticketAdInfos элементі объект емес"
                )
            val name = info.readStringRequired("type")
            val type = TicketAdTypeEnum.entries.firstOrNull { it.name == name }
                ?: throw IllegalArgumentException(
                    "Unknown ticket ad type $name / Неизвестный вид рекламного текста $name / " +
                        "Белгісіз жарнама түрі $name"
                )
            TicketAdInfo(type = type, version = info.readLongRequired("version"))
        }
    }
}
