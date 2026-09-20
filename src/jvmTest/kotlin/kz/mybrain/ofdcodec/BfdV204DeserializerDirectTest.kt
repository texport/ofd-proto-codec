package kz.mybrain.ofdcodec

import kotlinx.serialization.json.jsonObject
import kz.bfd.proto.v204.CommandTypeEnum
import kz.bfd.proto.v204.KkmRegInfo
import kz.bfd.proto.v204.NomenclatureResponse
import kz.bfd.proto.v204.OrgRegInfo
import kz.bfd.proto.v204.Response
import kz.bfd.proto.v204.Result
import kz.bfd.proto.v204.ServiceResponse
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.BfdV204ResponseDeserializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Разбор ответа, который не проходит проверку.
 *
 * Через codec.decode такой ответ не пропускает валидатор, поэтому
 * разборщик вызывается напрямую: он обязан выдержать отсутствие полей,
 * которые протокол объявляет необязательными, даже если конкретный ОФД
 * их всегда присылает.
 */
class BfdV204DeserializerDirectTest {

    private val deserializer = BfdV204ResponseDeserializer()

    /** Разборщик получает уже снятый заголовок — только тело ответа. */
    private fun frame(response: Response): ByteArray = Response.ADAPTER.encode(response)

    @Test
    fun shouldSurviveRegistrationDataWithoutIdentifiers() {
        val response = Response(
            command = CommandTypeEnum.COMMAND_SYSTEM,
            result = Result(result_code = 0),
            service = ServiceResponse(
                reg_info = ServiceResponse.RegInfo(
                    kkm = KkmRegInfo(point_of_payment_number = "ТТ-1"),
                    org = OrgRegInfo(
                        title = "ИП",
                        address = "Алматы",
                        iin = "960624350642",
                        oked = "47301",
                        address_kz = "Алматы"
                    )
                )
            )
        )

        val json = deserializer.deserialize(frame(response))

        val kkm = json["service"]?.jsonObject?.get("regInfo")?.jsonObject?.get("kkm")?.jsonObject
        assertTrue(kkm != null, "Блок кассы обязан быть построен")
        assertEquals(null, kkm["kkmId"], "Отсутствующий идентификатор не выдумывается")
        assertEquals(null, kkm["serialNumber"], "Отсутствующий заводской номер не выдумывается")
    }

    @Test
    fun shouldSurviveNomenclatureWithoutCreationTime() {
        val response = Response(
            command = CommandTypeEnum.COMMAND_NOMENCLATURE,
            result = Result(result_code = 0),
            nomenclature = NomenclatureResponse(
                version = 1,
                result = NomenclatureResponse.NomenclatureResultTypeEnum.RESULT_TYPE_OK
            )
        )

        val json = deserializer.deserialize(frame(response))

        val nomenclature = json["nomenclature"]?.jsonObject
        assertTrue(nomenclature != null, "Справочник обязан быть построен")
        assertEquals(null, nomenclature["createdTime"], "Времени создания не было — его и нет")
        assertEquals(null, nomenclature["elements"], "Пустой список не превращается в пустой массив")
    }
}
