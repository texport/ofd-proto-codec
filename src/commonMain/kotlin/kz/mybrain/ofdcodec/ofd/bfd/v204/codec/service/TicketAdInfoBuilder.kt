package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.service

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.TicketAdInfo
import kz.bfd.proto.v204.TicketAdTypeEnum
import kz.mybrain.ofdcodec.infrastructure.json.readLongRequired
import kz.mybrain.ofdcodec.infrastructure.json.readStringRequired

/**
 * Версии рекламных текстов, которые уже есть у кассы.
 *
 * Касса присылает по строке на каждый вид, сервер отдаёт только те тексты,
 * что новее. Пока этот список не собирался, сервер сравнивал ответ не с чем
 * и не присылал ни одного объявления.
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
