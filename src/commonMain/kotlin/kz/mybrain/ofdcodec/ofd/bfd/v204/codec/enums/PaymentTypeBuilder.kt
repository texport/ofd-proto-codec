package kz.mybrain.ofdcodec.ofd.bfd.v204.codec.enums

import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.PaymentTypeEnum
import kz.mybrain.ofdcodec.infrastructure.json.readString

/**
 * Чтение PaymentTypeEnum из JSON.
 */
internal class PaymentTypeBuilder {
    /**
     * Читает PaymentTypeEnum по ключу и возвращает его значение.
     *
     * Виды оплаты PAYMENT_CREDIT и PAYMENT_TARE объявлены устаревшими и в
     * схеме 2.0.4 отсутствуют. Отказ называет вид оплаты и версию протокола:
     * без этого наружу уходило имя класса из сгенерированного кода, по
     * которому кассир не мог понять, что именно он выбрал неправильно.
     */
    fun readRequired(json: JsonObject, key: String): PaymentTypeEnum {
        val value = json.readString(key)
            ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
        return PaymentTypeEnum.entries.firstOrNull { it.name == value }
            ?: throw IllegalArgumentException(
                "Payment type $value is not supported by protocol 2.0.4 / " +
                    "Вид оплаты $value не поддерживается протоколом 2.0.4 / " +
                    "$value төлем түрі 2.0.4 хаттамасында қолданылмайды"
            )
    }
}
