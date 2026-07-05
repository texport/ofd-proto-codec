package kz.mybrain.ofdcodec

import kotlinx.serialization.json.*
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.KazakhtelecomV203ResponseDeserializer
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.enums.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.DateTimeBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.nomenclature.NomenclatureRequestBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.report.ZXReportBuilder
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.KazakhtelecomV203Module
import kz.mybrain.ofdcodec.domain.registry.OfdRegistry
import kz.mybrain.ofdcodec.domain.registry.OfdProtocolHandler
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.port.Serializer
import kz.mybrain.ofdcodec.domain.port.Deserializer
import kz.mybrain.ofdcodec.domain.model.MessageHeader
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kz.mybrain.ofdcodec.domain.model.ErrorCode
import kz.mybrain.ofdcodec.domain.model.HeaderConstants
import kz.mybrain.ofdcodec.infrastructure.header.HeaderCodec
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.infrastructure.json.JsonMessageMapper
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.service.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.service.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import okio.ByteString

class RulesComplianceAdditionalTest {

    @Test
    fun testExhaustiveDeserialization() {
        val money = Money(bills = 123L, coins = 45)
        val date = Date(year = 2026, month = 7, day = 5)
        val time = Time(hour = 22, minute = 0, second = 0)
        val dateTime = DateTime(date = date, time = time)

        val kkmRegInfo = KkmRegInfo(
            point_of_payment_number = "POP123",
            terminal_number = "TERM456",
            fns_kkm_id = "FNS789",
            serial_number = "SN123",
            kkm_id = "KKM456"
        )
        val orgRegInfo = OrgRegInfo(
            title = "Org Title",
            address = "Org Address",
            address_kz = "Org Address KZ",
            inn = "123456789012",
            okved = "12345"
        )
        val posRegInfo = PosRegInfo(
            title = "Pos Title",
            address = "Pos Address",
            address_kz = "Pos Address KZ",
            latitude = 432156,
            longitude = 765432
        )
        val regInfo = ServiceResponse.RegInfo(
            kkm = kkmRegInfo,
            org = orgRegInfo,
            pos = posRegInfo
        )

        val ticketAdInfo = TicketAdInfo(
            type = TicketAdTypeEnum.TICKET_AD_OFD,
            version = 1
        )
        val ticketAd = TicketAd(
            info = ticketAdInfo,
            text = "Ad Text"
        )

        val serviceResponse = ServiceResponse(
            ticket_ads = listOf(ticketAd),
            reg_info = regInfo
        )

        val ticketResponse = TicketResponse(
            ticket_number = "TICKET001",
            qr_code = ByteString.of(1, 2, 3, 4)
        )

        val tax = NomenclatureResponse.Tax(
            taxation_type = NomenclatureResponse.TaxationTypeEnum.RTS,
            tax_type = NomenclatureResponse.TaxTypeEnum.VAT,
            tax_percent = 1200
        )

        val groupElement = NomenclatureResponse.Element(
            type = NomenclatureResponse.ElementTypeEnum.GROUP,
            title = "Group Title",
            title_kk = "Group Title KK",
            parent_group_id = 0,
            id = 1,
            item = null
        )

        val itemElement = NomenclatureResponse.Element(
            type = NomenclatureResponse.ElementTypeEnum.ITEM,
            title = "Item Title",
            title_kk = "Item Title KK",
            parent_group_id = 1,
            id = 2,
            item = NomenclatureResponse.Item(
                article = "ART123",
                barcode = "BARCODE456",
                description = "Item Desc",
                purchase_price = money,
                sell_price = money,
                discount_percent = 10,
                discount_sum = money,
                markup_percent = 5,
                markup_sum = money,
                taxes = listOf(tax),
                measure_count = 1,
                measure_title = "шт",
                measure_fractional = false,
                measure_unit_code = "C62",
                ntin = "NTIN123",
                is_markedeac = true,
                is_social = false
            )
        )

        val nomenclatureResponse = NomenclatureResponse(
            version = 2,
            created_time = dateTime,
            elements = listOf(groupElement, itemElement),
            result = NomenclatureResponse.NomenclatureResultTypeEnum.RESULT_TYPE_OK
        )

        val zxSection = ZXReport.Section(
            section_code = "SEC1",
            operations = listOf(
                ZXReport.Operation(
                    operation = OperationTypeEnum.OPERATION_SELL,
                    count = 5,
                    sum = money
                )
            )
        )

        val zxOperation = ZXReport.Operation(
            operation = OperationTypeEnum.OPERATION_SELL,
            count = 10,
            sum = money
        )

        val zxTax = ZXReport.Tax(
            tax_type = 1,
            percent = 1200,
            operations = listOf(
                ZXReport.Tax.TaxOperation(
                    operation = OperationTypeEnum.OPERATION_SELL,
                    turnover = money,
                    sum = money,
                    turnover_without_tax = money
                )
            )
        )

        val zxNonNullableSum = ZXReport.NonNullableSum(
            operation = OperationTypeEnum.OPERATION_SELL,
            sum = money
        )

        val zxTicketOperation = ZXReport.TicketOperation(
            operation = OperationTypeEnum.OPERATION_SELL,
            tickets_total_count = 100,
            tickets_count = 95,
            tickets_sum = money,
            payments = listOf(
                ZXReport.TicketOperation.Payment(
                    payment = PaymentTypeEnum.PAYMENT_CASH,
                    sum = money,
                    count = 90
                )
            ),
            offline_count = 5,
            discount_sum = money,
            markup_sum = money,
            change_sum = money
        )

        val zxMoneyPlacement = ZXReport.MoneyPlacement(
            operation = MoneyPlacementEnum.MONEY_PLACEMENT_DEPOSIT,
            operations_total_count = 10,
            operations_count = 10,
            operations_sum = money,
            offline_count = 0
        )

        val zxAnnulledTickets = ZXReport.AnnulledTickets(
            annulled_tickets_total_count = 1,
            annulled_tickets_count = 1,
            annulled_operations = listOf(zxOperation)
        )

        val zxReport = ZXReport(
            date_time = dateTime,
            shift_number = 1,
            sections = listOf(zxSection),
            operations = listOf(zxOperation),
            discounts = listOf(zxOperation),
            markups = listOf(zxOperation),
            total_result = listOf(zxOperation),
            taxes = listOf(zxTax),
            start_shift_non_nullable_sums = listOf(zxNonNullableSum),
            ticket_operations = listOf(zxTicketOperation),
            money_placements = listOf(zxMoneyPlacement),
            annulled_tickets = zxAnnulledTickets,
            cash_sum = money,
            revenue = ZXReport.Revenue(sum = money, is_negative = false),
            non_nullable_sums = listOf(zxNonNullableSum),
            open_shift_time = dateTime,
            close_shift_time = dateTime,
            checksum = "CRC123"
        )

        val reportResponse = ReportResponse(
            report = ReportTypeEnum.REPORT_Z,
            zx_report = zxReport
        )

        val authResponse = AuthResponse(
            result = AuthResponse.ResultTypeEnum.RESULT_TYPE_OK,
            operator_code = 1,
            operator_name = "Op Name",
            roles = listOf(UserRoleEnum.USER_ROLE_ADMINISTRATOR)
        )

        val fullResponse = Response(
            command = CommandTypeEnum.COMMAND_AUTH,
            result = Result(result_code = 0, result_text = "Success Result"),
            service = serviceResponse,
            ticket = ticketResponse,
            nomenclature = nomenclatureResponse,
            report = reportResponse,
            auth = authResponse
        )

        val bytes = Response.ADAPTER.encode(fullResponse)
        val json = KazakhtelecomV203ResponseDeserializer().deserialize(bytes)
        assertNotNull(json)
    }

    @Test
    fun testMinimalDeserialization() {
        val minimalResponse = Response(
            command = CommandTypeEnum.COMMAND_SYSTEM,
            result = Result(result_code = 0, result_text = null),
            service = null,
            ticket = null,
            nomenclature = null,
            report = null,
            auth = null
        )

        val bytes = Response.ADAPTER.encode(minimalResponse)
        val json = KazakhtelecomV203ResponseDeserializer().deserialize(bytes)
        assertNotNull(json)
    }

    @Test
    fun testMinimalReportResponseDeserialization() {
        val zxReport = ZXReport(
            date_time = DateTime(
                date = Date(year = 2026, month = 7, day = 5),
                time = Time(hour = 22, minute = 0, second = 0)
            ),
            shift_number = 1,
            sections = emptyList(),
            operations = emptyList(),
            discounts = emptyList(),
            markups = emptyList(),
            total_result = emptyList(),
            taxes = emptyList(),
            start_shift_non_nullable_sums = emptyList(),
            ticket_operations = emptyList(),
            money_placements = emptyList(),
            annulled_tickets = null,
            cash_sum = Money(bills = 0, coins = 0),
            revenue = ZXReport.Revenue(sum = Money(bills = 0, coins = 0), is_negative = false),
            non_nullable_sums = emptyList(),
            open_shift_time = null,
            close_shift_time = null,
            checksum = null
        )

        val reportResponse = ReportResponse(
            report = ReportTypeEnum.REPORT_Z,
            zx_report = zxReport
        )

        val response = Response(
            command = CommandTypeEnum.COMMAND_REPORT,
            result = Result(result_code = 0),
            report = reportResponse
        )

        val bytes = Response.ADAPTER.encode(response)
        val json = KazakhtelecomV203ResponseDeserializer().deserialize(bytes)
        assertNotNull(json)
    }

    @Test
    fun testNomenclatureWithNullOptionals() {
        val itemElement = NomenclatureResponse.Element(
            type = NomenclatureResponse.ElementTypeEnum.ITEM,
            title = "Item Title",
            title_kk = null,
            parent_group_id = null,
            id = 2,
            item = NomenclatureResponse.Item(
                article = null,
                barcode = null,
                description = null,
                purchase_price = null,
                sell_price = null,
                discount_percent = null,
                discount_sum = null,
                markup_percent = null,
                markup_sum = null,
                taxes = emptyList(),
                measure_count = null,
                measure_title = null,
                measure_fractional = null,
                measure_unit_code = null,
                ntin = null,
                is_markedeac = null,
                is_social = null
            )
        )

        val nomenclatureResponse = NomenclatureResponse(
            version = 1,
            created_time = null,
            elements = listOf(itemElement),
            result = NomenclatureResponse.NomenclatureResultTypeEnum.RESULT_TYPE_OK
        )

        val response = Response(
            command = CommandTypeEnum.COMMAND_NOMENCLATURE,
            result = Result(result_code = 0),
            nomenclature = nomenclatureResponse
        )

        val bytes = Response.ADAPTER.encode(response)
        val json = KazakhtelecomV203ResponseDeserializer().deserialize(bytes)
        assertNotNull(json)
    }

    @Test
    fun testServiceResponseWithNullOptionals() {
        val regInfo = ServiceResponse.RegInfo(
            kkm = KkmRegInfo(
                point_of_payment_number = null,
                terminal_number = null,
                fns_kkm_id = null,
                serial_number = null,
                kkm_id = null
            ),
            org = OrgRegInfo(
                title = null,
                address = null,
                address_kz = null,
                inn = null,
                okved = null
            ),
            pos = PosRegInfo(
                title = null,
                address = null,
                address_kz = null,
                latitude = null,
                longitude = null
            )
        )

        val serviceResponse = ServiceResponse(
            ticket_ads = emptyList(),
            reg_info = regInfo
        )

        val response = Response(
            command = CommandTypeEnum.COMMAND_SYSTEM,
            result = Result(result_code = 0),
            service = serviceResponse
        )

        val bytes = Response.ADAPTER.encode(response)
        val json = KazakhtelecomV203ResponseDeserializer().deserialize(bytes)
        assertNotNull(json)
    }

    @Test
    fun testOfdRegistryFindAndSupportedVersions() {
        val registry = DefaultRegistry.create()
        
        // 1. ofdId exists, but version is nonexistent
        assertNull(registry.find("kazakhtelecom", "nonexistent"))
        
        // 2. ofdId is nonexistent
        assertNull(registry.find("nonexistent", "203"))
        
        // 3. supportedVersions for nonexistent ofdId
        assertTrue(registry.supportedVersions("nonexistent").isEmpty())
    }

    @Test
    fun testEnumBuildersExceptions() {
        val emptyObj = buildJsonObject {}
        
        assertFailsWith<IllegalArgumentException> {
            OperationTypeBuilder().readRequired(emptyObj, "operation")
        }
        assertFailsWith<IllegalArgumentException> {
            PaymentTypeBuilder().readRequired(emptyObj, "payment")
        }
        assertFailsWith<IllegalArgumentException> {
            ReportTypeBuilder().readRequired(emptyObj, "report")
        }
        assertFailsWith<IllegalArgumentException> {
            TicketItemTypeBuilder().readRequired(emptyObj, "type")
        }
    }

    @Test
    fun testRequestBuildersExceptions() {
        val emptyObj = buildJsonObject {}
        
        assertFailsWith<IllegalArgumentException> {
            CommandAuthRequestBuilder().build(emptyObj)
        }
        assertFailsWith<IllegalArgumentException> {
            CommandAuthRequestBuilder().build(buildJsonObject { put("auth", buildJsonObject {}) })
        }
        assertFailsWith<IllegalArgumentException> {
            CommandAuthRequestBuilder().build(buildJsonObject { put("auth", buildJsonObject { put("login", "l") }) })
        }
        assertFailsWith<IllegalArgumentException> {
            CommandCloseShiftRequestBuilder().build(emptyObj)
        }
        assertFailsWith<IllegalArgumentException> {
            CommandMoneyPlacementRequestBuilder().build(emptyObj)
        }
        assertFailsWith<IllegalArgumentException> {
            CommandNomenclatureRequestBuilder().build(emptyObj)
        }
        assertFailsWith<IllegalArgumentException> {
            CommandReportRequestBuilder().build(emptyObj)
        }
        assertFailsWith<IllegalArgumentException> {
            CommandTicketRequestBuilder().build(emptyObj)
        }
    }

    @Test
    fun testReportResponsePartialConditionals() {
        val money = Money(bills = 0, coins = 0)
        
        // 1. ReportResponse with zx_report = null
        val reportResponseNullReport = ReportResponse(
            report = ReportTypeEnum.REPORT_Z,
            zx_report = null
        )
        val responseNullReport = Response(
            command = CommandTypeEnum.COMMAND_REPORT,
            result = Result(result_code = 0),
            report = reportResponseNullReport
        )
        val bytesNull = Response.ADAPTER.encode(responseNullReport)
        assertNotNull(KazakhtelecomV203ResponseDeserializer().deserialize(bytesNull))

        // 2. ZXReport with empty sections, taxes, ticket ops, placements, annulled tickets, turnoverWithoutTax = null, payments count = null
        val zxSectionEmpty = ZXReport.Section(
            section_code = "SEC",
            operations = emptyList()
        )
        val zxTaxEmpty = ZXReport.Tax(
            tax_type = 1,
            percent = 1200,
            operations = emptyList()
        )
        val zxTaxWithNullTurnover = ZXReport.Tax(
            tax_type = 1,
            percent = 1200,
            operations = listOf(
                ZXReport.Tax.TaxOperation(
                    operation = OperationTypeEnum.OPERATION_SELL,
                    turnover = money,
                    sum = money,
                    turnover_without_tax = null
                )
            )
        )
        val zxTicketOpEmpty = ZXReport.TicketOperation(
            operation = OperationTypeEnum.OPERATION_SELL,
            tickets_total_count = 10,
            tickets_count = 10,
            tickets_sum = money,
            payments = emptyList(),
            offline_count = null,
            discount_sum = null,
            markup_sum = null,
            change_sum = null
        )
        val zxTicketOpWithNullPaymentCount = ZXReport.TicketOperation(
            operation = OperationTypeEnum.OPERATION_SELL,
            tickets_total_count = 10,
            tickets_count = 10,
            tickets_sum = money,
            payments = listOf(
                ZXReport.TicketOperation.Payment(
                    payment = PaymentTypeEnum.PAYMENT_CASH,
                    sum = money,
                    count = null
                )
            ),
            offline_count = null,
            discount_sum = null,
            markup_sum = null,
            change_sum = null
        )
        val zxPlacementEmpty = ZXReport.MoneyPlacement(
            operation = MoneyPlacementEnum.MONEY_PLACEMENT_DEPOSIT,
            operations_total_count = 5,
            operations_count = 5,
            operations_sum = money,
            offline_count = null
        )
        val zxAnnulledEmpty = ZXReport.AnnulledTickets(
            annulled_tickets_total_count = 0,
            annulled_tickets_count = 0,
            annulled_operations = emptyList()
        )

        val zxReport = ZXReport(
            date_time = DateTime(Date(2026, 7, 5), Time(22, 0, 0)),
            shift_number = 1,
            sections = listOf(zxSectionEmpty),
            operations = emptyList(),
            discounts = emptyList(),
            markups = emptyList(),
            total_result = emptyList(),
            taxes = listOf(zxTaxEmpty, zxTaxWithNullTurnover),
            start_shift_non_nullable_sums = emptyList(),
            ticket_operations = listOf(zxTicketOpEmpty, zxTicketOpWithNullPaymentCount),
            money_placements = listOf(zxPlacementEmpty),
            annulled_tickets = zxAnnulledEmpty,
            cash_sum = money,
            revenue = ZXReport.Revenue(sum = money, is_negative = false),
            non_nullable_sums = emptyList(),
            open_shift_time = null,
            close_shift_time = null,
            checksum = null
        )

        val reportResponse = ReportResponse(
            report = ReportTypeEnum.REPORT_Z,
            zx_report = zxReport
        )
        val response = Response(
            command = CommandTypeEnum.COMMAND_REPORT,
            result = Result(result_code = 0),
            report = reportResponse
        )
        val bytes = Response.ADAPTER.encode(response)
        assertNotNull(KazakhtelecomV203ResponseDeserializer().deserialize(bytes))
    }

    @Test
    fun testResponseDeserializerPartialConditionals() {
        // 1. Result code unknown (ResultType.fromCode returns null)
        val responseUnknownCode = Response(
            command = CommandTypeEnum.COMMAND_SYSTEM,
            result = Result(result_code = 99999, result_text = "Unknown Error Text"),
            service = null
        )
        val bytesUnknown = Response.ADAPTER.encode(responseUnknownCode)
        val jsonUnknown = KazakhtelecomV203ResponseDeserializer().deserialize(bytesUnknown)
        assertNotNull(jsonUnknown)

        // 2. ServiceResponse with reg_info null, or partial reg_info nulls
        val serviceResponseNullReg = ServiceResponse(
            ticket_ads = emptyList(),
            reg_info = null
        )
        val serviceResponsePartialNullReg = ServiceResponse(
            ticket_ads = emptyList(),
            reg_info = ServiceResponse.RegInfo(kkm = null, org = null, pos = null)
        )
        
        val responseNullReg = Response(
            command = CommandTypeEnum.COMMAND_SYSTEM,
            result = Result(result_code = 0),
            service = serviceResponseNullReg
        )
        val responsePartialNullReg = Response(
            command = CommandTypeEnum.COMMAND_SYSTEM,
            result = Result(result_code = 0),
            service = serviceResponsePartialNullReg
        )
        assertNotNull(KazakhtelecomV203ResponseDeserializer().deserialize(Response.ADAPTER.encode(responseNullReg)))
        assertNotNull(KazakhtelecomV203ResponseDeserializer().deserialize(Response.ADAPTER.encode(responsePartialNullReg)))

        // 3. TicketResponse with qrCode = null
        val ticketResponseNullQr = TicketResponse(
            ticket_number = "T1",
            qr_code = null
        )
        val responseNullQr = Response(
            command = CommandTypeEnum.COMMAND_TICKET,
            result = Result(result_code = 0),
            ticket = ticketResponseNullQr
        )
        assertNotNull(KazakhtelecomV203ResponseDeserializer().deserialize(Response.ADAPTER.encode(responseNullQr)))

        // 4. NomenclatureResponse with empty elements
        val nomenclatureEmptyElements = NomenclatureResponse(
            version = 1,
            created_time = null,
            elements = emptyList(),
            result = NomenclatureResponse.NomenclatureResultTypeEnum.RESULT_TYPE_OK
        )
        val responseEmptyNomenclature = Response(
            command = CommandTypeEnum.COMMAND_NOMENCLATURE,
            result = Result(result_code = 0),
            nomenclature = nomenclatureEmptyElements
        )
        assertNotNull(KazakhtelecomV203ResponseDeserializer().deserialize(Response.ADAPTER.encode(responseEmptyNomenclature)))

        // 5. AuthResponse with null operator_code, null operator_name, empty roles
        val authEmpty = AuthResponse(
            result = AuthResponse.ResultTypeEnum.RESULT_TYPE_OK,
            operator_code = null,
            operator_name = null,
            roles = emptyList()
        )
        val responseEmptyAuth = Response(
            command = CommandTypeEnum.COMMAND_AUTH,
            result = Result(result_code = 0),
            auth = authEmpty
        )
        assertNotNull(KazakhtelecomV203ResponseDeserializer().deserialize(Response.ADAPTER.encode(responseEmptyAuth)))
    }

    @Test
    fun testOfdRegistryBranchCoverage() {
        val registry = OfdRegistry()
        
        // Find on completely empty registry (handlers map is empty)
        assertNull(registry.find("kazakhtelecom", "203"))
        
        // supportedVersions on empty registry
        assertTrue(registry.supportedVersions("kazakhtelecom").isEmpty())
    }

    @Test
    fun testCommandAuthRequestBuilderExtraBranches() {
        // Non-primitive values passed for login/password
        assertFailsWith<IllegalArgumentException> {
            CommandAuthRequestBuilder().build(buildJsonObject {
                put("auth", buildJsonObject {
                    put("login", buildJsonObject {}) // not a primitive
                })
            })
        }
        assertFailsWith<IllegalArgumentException> {
            CommandAuthRequestBuilder().build(buildJsonObject {
                put("auth", buildJsonObject {
                    put("login", "my_login")
                    put("password", buildJsonObject {}) // not a primitive
                })
            })
        }
    }

    @Test
    fun testDateTimeBuilderExceptions() {
        // time block is absent
        assertFailsWith<IllegalArgumentException> {
            DateTimeBuilder().build(buildJsonObject {
                put("key", buildJsonObject {
                    put("date", buildJsonObject {})
                })
            }, "key")
        }

        // key is not an object
        assertFailsWith<IllegalArgumentException> {
            DateTimeBuilder().build(buildJsonObject {
                put("key", "not-an-object")
            }, "key")
        }

        // date block is not an object
        assertFailsWith<IllegalArgumentException> {
            DateTimeBuilder().build(buildJsonObject {
                put("key", buildJsonObject {
                    put("date", "not-an-object")
                })
            }, "key")
        }

        // time block is not an object
        assertFailsWith<IllegalArgumentException> {
            DateTimeBuilder().build(buildJsonObject {
                put("key", buildJsonObject {
                    put("date", buildJsonObject {})
                    put("time", "not-an-object")
                })
            }, "key")
        }
    }

    @Test
    fun testNomenclatureRequestBuilderBranchCoverage() {
        // Only currentVersion is non-null
        val builder = NomenclatureRequestBuilder()
        val onlyVersionJson = buildJsonObject {
            put("nomenclature", buildJsonObject {
                put("currentVersion", 5)
            })
        }
        val req1 = builder.build(onlyVersionJson)
        assertNotNull(req1)

        // Only barcode is non-null
        val onlyBarcodeJson = buildJsonObject {
            put("nomenclature", buildJsonObject {
                put("barcode", "1234567")
            })
        }
        val req2 = builder.build(onlyBarcodeJson)
        assertNotNull(req2)
    }

    @Test
    fun testZXReportBuilderPartialAnnulledOperations() {
        val validDate = buildJsonObject {
            put("date", buildJsonObject {
                put("year", 2026)
                put("month", 7)
                put("day", 5)
            })
            put("time", buildJsonObject {
                put("hour", 22)
                put("minute", 0)
                put("second", 0)
            })
        }
        val reportJson = buildJsonObject {
            put("dateTime", validDate)
            put("shiftNumber", 1)
            put("cashSum", buildJsonObject {
                put("bills", 0)
                put("coins", 0)
            })
            put("revenue", buildJsonObject {
                put("sum", buildJsonObject {
                    put("bills", 0)
                    put("coins", 0)
                })
                put("isNegative", false)
            })
            put("openShiftTime", validDate)
            put("annulledTickets", buildJsonObject {
                put("annulledTicketsTotalCount", 0)
                put("annulledTicketsCount", 0)
                // annulledOperations is absent
            })
        }
        val report = ZXReportBuilder().build(reportJson)
        assertNotNull(report)
        assertTrue(report.annulled_tickets?.annulled_operations?.isEmpty() == true)
    }

    @Test
    fun testOfdCodecDefaultResolverMultipleAndZeroOfdIds() {
        // 1. Zero OFD IDs
        val registry0 = OfdRegistry()
        val codec0 = OfdCodec(registry0)
        val header = MessageHeader(
            appCode = 1,
            protocolVersion = 203,
            size = HeaderConstants.HEADER_SIZE.toLong(),
            deviceId = 1,
            token = 1,
            reqNum = 1
        )
        val bytes = HeaderCodec.encode(header, 0)
        val result0 = codec0.decode(bytes)
        assertTrue(result0.isFailure)
        val exception0 = result0.exceptionOrNull() as OfdCodecException
        assertTrue(exception0.errors.any { it.code == ErrorCode.MESSAGE_UNDETERMINED_OFD.name })

        // 2. Multiple OFD IDs
        val registry2 = OfdRegistry()
        KazakhtelecomV203Module.register(registry2, "kazakhtelecom")
        KazakhtelecomV203Module.register(registry2, "other_ofd")
        val codec2 = OfdCodec(registry2)
        val result2 = codec2.decode(bytes)
        assertTrue(result2.isFailure)
        val exception2 = result2.exceptionOrNull() as OfdCodecException
        assertTrue(exception2.errors.any { it.code == ErrorCode.MESSAGE_UNDETERMINED_OFD.name })
    }

    @Test
    fun testOfdRegistryRegisterMultipleVersionsSameOfdId() {
        val registry = OfdRegistry()
        val handler1 = OfdProtocolHandler(
            ofdId = "test",
            protocolVersion = "100",
            requestValidator = object : Validator {
                override fun validate(commandType: kz.mybrain.ofdcodec.domain.model.CommandType, payload: JsonObject) = emptyList<ValidationError>()
            },
            requestSerializer = object : Serializer {
                override fun serialize(commandType: kz.mybrain.ofdcodec.domain.model.CommandType, payload: JsonObject) = ByteArray(0)
            },
            responseValidator = object : Validator {
                override fun validate(commandType: kz.mybrain.ofdcodec.domain.model.CommandType, payload: JsonObject) = emptyList<ValidationError>()
            },
            responseDeserializer = object : Deserializer {
                override fun deserialize(bytes: ByteArray) = buildJsonObject {}
            }
        )
        val handler2 = handler1.copy(protocolVersion = "200")
        registry.register(handler1)
        registry.register(handler2) // This triggers the getOrPut "already exists" branch!
        
        assertNotNull(registry.find("test", "100"))
        assertNotNull(registry.find("test", "200"))
        assertTrue(registry.supportedVersions("test").size == 2)
    }

    @Test
    fun testValidationUtilsAdditionalBranches() {
        val errors = mutableListOf<ValidationError>()
        
        // 1. optionalNonBlankString with JsonObject (non-primitive)
        val jsonObj = buildJsonObject {
            put("key", buildJsonObject {})
        }
        kz.mybrain.ofdcodec.domain.validation.ValidationUtils.optionalNonBlankString(jsonObj, "key", "$.key", errors)
        assertTrue(errors.any { it.code == ErrorCode.JSON_INVALID_TYPE.name })
        errors.clear()

        // 2. optionalNonBlankString with non-string primitive
        val jsonInt = buildJsonObject {
            put("key", JsonPrimitive(123))
        }
        kz.mybrain.ofdcodec.domain.validation.ValidationUtils.optionalNonBlankString(jsonInt, "key", "$.key", errors)
        assertTrue(errors.any { it.code == ErrorCode.JSON_INVALID_TYPE.name })
        errors.clear()

        // 3. validateList with absent key
        val emptyJson = buildJsonObject {}
        kz.mybrain.ofdcodec.domain.validation.ValidationUtils.validateList(emptyJson, "key", "$.key", errors) { _, _ -> emptyList() }
        assertTrue(errors.isEmpty())
    }

    @Test
    fun testOfdCodecExceptionMessageFallback() {
        val customHandler = OfdProtocolHandler(
            ofdId = "custom",
            protocolVersion = "203",
            requestValidator = object : Validator {
                override fun validate(commandType: kz.mybrain.ofdcodec.domain.model.CommandType, payload: JsonObject) = emptyList<ValidationError>()
            },
            requestSerializer = object : Serializer {
                override fun serialize(commandType: kz.mybrain.ofdcodec.domain.model.CommandType, payload: JsonObject): ByteArray {
                    // Throw anonymous exception with null message to test orEmpty() fallback
                    throw object : Exception() {}
                }
            },
            responseValidator = object : Validator {
                override fun validate(commandType: kz.mybrain.ofdcodec.domain.model.CommandType, payload: JsonObject) = emptyList<ValidationError>()
            },
            responseDeserializer = object : Deserializer {
                override fun deserialize(bytes: ByteArray): JsonObject {
                    // Throw anonymous exception with null message to test orEmpty() fallback
                    throw object : Exception() {}
                }
            }
        )

        val registry = OfdRegistry()
        registry.register(customHandler)
        val codec = OfdCodec(registry)

        // 1. Encode serialization failure message fallback
        val envelope = buildJsonObject {
            put("ofdId", "custom")
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put("commandType", "COMMAND_SYSTEM")
            put("header", buildJsonObject {
                put("appCode", 1)
                put("deviceId", 1)
                put("token", 1)
                put("reqNum", 1)
            })
            put("payload", buildJsonObject {})
        }
        val encodeResult = codec.encode(envelope)
        assertTrue(encodeResult.isFailure)
        val encodeEx = encodeResult.exceptionOrNull() as OfdCodecException
        val serializeErr = encodeEx.errors.first { it.code == ErrorCode.SERIALIZATION_FAILED.name }
        // The reason should be empty because of anonymous exception simple name being null
        assertTrue(serializeErr.params["reason"] == "")

        // 2. Decode deserialization failure message fallback
        val header = MessageHeader(
            appCode = 1,
            protocolVersion = 203,
            size = HeaderConstants.HEADER_SIZE.toLong(),
            deviceId = 1,
            token = 1,
            reqNum = 1
        )
        val bytes = HeaderCodec.encode(header, 0)
        val decodeResult = codec.decode(bytes)
        val decodeEx = decodeResult.exceptionOrNull()
        if (decodeEx !is OfdCodecException) {
            throw AssertionError("Expected OfdCodecException but got: $decodeEx")
        }
        val codes = decodeEx.errors.map { it.code }
        if (!codes.contains(ErrorCode.DESERIALIZATION_FAILED.name)) {
            throw AssertionError("Expected DESERIALIZATION_FAILED error but got: $codes")
        }
        val deserializeErr = decodeEx.errors.first { it.code == ErrorCode.DESERIALIZATION_FAILED.name }
        assertTrue(deserializeErr.params["reason"] == "")
    }

    @Test
    fun testOfdCodecCommandTypeNonStringPrimitive() {
        val customHandler = OfdProtocolHandler(
            ofdId = "custom",
            protocolVersion = "203",
            requestValidator = object : Validator {
                override fun validate(commandType: kz.mybrain.ofdcodec.domain.model.CommandType, payload: JsonObject) = emptyList<ValidationError>()
            },
            requestSerializer = object : Serializer {
                override fun serialize(commandType: kz.mybrain.ofdcodec.domain.model.CommandType, payload: JsonObject) = ByteArray(0)
            },
            responseValidator = object : Validator {
                override fun validate(commandType: kz.mybrain.ofdcodec.domain.model.CommandType, payload: JsonObject) = emptyList<ValidationError>()
            },
            responseDeserializer = object : Deserializer {
                override fun deserialize(bytes: ByteArray): JsonObject {
                    // Return payload where commandType is not a string
                    return buildJsonObject {
                        put("commandType", JsonPrimitive(123))
                    }
                }
            }
        )

        val registry = OfdRegistry()
        registry.register(customHandler)
        val codec = OfdCodec(registry)

        val header = MessageHeader(
            appCode = 1,
            protocolVersion = 203,
            size = HeaderConstants.HEADER_SIZE.toLong(),
            deviceId = 1,
            token = 1,
            reqNum = 1
        )
        val bytes = HeaderCodec.encode(header, 0)
        val decodeResult = codec.decode(bytes)
        assertTrue(decodeResult.isFailure)
        val decodeEx = decodeResult.exceptionOrNull() as OfdCodecException
        assertTrue(decodeEx.errors.any { it.code == ErrorCode.JSON_INVALID_TYPE.name })
    }

    @Test
    fun testJsonMessageMapperNonStringPrimitive() {
        val json = buildJsonObject {
            put("ofdId", JsonPrimitive(true)) // not a string!
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put("commandType", "COMMAND_SYSTEM")
            put("header", buildJsonObject {
                put("deviceId", 1L)
                put("token", 1L)
                put("reqNum", 1)
            })
            put("payload", buildJsonObject {})
        }
        val (parsed, errors) = JsonMessageMapper.parseEnvelope(json)
        assertNull(parsed)
        assertTrue(errors.any { it.code == ErrorCode.JSON_INVALID_TYPE.name })
    }

    @Test
    fun testOfdCodecCommandTypeJsonObject() {
        val customHandler = OfdProtocolHandler(
            ofdId = "custom",
            protocolVersion = "203",
            requestValidator = object : Validator {
                override fun validate(commandType: kz.mybrain.ofdcodec.domain.model.CommandType, payload: JsonObject) = emptyList<ValidationError>()
            },
            requestSerializer = object : Serializer {
                override fun serialize(commandType: kz.mybrain.ofdcodec.domain.model.CommandType, payload: JsonObject) = ByteArray(0)
            },
            responseValidator = object : Validator {
                override fun validate(commandType: kz.mybrain.ofdcodec.domain.model.CommandType, payload: JsonObject) = emptyList<ValidationError>()
            },
            responseDeserializer = object : Deserializer {
                override fun deserialize(bytes: ByteArray): JsonObject {
                    return buildJsonObject {
                        put("commandType", buildJsonObject {}) // JsonObject instead of JsonPrimitive
                    }
                }
            }
        )

        val registry = OfdRegistry()
        registry.register(customHandler)
        val codec = OfdCodec(registry)

        val header = MessageHeader(
            appCode = 1,
            protocolVersion = 203,
            size = HeaderConstants.HEADER_SIZE.toLong(),
            deviceId = 1,
            token = 1,
            reqNum = 1
        )
        val bytes = HeaderCodec.encode(header, 0)
        val decodeResult = codec.decode(bytes)
        assertTrue(decodeResult.isFailure)
        val decodeEx = decodeResult.exceptionOrNull() as OfdCodecException
        assertTrue(decodeEx.errors.any { it.code == ErrorCode.JSON_INVALID_TYPE.name })
    }

    @Test
    fun testServiceRequestBuilderGaps() {
        val builder = ServiceRequestBuilder()
        
        // 1. service is not a JsonObject
        val req1 = builder.build(buildJsonObject { put("service", JsonPrimitive(123)) })
        assertNull(req1.get_reg_info)

        // 2. offlinePeriod is not a JsonObject
        assertFailsWith<IllegalArgumentException> {
            builder.build(buildJsonObject {
                put("service", buildJsonObject {
                    put("getRegInfo", true)
                    put("offlinePeriod", JsonPrimitive(123))
                })
            })
        }

        // 3. securityStats is not a JsonObject
        assertFailsWith<IllegalArgumentException> {
            builder.build(buildJsonObject {
                put("service", buildJsonObject {
                    put("getRegInfo", true)
                    put("offlinePeriod", buildJsonObject {
                        put("beginTime", buildJsonObject { put("date", buildJsonObject { put("year", 2026); put("month", 7); put("day", 5) }); put("time", buildJsonObject { put("hour", 22); put("minute", 0); put("second", 0) }) })
                        put("endTime", buildJsonObject { put("date", buildJsonObject { put("year", 2026); put("month", 7); put("day", 5) }); put("time", buildJsonObject { put("hour", 22); put("minute", 0); put("second", 0) }) })
                    })
                    put("securityStats", JsonPrimitive(123))
                })
            })
        }

        // 4. regInfo is not a JsonObject
        assertFailsWith<IllegalArgumentException> {
            builder.build(buildJsonObject {
                put("service", buildJsonObject {
                    put("getRegInfo", true)
                    put("offlinePeriod", buildJsonObject {
                        put("beginTime", buildJsonObject { put("date", buildJsonObject { put("year", 2026); put("month", 7); put("day", 5) }); put("time", buildJsonObject { put("hour", 22); put("minute", 0); put("second", 0) }) })
                        put("endTime", buildJsonObject { put("date", buildJsonObject { put("year", 2026); put("month", 7); put("day", 5) }); put("time", buildJsonObject { put("hour", 22); put("minute", 0); put("second", 0) }) })
                    })
                    put("securityStats", buildJsonObject {
                        put("ticketAdSentCount", 0)
                        put("ticketAdFailedCount", 0)
                    })
                    put("regInfo", JsonPrimitive(123))
                })
            })
        }
    }

    @Test
    fun testOrgRegInfoBuilderOkvedGap() {
        val orgJson = buildJsonObject {
            put("title", "Title")
            put("address", "Address")
            put("inn", "123")
            put("addressKz", "Address KZ")
            put("okved", buildJsonObject {}) // not a JsonPrimitive
        }
        assertFailsWith<IllegalArgumentException> {
            OrgRegInfoBuilder().build(orgJson)
        }
    }

    @Test
    fun testAllValidatorsFuzzEmpty() {
        val emptyObj = buildJsonObject {}
        val container = buildJsonObject {
            put("amounts", emptyObj)
            put("extensionOptions", emptyObj)
            put("modifier", emptyObj)
            put("parentTicket", emptyObj)
            put("payment", emptyObj)
            put("tax", emptyObj)
            put("revenue", emptyObj)
            put("zxReport", emptyObj)
        }
        
        // request validators
        RequestValidatorAuth().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_AUTH, emptyObj)
        RequestValidatorCloseShift().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_CLOSE_SHIFT, emptyObj)
        RequestValidatorInfo().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_INFO, emptyObj)
        RequestValidatorMoneyPlacement().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_MONEY_PLACEMENT, emptyObj)
        RequestValidatorNomenclature().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_NOMENCLATURE, emptyObj)
        RequestValidatorReport().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_REPORT, emptyObj)
        RequestValidatorReserved().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_RESERVED, emptyObj)
        RequestValidatorSystem().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_SYSTEM, emptyObj)
        RequestValidatorTicket().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_TICKET, emptyObj)
        
        // response validators
        ResponseValidatorAuth().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_AUTH, emptyObj)
        ResponseValidatorCloseShift().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_CLOSE_SHIFT, emptyObj)
        ResponseValidatorInfo().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_INFO, emptyObj)
        ResponseValidatorMoneyPlacement().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_MONEY_PLACEMENT, emptyObj)
        ResponseValidatorNomenclature().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_NOMENCLATURE, emptyObj)
        ResponseValidatorReport().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_REPORT, emptyObj)
        ResponseValidatorReserved().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_RESERVED, emptyObj)
        ResponseValidatorSystem().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_SYSTEM, emptyObj)
        ResponseValidatorTicket().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_TICKET, emptyObj)
        
        // ticket sub-validators
        TicketAmountsValidator().validate(container, "amounts", "$.path.amounts")
        TicketExtensionOptionsValidator().validate(container, "extensionOptions", "$.path.extensionOptions")
        TicketItemValidator().validate(emptyObj, "$.path")
        TicketModifierValidator().validate(container, "modifier", "$.path.modifier")
        TicketParentTicketValidator().validate(container, "parentTicket", "$.path.parentTicket")
        TicketPaymentValidator().validate(container, "payment", "$.path.payment")
        TicketTaxValidator().validate(container, "tax", "$.path.tax")
        
        // zxreport sub-validators
        ZXReportMoneyPlacementValidator().validate(emptyObj, "$.path")
        ZXReportNonNullableSumValidator().validate(emptyObj, "$.path")
        ZXReportOperationValidator().validate(emptyObj, "$.path")
        ZXReportRevenueValidator().validate(container, "revenue", "$.path.revenue")
        ZXReportSectionValidator().validate(emptyObj, "$.path")
        ZXReportTaxOperationValidator().validate(emptyObj, "$.path")
        ZXReportTaxValidator().validate(emptyObj, "$.path")
        ZXReportTicketOperationValidator().validate(emptyObj, "$.path")
        ZXReportTicketPaymentValidator().validate(emptyObj, "$.path")
        ZXReportValidator().validate(container, "zxReport", "$.path.zxReport")
        
        // service request/response
        ServiceRequestValidator().validate(emptyObj, "$.path")
        ServiceResponseValidator().validate(emptyObj, "$.path")
        TicketAdInfoValidator().validate(emptyObj, "$.path")
        TicketAdValidator().validate(emptyObj, "$.path")
    }

    @Test
    fun testAllValidatorsFuzzWrongTypes() {
        val wrongTypeObj = buildJsonObject {
            put("auth", JsonPrimitive("not-an-object"))
            put("login", buildJsonObject {})
            put("password", buildJsonObject {})
            put("service", JsonPrimitive("not-an-object"))
            put("ticket", JsonPrimitive("not-an-object"))
            put("nomenclature", JsonPrimitive("not-an-object"))
            put("report", JsonPrimitive("not-an-object"))
            put("offlinePeriod", JsonPrimitive("not-an-object"))
            put("securityStats", JsonPrimitive("not-an-object"))
            put("regInfo", JsonPrimitive("not-an-object"))
            put("kkm", JsonPrimitive("not-an-object"))
            put("org", JsonPrimitive("not-an-object"))
            put("ticketAdInfos", JsonPrimitive("not-an-object"))
            put("beginTime", JsonPrimitive("not-an-object"))
            put("endTime", JsonPrimitive("not-an-object"))
            put("geoPosition", JsonPrimitive("not-an-object"))
            put("elements", JsonPrimitive("not-an-object"))
            put("taxes", JsonPrimitive("not-an-object"))
            put("purchasePrice", JsonPrimitive("not-an-object"))
            put("sellPrice", JsonPrimitive("not-an-object"))
            put("discountSum", JsonPrimitive("not-an-object"))
            put("markupSum", JsonPrimitive("not-an-object"))
            put("sections", JsonPrimitive("not-an-object"))
            put("operations", JsonPrimitive("not-an-object"))
            put("discounts", JsonPrimitive("not-an-object"))
            put("markups", JsonPrimitive("not-an-object"))
            put("totalResult", JsonPrimitive("not-an-object"))
            put("startShiftNonNullableSums", JsonPrimitive("not-an-object"))
            put("ticketOperations", JsonPrimitive("not-an-object"))
            put("moneyPlacements", JsonPrimitive("not-an-object"))
            put("annulledTickets", JsonPrimitive("not-an-object"))
            put("cashSum", JsonPrimitive("not-an-object"))
            put("revenue", JsonPrimitive("not-an-object"))
            put("nonNullableSums", JsonPrimitive("not-an-object"))
            put("openShiftTime", JsonPrimitive("not-an-object"))
            put("closeShiftTime", JsonPrimitive("not-an-object"))
            put("items", JsonPrimitive("not-an-object"))
            put("payments", JsonPrimitive("not-an-object"))
            put("amounts", JsonPrimitive("not-an-object"))
            put("extensionOptions", JsonPrimitive("not-an-object"))
            put("parentTicket", JsonPrimitive("not-an-object"))
        }

        val container = buildJsonObject {
            put("amounts", wrongTypeObj)
            put("extensionOptions", wrongTypeObj)
            put("modifier", wrongTypeObj)
            put("parentTicket", wrongTypeObj)
            put("payment", wrongTypeObj)
            put("tax", wrongTypeObj)
            put("revenue", wrongTypeObj)
            put("zxReport", wrongTypeObj)
        }

        // request validators
        RequestValidatorAuth().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_AUTH, wrongTypeObj)
        RequestValidatorCloseShift().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_CLOSE_SHIFT, wrongTypeObj)
        RequestValidatorInfo().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_INFO, wrongTypeObj)
        RequestValidatorMoneyPlacement().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_MONEY_PLACEMENT, wrongTypeObj)
        RequestValidatorNomenclature().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_NOMENCLATURE, wrongTypeObj)
        RequestValidatorReport().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_REPORT, wrongTypeObj)
        RequestValidatorReserved().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_RESERVED, wrongTypeObj)
        RequestValidatorSystem().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_SYSTEM, wrongTypeObj)
        RequestValidatorTicket().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_TICKET, wrongTypeObj)
        
        // response validators
        ResponseValidatorAuth().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_AUTH, wrongTypeObj)
        ResponseValidatorCloseShift().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_CLOSE_SHIFT, wrongTypeObj)
        ResponseValidatorInfo().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_INFO, wrongTypeObj)
        ResponseValidatorMoneyPlacement().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_MONEY_PLACEMENT, wrongTypeObj)
        ResponseValidatorNomenclature().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_NOMENCLATURE, wrongTypeObj)
        ResponseValidatorReport().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_REPORT, wrongTypeObj)
        ResponseValidatorReserved().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_RESERVED, wrongTypeObj)
        ResponseValidatorSystem().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_SYSTEM, wrongTypeObj)
        ResponseValidatorTicket().validate(kz.mybrain.ofdcodec.domain.model.CommandType.COMMAND_TICKET, wrongTypeObj)
        
        // ticket sub-validators
        TicketAmountsValidator().validate(container, "amounts", "$.path.amounts")
        TicketExtensionOptionsValidator().validate(container, "extensionOptions", "$.path.extensionOptions")
        TicketItemValidator().validate(wrongTypeObj, "$.path")
        TicketModifierValidator().validate(container, "modifier", "$.path.modifier")
        TicketParentTicketValidator().validate(container, "parentTicket", "$.path.parentTicket")
        TicketPaymentValidator().validate(container, "payment", "$.path.payment")
        TicketTaxValidator().validate(container, "tax", "$.path.tax")
        
        // zxreport sub-validators
        ZXReportMoneyPlacementValidator().validate(wrongTypeObj, "$.path")
        ZXReportNonNullableSumValidator().validate(wrongTypeObj, "$.path")
        ZXReportOperationValidator().validate(wrongTypeObj, "$.path")
        ZXReportRevenueValidator().validate(container, "revenue", "$.path.revenue")
        ZXReportSectionValidator().validate(wrongTypeObj, "$.path")
        ZXReportTaxOperationValidator().validate(wrongTypeObj, "$.path")
        ZXReportTaxValidator().validate(wrongTypeObj, "$.path")
        ZXReportTicketOperationValidator().validate(wrongTypeObj, "$.path")
        ZXReportTicketPaymentValidator().validate(wrongTypeObj, "$.path")
        ZXReportValidator().validate(container, "zxReport", "$.path.zxReport")
        
        // service request/response
        ServiceRequestValidator().validate(wrongTypeObj, "$.path")
        ServiceResponseValidator().validate(wrongTypeObj, "$.path")
        TicketAdInfoValidator().validate(wrongTypeObj, "$.path")
        TicketAdValidator().validate(wrongTypeObj, "$.path")
    }

    @Test
    fun testAllValidatorsFuzzWrongValues() {
        val wrongValueObj = buildJsonObject {
            put("getRegInfo", JsonPrimitive(true))
            put("offlinePeriod", buildJsonObject {
                put("beginTime", buildJsonObject {
                    put("date", buildJsonObject { put("year", -1); put("month", 0); put("day", 32) })
                    put("time", buildJsonObject { put("hour", 25); put("minute", 60); put("second", 60) })
                })
                put("endTime", buildJsonObject {
                    put("date", buildJsonObject { put("year", 10000); put("month", 13); put("day", -1) })
                    put("time", buildJsonObject { put("hour", -1); put("minute", -1); put("second", -1) })
                })
            })
            put("securityStats", buildJsonObject {
                put("ticketAdSentCount", -5)
                put("ticketAdFailedCount", -10)
            })
        }
        
        ServiceRequestValidator().validate(wrongValueObj, "$.path")
    }

    @Test
    fun testAllValidatorsListFuzz() {
        val emptyObj = buildJsonObject {}
        val wrongTypeContainer = buildJsonObject {
            put("list", JsonPrimitive("not-an-array"))
        }
        val mixedArrayContainer = buildJsonObject {
            put("list", buildJsonArray {
                add(buildJsonObject {}) // valid type (JsonObject) but empty
                add(JsonPrimitive(123)) // invalid element type (not JsonObject)
            })
        }

        val listValidators = listOf(
            { container: JsonObject, key: String, path: String -> ZXReportMoneyPlacementValidator().validateList(container, key, path) },
            { container: JsonObject, key: String, path: String -> ZXReportNonNullableSumValidator().validateList(container, key, path) },
            { container: JsonObject, key: String, path: String -> ZXReportOperationValidator().validateList(container, key, path) },
            { container: JsonObject, key: String, path: String -> ZXReportSectionValidator().validateList(container, key, path) },
            { container: JsonObject, key: String, path: String -> ZXReportTaxValidator().validateList(container, key, path) },
            { container: JsonObject, key: String, path: String -> ZXReportTicketOperationValidator().validateList(container, key, path) },
            { container: JsonObject, key: String, path: String -> ZXReportTicketPaymentValidator().validateList(container, key, path) },
            { container: JsonObject, key: String, path: String -> TicketItemValidator().validateList(container, key, path) }
        )

        for (valList in listValidators) {
            // 1. Absent key
            valList(emptyObj, "nonexistent", "$.path")
            // 2. Not a JsonArray
            valList(wrongTypeContainer, "list", "$.path")
            // 3. JsonArray with mixed types (empty object and invalid type)
            valList(mixedArrayContainer, "list", "$.path")
        }
    }
}
