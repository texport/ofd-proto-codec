package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.enums

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.CommodityTypeEnum
import kz.mybrain.ofdcodec.infrastructure.json.readString

/**
 * Чтение CommodityTypeEnum из JSON.
 *
 * Тип предмета потребления введён версией 2.0.4 и обязателен для товаров
 * с НТИН: на позицию с НТИН без него ОФД отвечает кодом 15 «Касса
 * заблокирована», и касса встаёт. Реквизит необязательный — у позиции
 * без НТИН его не спрашивают, — поэтому чтение возвращает `null`.
 */
internal class CommodityTypeBuilder {
    /**
     * Читает CommodityTypeEnum по ключу; `null` — реквизита в JSON нет.
     *
     * Неизвестное значение называется в отказе целиком: без этого наружу
     * уходило имя класса из сгенерированного кода, по которому нельзя
     * понять, что именно прислала касса.
     */
    fun read(json: JsonObject, key: String): CommodityTypeEnum? {
        val value = json.readString(key) ?: return null
        return CommodityTypeEnum.entries.firstOrNull { it.name == value }
            ?: throw IllegalArgumentException(
                "Commodity type $value is not supported by protocol 2.0.4 / " +
                    "Тип предмета потребления $value не поддерживается протоколом 2.0.4 / " +
                    "$value тұтыну нысанының түрі 2.0.4 хаттамасында қолданылмайды"
            )
    }
}
