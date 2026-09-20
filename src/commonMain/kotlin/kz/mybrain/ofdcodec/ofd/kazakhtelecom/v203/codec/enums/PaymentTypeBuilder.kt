package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums

import kotlinx.serialization.json.JsonObject
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.ofdcodec.infrastructure.json.readString

/**
 * Чтение PaymentTypeEnum из JSON.
 */
internal class PaymentTypeBuilder {
    /**
     * Читает PaymentTypeEnum по ключу и возвращает его значение.
     */
    fun readRequired(json: JsonObject, key: String): PaymentTypeEnum {
        val value = json.readString(key)
            ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
        return PaymentTypeEnum.entries.firstOrNull { it.name == value }
            ?: throw IllegalArgumentException(
                "Payment type $value is not supported by protocol 2.0.3 / " +
                    "Вид оплаты $value не поддерживается протоколом 2.0.3 / " +
                    "$value төлем түрі 2.0.3 хаттамасында қолданылмайды"
            )
    }
}
