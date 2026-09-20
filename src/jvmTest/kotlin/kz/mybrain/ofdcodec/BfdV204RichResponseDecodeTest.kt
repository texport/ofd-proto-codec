package kz.mybrain.ofdcodec

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kz.bfd.proto.v204.CommandTypeEnum
import kz.bfd.proto.v204.Date
import kz.bfd.proto.v204.DateTime
import kz.bfd.proto.v204.KkmRegInfo
import kz.bfd.proto.v204.Money
import kz.bfd.proto.v204.MoneyPlacementEnum
import kz.bfd.proto.v204.NomenclatureResponse
import kz.bfd.proto.v204.OfdRegInfo
import kz.bfd.proto.v204.OperationTypeEnum
import kz.bfd.proto.v204.OrgRegInfo
import kz.bfd.proto.v204.PaymentTypeEnum
import kz.bfd.proto.v204.ReportResponse
import kz.bfd.proto.v204.ReportTypeEnum
import kz.bfd.proto.v204.Response
import kz.bfd.proto.v204.Result
import kz.bfd.proto.v204.ServiceResponse
import kz.bfd.proto.v204.TaxTypeEnum
import kz.bfd.proto.v204.Time
import kz.bfd.proto.v204.ZXReport
import kz.bfd.proto.v204.PosRegInfo
import kz.bfd.proto.v204.TicketAd
import kz.bfd.proto.v204.TicketAdInfo
import kz.bfd.proto.v204.TicketAdTypeEnum
import kz.bfd.proto.v204.TicketResponse
import kz.bfd.proto.v204.TaxationTypeEnum
import okio.ByteString.Companion.encodeUtf8
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.MessageHeader
import kz.mybrain.ofdcodec.infrastructure.header.HeaderCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Разбор ответов ОФД со всеми заполненными разделами.
 *
 * Пустой ответ разбирается всегда; ломается разбор на тех полях, которые
 * приходят редко: разделы Z-отчёта, справочник номенклатуры, регистрационные
 * данные точки. Поэтому здесь всё заполнено разом.
 */
class BfdV204RichResponseDecodeTest {

    private val codec = OfdCodec(DefaultRegistry.create())

    private fun frame(response: Response): ByteArray {
        val payload = Response.ADAPTER.encode(response)
        val header = MessageHeader(
            appCode = 0, protocolVersion = 204, size = 0,
            deviceId = 201873L, token = 208627316L, reqNum = 9
        )
        return HeaderCodec.encode(header, payload.size) + payload
    }

    private fun moment(hour: Int) = DateTime(
        date = Date(year = 2024, month = 9, day = 1),
        time = Time(hour = hour, minute = 0, second = 0)
    )

    private fun money(bills: Long) = Money(bills = bills, coins = 0)

    private fun fullZxReport() = ZXReport(
        date_time = moment(23),
        shift_number = 12,
        sections = listOf(
            ZXReport.Section(
                section_code = "1",
                operations = listOf(
                    ZXReport.Operation(
                        operation = OperationTypeEnum.OPERATION_SELL,
                        count = 3,
                        sum = money(9_000)
                    )
                )
            )
        ),
        operations = listOf(
            ZXReport.Operation(OperationTypeEnum.OPERATION_SELL, 3, money(9_000))
        ),
        discounts = listOf(
            ZXReport.Operation(OperationTypeEnum.OPERATION_SELL, 1, money(200))
        ),
        markups = listOf(
            ZXReport.Operation(OperationTypeEnum.OPERATION_SELL, 1, money(100))
        ),
        total_result = listOf(
            ZXReport.Operation(OperationTypeEnum.OPERATION_SELL, 3, money(8_900))
        ),
        taxes = listOf(
            ZXReport.Tax(
                type = TaxTypeEnum.VAT,
                percent = 16_000,
                operations = listOf(
                    ZXReport.Tax.TaxOperation(
                        operation = OperationTypeEnum.OPERATION_SELL,
                        turnover = money(8_900),
                        sum = money(1_227),
                        turnover_without_tax = money(7_673)
                    )
                )
            )
        ),
        start_shift_non_nullable_sums = listOf(
            ZXReport.NonNullableSum(OperationTypeEnum.OPERATION_SELL, money(100_000))
        ),
        non_nullable_sums = listOf(
            ZXReport.NonNullableSum(OperationTypeEnum.OPERATION_SELL, money(108_900))
        ),
        ticket_operations = listOf(
            ZXReport.TicketOperation(
                operation = OperationTypeEnum.OPERATION_SELL,
                tickets_total_count = 3,
                tickets_count = 3,
                tickets_sum = money(8_900),
                payments = listOf(
                    ZXReport.TicketOperation.Payment(
                        payment = PaymentTypeEnum.PAYMENT_CASH,
                        sum = money(8_900),
                        count = 3
                    )
                ),
                offline_count = 0,
                discount_sum = money(200),
                markup_sum = money(100),
                change_sum = money(0)
            )
        ),
        money_placements = listOf(
            ZXReport.MoneyPlacement(
                operation = MoneyPlacementEnum.MONEY_PLACEMENT_DEPOSIT,
                operations_total_count = 1,
                operations_count = 1,
                operations_sum = money(5_000),
                offline_count = 0
            )
        ),
        cash_sum = money(13_900),
        revenue = ZXReport.Revenue(sum = money(8_900), is_negative = false),
        open_shift_time = moment(8),
        close_shift_time = moment(23),
        checksum = "DEADBEEF"
    )

    @Test
    fun shouldDecodeFullReportResponse() {
        val response = Response(
            command = CommandTypeEnum.COMMAND_REPORT,
            result = Result(result_code = 0),
            report = ReportResponse(report = ReportTypeEnum.REPORT_Z, zx_report = fullZxReport())
        )

        val decoded = codec.decode(frame(response))

        assertTrue(decoded.isSuccess, "Полный отчёт обязан разбираться: ${decoded.exceptionOrNull()}")
        val zx = decoded.getOrNull()!!["payload"]?.jsonObject
            ?.get("report")?.jsonObject?.get("zxReport")?.jsonObject
        assertEquals(1, zx?.get("taxes")?.jsonArray?.size)
        assertEquals(
            "PAYMENT_CASH",
            zx?.get("ticketOperations")?.jsonArray?.get(0)?.jsonObject
                ?.get("payments")?.jsonArray?.get(0)?.jsonObject
                ?.get("payment")?.jsonPrimitive?.content
        )
    }

    @Test
    fun shouldDecodeTicketResponseWithQrCode() {
        val response = Response(
            command = CommandTypeEnum.COMMAND_TICKET,
            result = Result(result_code = 0),
            ticket = TicketResponse(
                ticket_number = "1234567890",
                qr_code = "https://receipt.example.kz/1234567890".encodeUtf8()
            )
        )

        val decoded = codec.decode(frame(response))

        assertTrue(decoded.isSuccess, "Ответ с QR обязан разбираться: ${decoded.exceptionOrNull()}")
        val ticket = decoded.getOrNull()!!["payload"]?.jsonObject?.get("ticket")?.jsonObject
        assertTrue(
            ticket?.get("qrCodeBase64")?.jsonPrimitive?.content?.isNotBlank() == true,
            "QR обязан доходить до кассы"
        )
    }

    @Test
    fun shouldDecodeServiceResponseWithRegistrationData() {
        val response = Response(
            command = CommandTypeEnum.COMMAND_SYSTEM,
            result = Result(result_code = 0),
            service = ServiceResponse(
                reg_info = ServiceResponse.RegInfo(
                    kkm = KkmRegInfo(
                        point_of_payment_number = "ТТ-1",
                        terminal_number = "ТРМ-7",
                        fns_kkm_id = "391827192812",
                        serial_number = "5465434234",
                        kkm_id = "201873"
                    ),
                    org = OrgRegInfo(
                        title = "ИП МИЧКА ПАВЕЛ АНДРЕЕВИЧ",
                        address = "обл. Павлодарская, Ауэзова 88",
                        iin = "960624350642",
                        oked = "47301",
                        address_kz = "Республика Қазақстан, Ауэзова 88"
                    ),
                    ofd = OfdRegInfo(
                        title = "ОФД БФД",
                        title_kz = "БФД ЭФД",
                        url = "https://receipt.example.kz"
                    ),
                    pos = PosRegInfo(
                        title = "Магазин у дома",
                        address = "Алматы, Абая 1",
                        address_kz = "Алматы, Абай 1",
                        latitude = 432156,
                        longitude = 765432
                    )
                ),
                ticket_ads = listOf(
                    TicketAd(
                        info = TicketAdInfo(type = TicketAdTypeEnum.TICKET_AD_OFD, version = 3),
                        text = "Проверьте чек на сайте ОФД"
                    )
                ),
                last_document_info = ServiceResponse.LastDocumentInfo(
                    fr_shift_number = 12,
                    printed_document_number = 345
                )
            )
        )

        val decoded = codec.decode(frame(response))

        assertTrue(decoded.isSuccess, "Служебный ответ обязан разбираться: ${decoded.exceptionOrNull()}")
        val service = decoded.getOrNull()!!["payload"]?.jsonObject?.get("service")?.jsonObject
        assertEquals(
            "201873",
            service?.get("regInfo")?.jsonObject?.get("kkm")?.jsonObject
                ?.get("kkmId")?.jsonPrimitive?.content
        )
    }

    @Test
    fun shouldDecodeNomenclatureResponseWithElements() {
        val response = Response(
            command = CommandTypeEnum.COMMAND_NOMENCLATURE,
            result = Result(result_code = 0),
            nomenclature = NomenclatureResponse(
                version = 7,
                created_time = moment(9),
                result = NomenclatureResponse.NomenclatureResultTypeEnum.RESULT_TYPE_OK,
                elements = listOf(
                    NomenclatureResponse.Element(
                        type = NomenclatureResponse.ElementTypeEnum.ITEM,
                        title = "Товар 1",
                        title_kk = "Тауар 1",
                        id = 1,
                        item = NomenclatureResponse.Item(
                            article = "ART-1",
                            barcode = "4870004302037",
                            description = "Товар 1",
                            purchase_price = money(800),
                            sell_price = money(1_000),
                            measure_title = "шт",
                            measure_fractional = false,
                            measure_count = 1,
                            discount_percent = 500,
                            discount_sum = money(50),
                            markup_percent = 200,
                            markup_sum = money(20),
                            taxes = listOf(
                                NomenclatureResponse.Tax(
                                    taxation_type = TaxationTypeEnum.STS,
                                    tax_type = TaxTypeEnum.VAT,
                                    tax_percent = 16_000
                                )
                            )
                        )
                    )
                )
            )
        )

        val decoded = codec.decode(frame(response))

        assertTrue(decoded.isSuccess, "Ответ справочника обязан разбираться: ${decoded.exceptionOrNull()}")
        val nomenclature = decoded.getOrNull()!!["payload"]?.jsonObject
            ?.get("nomenclature")?.jsonObject
        assertEquals(
            "4870004302037",
            nomenclature?.get("elements")?.jsonArray?.get(0)?.jsonObject
                ?.get("item")?.jsonObject?.get("barcode")?.jsonPrimitive?.content
        )
    }

    @Test
    fun shouldDecodeResponsesWithEveryOptionalFieldAbsent() {
        // Обратный случай к остальным проверкам этого класса: ОФД вправе
        // не прислать ни одного необязательного поля, и разбор обязан это
        // пережить так же спокойно, как и полностью заполненный ответ.
        val nomenclature = Response(
            command = CommandTypeEnum.COMMAND_NOMENCLATURE,
            result = Result(result_code = 0),
            nomenclature = NomenclatureResponse(
                version = 1,
                result = NomenclatureResponse.NomenclatureResultTypeEnum.RESULT_TYPE_OK,
                elements = listOf(
                    NomenclatureResponse.Element(
                        type = NomenclatureResponse.ElementTypeEnum.GROUP,
                        title = "Группа",
                        id = 10,
                        item = NomenclatureResponse.Item()
                    )
                )
            )
        )
        assertTrue(codec.decode(frame(nomenclature)).isSuccess, "Справочник без необязательных полей")

        val service = Response(
            command = CommandTypeEnum.COMMAND_SYSTEM,
            result = Result(result_code = 0),
            service = ServiceResponse(
                reg_info = ServiceResponse.RegInfo(
                    // Номер точки и номер терминала необязательны; сама касса
                    // обязана быть названа, иначе ответ не о ком.
                    kkm = KkmRegInfo(
                        fns_kkm_id = "391827192812",
                        serial_number = "5465434234",
                        kkm_id = "201873"
                    ),
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
        assertTrue(codec.decode(frame(service)).isSuccess, "Служебный ответ без необязательных полей")

        val report = Response(
            command = CommandTypeEnum.COMMAND_REPORT,
            result = Result(result_code = 0),
            report = ReportResponse(
                report = ReportTypeEnum.REPORT_X,
                zx_report = ZXReport(
                    date_time = moment(12),
                    shift_number = 1,
                    cash_sum = money(0),
                    revenue = ZXReport.Revenue(sum = money(0), is_negative = false),
                    open_shift_time = moment(8)
                )
            )
        )
        assertTrue(codec.decode(frame(report)).isSuccess, "X-отчёт без разделов")
    }
}
