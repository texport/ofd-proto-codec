package kz.mybrain.ofdcodec

import kotlinx.serialization.json.*
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.ofdcodec.application.*
import kz.mybrain.ofdcodec.domain.model.*
import kz.mybrain.ofdcodec.domain.port.*
import kz.mybrain.ofdcodec.domain.registry.*
import kz.mybrain.ofdcodec.domain.validation.*
import kz.mybrain.ofdcodec.infrastructure.header.*
import kz.mybrain.ofdcodec.infrastructure.json.*
import kz.mybrain.ofdcodec.infrastructure.util.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.closeshift.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.nomenclature.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.report.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.ticket.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.model.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.service.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.ticket.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.service.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.service.*
import kotlin.test.*

class ExhaustiveCoverageTest {

    private fun buildValidServiceJson() = buildJsonObject {
        put("getRegInfo", true)
        put(
            "offlinePeriod",
            buildJsonObject {
                put(
                    "beginTime",
                    buildJsonObject {
                        put(
                            "date",
                            buildJsonObject {
                                put("year", 2024)
                                put("month", 9)
                                put("day", 1)
                            }
                        )
                        put(
                            "time",
                            buildJsonObject {
                                put("hour", 10)
                                put("minute", 30)
                            }
                        )
                    }
                )
                put(
                    "endTime",
                    buildJsonObject {
                        put(
                            "date",
                            buildJsonObject {
                                put("year", 2024)
                                put("month", 9)
                                put("day", 1)
                            }
                        )
                        put(
                            "time",
                            buildJsonObject {
                                put("hour", 10)
                                put("minute", 40)
                            }
                        )
                    }
                )
            }
        )
        put(
            "securityStats",
            buildJsonObject {
                put(
                    "geoPosition",
                    buildJsonObject {
                        put("latitude", 432156)
                        put("longitude", 765432)
                        put("source", "CELL")
                    }
                )
            }
        )
        put(
            "regInfo",
            buildJsonObject {
                put(
                    "kkm",
                    buildJsonObject {
                        put("fnsKkmId", "391827192812")
                        put("serialNumber", "5465434234")
                        put("kkmId", "201873")
                    }
                )
                put(
                    "org",
                    buildJsonObject {
                        put("title", "ИП МИЧКА ПАВЕЛ АНДРЕЕВИЧ")
                        put("address", "обл. Павлодарская, Ауэзова 88")
                        put("addressKz", "Республика Қазақстан, обл. Павлодарская, қ. Екібастұз, Ауэзова 88")
                        put("inn", "960624350642")
                        put("okved", "47301")
                    }
                )
            }
        )
    }

    // --- 1. ValidationUtils and Common Validators ---

    @Test
    fun testValidationUtilsHelpers() {
        val json = buildJsonObject {
            put("num", 10)
            put("str", "abc")
            put("blankStr", "   ")
            put("bool", true)
            put("obj", buildJsonObject { })
            put("arr", buildJsonArray { })
            put("longVal", 9999999999L)
        }

        val errors = mutableListOf<ValidationError>()

        // requireObject
        val objRes1 = ValidationUtils.requireObject(json, "obj", "$.obj", errors)
        assertNotNull(objRes1)
        val objRes2 = ValidationUtils.requireObject(json, "missing", "$.missing", errors)
        assertNull(objRes2)
        assertEquals(1, errors.size)
        assertEquals(ErrorCode.JSON_MISSING_FIELD.name, errors.last().code)

        ValidationUtils.requireObject(json, "num", "$.num", errors)
        assertEquals(2, errors.size)
        assertEquals(ErrorCode.JSON_INVALID_TYPE.name, errors.last().code)

        // requireArray
        val arrRes1 = ValidationUtils.requireArray(json, "arr", "$.arr", errors)
        assertNotNull(arrRes1)
        val arrRes2 = ValidationUtils.requireArray(json, "missing", "$.missing", errors)
        assertNull(arrRes2)
        assertEquals(3, errors.size)

        ValidationUtils.requireArray(json, "num", "$.num", errors)
        assertEquals(4, errors.size)

        // requireNonBlankString
        ValidationUtils.requireNonBlankString(json, "str", "$.str", errors)
        assertEquals(4, errors.size)
        ValidationUtils.requireNonBlankString(json, "missing", "$.missing", errors)
        assertEquals(5, errors.size)
        ValidationUtils.requireNonBlankString(json, "num", "$.num", errors)
        assertEquals(6, errors.size)
        ValidationUtils.requireNonBlankString(json, "blankStr", "$.blankStr", errors)
        assertEquals(7, errors.size)

        // optionalNonBlankString
        ValidationUtils.optionalNonBlankString(json, "missing", "$.missing", errors)
        assertEquals(7, errors.size)
        ValidationUtils.optionalNonBlankString(json, "str", "$.str", errors)
        assertEquals(7, errors.size)
        ValidationUtils.optionalNonBlankString(json, "num", "$.num", errors)
        assertEquals(8, errors.size)
        ValidationUtils.optionalNonBlankString(json, "blankStr", "$.blankStr", errors)
        assertEquals(9, errors.size)

        // requireBoolean
        ValidationUtils.requireBoolean(json, "bool", "$.bool", errors)
        assertEquals(9, errors.size)
        ValidationUtils.requireBoolean(json, "missing", "$.missing", errors)
        assertEquals(10, errors.size)
        ValidationUtils.requireBoolean(json, "num", "$.num", errors)
        assertEquals(11, errors.size)

        // requireIntInRange
        ValidationUtils.requireIntInRange(json, "num", 5, 15, "$.num", errors)
        assertEquals(11, errors.size)
        ValidationUtils.requireIntInRange(json, "num", 15, 25, "$.num", errors)
        assertEquals(12, errors.size)
        ValidationUtils.requireIntInRange(json, "missing", 0, 10, "$.missing", errors)
        assertEquals(13, errors.size)
        ValidationUtils.requireIntInRange(json, "str", 0, 10, "$.str", errors)
        assertEquals(14, errors.size)

        // requireLongInRange
        ValidationUtils.requireLongInRange(json, "longVal", 5L, 99999999999L, "$.longVal", errors)
        assertEquals(14, errors.size)
        ValidationUtils.requireLongInRange(json, "longVal", 0L, 10L, "$.longVal", errors)
        assertEquals(15, errors.size)
        ValidationUtils.requireLongInRange(json, "missing", 0L, 10L, "$.missing", errors)
        assertEquals(16, errors.size)
        ValidationUtils.requireLongInRange(json, "str", 0L, 10L, "$.str", errors)
        assertEquals(17, errors.size)

        // validateList
        val listContainer = buildJsonObject {
            put(
                "validList",
                buildJsonArray {
                    add(buildJsonObject { put("val", 1) })
                }
            )
            put("invalidListType", 123)
            put(
                "invalidListItem",
                buildJsonArray {
                    add(123)
                }
            )
        }
        ValidationUtils.validateList(listContainer, "validList", "$.validList", errors) { item, path ->
            val errs = mutableListOf<ValidationError>()
            ValidationUtils.requireIntInRange(item, "val", 0, 10, "$path.val", errs)
            errs
        }
        assertEquals(17, errors.size)

        ValidationUtils.validateList(
            listContainer,
            "invalidListType",
            "$.invalidListType",
            errors
        ) { _, _ -> emptyList() }
        assertEquals(18, errors.size)

        ValidationUtils.validateList(
            listContainer,
            "invalidListItem",
            "$.invalidListItem",
            errors
        ) { _, _ -> emptyList() }
        assertEquals(19, errors.size)

        // validateEnum
        val enumContainer = buildJsonObject {
            put("val", "A")
            put("invalidVal", "C")
            put("invalidType", 123)
        }
        val allowed = setOf("A", "B")
        errors.addAll(ValidationUtils.validateEnum(enumContainer, "val", "$.val", allowed))
        assertEquals(19, errors.size)
        errors.addAll(ValidationUtils.validateEnum(enumContainer, "invalidVal", "$.invalidVal", allowed))
        assertEquals(20, errors.size)
        errors.addAll(ValidationUtils.validateEnum(enumContainer, "missing", "$.missing", allowed))
        assertEquals(21, errors.size)
        errors.addAll(ValidationUtils.validateEnum(enumContainer, "invalidType", "$.invalidType", allowed))
        assertEquals(22, errors.size)
    }

    @Test
    fun testMoneyValidator() {
        val validator = MoneyValidator()
        val valid = buildJsonObject {
            put(
                "price",
                buildJsonObject {
                    put("bills", 100)
                    put("coins", 50)
                }
            )
        }
        assertTrue(validator.validate(valid, "price", "$.price").isEmpty())

        val missing = buildJsonObject {}
        assertEquals(1, validator.validate(missing, "price", "$.price").size)

        val invalidType = buildJsonObject {
            put("price", 123)
        }
        assertEquals(1, validator.validate(invalidType, "price", "$.price").size)

        val invalidFields = buildJsonObject {
            put(
                "price",
                buildJsonObject {
                    put("bills", "string")
                    put("coins", -5)
                }
            )
        }
        assertEquals(2, validator.validate(invalidFields, "price", "$.price").size)
    }

    @Test
    fun testDateTimeValidator() {
        val validator = DateTimeValidator()
        val valid = buildJsonObject {
            put(
                "dt",
                buildJsonObject {
                    put(
                        "date",
                        buildJsonObject {
                            put("year", 2024)
                            put("month", 9)
                            put("day", 15)
                        }
                    )
                    put(
                        "time",
                        buildJsonObject {
                            put("hour", 12)
                            put("minute", 30)
                            put("second", 15)
                        }
                    )
                }
            )
        }
        assertTrue(validator.validate(valid, "dt", "$.dt").isEmpty())

        val missing = buildJsonObject {}
        assertEquals(1, validator.validate(missing, "dt", "$.dt").size)

        val missingParts = buildJsonObject {
            put("dt", buildJsonObject {})
        }
        assertEquals(2, validator.validate(missingParts, "dt", "$.dt").size)

        val invalidRanges = buildJsonObject {
            put(
                "dt",
                buildJsonObject {
                    put(
                        "date",
                        buildJsonObject {
                            put("year", 500)
                            put("month", 13)
                            put("day", 32)
                        }
                    )
                    put(
                        "time",
                        buildJsonObject {
                            put("hour", 25)
                            put("minute", 65)
                            put("second", 65)
                        }
                    )
                }
            )
        }
        assertEquals(6, validator.validate(invalidRanges, "dt", "$.dt").size)
    }

    @Test
    fun testOperatorValidator() {
        val validator = OperatorValidator()
        val valid = buildJsonObject {
            put(
                "operator",
                buildJsonObject {
                    put("code", 1)
                    put("name", "Кассир")
                }
            )
        }
        assertTrue(validator.validate(valid, "operator", "$.operator").isEmpty())

        val missing = buildJsonObject {}
        assertEquals(1, validator.validate(missing, "operator", "$.operator").size)

        val invalid = buildJsonObject {
            put(
                "operator",
                buildJsonObject {
                    put("code", "abc")
                    put("name", "")
                }
            )
        }
        assertEquals(2, validator.validate(invalid, "operator", "$.operator").size)
    }

    @Test
    fun testEnumValidators() {
        val enumContainer = buildJsonObject {
            put("itemType", "ITEM_TYPE_COMMODITY")
            put("reportType", "REPORT_Z")
            put("opType", "OPERATION_SELL")
            put("payType", "PAYMENT_CASH")
        }

        assertTrue(TicketItemTypeEnumValidator().validate(enumContainer, "itemType", "$.itemType").isEmpty())
        assertTrue(ReportTypeEnumValidator().validate(enumContainer, "reportType", "$.reportType").isEmpty())
        assertTrue(OperationTypeEnumValidator().validate(enumContainer, "opType", "$.opType").isEmpty())
        assertTrue(PaymentTypeEnumValidator().validate(enumContainer, "payType", "$.payType").isEmpty())
    }

    // --- 2. Service Component Validators & Builders ---

    @Test
    fun testServiceValidatorsAndBuilders() {
        val orgReg = buildJsonObject {
            put("title", "ИП")
            put("address", "Адрес")
            put("addressKz", "Адрес Кз")
            put("inn", "123456789012")
            put("okved", "12345")
        }
        val posReg = buildJsonObject {
            put("title", "Точка")
            put("address", "Адрес")
            put("addressKz", "Адрес Кз")
            put("latitude", 123)
            put("longitude", 456)
        }
        val kkmReg = buildJsonObject {
            put("fnsKkmId", "123")
            put("serialNumber", "456")
            put("kkmId", "789")
        }

        val errors = mutableListOf<ValidationError>()
        errors.addAll(OrgRegInfoValidator().validate(orgReg, "$.org"))
        errors.addAll(PosRegInfoValidator().validate(posReg, "$.pos"))
        errors.addAll(KkmRegInfoValidator().validate(kkmReg, "$.kkm"))
        assertTrue(errors.isEmpty())

        // Test builders
        val orgProto = OrgRegInfoBuilder().build(orgReg)
        assertEquals("ИП", orgProto.title)
        assertEquals("Адрес", orgProto.address)
        assertEquals("Адрес Кз", orgProto.addressKz)
        assertEquals("123456789012", orgProto.inn)
        assertEquals("12345", orgProto.okved)

        val kkmProto = KkmRegInfoBuilder().build(kkmReg)
        assertEquals("123", kkmProto.fnsKkmId)
        assertEquals("456", kkmProto.serialNumber)
        assertEquals("789", kkmProto.kkmId)

        val stats = buildJsonObject {
            put(
                "geoPosition",
                buildJsonObject {
                    put("latitude", 123)
                    put("longitude", 456)
                    put("source", "GPS")
                }
            )
        }
        val statsProto = SecurityStatsBuilder().build(stats)
        assertEquals(123, statsProto.geoPosition.latitude)
        assertEquals(456, statsProto.geoPosition.longitude)
        assertEquals("GPS", statsProto.geoPosition.source)

        // Invalid builder types
        assertFailsWith<IllegalArgumentException> { OrgRegInfoBuilder().build(buildJsonObject {}) }
        assertFailsWith<IllegalArgumentException> { KkmRegInfoBuilder().build(buildJsonObject {}) }
        assertFailsWith<IllegalArgumentException> { SecurityStatsBuilder().build(buildJsonObject {}) }
    }

    // --- 3. Ticket Component Validators & Builders ---

    @Test
    fun testTicketSubValidators() {
        val parent = buildJsonObject {
            put("parentTicketNumber", "123")
            put(
                "parentTicketDateTime",
                buildJsonObject {
                    put(
                        "date",
                        buildJsonObject {
                            put("year", 2024)
                            put("month", 9)
                            put("day", 1)
                        }
                    )
                    put(
                        "time",
                        buildJsonObject {
                            put("hour", 12)
                            put("minute", 0)
                        }
                    )
                }
            )
            put("kgdKkmId", "kgd123")
            put(
                "parentTicketTotal",
                buildJsonObject {
                    put("bills", 100)
                    put("coins", 0)
                }
            )
            put("parentTicketIsOffline", true)
        }
        val parentContainer = buildJsonObject { put("parent", parent) }
        val parentErrors = TicketParentTicketValidator().validate(parentContainer, "parent", "$.parent")
        assertTrue(parentErrors.isEmpty())

        val invalidParent = buildJsonObject {
            put("parentTicketNumber", "")
        }
        val invalidContainer = buildJsonObject { put("parent", invalidParent) }
        val parentErrors2 = TicketParentTicketValidator().validate(invalidContainer, "parent", "$.parent")
        assertTrue(parentErrors2.size > 0)

        // Amounts
        val amounts = buildJsonObject {
            put(
                "total",
                buildJsonObject {
                    put("bills", 100)
                    put("coins", 0)
                }
            )
            put(
                "taken",
                buildJsonObject {
                    put("bills", 100)
                    put("coins", 0)
                }
            )
            put(
                "change",
                buildJsonObject {
                    put("bills", 0)
                    put("coins", 0)
                }
            )
        }
        val amountsContainer = buildJsonObject { put("amounts", amounts) }
        val amountErrors = TicketAmountsValidator().validate(amountsContainer, "amounts", "$.amounts")
        assertTrue(amountErrors.isEmpty())

        // Payments
        val payment = buildJsonObject {
            put("type", "PAYMENT_CASH")
            put(
                "sum",
                buildJsonObject {
                    put("bills", 100)
                    put("coins", 0)
                }
            )
        }
        val paymentContainer = buildJsonObject { put("payment", payment) }
        val paymentErrors = TicketPaymentValidator().validate(paymentContainer, "payment", "$.payment")
        assertTrue(paymentErrors.isEmpty())

        // Taxes
        val tax = buildJsonObject {
            put("type", "TAX_VAT")
            put(
                "sum",
                buildJsonObject {
                    put("bills", 12)
                    put("coins", 0)
                }
            )
            put("percent", 12000) // 12%
            put("taxationType", 1)
            put("taxType", 1)
            put("isInTotalSum", true)
        }
        val taxContainer = buildJsonObject { put("tax", tax) }
        val taxErrors = TicketTaxValidator().validate(taxContainer, "tax", "$.tax")
        assertTrue(taxErrors.isEmpty())

        // Modifiers
        val modifier = buildJsonObject {
            put("type", "MODIFIER_DISCOUNT")
            put(
                "sum",
                buildJsonObject {
                    put("bills", 10)
                    put("coins", 0)
                }
            )
            put("name", "Скидка 10%")
            put("percent", 10000)
            put("taxationType", "TAXATION_TYPE_VAT")
        }
        val modifierContainer = buildJsonObject { put("modifier", modifier) }
        val modifierErrors = TicketModifierValidator().validate(modifierContainer, "modifier", "$.modifier")
        assertTrue(modifierErrors.isEmpty())

        // ExtensionOptions
        val ext = buildJsonObject {
            put("customerIinOrBin", "123456789012")
            put("customerEmail", "test@test.com")
            put("customerPhone", "+77771234567")
        }
        val extContainer = buildJsonObject { put("ext", ext) }
        val extErrors = TicketExtensionOptionsValidator().validate(extContainer, "ext", "$.ext")
        assertTrue(extErrors.isEmpty())

        // Items
        val items = buildJsonArray {
            add(
                buildJsonObject {
                    put("type", "ITEM_TYPE_COMMODITY")
                    put(
                        "commodity",
                        buildJsonObject {
                            put("name", "Товар")
                            put("sectionCode", "1")
                            put("quantity", 1000) // 1.000
                            put(
                                "price",
                                buildJsonObject {
                                    put("bills", 100)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "sum",
                                buildJsonObject {
                                    put("bills", 100)
                                    put("coins", 0)
                                }
                            )
                            put("measureUnitCode", "796")
                        }
                    )
                }
            )
        }
        val itemsContainer = buildJsonObject { put("items", items) }
        val itemErrors = TicketItemValidator().validateList(itemsContainer, "items", "$.items")
        assertTrue(itemErrors.isEmpty())
    }

    // --- 4. Request Validators ---

    @Test
    fun testRequestValidators() {
        val defaultReg = DefaultRegistry.create()
        val handler = defaultReg.find("kazakhtelecom", "203")!!

        val ticketPayload = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put(
                        "dateTime",
                        buildJsonObject {
                            put(
                                "date",
                                buildJsonObject {
                                    put("year", 2024)
                                    put("month", 9)
                                    put("day", 1)
                                }
                            )
                            put(
                                "time",
                                buildJsonObject {
                                    put("hour", 12)
                                    put("minute", 0)
                                }
                            )
                        }
                    )
                    put(
                        "operator",
                        buildJsonObject {
                            put("code", 1)
                            put("name", "Кассир")
                        }
                    )
                    put(
                        "items",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_COMMODITY")
                                    put(
                                        "commodity",
                                        buildJsonObject {
                                            put("name", "Товар")
                                            put("sectionCode", "1")
                                            put("quantity", 1000)
                                            put(
                                                "price",
                                                buildJsonObject {
                                                    put("bills", 100)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 100)
                                                    put("coins", 0)
                                                }
                                            )
                                            put("measureUnitCode", "796")
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put(
                        "payments",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "PAYMENT_CASH")
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 100)
                                            put("coins", 0)
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put(
                        "amounts",
                        buildJsonObject {
                            put(
                                "total",
                                buildJsonObject {
                                    put("bills", 100)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "taken",
                                buildJsonObject {
                                    put("bills", 100)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "change",
                                buildJsonObject {
                                    put("bills", 0)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
        }
        assertTrue(handler.requestValidator.validate(CommandType.COMMAND_TICKET, ticketPayload).isEmpty())

        // System
        val systemPayload = buildJsonObject {
            put("service", buildValidServiceJson())
        }
        assertTrue(handler.requestValidator.validate(CommandType.COMMAND_SYSTEM, systemPayload).isEmpty())

        // Info
        val infoPayload = buildJsonObject {
            put("service", buildValidServiceJson())
        }
        assertTrue(handler.requestValidator.validate(CommandType.COMMAND_INFO, infoPayload).isEmpty())

        // Close shift
        val closeShiftPayload = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "closeShift",
                buildJsonObject {
                    put(
                        "closeTime",
                        buildJsonObject {
                            put(
                                "date",
                                buildJsonObject {
                                    put("year", 2024)
                                    put("month", 9)
                                    put("day", 1)
                                }
                            )
                            put(
                                "time",
                                buildJsonObject {
                                    put("hour", 12)
                                    put("minute", 0)
                                }
                            )
                        }
                    )
                    put(
                        "zReport",
                        buildJsonObject {
                            put(
                                "dateTime",
                                buildJsonObject {
                                    put(
                                        "date",
                                        buildJsonObject {
                                            put("year", 2024)
                                            put("month", 9)
                                            put("day", 1)
                                        }
                                    )
                                    put(
                                        "time",
                                        buildJsonObject {
                                            put("hour", 12)
                                            put("minute", 0)
                                        }
                                    )
                                }
                            )
                            put("shiftNumber", 10)
                            put(
                                "cashSum",
                                buildJsonObject {
                                    put("bills", 1000L)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "revenue",
                                buildJsonObject {
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 1000L)
                                            put("coins", 0)
                                        }
                                    )
                                    put("isNegative", false)
                                }
                            )
                            put(
                                "openShiftTime",
                                buildJsonObject {
                                    put(
                                        "date",
                                        buildJsonObject {
                                            put("year", 2024)
                                            put("month", 9)
                                            put("day", 1)
                                        }
                                    )
                                    put(
                                        "time",
                                        buildJsonObject {
                                            put("hour", 9)
                                            put("minute", 0)
                                        }
                                    )
                                }
                            )
                            put(
                                "closeShiftTime",
                                buildJsonObject {
                                    put(
                                        "date",
                                        buildJsonObject {
                                            put("year", 2024)
                                            put("month", 9)
                                            put("day", 1)
                                        }
                                    )
                                    put(
                                        "time",
                                        buildJsonObject {
                                            put("hour", 12)
                                            put("minute", 0)
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put(
                        "operator",
                        buildJsonObject {
                            put("code", 1)
                            put("name", "Кассир")
                        }
                    )
                }
            )
        }
        assertTrue(handler.requestValidator.validate(CommandType.COMMAND_CLOSE_SHIFT, closeShiftPayload).isEmpty())

        // Money placement
        val moneyPlacementPayload = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "moneyPlacement",
                buildJsonObject {
                    put(
                        "dateTime",
                        buildJsonObject {
                            put(
                                "date",
                                buildJsonObject {
                                    put("year", 2024)
                                    put("month", 9)
                                    put("day", 1)
                                }
                            )
                            put(
                                "time",
                                buildJsonObject {
                                    put("hour", 12)
                                    put("minute", 0)
                                }
                            )
                        }
                    )
                    put("operation", "MONEY_PLACEMENT_DEPOSIT")
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 1000)
                            put("coins", 0)
                        }
                    )
                    put(
                        "operator",
                        buildJsonObject {
                            put("code", 1)
                            put("name", "Кассир")
                        }
                    )
                }
            )
        }
        assertTrue(
            handler.requestValidator.validate(CommandType.COMMAND_MONEY_PLACEMENT, moneyPlacementPayload).isEmpty()
        )

        // Nomenclature
        val nomenclaturePayload = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "nomenclature",
                buildJsonObject {
                    put("currentVersion", 123)
                }
            )
        }
        assertTrue(handler.requestValidator.validate(CommandType.COMMAND_NOMENCLATURE, nomenclaturePayload).isEmpty())

        // Reserved
        val reservedPayload = buildJsonObject {}
        assertTrue(handler.requestValidator.validate(CommandType.COMMAND_RESERVED, reservedPayload).isEmpty())
    }

    // --- 5. Response Validators ---

    @Test
    fun testResponseValidators() {
        // System
        val systemRes = buildJsonObject {
            put("commandType", "COMMAND_SYSTEM")
            put(
                "result",
                buildJsonObject {
                    put("resultCode", 0)
                }
            )
        }
        assertTrue(ResponseValidatorSystem().validate(CommandType.COMMAND_SYSTEM, systemRes).isEmpty())

        // Info
        val infoRes = buildJsonObject {
            put("commandType", "COMMAND_INFO")
            put(
                "result",
                buildJsonObject {
                    put("resultCode", 0)
                }
            )
        }
        assertTrue(ResponseValidatorInfo().validate(CommandType.COMMAND_INFO, infoRes).isEmpty())

        // Reserved
        val reservedRes = buildJsonObject {
            put("commandType", "COMMAND_RESERVED")
            put(
                "result",
                buildJsonObject {
                    put("resultCode", 0)
                }
            )
        }
        assertTrue(ResponseValidatorReserved().validate(CommandType.COMMAND_RESERVED, reservedRes).isEmpty())

        // Money Placement
        val moneyRes = buildJsonObject {
            put("commandType", "COMMAND_MONEY_PLACEMENT")
            put(
                "result",
                buildJsonObject {
                    put("resultCode", 0)
                }
            )
        }
        assertTrue(ResponseValidatorMoneyPlacement().validate(CommandType.COMMAND_MONEY_PLACEMENT, moneyRes).isEmpty())

        // Report
        val reportRes = buildJsonObject {
            put("commandType", "COMMAND_REPORT")
            put(
                "result",
                buildJsonObject {
                    put("resultCode", 0)
                }
            )
            put(
                "report",
                buildJsonObject {
                    put("reportType", "REPORT_Z")
                    put(
                        "zxReport",
                        buildJsonObject {
                            put(
                                "dateTime",
                                buildJsonObject {
                                    put(
                                        "date",
                                        buildJsonObject {
                                            put("year", 2024)
                                            put("month", 9)
                                            put("day", 1)
                                        }
                                    )
                                    put(
                                        "time",
                                        buildJsonObject {
                                            put("hour", 12)
                                            put("minute", 0)
                                        }
                                    )
                                }
                            )
                            put("shiftNumber", 10)
                            put(
                                "cashSum",
                                buildJsonObject {
                                    put("bills", 1000L)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "revenue",
                                buildJsonObject {
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 1000L)
                                            put("coins", 0)
                                        }
                                    )
                                    put("isNegative", false)
                                }
                            )
                            put(
                                "openShiftTime",
                                buildJsonObject {
                                    put(
                                        "date",
                                        buildJsonObject {
                                            put("year", 2024)
                                            put("month", 9)
                                            put("day", 1)
                                        }
                                    )
                                    put(
                                        "time",
                                        buildJsonObject {
                                            put("hour", 9)
                                            put("minute", 0)
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
        assertTrue(ResponseValidatorReport().validate(CommandType.COMMAND_REPORT, reportRes).isEmpty())

        // Close Shift
        val closeRes = buildJsonObject {
            put("commandType", "COMMAND_CLOSE_SHIFT")
            put(
                "result",
                buildJsonObject {
                    put("resultCode", 0)
                }
            )
            put(
                "report",
                buildJsonObject {
                    put("reportType", "REPORT_Z")
                    put(
                        "zxReport",
                        buildJsonObject {
                            put(
                                "dateTime",
                                buildJsonObject {
                                    put(
                                        "date",
                                        buildJsonObject {
                                            put("year", 2024)
                                            put("month", 9)
                                            put("day", 1)
                                        }
                                    )
                                    put(
                                        "time",
                                        buildJsonObject {
                                            put("hour", 12)
                                            put("minute", 0)
                                        }
                                    )
                                }
                            )
                            put("shiftNumber", 12)
                            put(
                                "cashSum",
                                buildJsonObject {
                                    put("bills", 1000L)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "revenue",
                                buildJsonObject {
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 1000L)
                                            put("coins", 0)
                                        }
                                    )
                                    put("isNegative", false)
                                }
                            )
                            put(
                                "openShiftTime",
                                buildJsonObject {
                                    put(
                                        "date",
                                        buildJsonObject {
                                            put("year", 2024)
                                            put("month", 9)
                                            put("day", 1)
                                        }
                                    )
                                    put(
                                        "time",
                                        buildJsonObject {
                                            put("hour", 9)
                                            put("minute", 0)
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
        assertTrue(ResponseValidatorCloseShift().validate(CommandType.COMMAND_CLOSE_SHIFT, closeRes).isEmpty())

        // Ticket
        val ticketRes = buildJsonObject {
            put("commandType", "COMMAND_TICKET")
            put(
                "result",
                buildJsonObject {
                    put("resultCode", 0)
                }
            )
            put(
                "ticket",
                buildJsonObject {
                    put("ticketNumber", "1")
                    put(
                        "dateTime",
                        buildJsonObject {
                            put(
                                "date",
                                buildJsonObject {
                                    put("year", 2024)
                                    put("month", 9)
                                    put("day", 1)
                                }
                            )
                            put(
                                "time",
                                buildJsonObject {
                                    put("hour", 12)
                                    put("minute", 0)
                                }
                            )
                        }
                    )
                    put("registrationNumber", "rnm123")
                }
            )
        }
        assertTrue(ResponseValidatorTicket().validate(CommandType.COMMAND_TICKET, ticketRes).isEmpty())

        // TicketAd / TicketAdInfo
        val ad = buildJsonObject {
            put("text", "Реклама")
            put(
                "info",
                buildJsonObject {
                    put("type", "AD_TYPE_TEXT")
                    put("version", 1L)
                }
            )
        }
        val adInfo = buildJsonObject {
            put("type", "AD_TYPE_TEXT")
            put("version", 1L)
        }
        assertTrue(TicketAdValidator().validate(ad, "$.ad").isEmpty())
        assertTrue(TicketAdInfoValidator().validate(adInfo, "$.adInfo").isEmpty())

        // Nomenclature response
        val nomRes = buildJsonObject {
            put("commandType", "COMMAND_NOMENCLATURE")
            put(
                "result",
                buildJsonObject {
                    put("resultCode", 0)
                }
            )
            put(
                "nomenclature",
                buildJsonObject {
                    put("version", 1)
                    put(
                        "result",
                        buildJsonObject {
                            put("code", 0)
                            put("name", "OK")
                        }
                    )
                    put(
                        "elements",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "ITEM")
                                    put("title", "Товар")
                                    put("id", 1L)
                                    put(
                                        "item",
                                        buildJsonObject {
                                            put("article", "art123")
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
        assertTrue(ResponseValidatorNomenclature().validate(CommandType.COMMAND_NOMENCLATURE, nomRes).isEmpty())
    }

    // --- 6. ZXReport Validators ---

    @Test
    fun testZXReportSubValidators() {
        val tax = buildJsonObject {
            put("taxType", 1)
            put("percent", 12000)
            put(
                "operations",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("operation", "OPERATION_SELL")
                            put(
                                "turnover",
                                buildJsonObject {
                                    put("bills", 1000L)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "sum",
                                buildJsonObject {
                                    put("bills", 120L)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "turnoverWithoutTax",
                                buildJsonObject {
                                    put("bills", 880L)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
        }

        val taxErrors = ZXReportTaxValidator().validate(tax, "$.tax")
        assertTrue(taxErrors.isEmpty())

        val revenue = buildJsonObject {
            put(
                "sum",
                buildJsonObject {
                    put("bills", 100L)
                    put("coins", 0)
                }
            )
            put("isNegative", false)
        }
        val revenueErrors = ZXReportRevenueValidator().validate(
            buildJsonObject { put("revenue", revenue) },
            "revenue",
            "$.revenue"
        )
        assertTrue(revenueErrors.isEmpty())

        val op = buildJsonObject {
            put("operation", "OPERATION_SELL")
            put("count", 10)
            put(
                "sum",
                buildJsonObject {
                    put("bills", 1000L)
                    put("coins", 0)
                }
            )
        }
        val opErrors = ZXReportOperationValidator().validate(op, "$.op")
        assertTrue(opErrors.isEmpty())

        val moneyPlace = buildJsonObject {
            put("operation", "MONEY_PLACEMENT_DEPOSIT")
            put("operationsTotalCount", 1)
            put("operationsCount", 1)
            put(
                "operationsSum",
                buildJsonObject {
                    put("bills", 100L)
                    put("coins", 0)
                }
            )
            put("offlineCount", 0)
        }
        val moneyPlaceErrors = ZXReportMoneyPlacementValidator().validate(moneyPlace, "$.moneyPlace")
        assertTrue(moneyPlaceErrors.isEmpty())

        val report = buildJsonObject {
            put(
                "dateTime",
                buildJsonObject {
                    put(
                        "date",
                        buildJsonObject {
                            put("year", 2024)
                            put("month", 9)
                            put("day", 1)
                        }
                    )
                    put(
                        "time",
                        buildJsonObject {
                            put("hour", 12)
                            put("minute", 0)
                        }
                    )
                }
            )
            put("shiftNumber", 10)
            put(
                "cashSum",
                buildJsonObject {
                    put("bills", 1000L)
                    put("coins", 0)
                }
            )
            put(
                "revenue",
                buildJsonObject {
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 1000L)
                            put("coins", 0)
                        }
                    )
                    put("isNegative", false)
                }
            )
            put(
                "openShiftTime",
                buildJsonObject {
                    put(
                        "date",
                        buildJsonObject {
                            put("year", 2024)
                            put("month", 9)
                            put("day", 1)
                        }
                    )
                    put(
                        "time",
                        buildJsonObject {
                            put("hour", 9)
                            put("minute", 0)
                        }
                    )
                }
            )
        }
        val reportErrors = ZXReportValidator().validate(buildJsonObject { put("report", report) }, "report", "$.report")
        assertTrue(reportErrors.isEmpty())
    }

    // --- 7. Builders Happy and Error Paths ---

    @Test
    fun testBuildersExhaustively() {
        val dtJson = buildJsonObject {
            put(
                "date",
                buildJsonObject {
                    put("year", 2024)
                    put("month", 9)
                    put("day", 1)
                }
            )
            put(
                "time",
                buildJsonObject {
                    put("hour", 10)
                    put("minute", 30)
                    put("second", 15)
                }
            )
        }
        val container = buildJsonObject { put("timeVal", dtJson) }
        val dtProto = DateTimeBuilder().build(container, "timeVal")
        assertEquals(2024, dtProto.date.year)
        assertEquals(15, dtProto.time.second)

        // CloseShift
        val csJson = buildJsonObject {
            put(
                "closeShift",
                buildJsonObject {
                    put("closeTime", dtJson)
                    put(
                        "zReport",
                        buildJsonObject {
                            put("dateTime", dtJson)
                            put("shiftNumber", 1)
                            put(
                                "cashSum",
                                buildJsonObject {
                                    put("bills", 1000L)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "revenue",
                                buildJsonObject {
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 1000L)
                                            put("coins", 0)
                                        }
                                    )
                                    put("isNegative", false)
                                }
                            )
                            put("openShiftTime", dtJson)
                        }
                    )
                    put(
                        "operator",
                        buildJsonObject {
                            put("code", 1)
                            put("name", "Кассир")
                        }
                    )
                }
            )
        }
        val csProto = CloseShiftRequestBuilder().build(csJson)
        assertEquals(2024, csProto.closeTime.date.year)

        // System
        val systemProto = CommandSystemRequestBuilder().build(buildJsonObject {})
        assertNotNull(systemProto)

        // Service JSON
        val serviceJson = buildJsonObject {
            put("getRegInfo", true)
            put(
                "offlinePeriod",
                buildJsonObject {
                    put("beginTime", dtJson)
                    put("endTime", dtJson)
                }
            )
            put(
                "securityStats",
                buildJsonObject {
                    put(
                        "geoPosition",
                        buildJsonObject {
                            put("latitude", 12)
                            put("longitude", 34)
                            put("source", "GPS")
                        }
                    )
                }
            )
            put(
                "regInfo",
                buildJsonObject {
                    put(
                        "kkm",
                        buildJsonObject {
                            put("fnsKkmId", "1")
                            put("serialNumber", "2")
                            put("kkmId", "3")
                        }
                    )
                    put(
                        "org",
                        buildJsonObject {
                            put(
                                "title",
                                "A"
                            )
                            put("address", "B")
                            put("addressKz", "C")
                            put("inn", "D")
                            put("okved", "E")
                        }
                    )
                }
            )
        }

        // Nomenclature
        val nomJson = buildJsonObject {
            put("service", serviceJson)
            put(
                "nomenclature",
                buildJsonObject {
                    put("currentVersion", 55)
                }
            )
        }
        val nomProto = CommandNomenclatureRequestBuilder().build(nomJson)
        assertEquals(55, nomProto.nomenclature.currentVersion)

        // Service Builder
        val serviceProto = ServiceRequestBuilder().build(nomJson)
        assertTrue(serviceProto.getRegInfo)
        assertEquals(12, serviceProto.securityStats.geoPosition.latitude)

        // Ticket
        val ticketJson = buildJsonObject {
            put("service", serviceJson)
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", dtJson)
                    put(
                        "operator",
                        buildJsonObject {
                            put("code", 1)
                            put("name", "K")
                        }
                    )
                    put(
                        "items",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_COMMODITY")
                                    put(
                                        "commodity",
                                        buildJsonObject {
                                            put("name", "T")
                                            put("sectionCode", "1")
                                            put("quantity", 1000)
                                            put(
                                                "price",
                                                buildJsonObject {
                                                    put("bills", 10L)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 10L)
                                                    put("coins", 0)
                                                }
                                            )
                                            put("measureUnitCode", "796")
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put(
                        "payments",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "PAYMENT_CASH")
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 10L)
                                            put("coins", 0)
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put(
                        "amounts",
                        buildJsonObject {
                            put(
                                "total",
                                buildJsonObject {
                                    put("bills", 10L)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "taken",
                                buildJsonObject {
                                    put("bills", 10L)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "change",
                                buildJsonObject {
                                    put("bills", 0L)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
        }
        val ticketProto = CommandTicketRequestBuilder().build(ticketJson)
        assertEquals(Common.OperationTypeEnum.OPERATION_SELL, ticketProto.ticket.operation)
    }

    // --- 8. Codecs, Parsers and Utility classes ---

    @Test
    fun testHeaderCodec() {
        val header = MessageHeader(
            appCode = 0x81A2,
            protocolVersion = 203,
            size = 50,
            deviceId = 12345,
            token = 67890L,
            reqNum = 5
        )
        val bytes = HeaderCodec.encode(header, 32)
        assertEquals(18, bytes.size)

        val decodedResult = HeaderCodec.decode(bytes)
        assertTrue(decodedResult is HeaderDecodeResult.Success)
        val decoded = (decodedResult as HeaderDecodeResult.Success).header
        assertEquals(header.appCode, decoded.appCode)
        assertEquals(header.protocolVersion, decoded.protocolVersion)
        assertEquals(50, decoded.size)
        assertEquals(header.deviceId, decoded.deviceId)
        assertEquals(header.token, decoded.token)
        assertEquals(header.reqNum, decoded.reqNum)

        // Errors
        val tooShort = ByteArray(10)
        assertTrue(HeaderCodec.decode(tooShort) is HeaderDecodeResult.Errors)

        val wrongSignature = bytes.copyOf()
        wrongSignature[0] = 0x00
        wrongSignature[1] = 0x00
        assertTrue(HeaderCodec.decode(wrongSignature) is HeaderDecodeResult.Errors)

        val wrongSize = bytes.copyOf()
        wrongSize[4] = 0x00
        wrongSize[5] = 0x00
        wrongSize[6] = 0x00
        wrongSize[7] = 0x00
        assertTrue(HeaderCodec.decode(wrongSize) is HeaderDecodeResult.Errors)
    }

    @Test
    fun testOfdCodecEdgeCases() {
        val codec = OfdCodec(DefaultRegistry.create())

        // Unsupported protocol/ofdId
        val unsupportedJson = buildJsonObject {
            put("ofdId", "invalidOfd")
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
        }
        val res1 = codec.encode(unsupportedJson)
        assertTrue(res1.isFailure)

        // Unsupported message type
        val unsupportedMsgType = buildJsonObject {
            put("ofdId", "kazakhtelecom")
            put("protocolVersion", "203")
            put("messageType", "RESPONSE")
        }
        val res2 = codec.encode(unsupportedMsgType)
        assertTrue(res2.isFailure)

        // Decode: size mismatch
        val bytes = ByteArray(100)
        bytes[0] = 0xA2.toByte()
        bytes[1] = 0x81.toByte()
        bytes[2] = 0xCB.toByte() // CB -> 203
        bytes[3] = 0x00.toByte()
        // Size field at index 4-7: little endian. Let's make it 200 bytes.
        bytes[4] = 200.toByte()
        bytes[5] = 0
        bytes[6] = 0
        bytes[7] = 0

        val res3 = codec.decode(bytes)
        assertTrue(res3.isFailure)
        val ex3 = res3.exceptionOrNull() as OfdCodecException
        assertEquals(ErrorCode.HEADER_INVALID_SIZE.name, ex3.errors.first().code)
    }

    @Test
    fun testValidationFailurePaths() {
        val emptyObj = buildJsonObject {}
        val invalidType = buildJsonObject { put("key", 123) }

        // Enum Validators
        assertTrue(TicketItemTypeEnumValidator().validate(emptyObj, "key", "$.key").isNotEmpty())
        assertTrue(TicketItemTypeEnumValidator().validate(invalidType, "key", "$.key").isNotEmpty())
        assertTrue(
            TicketItemTypeEnumValidator().validate(
                buildJsonObject { put("key", "INVALID") },
                "key",
                "$.key"
            ).isNotEmpty()
        )

        assertTrue(ReportTypeEnumValidator().validate(emptyObj, "key", "$.key").isNotEmpty())
        assertTrue(ReportTypeEnumValidator().validate(invalidType, "key", "$.key").isNotEmpty())
        assertTrue(
            ReportTypeEnumValidator().validate(buildJsonObject { put("key", "INVALID") }, "key", "$.key").isNotEmpty()
        )

        assertTrue(OperationTypeEnumValidator().validate(emptyObj, "key", "$.key").isNotEmpty())
        assertTrue(OperationTypeEnumValidator().validate(invalidType, "key", "$.key").isNotEmpty())
        assertTrue(
            OperationTypeEnumValidator().validate(
                buildJsonObject { put("key", "INVALID") },
                "key",
                "$.key"
            ).isNotEmpty()
        )

        assertTrue(PaymentTypeEnumValidator().validate(emptyObj, "key", "$.key").isNotEmpty())
        assertTrue(PaymentTypeEnumValidator().validate(invalidType, "key", "$.key").isNotEmpty())
        assertTrue(
            PaymentTypeEnumValidator().validate(buildJsonObject { put("key", "INVALID") }, "key", "$.key").isNotEmpty()
        )

        // MoneyValidator
        assertTrue(MoneyValidator().validate(emptyObj, "key", "$.key").isNotEmpty())
        assertTrue(MoneyValidator().validate(invalidType, "key", "$.key").isNotEmpty())
        val moneyBadBills = buildJsonObject {
            put(
                "key",
                buildJsonObject {
                    put("bills", "abc")
                    put("coins", 0)
                }
            )
        }
        val moneyBadCoins = buildJsonObject {
            put(
                "key",
                buildJsonObject {
                    put("bills", 0L)
                    put("coins", -5)
                }
            )
        }
        assertTrue(MoneyValidator().validate(moneyBadBills, "key", "$.key").isNotEmpty())
        assertTrue(MoneyValidator().validate(moneyBadCoins, "key", "$.key").isNotEmpty())

        // DateTimeValidator
        assertTrue(DateTimeValidator().validate(emptyObj, "key", "$.key").isNotEmpty())
        assertTrue(DateTimeValidator().validate(invalidType, "key", "$.key").isNotEmpty())
        val dtBadDate = buildJsonObject {
            put(
                "key",
                buildJsonObject {
                    put("date", 123)
                    put(
                        "time",
                        buildJsonObject {
                            put("hour", 12)
                            put("minute", 0)
                        }
                    )
                }
            )
        }
        val dtBadTime = buildJsonObject {
            put(
                "key",
                buildJsonObject {
                    put(
                        "date",
                        buildJsonObject {
                            put("year", 2024)
                            put("month", 9)
                            put("day", 1)
                        }
                    )
                    put("time", 123)
                }
            )
        }
        assertTrue(DateTimeValidator().validate(dtBadDate, "key", "$.key").isNotEmpty())
        assertTrue(DateTimeValidator().validate(dtBadTime, "key", "$.key").isNotEmpty())

        // OperatorValidator
        assertTrue(OperatorValidator().validate(emptyObj, "key", "$.key").isNotEmpty())
        assertTrue(OperatorValidator().validate(invalidType, "key", "$.key").isNotEmpty())
        val opBadCode = buildJsonObject {
            put(
                "key",
                buildJsonObject {
                    put("code", "abc")
                    put("name", "K")
                }
            )
        }
        val opBadName = buildJsonObject {
            put(
                "key",
                buildJsonObject {
                    put("code", 1)
                    put("name", 123)
                }
            )
        }
        assertTrue(OperatorValidator().validate(opBadCode, "key", "$.key").isNotEmpty())
        assertTrue(OperatorValidator().validate(opBadName, "key", "$.key").isNotEmpty())

        // ZXReportTaxValidator
        assertTrue(ZXReportTaxValidator().validateList(emptyObj, "key", "$.key").isEmpty())
        assertTrue(
            ZXReportTaxValidator().validateList(buildJsonObject { put("key", 123) }, "key", "$.key").isNotEmpty()
        )
        assertTrue(
            ZXReportTaxValidator().validateList(
                buildJsonObject { put("key", buildJsonArray { add(123) }) },
                "key",
                "$.key"
            ).isNotEmpty()
        )
        val taxBadPercent = buildJsonObject {
            put("taxType", 1)
            put("percent", "abc")
            put("operations", buildJsonArray {})
        }
        assertTrue(ZXReportTaxValidator().validate(taxBadPercent, "$.tax").isNotEmpty())
        val taxBadOps = buildJsonObject {
            put("taxType", 1)
            put("percent", 12000)
            put("operations", 123)
        }
        assertTrue(ZXReportTaxValidator().validate(taxBadOps, "$.tax").isNotEmpty())

        // ZXReportTaxOperationValidator
        assertTrue(ZXReportTaxOperationValidator().validateList(emptyObj, "key", "$.key").isEmpty())
        assertTrue(
            ZXReportTaxOperationValidator().validateList(
                buildJsonObject { put("key", 123) },
                "key",
                "$.key"
            ).isNotEmpty()
        )
        assertTrue(
            ZXReportTaxOperationValidator().validateList(
                buildJsonObject { put("key", buildJsonArray { add(123) }) },
                "key",
                "$.key"
            ).isNotEmpty()
        )

        // ZXReportOperationValidator
        assertTrue(ZXReportOperationValidator().validateList(emptyObj, "key", "$.key").isEmpty())
        assertTrue(
            ZXReportOperationValidator().validateList(buildJsonObject { put("key", 123) }, "key", "$.key").isNotEmpty()
        )
        assertTrue(
            ZXReportOperationValidator().validateList(
                buildJsonObject { put("key", buildJsonArray { add(123) }) },
                "key",
                "$.key"
            ).isNotEmpty()
        )

        // ZXReportTicketPaymentValidator
        assertTrue(ZXReportTicketPaymentValidator().validateList(emptyObj, "key", "$.key").isEmpty())
        assertTrue(
            ZXReportTicketPaymentValidator().validateList(
                buildJsonObject { put("key", 123) },
                "key",
                "$.key"
            ).isNotEmpty()
        )
        assertTrue(
            ZXReportTicketPaymentValidator().validateList(
                buildJsonObject { put("key", buildJsonArray { add(123) }) },
                "key",
                "$.key"
            ).isNotEmpty()
        )

        // ZXReportTicketOperationValidator
        assertTrue(ZXReportTicketOperationValidator().validateList(emptyObj, "key", "$.key").isEmpty())
        assertTrue(
            ZXReportTicketOperationValidator().validateList(
                buildJsonObject { put("key", 123) },
                "key",
                "$.key"
            ).isNotEmpty()
        )
        assertTrue(
            ZXReportTicketOperationValidator().validateList(
                buildJsonObject { put("key", buildJsonArray { add(123) }) },
                "key",
                "$.key"
            ).isNotEmpty()
        )
        val ticketOpBadPayments = buildJsonObject {
            put("operation", "OPERATION_SELL")
            put("ticketsTotalCount", 1)
            put("ticketsCount", 1)
            put(
                "ticketsSum",
                buildJsonObject {
                    put("bills", 100L)
                    put("coins", 0)
                }
            )
            put("payments", 123)
        }
        assertTrue(ZXReportTicketOperationValidator().validate(ticketOpBadPayments, "$.op").isNotEmpty())

        // ZXReportNonNullableSumValidator
        assertTrue(ZXReportNonNullableSumValidator().validateList(emptyObj, "key", "$.key").isEmpty())
        assertTrue(
            ZXReportNonNullableSumValidator().validateList(
                buildJsonObject { put("key", 123) },
                "key",
                "$.key"
            ).isNotEmpty()
        )
        assertTrue(
            ZXReportNonNullableSumValidator().validateList(
                buildJsonObject { put("key", buildJsonArray { add(123) }) },
                "key",
                "$.key"
            ).isNotEmpty()
        )

        // ZXReportMoneyPlacementValidator
        assertTrue(ZXReportMoneyPlacementValidator().validateList(emptyObj, "key", "$.key").isEmpty())
        assertTrue(
            ZXReportMoneyPlacementValidator().validateList(
                buildJsonObject { put("key", 123) },
                "key",
                "$.key"
            ).isNotEmpty()
        )
        assertTrue(
            ZXReportMoneyPlacementValidator().validateList(
                buildJsonObject { put("key", buildJsonArray { add(123) }) },
                "key",
                "$.key"
            ).isNotEmpty()
        )

        // ServiceRequestValidator
        assertTrue(ServiceRequestValidator().validate(emptyObj, "$.service").isNotEmpty())
        val serviceBadOffline = buildJsonObject {
            put("getRegInfo", true)
            put("offlinePeriod", 123)
        }
        assertTrue(ServiceRequestValidator().validate(serviceBadOffline, "$.service").isNotEmpty())

        // SecurityStatsValidator
        val secBadGeo = buildJsonObject { put("geoPosition", 123) }
        assertTrue(SecurityStatsValidator().validate(secBadGeo, "$.sec").isNotEmpty())

        // TicketExtensionOptionsValidator
        assertTrue(TicketExtensionOptionsValidator().validate(emptyObj, "key", "$.key").isNotEmpty())
        assertTrue(
            TicketExtensionOptionsValidator().validate(buildJsonObject { put("key", 123) }, "key", "$.key").isNotEmpty()
        )
        assertTrue(
            TicketExtensionOptionsValidator().validate(
                buildJsonObject { put("key", buildJsonObject { put("customerEmail", 123) }) },
                "key",
                "$.key"
            ).isNotEmpty()
        )

        // TicketParentTicketValidator
        assertTrue(TicketParentTicketValidator().validate(emptyObj, "key", "$.key").isNotEmpty())
        val parentBadOffline = buildJsonObject {
            put(
                "key",
                buildJsonObject {
                    put("parentTicketNumber", "1")
                    put(
                        "parentTicketDateTime",
                        buildJsonObject {
                            put(
                                "date",
                                buildJsonObject {
                                    put("year", 2024)
                                    put("month", 9)
                                    put("day", 1)
                                }
                            )
                            put(
                                "time",
                                buildJsonObject {
                                    put("hour", 12)
                                    put("minute", 0)
                                }
                            )
                        }
                    )
                    put("kgdKkmId", "123")
                    put(
                        "parentTicketTotal",
                        buildJsonObject {
                            put("bills", 100L)
                            put("coins", 0)
                        }
                    )
                    put("parentTicketIsOffline", 123)
                }
            )
        }
        assertTrue(TicketParentTicketValidator().validate(parentBadOffline, "key", "$.key").isNotEmpty())

        // TicketPaymentValidator
        assertTrue(TicketPaymentValidator().validate(emptyObj, "key", "$.key").isNotEmpty())
        val payBadSum = buildJsonObject {
            put(
                "key",
                buildJsonObject {
                    put("type", "PAYMENT_CASH")
                    put("sum", 123)
                }
            )
        }
        assertTrue(TicketPaymentValidator().validate(payBadSum, "key", "$.key").isNotEmpty())

        // TicketAmountsValidator
        assertTrue(TicketAmountsValidator().validate(emptyObj, "key", "$.key").isNotEmpty())

        // TicketTaxValidator
        assertTrue(TicketTaxValidator().validate(emptyObj, "key", "$.key").isNotEmpty())
        val taxBadIsInTotal = buildJsonObject {
            put(
                "key",
                buildJsonObject {
                    put("taxType", 1)
                    put("percent", 12000)
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 12L)
                            put("coins", 0)
                        }
                    )
                    put("isInTotalSum", 123)
                }
            )
        }
        assertTrue(TicketTaxValidator().validate(taxBadIsInTotal, "key", "$.key").isNotEmpty())

        // TicketItemValidator
        assertTrue(TicketItemValidator().validateList(emptyObj, "key", "$.key").isEmpty())
        assertTrue(TicketItemValidator().validateList(buildJsonObject { put("key", 123) }, "key", "$.key").isNotEmpty())
        val itemBadObj = buildJsonObject {
            put("key", buildJsonArray { add(123) })
        }
        assertTrue(TicketItemValidator().validateList(itemBadObj, "key", "$.key").isNotEmpty())
        val itemBadTaxes = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
            put(
                "commodity",
                buildJsonObject {
                    put("name", "T")
                    put("sectionCode", "1")
                    put("quantity", 1L)
                    put(
                        "price",
                        buildJsonObject {
                            put("bills", 10L)
                            put("coins", 0)
                        }
                    )
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 10L)
                            put("coins", 0)
                        }
                    )
                    put("measureUnitCode", "796")
                    put("taxes", 123)
                }
            )
        }
        assertTrue(TicketItemValidator().validate(itemBadTaxes, "$.item").isNotEmpty())
        val itemBadTaxesDup = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
            put(
                "commodity",
                buildJsonObject {
                    put("name", "T")
                    put("sectionCode", "1")
                    put("quantity", 1L)
                    put(
                        "price",
                        buildJsonObject {
                            put("bills", 10L)
                            put("coins", 0)
                        }
                    )
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 10L)
                            put("coins", 0)
                        }
                    )
                    put("measureUnitCode", "796")
                    put(
                        "taxes",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put(
                                        "taxType",
                                        1
                                    )
                                    put(
                                        "percent",
                                        12000
                                    )
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 1L)
                                            put("coins", 0)
                                        }
                                    )
                                    put("isInTotalSum", true)
                                }
                            )
                            add(
                                buildJsonObject {
                                    put(
                                        "taxType",
                                        1
                                    )
                                    put(
                                        "percent",
                                        12000
                                    )
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 1L)
                                            put("coins", 0)
                                        }
                                    )
                                    put("isInTotalSum", true)
                                }
                            )
                        }
                    )
                }
            )
        }
        assertTrue(TicketItemValidator().validate(itemBadTaxesDup, "$.item").isNotEmpty())
        val itemBadExcise = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
            put(
                "commodity",
                buildJsonObject {
                    put("name", "T")
                    put("sectionCode", "1")
                    put("quantity", 1L)
                    put(
                        "price",
                        buildJsonObject {
                            put("bills", 10L)
                            put("coins", 0)
                        }
                    )
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 10L)
                            put("coins", 0)
                        }
                    )
                    put("measureUnitCode", "796")
                    put("listExciseStamp", 123)
                }
            )
        }
        assertTrue(TicketItemValidator().validate(itemBadExcise, "$.item").isNotEmpty())
        val itemBadExciseItem = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
            put(
                "commodity",
                buildJsonObject {
                    put("name", "T")
                    put("sectionCode", "1")
                    put("quantity", 1L)
                    put(
                        "price",
                        buildJsonObject {
                            put("bills", 10L)
                            put("coins", 0)
                        }
                    )
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 10L)
                            put("coins", 0)
                        }
                    )
                    put("measureUnitCode", "796")
                    put("listExciseStamp", buildJsonArray { add(123) })
                }
            )
        }
        assertTrue(TicketItemValidator().validate(itemBadExciseItem, "$.item").isNotEmpty())

        // TicketModifierValidator
        assertTrue(TicketModifierValidator().validate(emptyObj, "key", "$.key").isNotEmpty())
        val modBadTaxes = buildJsonObject {
            put(
                "key",
                buildJsonObject {
                    put("name", "M")
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 10L)
                            put("coins", 0)
                        }
                    )
                    put("taxes", 123)
                }
            )
        }
        assertTrue(TicketModifierValidator().validate(modBadTaxes, "key", "$.key").isNotEmpty())

        // RequestValidatorTicket
        val tickBadPayments = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put(
                        "dateTime",
                        buildJsonObject {
                            put(
                                "date",
                                buildJsonObject {
                                    put("year", 2024)
                                    put("month", 9)
                                    put("day", 1)
                                }
                            )
                            put(
                                "time",
                                buildJsonObject {
                                    put("hour", 12)
                                    put("minute", 0)
                                }
                            )
                        }
                    )
                    put(
                        "operator",
                        buildJsonObject {
                            put("code", 1)
                            put("name", "K")
                        }
                    )
                    put(
                        "items",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_COMMODITY")
                                    put(
                                        "commodity",
                                        buildJsonObject {
                                            put("name", "T")
                                            put("sectionCode", "1")
                                            put("quantity", 1L)
                                            put(
                                                "price",
                                                buildJsonObject {
                                                    put("bills", 10L)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 10L)
                                                    put("coins", 0)
                                                }
                                            )
                                            put("measureUnitCode", "796")
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put("payments", 123)
                }
            )
        }
        assertTrue(RequestValidatorTicket().validate(CommandType.COMMAND_TICKET, tickBadPayments).isNotEmpty())
        val tickBadPaymentsDup = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put(
                        "dateTime",
                        buildJsonObject {
                            put(
                                "date",
                                buildJsonObject {
                                    put("year", 2024)
                                    put("month", 9)
                                    put("day", 1)
                                }
                            )
                            put(
                                "time",
                                buildJsonObject {
                                    put("hour", 12)
                                    put("minute", 0)
                                }
                            )
                        }
                    )
                    put(
                        "operator",
                        buildJsonObject {
                            put("code", 1)
                            put("name", "K")
                        }
                    )
                    put(
                        "items",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_COMMODITY")
                                    put(
                                        "commodity",
                                        buildJsonObject {
                                            put("name", "T")
                                            put("sectionCode", "1")
                                            put("quantity", 1L)
                                            put(
                                                "price",
                                                buildJsonObject {
                                                    put("bills", 10L)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 10L)
                                                    put("coins", 0)
                                                }
                                            )
                                            put("measureUnitCode", "796")
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put(
                        "payments",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put(
                                        "type",
                                        "PAYMENT_CASH"
                                    )
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 10L)
                                            put("coins", 0)
                                        }
                                    )
                                }
                            )
                            add(
                                buildJsonObject {
                                    put(
                                        "type",
                                        "PAYMENT_CASH"
                                    )
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 10L)
                                            put("coins", 0)
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put(
                        "amounts",
                        buildJsonObject {
                            put(
                                "total",
                                buildJsonObject {
                                    put("bills", 20L)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
        }
        assertTrue(RequestValidatorTicket().validate(CommandType.COMMAND_TICKET, tickBadPaymentsDup).isNotEmpty())

        // ResponseValidatorTicket
        assertTrue(ResponseValidatorTicket().validate(CommandType.COMMAND_TICKET, emptyObj).isNotEmpty())
        val resTickBadTicket = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put("ticket", 123)
        }
        assertTrue(ResponseValidatorTicket().validate(CommandType.COMMAND_TICKET, resTickBadTicket).isNotEmpty())
        val resTickBadService = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put("ticket", buildJsonObject { put("ticketNumber", "1") })
            put("service", 123)
        }
        assertTrue(ResponseValidatorTicket().validate(CommandType.COMMAND_TICKET, resTickBadService).isNotEmpty())

        // ResponseValidatorCloseShift
        assertTrue(ResponseValidatorCloseShift().validate(CommandType.COMMAND_CLOSE_SHIFT, emptyObj).isNotEmpty())
        val resCsBadType = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "report",
                buildJsonObject {
                    put("reportType", "REPORT_X")
                }
            )
        }
        assertTrue(ResponseValidatorCloseShift().validate(CommandType.COMMAND_CLOSE_SHIFT, resCsBadType).isNotEmpty())
        val resCsBadService = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "report",
                buildJsonObject {
                    put("reportType", "REPORT_Z")
                    put(
                        "zxReport",
                        buildJsonObject {
                            put(
                                "dateTime",
                                buildJsonObject {
                                    put(
                                        "date",
                                        buildJsonObject {
                                            put("year", 2024)
                                            put("month", 9)
                                            put("day", 1)
                                        }
                                    )
                                    put(
                                        "time",
                                        buildJsonObject {
                                            put("hour", 12)
                                            put("minute", 0)
                                        }
                                    )
                                }
                            )
                            put("shiftNumber", 10)
                            put(
                                "cashSum",
                                buildJsonObject {
                                    put("bills", 1000L)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "revenue",
                                buildJsonObject {
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 1000L)
                                            put("coins", 0)
                                        }
                                    )
                                    put("isNegative", false)
                                }
                            )
                            put(
                                "openShiftTime",
                                buildJsonObject {
                                    put(
                                        "date",
                                        buildJsonObject {
                                            put("year", 2024)
                                            put("month", 9)
                                            put("day", 1)
                                        }
                                    )
                                    put(
                                        "time",
                                        buildJsonObject {
                                            put("hour", 9)
                                            put("minute", 0)
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
            put("service", 123)
        }
        assertTrue(
            ResponseValidatorCloseShift().validate(CommandType.COMMAND_CLOSE_SHIFT, resCsBadService).isNotEmpty()
        )

        // Builders exception paths
        val dtJson = buildJsonObject {
            put(
                "date",
                buildJsonObject {
                    put("year", 2024)
                    put("month", 9)
                    put("day", 1)
                }
            )
            put(
                "time",
                buildJsonObject {
                    put("hour", 10)
                    put("minute", 30)
                    put("second", 15)
                }
            )
        }
        assertFailsWith<IllegalArgumentException> { DateTimeBuilder().build(emptyObj, "key") }
        assertFailsWith<IllegalArgumentException> { ZXReportBuilder().build(emptyObj) }
        assertFailsWith<IllegalArgumentException> { CloseShiftRequestBuilder().build(emptyObj) }
        assertFailsWith<IllegalArgumentException> {
            CloseShiftRequestBuilder().build(
                buildJsonObject {
                    put(
                        "closeShift",
                        buildJsonObject {
                            put(
                                "closeTime",
                                buildJsonObject {
                                    put(
                                        "date",
                                        buildJsonObject {
                                            put("year", 2024)
                                            put("month", 9)
                                            put("day", 1)
                                        }
                                    )
                                }
                            )
                            put("zReport", emptyObj)
                        }
                    )
                }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            ServiceRequestBuilder().build(
                buildJsonObject { put("service", emptyObj) }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            ServiceRequestBuilder().build(
                buildJsonObject { put("service", buildJsonObject { put("getRegInfo", true) }) }
            )
        }
        assertFailsWith<IllegalArgumentException> { TicketRequestBuilder().build(emptyObj) }
        assertFailsWith<IllegalArgumentException> { NomenclatureRequestBuilder().build(emptyObj) }
        assertFailsWith<IllegalArgumentException> { ReportRequestBuilder().build(emptyObj) }

        // JsonMessageMapper parseEnvelope failure paths
        val badVersionEnv = buildJsonObject {
            put("ofdId", "kazakhtelecom")
            put("protocolVersion", "abc")
            put("messageType", "REQUEST")
        }
        assertTrue(JsonMessageMapper.parseEnvelope(badVersionEnv).second.isNotEmpty())

        val badHeaderEnv = buildJsonObject {
            put("ofdId", "kazakhtelecom")
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put("header", 123)
        }
        assertTrue(JsonMessageMapper.parseEnvelope(badHeaderEnv).second.isNotEmpty())

        val missingHeaderEnv = buildJsonObject {
            put("ofdId", "kazakhtelecom")
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put("header", buildJsonObject {})
        }
        assertTrue(JsonMessageMapper.parseEnvelope(missingHeaderEnv).second.isNotEmpty())

        val badHeaderTypeEnv = buildJsonObject {
            put("ofdId", "kazakhtelecom")
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put(
                "header",
                buildJsonObject {
                    put("deviceId", "abc")
                    put("token", 123)
                    put("reqNum", 1)
                }
            )
        }
        assertTrue(JsonMessageMapper.parseEnvelope(badHeaderTypeEnv).second.isNotEmpty())

        // OfdCodec multi ofd resolving error path
        val multiOfdRegistry = OfdRegistry()
        val dummyValidator = object : Validator {
            override fun validate(commandType: CommandType, json: JsonObject) = emptyList<ValidationError>()
        }
        val dummySerializer = object : Serializer {
            override fun serialize(commandType: CommandType, json: JsonObject) = ByteArray(0)
        }
        val dummyDeserializer = object : Deserializer {
            override fun deserialize(bytes: ByteArray) = buildJsonObject {}
        }
        multiOfdRegistry.register(
            OfdProtocolHandler("ofd1", "203", dummyValidator, dummySerializer, dummyValidator, dummyDeserializer)
        )
        multiOfdRegistry.register(
            OfdProtocolHandler("ofd2", "203", dummyValidator, dummySerializer, dummyValidator, dummyDeserializer)
        )
        val multiCodec = OfdCodec(multiOfdRegistry)
        val header = MessageHeader(0x81A2, 203, 18, 1, 2, 3)
        val bytes = HeaderCodec.encode(header, 0)
        val res = multiCodec.decode(bytes)
        assertTrue(res.isFailure)
    }

    @Test
    fun testProtocolVersion() {
        assertEquals("203", ProtocolVersion.toNumericString(203))
        assertEquals(203, ProtocolVersion.parseNumeric("203"))
        assertNull(ProtocolVersion.parseNumeric("invalid"))
    }
}
