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
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.closeshift.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.common.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.moneyplacement.*
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
import java.util.Base64
import kotlin.test.*

class AdditionalCoverageTest {

    private val emptyObj = buildJsonObject {}

    @Test
    fun testKazakhtelecomV203ModuleCustomOfd() {
        val handler = KazakhtelecomV203Module.defaultHandler("customOfd")
        assertEquals("customOfd", handler.ofdId)
    }

    @Test
    fun testRequestSerializerUnsupportedCommand() {
        val serializer = KazakhtelecomV203RequestSerializer(emptyMap())
        assertFailsWith<IllegalArgumentException> {
            serializer.serialize(CommandType.COMMAND_TICKET, emptyObj)
        }
    }

    @Test
    fun testOfdRegistrySupportedVersions() {
        val registry = OfdRegistry()
        assertTrue(registry.supportedVersions("nonexistent").isEmpty())
    }

    @Test
    fun testOfdCodecDecodeEdgeCases() {
        val registry = DefaultRegistry.create()
        val codec = OfdCodec(registry)

        // 1. Invalid size error path (header.size > bytes.size)
        // Header with correct size (say 50) but passing 18 bytes
        val header = MessageHeader(
            appCode = HeaderConstants.APPCODE,
            protocolVersion = 203,
            size = 50, // size is 50
            deviceId = 123,
            token = 456,
            reqNum = 1
        )
        val headerBytes = HeaderCodec.encode(header, 32)
        val res1 = codec.decode(headerBytes) // bytes size is 18, header.size is 50
        assertTrue(res1.isFailure)
        val ex1 = res1.exceptionOrNull() as? OfdCodecException
        assertNotNull(ex1)
        assertTrue(ex1.errors.any { it.code == ErrorCode.HEADER_INVALID_SIZE.name })

        // 2. Undetermined OFD: registry has no handlers registered, so resolver returns null
        val emptyRegistry = OfdRegistry()
        val emptyCodec = OfdCodec(emptyRegistry)
        val validHeader = MessageHeader(
            appCode = HeaderConstants.APPCODE,
            protocolVersion = 203,
            size = 18,
            deviceId = 123,
            token = 456,
            reqNum = 1
        )
        val validHeaderBytes = HeaderCodec.encode(validHeader, 0)
        val res2 = emptyCodec.decode(validHeaderBytes)
        assertTrue(res2.isFailure)
        val ex2 = res2.exceptionOrNull() as? OfdCodecException
        assertNotNull(ex2)
        assertTrue(ex2.errors.any { it.code == ErrorCode.MESSAGE_UNDETERMINED_OFD.name })

        // 3. Deserialization failed (invalid protobuf payload)
        // Valid header but corrupt/random payload bytes
        val headerWithPayload = MessageHeader(
            appCode = HeaderConstants.APPCODE,
            protocolVersion = 203,
            size = 28,
            deviceId = 123,
            token = 456,
            reqNum = 1
        )
        val hBytes = HeaderCodec.encode(headerWithPayload, 10)
        val fullBytes = ByteArray(28)
        System.arraycopy(hBytes, 0, fullBytes, 0, 18)
        // Corrupt payload bytes (all 0xFF which represents invalid protobuf tags)
        for (i in 18 until 28) {
            fullBytes[i] = 0xFF.toByte()
        }
        val res3 = codec.decode(fullBytes)
        assertTrue(res3.isFailure)
        val ex3 = res3.exceptionOrNull() as? OfdCodecException
        assertNotNull(ex3)
        assertTrue(ex3.errors.any { it.code == ErrorCode.DESERIALIZATION_FAILED.name })
    }

    @Test
    fun testHeaderCodecInvalidVersionsAndSizes() {
        // AppCode correct, version = 0, size = 10
        val bytes = ByteArray(18)
        // AppCode: 0x81A2
        bytes[0] = 0xA2.toByte()
        bytes[1] = 0x81.toByte()
        // Version: 0
        bytes[2] = 0
        bytes[3] = 0
        // Size: 10
        bytes[4] = 10
        bytes[5] = 0
        bytes[6] = 0
        bytes[7] = 0

        val decodeResult = HeaderCodec.decode(bytes)
        assertTrue(decodeResult is HeaderDecodeResult.Errors)
        val errors = decodeResult.errors
        assertTrue(errors.any { it.code == ErrorCode.HEADER_INVALID_VERSION_FORMAT.name })
        assertTrue(errors.any { it.code == ErrorCode.HEADER_INVALID_SIZE.name })
    }

    @Test
    fun testJsonExtensionsAllPaths() {
        val obj = buildJsonObject {
            put("str", "hello")
            put("int", 123)
            put("bool", true)
            put("long", 99999999999L)
            put("nullVal", JsonNull)
            put("arr", buildJsonArray { add(1) })
            put("obj", buildJsonObject {})
            put(
                "objList",
                buildJsonArray {
                    add(buildJsonObject { put("x", 1) })
                    add(123)
                }
            )
        }

        // Test fallback paths (returning null or throwing)
        assertNull(obj.readString("nonexistent"))
        assertNull(obj.readString("int")) // not a string primitive
        assertFailsWith<IllegalArgumentException> { obj.readStringRequired("nonexistent") }

        assertNull(obj.readInt("nonexistent"))
        assertNull(obj.readInt("str"))
        assertFailsWith<IllegalArgumentException> { obj.readIntRequired("nonexistent") }

        assertNull(obj.readLong("nonexistent"))
        assertNull(obj.readLong("str"))
        assertFailsWith<IllegalArgumentException> { obj.readLongRequired("nonexistent") }

        assertNull(obj.readBool("nonexistent"))
        assertNull(obj.readBool("str"))
        assertFailsWith<IllegalArgumentException> { obj.readBoolRequired("nonexistent") }

        assertNull(obj.readObject("nonexistent"))
        assertNull(obj.readObject("str"))
        assertFailsWith<IllegalArgumentException> { obj.readObjectRequired("nonexistent") }

        assertNull(obj.readArray("nonexistent"))
        assertNull(obj.readArray("str"))
        assertFailsWith<IllegalArgumentException> { obj.readArrayRequired("nonexistent") }

        assertNull(obj.readObjectList("nonexistent"))
        assertNull(obj.readObjectList("str"))
        val list = obj.readObjectList("objList")
        assertNotNull(list)
        assertEquals(1, list.size)

        // test requireObject
        val element: JsonElement = JsonPrimitive("not_obj")
        assertFailsWith<IllegalArgumentException> { element.requireObject("testKey") }

        // test readStringElement
        val notPrimitive: JsonElement = buildJsonObject {}
        assertFailsWith<IllegalArgumentException> { notPrimitive.readStringElement() }
        val numPrimitive: JsonElement = JsonPrimitive(123)
        assertFailsWith<IllegalArgumentException> { numPrimitive.readStringElement() }
    }

    @Test
    fun testJsonMessageMapperErrorPaths() {
        // 1. MessageType.valueOf throwing IllegalArgumentException (invalid messageType value)
        val badMsgType = buildJsonObject {
            put("ofdId", "kazakhtelecom")
            put("protocolVersion", "203")
            put("messageType", "INVALID_TYPE")
            put("commandType", "COMMAND_TICKET")
            put(
                "header",
                buildJsonObject {
                    put("deviceId", 123)
                    put("token", 456)
                    put("reqNum", 1)
                }
            )
            put("payload", buildJsonObject {})
        }
        val (_, errs1) = JsonMessageMapper.parseEnvelope(badMsgType)
        assertTrue(errs1.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.messageType" })

        // 2. CommandType.fromName(value) == null (unsupported commandType)
        val badCmdType = buildJsonObject {
            put("ofdId", "kazakhtelecom")
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put("commandType", "COMMAND_UNKNOWN_STUFF")
            put(
                "header",
                buildJsonObject {
                    put("deviceId", 123)
                    put("token", 456)
                    put("reqNum", 1)
                }
            )
            put("payload", buildJsonObject {})
        }
        val (_, errs2) = JsonMessageMapper.parseEnvelope(badCmdType)
        assertTrue(errs2.any { it.code == ErrorCode.COMMAND_UNSUPPORTED.name && it.path == "$.commandType" })

        // 3. element !is JsonObject for payload
        val badPayloadType = buildJsonObject {
            put("ofdId", "kazakhtelecom")
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put("commandType", "COMMAND_TICKET")
            put(
                "header",
                buildJsonObject {
                    put("deviceId", 123)
                    put("token", 456)
                    put("reqNum", 1)
                }
            )
            put("payload", 123)
        }
        val (_, errs3) = JsonMessageMapper.parseEnvelope(badPayloadType)
        assertTrue(errs3.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload" })

        // 4. element !is JsonPrimitive || !element.isString for readString (e.g. ofdId is object)
        val badOfdIdType = buildJsonObject {
            put("ofdId", buildJsonObject {})
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put("commandType", "COMMAND_TICKET")
            put(
                "header",
                buildJsonObject {
                    put("deviceId", 123)
                    put("token", 456)
                    put("reqNum", 1)
                }
            )
            put("payload", buildJsonObject {})
        }
        val (_, errs4) = JsonMessageMapper.parseEnvelope(badOfdIdType)
        assertTrue(errs4.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.ofdId" })

        // 5. element.longOrNull == null for readLong
        val badDeviceIdVal = buildJsonObject {
            put("ofdId", "kazakhtelecom")
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put("commandType", "COMMAND_TICKET")
            put(
                "header",
                buildJsonObject {
                    put("deviceId", buildJsonObject {})
                    put("token", 456)
                    put("reqNum", 1)
                }
            )
            put("payload", buildJsonObject {})
        }
        val (_, errs5) = JsonMessageMapper.parseEnvelope(badDeviceIdVal)
        assertTrue(errs5.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.deviceId" })
    }

    @Test
    fun testTicketPaymentValidatorAllPaths() {
        val validator = TicketPaymentValidator()

        // 1. Missing cardPaymentFields and mobilePaymentFields invalid type
        val p1 = buildJsonObject {
            put("type", "PAYMENT_CARD")
            put(
                "sum",
                buildJsonObject {
                    put("bills", 100)
                    put("coins", 0)
                }
            )
            put("cardPaymentFields", 123)
            put("mobilePaymentFields", "invalid")
        }
        val errs1 = validator.validateObject(p1, "$.payment")
        assertTrue(
            errs1.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payment.cardPaymentFields" }
        )
        assertTrue(
            errs1.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payment.mobilePaymentFields" }
        )

        // 2. Valid cardPaymentFields and mobilePaymentFields, testing positive ranges
        val p2 = buildJsonObject {
            put("type", "PAYMENT_CARD")
            put(
                "sum",
                buildJsonObject {
                    put("bills", 100)
                    put("coins", 0)
                }
            )
            put(
                "cardPaymentFields",
                buildJsonObject {
                    put("posTerminalId", "TERM123")
                    put("posCardType", "VISA")
                    put("posAutorizationCode", 123456)
                    put("posRrn", 99999999999L)
                    put("posReceiptNumber", 12)
                }
            )
            put(
                "mobilePaymentFields",
                buildJsonObject {
                    put("qrType", "QR1")
                    put("qrId", "QRID123")
                }
            )
        }
        val errs2 = validator.validateObject(p2, "$.payment")
        assertTrue(errs2.isEmpty())

        // 3. Testing negative/invalid ranges inside cardPaymentFields
        val p3 = buildJsonObject {
            put("type", "PAYMENT_CARD")
            put(
                "sum",
                buildJsonObject {
                    put("bills", 100)
                    put("coins", 0)
                }
            )
            put(
                "cardPaymentFields",
                buildJsonObject {
                    put("posTerminalId", " ")
                    put("posCardType", "")
                    put("posAutorizationCode", -1)
                    put("posRrn", -1L)
                    put("posReceiptNumber", -5)
                }
            )
        }
        val errs3 = validator.validateObject(p3, "$.payment")
        assertTrue(errs3.size >= 5)

        // 4. Missing payment in container
        val errs4 = validator.validate(buildJsonObject {}, "someKey", "$.pay")
        assertTrue(errs4.any { it.code == ErrorCode.JSON_MISSING_FIELD.name })
    }

    @Test
    fun testTicketItemValidatorAllPaths() {
        val validator = TicketItemValidator()

        // 1. Missing name and code for commodity
        val item1 = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
            put(
                "commodity",
                buildJsonObject {
                    put("sectionCode", "1")
                    put("quantity", 1)
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
        val errs1 = validator.validate(item1, "$.item")
        assertTrue(errs1.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.item.commodity.code" })

        // 2. Blank name for commodity
        val item2 = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
            put(
                "commodity",
                buildJsonObject {
                    put("name", "   ")
                    put("sectionCode", "1")
                    put("quantity", 1)
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
        val errs2 = validator.validate(item2, "$.item")
        assertTrue(errs2.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.item.commodity.name" })

        // 3. Storno commodity validation path
        val item3 = buildJsonObject {
            put("type", "ITEM_TYPE_STORNO_COMMODITY")
            put(
                "stornoCommodity",
                buildJsonObject {
                    put("name", "   ")
                    put("sectionCode", "1")
                    put("quantity", 1)
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
        val errs3 = validator.validate(item3, "$.item")
        assertTrue(
            errs3.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.item.stornoCommodity.name" }
        )

        // 4. Storno commodity missing block
        val item4 = buildJsonObject {
            put("type", "ITEM_TYPE_STORNO_COMMODITY")
        }
        val errs4 = validator.validate(item4, "$.item")
        assertTrue(errs4.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.item.stornoCommodity" })

        // 5. Modifier items (markup, discount, stornoMarkup, stornoDiscount)
        val item5 = buildJsonObject {
            put("type", "ITEM_TYPE_MARKUP")
            put(
                "markup",
                buildJsonObject {
                    put("name", "Extra")
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
        val errs5 = validator.validate(item5, "$.item")
        assertTrue(errs5.isEmpty())

        val item6 = buildJsonObject {
            put("type", "ITEM_TYPE_STORNO_MARKUP")
            put(
                "stornoMarkup",
                buildJsonObject {
                    put("name", "Extra")
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
        val errs6 = validator.validate(item6, "$.item")
        assertTrue(errs6.isEmpty())

        val item7 = buildJsonObject {
            put("type", "ITEM_TYPE_DISCOUNT")
            put(
                "discount",
                buildJsonObject {
                    put("name", "Sale")
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
        val errs7 = validator.validate(item7, "$.item")
        assertTrue(errs7.isEmpty())

        val item8 = buildJsonObject {
            put("type", "ITEM_TYPE_STORNO_DISCOUNT")
            put(
                "stornoDiscount",
                buildJsonObject {
                    put("name", "Sale")
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
        val errs8 = validator.validate(item8, "$.item")
        assertTrue(errs8.isEmpty())

        // 6. Commodity taxes invalid type and duplicates
        val item9 = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
            put(
                "commodity",
                buildJsonObject {
                    put("name", "Item")
                    put("sectionCode", "1")
                    put("quantity", 1)
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
                    put("taxes", 123)
                }
            )
        }
        val errs9 = validator.validate(item9, "$.item")
        assertTrue(errs9.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.item.commodity.taxes" })

        val item10 = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
            put(
                "commodity",
                buildJsonObject {
                    put("name", "Item")
                    put("sectionCode", "1")
                    put("quantity", 1)
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
                    put(
                        "taxes",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("percent", 1200)
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 10)
                                            put("coins", 0)
                                        }
                                    )
                                }
                            )
                            add(
                                buildJsonObject {
                                    put("percent", 1200)
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 10)
                                            put("coins", 0)
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
        val errs10 = validator.validate(item10, "$.item")
        assertTrue(errs10.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.item.commodity.taxes" })

        // 7. Commodity listExciseStamp invalid type and invalid element
        val item11 = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
            put(
                "commodity",
                buildJsonObject {
                    put("name", "Item")
                    put("sectionCode", "1")
                    put("quantity", 1)
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
                    put("listExciseStamp", 123)
                }
            )
        }
        val errs11 = validator.validate(item11, "$.item")
        assertTrue(
            errs11.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.item.commodity.listExciseStamp" }
        )

        val item12 = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
            put(
                "commodity",
                buildJsonObject {
                    put("name", "Item")
                    put("sectionCode", "1")
                    put("quantity", 1)
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
                    put(
                        "listExciseStamp",
                        buildJsonArray {
                            add("   ")
                        }
                    )
                }
            )
        }
        val errs12 = validator.validate(item12, "$.item")
        assertTrue(
            errs12.any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.item.commodity.listExciseStamp[0]"
            }
        )
    }

    @Test
    fun testTicketModifierValidatorAllPaths() {
        val validator = TicketModifierValidator()
        // Taxes not an array
        val m1 = buildJsonObject {
            put("name", "Mod")
            put(
                "sum",
                buildJsonObject {
                    put("bills", 10)
                    put("coins", 0)
                }
            )
            put("taxes", 123)
        }
        val errs1 = validator.validate(buildJsonObject { put("mod", m1) }, "mod", "$.mod")
        assertTrue(errs1.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.mod.taxes" })

        // Taxes duplicate percent
        val m2 = buildJsonObject {
            put("name", "Mod")
            put(
                "sum",
                buildJsonObject {
                    put("bills", 10)
                    put("coins", 0)
                }
            )
            put(
                "taxes",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("percent", 1200)
                            put(
                                "sum",
                                buildJsonObject {
                                    put("bills", 1)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                    add(
                        buildJsonObject {
                            put("percent", 1200)
                            put(
                                "sum",
                                buildJsonObject {
                                    put("bills", 1)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
        }
        val errs2 = validator.validate(buildJsonObject { put("mod", m2) }, "mod", "$.mod")
        assertTrue(errs2.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.mod.taxes" })
    }

    @Test
    fun testTicketAmountsValidatorAllPaths() {
        val validator = TicketAmountsValidator()
        // total missing
        val errs1 = validator.validate(buildJsonObject { put("amounts", buildJsonObject {}) }, "amounts", "$.amounts")
        assertTrue(errs1.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.amounts.total" })
    }

    @Test
    fun testTicketTaxValidatorAllPaths() {
        val validator = TicketTaxValidator()
        // Missing tax percent in container
        val errs1 = validator.validate(
            buildJsonObject {
                put(
                    "tax",
                    buildJsonObject {
                        put(
                            "sum",
                            buildJsonObject {
                                put("bills", 1)
                                put("coins", 0)
                            }
                        )
                    }
                )
            },
            "tax",
            "$.tax"
        )
        assertTrue(errs1.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.tax.percent" })
    }

    @Test
    fun testRequestValidatorTicketAllPaths() {
        val validator = RequestValidatorTicket()

        // 1. Missing service or ticket
        assertTrue(
            validator.validate(
                CommandType.COMMAND_TICKET,
                buildJsonObject {
                    put("ticket", emptyObj)
                }
            ).any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.service" }
        )
        assertTrue(
            validator.validate(
                CommandType.COMMAND_TICKET,
                buildJsonObject {
                    put("service", emptyObj)
                }
            ).any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.ticket" }
        )

        // 2. Ticket items missing, invalid type, empty
        val payload1 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_TICKET, payload1).any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.ticket.items"
            }
        )

        val payload2 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", 123)
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_TICKET, payload2).any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.ticket.items"
            }
        )

        // 3. Duplicate payments
        val payload3 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
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
                                    put("bills", 200)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "taken",
                                buildJsonObject {
                                    put("bills", 200)
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
        assertTrue(
            validator.validate(CommandType.COMMAND_TICKET, payload3).any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.ticket.payments"
            }
        )

        // 4. Invalid payments type (not array)
        val payload4 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
                    put("payments", 123)
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_TICKET, payload4).any {
                it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.ticket.payments"
            }
        )

        // 5. Invalid taxes type (not array)
        val payload5 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
                    put("taxes", 123)
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_TICKET, payload5).any {
                it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.ticket.taxes"
            }
        )

        // 6. Duplicate taxes percent
        val payload6 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
                    put(
                        "taxes",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("percent", 1200)
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 10)
                                            put("coins", 0)
                                        }
                                    )
                                }
                            )
                            add(
                                buildJsonObject {
                                    put("percent", 1200)
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 10)
                                            put("coins", 0)
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_TICKET, payload6).any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.ticket.taxes"
            }
        )

        // 7. Invalid extensionOptions type (not object)
        val payload7 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
                    put("extensionOptions", 123)
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_TICKET, payload7).any {
                it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.ticket.extensionOptions"
            }
        )

        // 8. Negative integers (offlineTicketNumber, frShiftNumber, shiftDocumentNumber, printedDocumentNumber)
        val payload8 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
                    put("offlineTicketNumber", -1)
                    put("frShiftNumber", -1)
                    put("shiftDocumentNumber", -1)
                    put("printedDocumentNumber", -1L)
                }
            )
        }
        val errs8 = validator.validate(CommandType.COMMAND_TICKET, payload8)
        assertTrue(errs8.any { it.path == "$.payload.ticket.offlineTicketNumber" })
        assertTrue(errs8.any { it.path == "$.payload.ticket.frShiftNumber" })
        assertTrue(errs8.any { it.path == "$.payload.ticket.shiftDocumentNumber" })
        assertTrue(errs8.any { it.path == "$.payload.ticket.printedDocumentNumber" })

        // 9. Invalid parentTicket type
        val payload9 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
                    put("parentTicket", 123)
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_TICKET, payload9).any {
                it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.ticket.parentTicket"
            }
        )

        // 10. Operation return but parentTicket null
        val payload10 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL_RETURN")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
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
                        }
                    )
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_TICKET, payload10).any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.ticket.parentTicket"
            }
        )

        // 11. Both ticket-level and item-level taxes present
        val payload11 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildItemWithTaxesJson()) })
                    put(
                        "taxes",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("percent", 1200)
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 10)
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
                        }
                    )
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_TICKET, payload11).any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.ticket.taxes"
            }
        )

        // 12. Both markup and discount present in amounts
        val payload12 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
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
                                "markup",
                                buildJsonObject {
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "discount",
                                buildJsonObject {
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_TICKET, payload12).any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.ticket.amounts"
            }
        )
    }

    @Test
    fun testRequestValidatorMoneyPlacementAllPaths() {
        val validator = RequestValidatorMoneyPlacement()
        // Missing service or moneyPlacement
        assertTrue(
            validator.validate(
                CommandType.COMMAND_MONEY_PLACEMENT,
                buildJsonObject {
                    put("moneyPlacement", emptyObj)
                }
            ).any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.service" }
        )
        assertTrue(
            validator.validate(
                CommandType.COMMAND_MONEY_PLACEMENT,
                buildJsonObject {
                    put("service", emptyObj)
                }
            ).any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.moneyPlacement" }
        )

        // Invalid operation type or value
        val p1 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "moneyPlacement",
                buildJsonObject {
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 100)
                            put("coins", 0)
                        }
                    )
                    put("operation", 123)
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_MONEY_PLACEMENT, p1).any {
                it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.moneyPlacement.operation"
            }
        )

        val p2 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "moneyPlacement",
                buildJsonObject {
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 100)
                            put("coins", 0)
                        }
                    )
                    put("operation", "INVALID_OP")
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_MONEY_PLACEMENT, p2).any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.moneyPlacement.operation"
            }
        )

        // Invalid isOffline type
        val p3 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "moneyPlacement",
                buildJsonObject {
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 100)
                            put("coins", 0)
                        }
                    )
                    put("operation", "MONEY_PLACEMENT_DEPOSIT")
                    put("isOffline", 123)
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_MONEY_PLACEMENT, p3).any {
                it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.moneyPlacement.isOffline"
            }
        )
    }

    @Test
    fun testRequestValidatorCloseShiftAllPaths() {
        val validator = RequestValidatorCloseShift()
        // Missing service or closeShift
        assertTrue(
            validator.validate(
                CommandType.COMMAND_CLOSE_SHIFT,
                buildJsonObject {
                    put("closeShift", emptyObj)
                }
            ).any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.service" }
        )
        assertTrue(
            validator.validate(
                CommandType.COMMAND_CLOSE_SHIFT,
                buildJsonObject {
                    put("service", emptyObj)
                }
            ).any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.closeShift" }
        )

        // Invalid isOffline or withdrawMoney type
        val p1 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "closeShift",
                buildJsonObject {
                    put("closeTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("zReport", buildValidZReportJson())
                    put("isOffline", 123)
                    put("withdrawMoney", "invalid")
                }
            )
        }
        val errs = validator.validate(CommandType.COMMAND_CLOSE_SHIFT, p1)
        assertTrue(
            errs.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.closeShift.isOffline" }
        )
        assertTrue(
            errs.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.closeShift.withdrawMoney" }
        )
    }

    @Test
    fun testRequestValidatorNomenclatureAllPaths() {
        val validator = RequestValidatorNomenclature()
        // Missing service or nomenclature
        assertTrue(
            validator.validate(
                CommandType.COMMAND_NOMENCLATURE,
                buildJsonObject {
                    put("nomenclature", emptyObj)
                }
            ).any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.service" }
        )
        assertTrue(
            validator.validate(
                CommandType.COMMAND_NOMENCLATURE,
                buildJsonObject {
                    put("service", emptyObj)
                }
            ).any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.nomenclature" }
        )

        // Neither currentVersion nor barcode present
        val p1 = buildJsonObject {
            put("service", buildValidServiceJson())
            put("nomenclature", emptyObj)
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_NOMENCLATURE, p1).any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.nomenclature"
            }
        )
    }

    @Test
    fun testRequestValidatorSystemAndInfoAllPaths() {
        assertTrue(
            RequestValidatorSystem().validate(CommandType.COMMAND_SYSTEM, emptyObj).any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.service"
            }
        )
        assertTrue(
            RequestValidatorInfo().validate(CommandType.COMMAND_INFO, emptyObj).any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.service"
            }
        )
    }

    @Test
    fun testRequestValidatorReportAllPaths() {
        val validator = RequestValidatorReport()
        // Missing service or report
        assertTrue(
            validator.validate(
                CommandType.COMMAND_REPORT,
                buildJsonObject {
                    put("report", emptyObj)
                }
            ).any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.service" }
        )
        assertTrue(
            validator.validate(
                CommandType.COMMAND_REPORT,
                buildJsonObject {
                    put("service", emptyObj)
                }
            ).any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.report" }
        )

        // Invalid isOffline type
        val p1 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "report",
                buildJsonObject {
                    put("reportType", "REPORT_Z")
                    put("dateTime", buildValidDateTimeJson())
                    put("zxReport", buildValidZReportJson())
                    put("isOffline", 123)
                }
            )
        }
        assertTrue(
            validator.validate(CommandType.COMMAND_REPORT, p1).any {
                it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.report.isOffline"
            }
        )
    }

    @Test
    fun testResponseValidatorsAllPaths() {
        // 1. Missing result
        assertTrue(
            ResponseValidatorReport().validate(CommandType.COMMAND_REPORT, emptyObj).any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.result"
            }
        )

        // 2. Success result code (0) but missing specific response blocks
        val rSuccess = buildJsonObject {
            put(
                "result",
                buildJsonObject {
                    put("resultCode", 0)
                    put("resultText", "Success")
                }
            )
        }
        assertTrue(
            ResponseValidatorReport().validate(CommandType.COMMAND_REPORT, rSuccess).any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.report"
            }
        )
        assertTrue(
            ResponseValidatorTicket().validate(CommandType.COMMAND_TICKET, rSuccess).any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.ticket"
            }
        )
        assertTrue(
            ResponseValidatorNomenclature().validate(CommandType.COMMAND_NOMENCLATURE, rSuccess).any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.nomenclature"
            }
        )
        assertTrue(
            ResponseValidatorCloseShift().validate(CommandType.COMMAND_CLOSE_SHIFT, rSuccess).any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.report"
            }
        )

        assertTrue(ResponseValidatorMoneyPlacement().validate(CommandType.COMMAND_MONEY_PLACEMENT, rSuccess).isEmpty())
        assertTrue(ResponseValidatorSystem().validate(CommandType.COMMAND_SYSTEM, rSuccess).isEmpty())
        assertTrue(ResponseValidatorInfo().validate(CommandType.COMMAND_INFO, rSuccess).isEmpty())

        // 3. ResponseValidatorCloseShift error paths
        val rc1 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "report",
                buildJsonObject {
                    put("reportType", "REPORT_X")
                }
            )
        }
        assertTrue(
            ResponseValidatorCloseShift().validate(CommandType.COMMAND_CLOSE_SHIFT, rc1).any {
                it.path == "$.payload.report.reportType"
            }
        )

        // 4. ResponseValidatorNomenclature error paths (createdTime type, parentGroupId negative, measureFractional type, boolean types)
        val rn1 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "nomenclature",
                buildJsonObject {
                    put("version", 1)
                    put("createdTime", 123)
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
                                    put("title", "Name")
                                    put("id", 123)
                                    put("parentGroupId", -1)
                                    put(
                                        "item",
                                        buildJsonObject {
                                            put("taxes", 123)
                                            put("measureFractional", 123)
                                            put("isMarkedeac", "not_a_bool")
                                            put("isSocial", "not_a_bool")
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
        val errsN = ResponseValidatorNomenclature().validate(CommandType.COMMAND_NOMENCLATURE, rn1)
        assertTrue(errsN.any { it.path == "$.payload.nomenclature.createdTime" })
        assertTrue(errsN.any { it.path == "$.payload.nomenclature.elements[0].parentGroupId" })
        assertTrue(errsN.any { it.path == "$.payload.nomenclature.elements[0].item.taxes" })
        assertTrue(errsN.any { it.path == "$.payload.nomenclature.elements[0].item.measureFractional" })
        assertTrue(errsN.any { it.path == "$.payload.nomenclature.elements[0].item.isMarkedeac" })
        assertTrue(errsN.any { it.path == "$.payload.nomenclature.elements[0].item.isSocial" })
    }

    @Test
    fun testZXReportSectionAndTicketPaymentValidators() {
        val sectionVal = ZXReportSectionValidator()
        // validateList invalid types
        val errs1 = sectionVal.validateList(buildJsonObject { put("sections", 123) }, "sections", "$.sections")
        assertTrue(errs1.any { it.code == ErrorCode.JSON_INVALID_TYPE.name })

        val errs2 = sectionVal.validateList(
            buildJsonObject {
                put("sections", buildJsonArray { add(123) })
            },
            "sections",
            "$.sections"
        )
        assertTrue(errs2.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.sections[0]" })

        val payVal = ZXReportTicketPaymentValidator()
        val errs3 = payVal.validateList(buildJsonObject { put("payments", 123) }, "payments", "$.payments")
        assertTrue(errs3.any { it.code == ErrorCode.JSON_INVALID_TYPE.name })

        val errs4 = payVal.validateList(
            buildJsonObject {
                put("payments", buildJsonArray { add(123) })
            },
            "payments",
            "$.payments"
        )
        assertTrue(errs4.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payments[0]" })
    }

    @Test
    fun testReportDeserializationAndHelpers() {
        val zxReport = createFullZXReport()
        val reportResponse = ReportResponse(
            report = ReportTypeEnum.REPORT_Z,
            zx_report = zxReport
        )

        val message = Response(
            command = CommandTypeEnum.COMMAND_REPORT,
            result = Result(result_code = 0, result_text = "Success"),
            report = reportResponse
        )

        val bytes = Response.ADAPTER.encode(message)
        val deserializer = KazakhtelecomV203ResponseDeserializer()
        val json = deserializer.deserialize(bytes)

        assertEquals("COMMAND_REPORT", json["commandType"]?.jsonPrimitive?.content)
        val reportJson = json["report"]?.jsonObject
        assertNotNull(reportJson)
        assertEquals("REPORT_Z", reportJson["reportType"]?.jsonPrimitive?.content)

        val zxReportJson = reportJson["zxReport"]?.jsonObject
        assertNotNull(zxReportJson)
        assertEquals(42, zxReportJson["shiftNumber"]?.jsonPrimitive?.int)
        assertEquals(12345, zxReportJson["checksum"]?.jsonPrimitive?.int)
    }

    @Test
    fun testNomenclatureDeserialization() {
        val element = NomenclatureResponse.Element(
            type = NomenclatureResponse.ElementTypeEnum.ITEM,
            title = "Title",
            title_kk = "TitleKk",
            parent_group_id = 456,
            id = 123,
            item = NomenclatureResponse.Item(
                article = "Art",
                barcode = "Bar",
                description = "Desc",
                purchase_price = Money(bills = 10, coins = 0),
                sell_price = Money(bills = 12, coins = 0),
                discount_percent = 10,
                discount_sum = Money(bills = 1, coins = 0),
                markup_percent = 5,
                markup_sum = Money(bills = 1, coins = 0),
                taxes = listOf(
                    NomenclatureResponse.Tax(
                        taxation_type = NomenclatureResponse.TaxationTypeEnum.RTS,
                        tax_type = NomenclatureResponse.TaxTypeEnum.VAT,
                        tax_percent = 1200
                    )
                ),
                measure_count = 1,
                measure_title = "pcs",
                measure_fractional = false,
                measure_unit_code = "796",
                ntin = "Ntin123",
                is_markedeac = true,
                is_social = false
            )
        )

        val nomenclatureResponse = NomenclatureResponse(
            version = 1,
            created_time = DateTime(
                date = Date(year = 2024, month = 9, day = 1),
                time = Time(hour = 12, minute = 0)
            ),
            elements = listOf(element),
            result = NomenclatureResponse.NomenclatureResultTypeEnum.RESULT_TYPE_OK
        )

        val message = Response(
            command = CommandTypeEnum.COMMAND_NOMENCLATURE,
            result = Result(result_code = 0),
            nomenclature = nomenclatureResponse
        )

        val bytes = Response.ADAPTER.encode(message)
        val json = KazakhtelecomV203ResponseDeserializer().deserialize(bytes)
        assertNotNull(json["nomenclature"])
    }

    private fun createFullZXReport(): ZXReport {
        val date = Date(year = 2024, month = 9, day = 1)
        val time = Time(hour = 12, minute = 5, second = 30)
        val dateTime = DateTime(date = date, time = time)
        val money = Money(bills = 100L, coins = 50)

        val section = ZXReport.Section(
            section_code = "SEC01",
            operations = listOf(
                ZXReport.Operation(
                    operation = OperationTypeEnum.OPERATION_SELL,
                    count = 5,
                    sum = money
                )
            )
        )

        val operation = ZXReport.Operation(
            operation = OperationTypeEnum.OPERATION_SELL,
            count = 10,
            sum = money
        )

        val taxOp = ZXReport.Tax.TaxOperation(
            operation = OperationTypeEnum.OPERATION_SELL,
            turnover = money,
            sum = money,
            turnover_without_tax = money
        )

        val tax = ZXReport.Tax(
            tax_type = 1,
            percent = 1200,
            operations = listOf(taxOp)
        )

        val nonNullableSum = ZXReport.NonNullableSum(
            operation = OperationTypeEnum.OPERATION_SELL,
            sum = money
        )

        val zxPayment = ZXReport.TicketOperation.Payment(
            payment = PaymentTypeEnum.PAYMENT_CARD,
            sum = money,
            count = 2
        )

        val ticketOp = ZXReport.TicketOperation(
            operation = OperationTypeEnum.OPERATION_SELL,
            tickets_total_count = 10,
            tickets_count = 8,
            tickets_sum = money,
            payments = listOf(zxPayment),
            offline_count = 1,
            discount_sum = money,
            markup_sum = money,
            change_sum = money
        )

        val moneyPlacement = ZXReport.MoneyPlacement(
            operation = MoneyPlacementEnum.MONEY_PLACEMENT_DEPOSIT,
            operations_total_count = 3,
            operations_count = 3,
            operations_sum = money,
            offline_count = 0
        )

        val annulledTickets = ZXReport.AnnulledTickets(
            annulled_tickets_total_count = 2,
            annulled_tickets_count = 2,
            annulled_operations = listOf(operation)
        )

        val revenue = ZXReport.Revenue(
            sum = money,
            is_negative = false
        )

        return ZXReport(
            date_time = dateTime,
            shift_number = 42,
            sections = listOf(section),
            operations = listOf(operation),
            discounts = listOf(operation),
            markups = listOf(operation),
            total_result = listOf(operation),
            taxes = listOf(tax),
            start_shift_non_nullable_sums = listOf(nonNullableSum),
            ticket_operations = listOf(ticketOp),
            money_placements = listOf(moneyPlacement),
            annulled_tickets = annulledTickets,
            cash_sum = money,
            revenue = revenue,
            non_nullable_sums = listOf(nonNullableSum),
            open_shift_time = dateTime,
            close_shift_time = dateTime,
            checksum = "12345"
        )
    }

    private fun buildValidServiceJson() = buildJsonObject {
        put("getRegInfo", true)
        put(
            "offlinePeriod",
            buildJsonObject {
                put("beginTime", buildValidDateTimeJson())
                put("endTime", buildValidDateTimeJson())
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
                        put("title", "Title")
                        put("address", "Addr")
                        put("addressKz", "AddrKz")
                        put("inn", "123456789012")
                        put("okved", "12345")
                    }
                )
            }
        )
    }

    private fun buildValidDateTimeJson() = buildJsonObject {
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
                put("second", 0)
            }
        )
    }

    private fun buildValidOperatorJson() = buildJsonObject {
        put("code", 1)
        put("name", "Operator")
    }

    private fun buildValidItemJson() = buildJsonObject {
        put("type", "ITEM_TYPE_COMMODITY")
        put(
            "commodity",
            buildJsonObject {
                put("name", "Item")
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

    private fun buildItemWithTaxesJson() = buildJsonObject {
        put("type", "ITEM_TYPE_COMMODITY")
        put(
            "commodity",
            buildJsonObject {
                put("name", "Item")
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
                put(
                    "taxes",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("percent", 1200)
                                put(
                                    "sum",
                                    buildJsonObject {
                                        put("bills", 10)
                                        put("coins", 0)
                                    }
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    private fun buildValidZReportJson() = buildJsonObject {
        put("dateTime", buildValidDateTimeJson())
        put("shiftNumber", 1)
        put(
            "cashSum",
            buildJsonObject {
                put("bills", 10000)
                put("coins", 0)
            }
        )
        put(
            "revenue",
            buildJsonObject {
                put(
                    "sum",
                    buildJsonObject {
                        put("bills", 10000)
                        put("coins", 0)
                    }
                )
                put("isNegative", false)
            }
        )
        put("openShiftTime", buildValidDateTimeJson())
    }

    @Test
    fun testHeaderFieldsDefault() {
        val h = HeaderFields(deviceId = 1L, token = 2L, reqNum = 3)
        assertEquals(HeaderConstants.APPCODE, h.appCode)
    }

    @Test
    fun testOfdCodecCustomRegistry() {
        val customRegistry = OfdRegistry()
        val mockHandler = OfdProtocolHandler(
            ofdId = "mockOfd",
            protocolVersion = "203",
            requestValidator = object : Validator {
                override fun validate(commandType: CommandType, json: JsonObject) = emptyList<ValidationError>()
            },
            requestSerializer = object : Serializer {
                override fun serialize(commandType: CommandType, json: JsonObject): ByteArray {
                    throw RuntimeException("Mock serialization error")
                }
            },
            responseValidator = object : Validator {
                override fun validate(commandType: CommandType, json: JsonObject) = emptyList<ValidationError>()
            },
            responseDeserializer = object : Deserializer {
                override fun deserialize(bytes: ByteArray): JsonObject {
                    throw RuntimeException("Mock deserialization error")
                }
            }
        )
        customRegistry.register(mockHandler)
        val codec = OfdCodec(customRegistry)

        // 1. Serialization exception
        val reqJson = buildJsonObject {
            put("ofdId", "mockOfd")
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put("commandType", "COMMAND_SYSTEM")
            put(
                "header",
                buildJsonObject {
                    put("deviceId", 123)
                    put("token", 456)
                    put("reqNum", 1)
                }
            )
            put("payload", buildJsonObject {})
        }
        val res1 = codec.encode(reqJson)
        assertTrue(res1.isFailure)
        val ex1 = res1.exceptionOrNull() as? OfdCodecException
        assertNotNull(ex1)
        assertTrue(ex1.errors.any { it.code == ErrorCode.SERIALIZATION_FAILED.name })

        // 2. Deserialization exception
        val header = MessageHeader(
            appCode = HeaderConstants.APPCODE,
            protocolVersion = 203,
            size = 18,
            deviceId = 123,
            token = 456,
            reqNum = 1
        )
        val headerBytes = HeaderCodec.encode(header, 0)
        val res2 = codec.decode(headerBytes)
        assertTrue(res2.isFailure)
        val ex2 = res2.exceptionOrNull() as? OfdCodecException
        assertNotNull(ex2)
        assertTrue(ex2.errors.any { it.code == ErrorCode.DESERIALIZATION_FAILED.name })
    }

    @Test
    fun testOfdCodecDecodeNullHandlerAndMissingCommand() {
        val codec = OfdCodec(DefaultRegistry.create())

        // 1. Decode null handler (unsupported protocol version 999)
        val header = MessageHeader(
            appCode = HeaderConstants.APPCODE,
            protocolVersion = 999,
            size = 18,
            deviceId = 123,
            token = 456,
            reqNum = 1
        )
        val bytes = HeaderCodec.encode(header, 0)
        val res1 = codec.decode(bytes)
        assertTrue(res1.isFailure)
        val ex1 = res1.exceptionOrNull() as? OfdCodecException
        assertNotNull(ex1)
        assertTrue(ex1.errors.any { it.code == ErrorCode.PROTOCOL_UNSUPPORTED.name })

        // 2. Decode with mock deserializer returning missing commandType
        val customRegistry = OfdRegistry()
        val mockHandler = OfdProtocolHandler(
            ofdId = "kazakhtelecom",
            protocolVersion = "203",
            requestValidator = CommandValidatorRegistry(emptyMap()),
            requestSerializer = object : Serializer {
                override fun serialize(commandType: CommandType, json: JsonObject) = ByteArray(0)
            },
            responseValidator = CommandValidatorRegistry(emptyMap()),
            responseDeserializer = object : Deserializer {
                override fun deserialize(bytes: ByteArray): JsonObject {
                    return buildJsonObject {} // empty
                }
            }
        )
        customRegistry.register(mockHandler)
        val codec2 = OfdCodec(customRegistry)
        val header2 = MessageHeader(
            appCode = HeaderConstants.APPCODE,
            protocolVersion = 203,
            size = 18,
            deviceId = 123,
            token = 456,
            reqNum = 1
        )
        val bytes2 = HeaderCodec.encode(header2, 0)
        val res2 = codec2.decode(bytes2)
        assertTrue(res2.isFailure)
        val ex2 = res2.exceptionOrNull() as? OfdCodecException
        assertNotNull(ex2)
        assertTrue(ex2.errors.any { it.code == ErrorCode.JSON_MISSING_FIELD.name })

        // 3. Decode with mock deserializer returning invalid commandType type
        val mockHandler3 = OfdProtocolHandler(
            ofdId = "kazakhtelecom",
            protocolVersion = "203",
            requestValidator = CommandValidatorRegistry(emptyMap()),
            requestSerializer = object : Serializer {
                override fun serialize(commandType: CommandType, json: JsonObject) = ByteArray(0)
            },
            responseValidator = CommandValidatorRegistry(emptyMap()),
            responseDeserializer = object : Deserializer {
                override fun deserialize(bytes: ByteArray): JsonObject {
                    return buildJsonObject {
                        put("commandType", 123)
                    }
                }
            }
        )
        val customRegistry3 = OfdRegistry()
        customRegistry3.register(mockHandler3)
        val codec3 = OfdCodec(customRegistry3)
        val res3 = codec3.decode(bytes2)
        assertTrue(res3.isFailure)
        val ex3 = res3.exceptionOrNull() as? OfdCodecException
        assertNotNull(ex3)
        assertTrue(ex3.errors.any { it.code == ErrorCode.JSON_INVALID_TYPE.name })

        // 4. Decode with mock deserializer returning unsupported commandType
        val mockHandler4 = OfdProtocolHandler(
            ofdId = "kazakhtelecom",
            protocolVersion = "203",
            requestValidator = CommandValidatorRegistry(emptyMap()),
            requestSerializer = object : Serializer {
                override fun serialize(commandType: CommandType, json: JsonObject) = ByteArray(0)
            },
            responseValidator = CommandValidatorRegistry(emptyMap()),
            responseDeserializer = object : Deserializer {
                override fun deserialize(bytes: ByteArray): JsonObject {
                    return buildJsonObject {
                        put("commandType", "COMMAND_NOT_EXIST")
                    }
                }
            }
        )
        val customRegistry4 = OfdRegistry()
        customRegistry4.register(mockHandler4)
        val codec4 = OfdCodec(customRegistry4)
        val res4 = codec4.decode(bytes2)
        assertTrue(res4.isFailure)
        val ex4 = res4.exceptionOrNull() as? OfdCodecException
        assertNotNull(ex4)
        assertTrue(ex4.errors.any { it.code == ErrorCode.COMMAND_UNSUPPORTED.name })
    }

    @Test
    fun testResponseValidatorNomenclatureExhaustive() {
        val validator = ResponseValidatorNomenclature()

        // 1. Result is not JsonObject
        val r1 = buildJsonObject {
            put("result", 123)
        }
        val errs1 = validator.validate(CommandType.COMMAND_NOMENCLATURE, r1)
        assertTrue(errs1.any { it.code == ErrorCode.JSON_MISSING_FIELD.name })

        // 2. Nomenclature is null
        val r2 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
        }
        val errs2 = validator.validate(CommandType.COMMAND_NOMENCLATURE, r2)
        assertTrue(errs2.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.nomenclature" })

        // 3. Nomenclature createdTime not JsonObject, elements not JsonArray, elements element not JsonObject
        val r3 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "nomenclature",
                buildJsonObject {
                    put("version", 1)
                    put("createdTime", "not_obj")
                    put(
                        "result",
                        buildJsonObject {
                            put("code", 0)
                            put("name", "OK")
                        }
                    )
                    put("elements", "not_array")
                }
            )
        }
        val errs3 = validator.validate(CommandType.COMMAND_NOMENCLATURE, r3)
        assertTrue(
            errs3.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.nomenclature.createdTime" }
        )
        assertTrue(
            errs3.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.nomenclature.elements" }
        )

        // 4. Elements element is not JsonObject
        val r4 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
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
                    put("elements", buildJsonArray { add(123) })
                }
            )
        }
        val errs4 = validator.validate(CommandType.COMMAND_NOMENCLATURE, r4)
        assertTrue(
            errs4.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.nomenclature.elements[0]" }
        )

        // 5. Element: missing type, title, id. parentGroupId negative. item missing when type is ITEM.
        val r5 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
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
                                    put("parentGroupId", -5L)
                                }
                            )
                        }
                    )
                }
            )
        }
        val errs5 = validator.validate(CommandType.COMMAND_NOMENCLATURE, r5)
        assertTrue(
            errs5.any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.nomenclature.elements[0].title"
            }
        )
        assertTrue(
            errs5.any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.nomenclature.elements[0].id"
            }
        )
        assertTrue(
            errs5.any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.nomenclature.elements[0].parentGroupId"
            }
        )
        assertTrue(
            errs5.any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.nomenclature.elements[0].item"
            }
        )

        // 6. Item: optional fields invalid types / negative / elements list not array
        val r6 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
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
                                    put("title", "T")
                                    put("id", 1L)
                                    put(
                                        "item",
                                        buildJsonObject {
                                            put("article", 123)
                                            put("purchasePrice", 123)
                                            put("sellPrice", 123)
                                            put("discountPercent", -1)
                                            put("markupPercent", -1)
                                            put("discountSum", 123)
                                            put("markupSum", 123)
                                            put("taxes", 123)
                                            put("measureCount", -1)
                                            put("measureTitle", 123)
                                            put("measureFractional", 123)
                                            put("measureUnitCode", 123)
                                            put("ntin", 123)
                                            put("isMarkedeac", 123)
                                            put("isSocial", 123)
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
        val errs6 = validator.validate(CommandType.COMMAND_NOMENCLATURE, r6)
        val basePath = "$.payload.nomenclature.elements[0].item"
        assertTrue(errs6.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$basePath.article" })
        assertTrue(errs6.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$basePath.purchasePrice" })
        assertTrue(errs6.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$basePath.sellPrice" })
        assertTrue(errs6.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$basePath.discountPercent" })
        assertTrue(errs6.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$basePath.markupPercent" })
        assertTrue(errs6.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$basePath.discountSum" })
        assertTrue(errs6.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$basePath.markupSum" })
        assertTrue(errs6.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$basePath.taxes" })
        assertTrue(errs6.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$basePath.measureCount" })
        assertTrue(errs6.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$basePath.measureTitle" })
        assertTrue(
            errs6.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$basePath.measureFractional" }
        )
        assertTrue(errs6.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$basePath.measureUnitCode" })
        assertTrue(errs6.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$basePath.ntin" })
        assertTrue(errs6.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$basePath.isMarkedeac" })
        assertTrue(errs6.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$basePath.isSocial" })

        // 7. Taxes array elements not object
        val r7 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
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
                                    put("title", "T")
                                    put("id", 1L)
                                    put(
                                        "item",
                                        buildJsonObject {
                                            put("taxes", buildJsonArray { add(123) })
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
        val errs7 = validator.validate(CommandType.COMMAND_NOMENCLATURE, r7)
        assertTrue(errs7.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$basePath.taxes[0]" })
    }

    @Test
    fun testResponseValidatorTicketErrors() {
        val validator = ResponseValidatorTicket()

        // 1. resultCode != 0 but ticket is not JsonObject
        val r1 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 1) })
            put("ticket", "not_obj")
        }
        val errs1 = validator.validate(CommandType.COMMAND_TICKET, r1)
        assertTrue(errs1.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.ticket" })

        // 2. service present but not JsonObject
        val r2 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put("ticket", buildJsonObject { put("ticketNumber", "123") })
            put("service", 123)
        }
        val errs2 = validator.validate(CommandType.COMMAND_TICKET, r2)
        assertTrue(errs2.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.service" })
    }

    @Test
    fun testResponseValidatorInfoSystemAndMoneyPlacementErrors() {
        val vInfo = ResponseValidatorInfo()
        val vSystem = ResponseValidatorSystem()
        val vMoney = ResponseValidatorMoneyPlacement()

        val r1 = buildJsonObject { put("result", 123) }
        assertTrue(vInfo.validate(CommandType.COMMAND_INFO, r1).any { it.code == ErrorCode.JSON_MISSING_FIELD.name })
        assertTrue(
            vSystem.validate(CommandType.COMMAND_SYSTEM, r1).any { it.code == ErrorCode.JSON_MISSING_FIELD.name }
        )
        assertTrue(
            vMoney.validate(CommandType.COMMAND_MONEY_PLACEMENT, r1).any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name
            }
        )

        val r2 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put("service", 123)
        }
        assertTrue(vInfo.validate(CommandType.COMMAND_INFO, r2).any { it.code == ErrorCode.JSON_INVALID_TYPE.name })
        assertTrue(vSystem.validate(CommandType.COMMAND_SYSTEM, r2).any { it.code == ErrorCode.JSON_INVALID_TYPE.name })
        assertTrue(
            vMoney.validate(CommandType.COMMAND_MONEY_PLACEMENT, r2).any { it.code == ErrorCode.JSON_INVALID_TYPE.name }
        )
    }

    @Test
    fun testResponseValidatorReportErrors() {
        val validator = ResponseValidatorReport()

        // 1. Result code != 0 returns early
        val r1 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 1) })
        }
        val errs1 = validator.validate(CommandType.COMMAND_REPORT, r1)
        assertTrue(errs1.isEmpty())

        // 2. zxReport missing
        val r2 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "report",
                buildJsonObject {
                    put("reportType", "REPORT_Z")
                }
            )
        }
        val errs2 = validator.validate(CommandType.COMMAND_REPORT, r2)
        assertTrue(errs2.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.report.zxReport" })
    }

    @Test
    fun testResponseValidatorCloseShiftErrors() {
        val validator = ResponseValidatorCloseShift()

        // 1. Result code != 0 returns early
        val r1 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 1) })
        }
        val errs1 = validator.validate(CommandType.COMMAND_CLOSE_SHIFT, r1)
        assertTrue(errs1.isEmpty())

        // 2. service present but not JsonObject
        val r2 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "report",
                buildJsonObject {
                    put("reportType", "REPORT_Z")
                    put(
                        "zxReport",
                        buildJsonObject {
                            put("dateTime", buildValidDateTimeJson())
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
                            put("openShiftTime", buildValidDateTimeJson())
                        }
                    )
                }
            )
            put("service", 123)
        }
        val errs2 = validator.validate(CommandType.COMMAND_CLOSE_SHIFT, r2)
        assertTrue(errs2.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.service" })
    }

    @Test
    fun testZXReportRevenueValidatorErrors() {
        val validator = ZXReportRevenueValidator()

        // 1. revenue == null
        val errs1 = validator.validate(buildJsonObject {}, "revenue", "$.revenue")
        assertTrue(errs1.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.revenue" })

        // 2. isNegative == null
        val errs2 = validator.validate(
            buildJsonObject {
                put(
                    "revenue",
                    buildJsonObject {
                        put(
                            "sum",
                            buildJsonObject {
                                put("bills", 10)
                                put("coins", 0)
                            }
                        )
                    }
                )
            },
            "revenue",
            "$.revenue"
        )
        assertTrue(errs2.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.revenue.isNegative" })

        // 3. isNegative is not boolean
        val errs3 = validator.validate(
            buildJsonObject {
                put(
                    "revenue",
                    buildJsonObject {
                        put(
                            "sum",
                            buildJsonObject {
                                put("bills", 10)
                                put("coins", 0)
                            }
                        )
                        put("isNegative", "not_a_bool")
                    }
                )
            },
            "revenue",
            "$.revenue"
        )
        assertTrue(errs3.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.revenue.isNegative" })
    }

    @Test
    fun testZXReportSectionValidatorErrors() {
        val validator = ZXReportSectionValidator()

        // 1. sectionCode missing
        val s1 = buildJsonObject {
            put("operations", buildJsonArray {})
        }
        val errs1 = validator.validate(s1, "$.section")
        assertTrue(errs1.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.section.sectionCode" })

        // 2. operations missing
        val s2 = buildJsonObject {
            put("sectionCode", "1")
        }
        val errs2 = validator.validate(s2, "$.section")
        assertTrue(errs2.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.section.operations" })
    }

    @Test
    fun testZXReportValidatorErrors() {
        val validator = ZXReportValidator()

        // 1. zxReport == null
        val errs1 = validator.validate(buildJsonObject {}, "report", "$.report")
        assertTrue(errs1.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.report" })

        // 2. closeShiftTime present in non-REPORT_Z case (e.g. REPORT_X)
        val rep = buildJsonObject {
            put("dateTime", buildValidDateTimeJson())
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
            put("openShiftTime", buildValidDateTimeJson())
            put("closeShiftTime", "not_a_date")
            put("checksum", "  ")
        }
        val errs2 = validator.validate(buildJsonObject { put("report", rep) }, "report", "$.report", "REPORT_X")
        assertTrue(errs2.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.report.closeShiftTime" })
        assertTrue(errs2.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.report.checksum" })
    }

    @Test
    fun testZXReportSubValidatorsExtra() {
        val payVal = ZXReportTicketPaymentValidator()
        val sumVal = ZXReportNonNullableSumValidator()

        // validate with invalid elements
        val p = buildJsonObject {
            put("payment", "INVALID")
            put("sum", 123)
            put("count", -1)
        }
        assertTrue(payVal.validate(p, "$.p").isNotEmpty())

        val s = buildJsonObject {
            put("operation", "INVALID")
            put("sum", 123)
        }
        assertTrue(sumVal.validate(s, "$.s").isNotEmpty())
    }

    @Test
    fun testNomenclatureRequestBuilderErrors() {
        val builder = NomenclatureRequestBuilder()
        val emptyNom = buildJsonObject { put("nomenclature", buildJsonObject {}) }
        assertFailsWith<IllegalArgumentException> {
            builder.build(emptyNom)
        }
    }

    @Test
    fun testReportRequestBuilderErrors() {
        val builder = ReportRequestBuilder()

        // 1. isOffline not present, zxReport missing
        val r1 = buildJsonObject {
            put(
                "report",
                buildJsonObject {
                    put("reportType", "REPORT_Z")
                    put("dateTime", buildValidDateTimeJson())
                }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            builder.build(r1)
        }
    }

    @Test
    fun testServiceRequestValidatorErrors() {
        val validator = ServiceRequestValidator()

        // 1. Call validate with single argument (basePath defaults to "$.payload.service")
        val s1 = buildJsonObject {
            put("getRegInfo", true)
            put(
                "offlinePeriod",
                buildJsonObject {
                    put("beginTime", buildValidDateTimeJson())
                    put("endTime", buildValidDateTimeJson())
                }
            )
            put(
                "securityStats",
                buildJsonObject {
                    put(
                        "geoPosition",
                        buildJsonObject {
                            put("latitude", 1)
                            put("longitude", 2)
                            put("source", "CELL")
                        }
                    )
                }
            )
            put(
                "regInfo",
                buildJsonObject {
                    put("kkm", 123) // invalid type
                    put("org", 123) // invalid type
                }
            )
        }
        val errs = validator.validate(s1)
        assertTrue(
            errs.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.service.regInfo.kkm" }
        )
        assertTrue(
            errs.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.service.regInfo.org" }
        )
    }

    @Test
    fun testKazakhtelecomV203ModuleDefaultHandler() {
        val handler = KazakhtelecomV203Module.defaultHandler()
        assertEquals("kazakhtelecom", handler.ofdId)
    }

    @Test
    fun testCloseShiftRequestBuilderErrors() {
        val builder = CloseShiftRequestBuilder()

        // 1. zReport missing
        val r1 = buildJsonObject {
            put(
                "closeShift",
                buildJsonObject {
                    put("closeTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            builder.build(r1)
        }

        // 2. operator missing
        val r2 = buildJsonObject {
            put(
                "closeShift",
                buildJsonObject {
                    put("closeTime", buildValidDateTimeJson())
                    put("zReport", buildValidZReportJson())
                }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            builder.build(r2)
        }
    }

    @Test
    fun testServiceRequestBuilderErrors() {
        val builder = ServiceRequestBuilder()

        // 1. securityStats missing
        val r1 = buildJsonObject {
            put(
                "service",
                buildJsonObject {
                    put("getRegInfo", true)
                    put(
                        "offlinePeriod",
                        buildJsonObject {
                            put("beginTime", buildValidDateTimeJson())
                            put("endTime", buildValidDateTimeJson())
                        }
                    )
                }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            builder.build(r1)
        }

        // 2. regInfo missing
        val r2 = buildJsonObject {
            put(
                "service",
                buildJsonObject {
                    put("getRegInfo", true)
                    put(
                        "offlinePeriod",
                        buildJsonObject {
                            put("beginTime", buildValidDateTimeJson())
                            put("endTime", buildValidDateTimeJson())
                        }
                    )
                    put(
                        "securityStats",
                        buildJsonObject {
                            put(
                                "geoPosition",
                                buildJsonObject {
                                    put("latitude", 1)
                                    put("longitude", 2)
                                    put("source", "CELL")
                                }
                            )
                        }
                    )
                }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            builder.build(r2)
        }
    }

    @Test
    fun testJsonExtensionsReadString() {
        val element: JsonElement = JsonPrimitive("hello")
        assertEquals("hello", element.readStringElement())
    }

    @Test
    fun testJsonMessageMapperReadLongBoolean() {
        val badEnv = buildJsonObject {
            put("ofdId", "kazakhtelecom")
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put("commandType", "COMMAND_SYSTEM")
            put(
                "header",
                buildJsonObject {
                    put("deviceId", true) // boolean
                    put("token", 123)
                    put("reqNum", 1)
                }
            )
            put("payload", buildJsonObject {})
        }
        val (_, errors) = JsonMessageMapper.parseEnvelope(badEnv)
        assertTrue(errors.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.deviceId" })
    }

    @Test
    fun testTicketItemValidatorErrors() {
        val validator = TicketItemValidator()

        // 1. validateCommodity missing commodity
        val item1 = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
        }
        val errs1 = validator.validate(item1, "$.item")
        assertTrue(errs1.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.item.commodity" })

        // 2. validateTaxes element not JsonObject
        val item2 = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
            put(
                "commodity",
                buildJsonObject {
                    put("name", "T")
                    put("sectionCode", "1")
                    put("quantity", 1)
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
                    put("taxes", buildJsonArray { add(123) })
                }
            )
        }
        val errs2 = validator.validate(item2, "$.item")
        assertTrue(errs2.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.item.commodity.taxes[0]" })

        // 3. validateExciseStampList element not string primitive
        val item3 = buildJsonObject {
            put("type", "ITEM_TYPE_COMMODITY")
            put(
                "commodity",
                buildJsonObject {
                    put("name", "T")
                    put("sectionCode", "1")
                    put("quantity", 1)
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
                    put("listExciseStamp", buildJsonArray { add(buildJsonObject {}) })
                }
            )
        }
        val errs3 = validator.validate(item3, "$.item")
        assertTrue(
            errs3.any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.item.commodity.listExciseStamp[0]"
            }
        )
    }

    @Test
    fun testTicketModifierValidatorErrors() {
        val validator = TicketModifierValidator()

        // 1. taxes array element not JsonObject
        val m = buildJsonObject {
            put("name", "M")
            put(
                "sum",
                buildJsonObject {
                    put("bills", 10)
                    put("coins", 0)
                }
            )
            put("taxes", buildJsonArray { add(123) })
        }
        val errs = validator.validate(buildJsonObject { put("key", m) }, "key", "$.key")
        assertTrue(errs.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.key.taxes[0]" })
    }

    @Test
    fun testRequestValidatorMoneyPlacementErrors() {
        val validator = RequestValidatorMoneyPlacement()

        // 1. operation missing
        val p = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "moneyPlacement",
                buildJsonObject {
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
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
        val errs = validator.validate(CommandType.COMMAND_MONEY_PLACEMENT, p)
        assertTrue(
            errs.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.moneyPlacement.operation" }
        )
    }

    @Test
    fun testRequestValidatorTicketExtraErrors() {
        val validator = RequestValidatorTicket()

        // 1. stornoTaxes branch
        val p1 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put(
                        "items",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_STORNO_COMMODITY")
                                    put(
                                        "stornoCommodity",
                                        buildJsonObject {
                                            put("name", "T")
                                            put("sectionCode", "1")
                                            put("quantity", 1)
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
                                            put(
                                                "taxes",
                                                buildJsonArray {
                                                    add(
                                                        buildJsonObject {
                                                            put("taxType", 1)
                                                            put("percent", 1200)
                                                            put(
                                                                "sum",
                                                                buildJsonObject {
                                                                    put("bills", 10)
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
                        }
                    )
                }
            )
        }
        val errs1 = validator.validate(CommandType.COMMAND_TICKET, p1)
        assertTrue(errs1.isEmpty()) // valid

        // 2. payments element not JsonObject
        val p2 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
                    put("payments", buildJsonArray { add(123) })
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
                        }
                    )
                }
            )
        }
        val errs2 = validator.validate(CommandType.COMMAND_TICKET, p2)
        assertTrue(
            errs2.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.ticket.payments[0]" }
        )

        // 3. hasCashPayment is true but amounts is null
        val p3 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
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
                }
            )
        }
        val errs3 = validator.validate(CommandType.COMMAND_TICKET, p3)
        assertTrue(errs3.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.ticket.amounts" })

        // 4. hasCashPayment is true but amounts lacks taken and change
        val p4 = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
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
                        }
                    )
                }
            )
        }
        val errs4 = validator.validate(CommandType.COMMAND_TICKET, p4)
        assertTrue(
            errs4.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.ticket.amounts.taken" }
        )
        assertTrue(
            errs4.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.ticket.amounts.change" }
        )
    }

    @Test
    fun testTicketRequestBuilderExhaustive() {
        val builder = TicketRequestBuilder()

        // 1. All optional fields present
        val payload = buildJsonObject {
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put(
                        "items",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_COMMODITY")
                                    put(
                                        "commodity",
                                        buildJsonObject {
                                            put("name", "Commodity")
                                            put("sectionCode", "1")
                                            put("quantity", 1000)
                                            put(
                                                "price",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "taxes",
                                                buildJsonArray {
                                                    add(
                                                        buildJsonObject {
                                                            put("taxType", 1)
                                                            put("taxationType", 2)
                                                            put("percent", 1200)
                                                            put(
                                                                "sum",
                                                                buildJsonObject {
                                                                    put("bills", 1)
                                                                    put("coins", 0)
                                                                }
                                                            )
                                                            put("isInTotalSum", true)
                                                        }
                                                    )
                                                }
                                            )
                                            put("listExciseStamp", buildJsonArray { add("STAMP123") })
                                            put("physicalLabel", "LABEL")
                                            put("productId", "PROD")
                                            put("barcode", "BAR")
                                            put("measureUnitCode", "796")
                                            put("ntin", "NTIN")
                                        }
                                    )
                                }
                            )
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_STORNO_COMMODITY")
                                    put(
                                        "stornoCommodity",
                                        buildJsonObject {
                                            put("name", "Storno")
                                            put("sectionCode", "1")
                                            put("quantity", 1000)
                                            put(
                                                "price",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "taxes",
                                                buildJsonArray {
                                                    add(
                                                        buildJsonObject {
                                                            put("taxType", 1)
                                                            put("percent", 1200)
                                                            put(
                                                                "sum",
                                                                buildJsonObject {
                                                                    put("bills", 1)
                                                                    put("coins", 0)
                                                                }
                                                            )
                                                            put("isInTotalSum", true)
                                                        }
                                                    )
                                                }
                                            )
                                            put("listExciseStamp", buildJsonArray { add("STAMP456") })
                                            put("physicalLabel", "LABEL")
                                            put("productId", "PROD")
                                            put("barcode", "BAR")
                                            put("measureUnitCode", "796")
                                            put("ntin", "NTIN")
                                        }
                                    )
                                }
                            )
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_MARKUP")
                                    put(
                                        "markup",
                                        buildJsonObject {
                                            put("name", "Markup")
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 1)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "taxes",
                                                buildJsonArray {
                                                    add(
                                                        buildJsonObject {
                                                            put("taxType", 1)
                                                            put("percent", 1200)
                                                            put(
                                                                "sum",
                                                                buildJsonObject {
                                                                    put("bills", 0)
                                                                    put("coins", 10)
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
                            )
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_STORNO_MARKUP")
                                    put(
                                        "stornoMarkup",
                                        buildJsonObject {
                                            put("name", "StornoMarkup")
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 1)
                                                    put("coins", 0)
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_DISCOUNT")
                                    put(
                                        "discount",
                                        buildJsonObject {
                                            put("name", "Discount")
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 1)
                                                    put("coins", 0)
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_STORNO_DISCOUNT")
                                    put(
                                        "stornoDiscount",
                                        buildJsonObject {
                                            put("name", "StornoDiscount")
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 1)
                                                    put("coins", 0)
                                                }
                                            )
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
                                    put("type", "PAYMENT_CARD")
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 10)
                                            put("coins", 0)
                                        }
                                    )
                                    put(
                                        "cardPaymentFields",
                                        buildJsonObject {
                                            put("posTerminalId", "TERM1")
                                            put("posCardType", "VISA")
                                            put("posAutorizationCode", 123)
                                            put("posRrn", 456L)
                                            put("posReceiptNumber", 789)
                                        }
                                    )
                                    put(
                                        "mobilePaymentFields",
                                        buildJsonObject {
                                            put("qrType", "QR")
                                            put("qrId", "QR1")
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put(
                        "taxes",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("taxType", 1)
                                    put("percent", 1200)
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 1)
                                            put("coins", 0)
                                        }
                                    )
                                    put("isInTotalSum", true)
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
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "taken",
                                buildJsonObject {
                                    put("bills", 10)
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
                            put(
                                "markup",
                                buildJsonObject {
                                    put("name", "M")
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 1)
                                            put("coins", 0)
                                        }
                                    )
                                }
                            )
                            put(
                                "discount",
                                buildJsonObject {
                                    put("name", "D")
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 1)
                                            put("coins", 0)
                                        }
                                    )
                                }
                            )
                        }
                    )
                    put(
                        "extensionOptions",
                        buildJsonObject {
                            put("customerEmail", "email")
                            put("customerPhone", "phone")
                            put("customerIinOrBin", "iin")
                        }
                    )
                    put("offlineTicketNumber", 100)
                    put("printedTicket", "printed")
                    put("frShiftNumber", 200)
                    put("shiftDocumentNumber", 300)
                    put("printedDocumentNumber", 400L)
                    put(
                        "parentTicket",
                        buildJsonObject {
                            put("parentTicketNumber", "123")
                            put("parentTicketDateTime", buildValidDateTimeJson())
                            put("kgdKkmId", "kgd")
                            put(
                                "parentTicketTotal",
                                buildJsonObject {
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                            put("parentTicketIsOffline", false)
                        }
                    )
                }
            )
        }
        val ticketProto = builder.build(payload)
        assertEquals(OperationTypeEnum.OPERATION_SELL, ticketProto.operation)
        assertEquals(6, ticketProto.items.size)

        // 2. commodity name missing but code present
        val p2 = buildJsonObject {
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put(
                        "items",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_COMMODITY")
                                    put(
                                        "commodity",
                                        buildJsonObject {
                                            put("code", 12345L) // code instead of name
                                            put("sectionCode", "1")
                                            put("quantity", 1000)
                                            put(
                                                "price",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
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
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
        }
        val ticket2 = builder.build(p2)
        assertEquals(12345L, ticket2.items[0].commodity?.code)

        // 3. both name and code missing in commodity (throws require)
        val p3 = buildJsonObject {
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put(
                        "items",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", "ITEM_TYPE_COMMODITY")
                                    put(
                                        "commodity",
                                        buildJsonObject {
                                            put("sectionCode", "1")
                                            put("quantity", 1000)
                                            put(
                                                "price",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
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
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            builder.build(p3)
        }
    }

    @Test
    fun testZXReportBuilderExceptions() {
        val builder = ZXReportBuilder()

        // 1. sections operations missing throws exception
        val s1 = buildJsonObject {
            put("dateTime", buildValidDateTimeJson())
            put("shiftNumber", 1)
            put(
                "cashSum",
                buildJsonObject {
                    put("bills", 10)
                    put("coins", 0)
                }
            )
            put(
                "revenue",
                buildJsonObject {
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 10)
                            put("coins", 0)
                        }
                    )
                    put("isNegative", false)
                }
            )
            put("openShiftTime", buildValidDateTimeJson())
            put(
                "sections",
                buildJsonArray {
                    add(buildJsonObject { put("sectionCode", "1") }) // operations missing
                }
            )
        }
        assertFailsWith<IllegalArgumentException> { builder.build(s1) }

        // 2. taxes operations missing throws exception
        val s2 = buildJsonObject {
            put("dateTime", buildValidDateTimeJson())
            put("shiftNumber", 1)
            put(
                "cashSum",
                buildJsonObject {
                    put("bills", 10)
                    put("coins", 0)
                }
            )
            put(
                "revenue",
                buildJsonObject {
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 10)
                            put("coins", 0)
                        }
                    )
                    put("isNegative", false)
                }
            )
            put("openShiftTime", buildValidDateTimeJson())
            put(
                "taxes",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("taxType", 1)
                            put("percent", 1200)
                        }
                    ) // operations missing
                }
            )
        }
        assertFailsWith<IllegalArgumentException> { builder.build(s2) }

        // 3. ticketOperations payments missing throws exception
        val s3 = buildJsonObject {
            put("dateTime", buildValidDateTimeJson())
            put("shiftNumber", 1)
            put(
                "cashSum",
                buildJsonObject {
                    put("bills", 10)
                    put("coins", 0)
                }
            )
            put(
                "revenue",
                buildJsonObject {
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 10)
                            put("coins", 0)
                        }
                    )
                    put("isNegative", false)
                }
            )
            put("openShiftTime", buildValidDateTimeJson())
            put(
                "ticketOperations",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("operation", "OPERATION_SELL")
                            put("ticketsTotalCount", 1)
                            put("ticketsCount", 1)
                            put(
                                "ticketsSum",
                                buildJsonObject {
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                            put("offlineCount", 0)
                            put(
                                "discountSum",
                                buildJsonObject {
                                    put("bills", 0)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "markupSum",
                                buildJsonObject {
                                    put("bills", 0)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "changeSum",
                                buildJsonObject {
                                    put("bills", 0)
                                    put("coins", 0)
                                }
                            )
                        }
                    ) // payments missing
                }
            )
        }
        assertFailsWith<IllegalArgumentException> { builder.build(s3) }

        // 4. DateTimeBuilder build missing date throws exception
        val dt = buildJsonObject {
            put(
                "time",
                buildJsonObject {
                    put("hour", 12)
                    put("minute", 0)
                }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            DateTimeBuilder().build(buildJsonObject { put("dt", dt) }, "dt")
        }
    }

    @Test
    fun testZXReportBuilderAllOptionalLists() {
        val builder = ZXReportBuilder()
        val zxReportJson = buildJsonObject {
            put("dateTime", buildValidDateTimeJson())
            put("shiftNumber", 1)
            put(
                "sections",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("sectionCode", "1")
                            put(
                                "operations",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("operation", "OPERATION_SELL")
                                            put("count", 1)
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
            put(
                "operations",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("operation", "OPERATION_SELL")
                            put("count", 1)
                            put(
                                "sum",
                                buildJsonObject {
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
            put(
                "discounts",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("operation", "OPERATION_SELL")
                            put("count", 1)
                            put(
                                "sum",
                                buildJsonObject {
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
            put(
                "markups",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("operation", "OPERATION_SELL")
                            put("count", 1)
                            put(
                                "sum",
                                buildJsonObject {
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
            put(
                "totalResult",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("operation", "OPERATION_SELL")
                            put("count", 1)
                            put(
                                "sum",
                                buildJsonObject {
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
            put(
                "taxes",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("taxType", 1)
                            put("percent", 1200)
                            put(
                                "operations",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("operation", "OPERATION_SELL")
                                            put(
                                                "turnover",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
                                            put(
                                                "turnoverWithoutTax",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
            put(
                "startShiftNonNullableSums",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("operation", "OPERATION_SELL")
                            put(
                                "sum",
                                buildJsonObject {
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
            put(
                "ticketOperations",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("operation", "OPERATION_SELL")
                            put("ticketsTotalCount", 1)
                            put("ticketsCount", 1)
                            put(
                                "ticketsSum",
                                buildJsonObject {
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "payments",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("payment", "PAYMENT_CASH")
                                            put(
                                                "sum",
                                                buildJsonObject {
                                                    put("bills", 10)
                                                    put("coins", 0)
                                                }
                                            )
                                            put("count", 1)
                                        }
                                    )
                                }
                            )
                            put("offlineCount", 0)
                            put(
                                "discountSum",
                                buildJsonObject {
                                    put("bills", 0)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "markupSum",
                                buildJsonObject {
                                    put("bills", 0)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "changeSum",
                                buildJsonObject {
                                    put("bills", 0)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
            put(
                "moneyPlacements",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("operation", "MONEY_PLACEMENT_DEPOSIT")
                            put("operationsTotalCount", 1)
                            put("operationsCount", 1)
                            put(
                                "operationsSum",
                                buildJsonObject {
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                            put("offlineCount", 0)
                        }
                    )
                }
            )
            put(
                "annulledTickets",
                buildJsonObject {
                    put("annulledTicketsTotalCount", 1)
                    put("annulledTicketsCount", 1)
                    put(
                        "annulledOperations",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("operation", "OPERATION_SELL")
                                    put("count", 1)
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 10)
                                            put("coins", 0)
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
            put(
                "cashSum",
                buildJsonObject {
                    put("bills", 10)
                    put("coins", 0)
                }
            )
            put(
                "revenue",
                buildJsonObject {
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 10)
                            put("coins", 0)
                        }
                    )
                    put("isNegative", false)
                }
            )
            put(
                "nonNullableSums",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("operation", "OPERATION_SELL")
                            put(
                                "sum",
                                buildJsonObject {
                                    put("bills", 10)
                                    put("coins", 0)
                                }
                            )
                        }
                    )
                }
            )
            put("openShiftTime", buildValidDateTimeJson())
            put("closeShiftTime", buildValidDateTimeJson())
        }
        val proto = builder.build(zxReportJson)
        assertEquals(1, proto.shift_number)
        assertEquals(1, proto.sections.size)
        assertEquals(1, proto.operations.size)
        assertEquals(1, proto.discounts.size)
        assertEquals(1, proto.markups.size)
        assertEquals(1, proto.total_result.size)
        assertEquals(1, proto.taxes.size)
        assertEquals(1, proto.start_shift_non_nullable_sums.size)
        assertEquals(1, proto.ticket_operations.size)
        assertEquals(1, proto.money_placements.size)
        assertTrue(proto.annulled_tickets != null)
        assertTrue(proto.close_shift_time != null)
    }

    @Test
    fun testServiceResponseValidatorAllPaths() {
        val validator = ServiceResponseValidator()

        // 1. service is empty (missing regInfo)
        val s1 = buildJsonObject {}
        val errs1 = validator.validate(s1)
        assertTrue(errs1.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.service.regInfo" })

        // 2. regInfo is not JsonObject
        val s2 = buildJsonObject { put("regInfo", 123) }
        val errs2 = validator.validate(s2)
        assertTrue(errs2.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.service.regInfo" })

        // 3. regInfo has missing/invalid kkm or org, and pos is not JsonObject
        val s3 = buildJsonObject {
            put(
                "regInfo",
                buildJsonObject {
                    put("kkm", 123)
                    put("org", 123)
                    put("pos", 123)
                }
            )
        }
        val errs3 = validator.validate(s3)
        assertTrue(
            errs3.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.service.regInfo.kkm" }
        )
        assertTrue(
            errs3.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.service.regInfo.org" }
        )
        assertTrue(
            errs3.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.service.regInfo.pos" }
        )

        // 4. ticketAds is not an array, pos has invalid fields
        val s4 = buildJsonObject {
            put(
                "regInfo",
                buildJsonObject {
                    put(
                        "kkm",
                        buildJsonObject {
                            put("fnsKkmId", "fns")
                            put("serialNumber", "sn")
                            put("kkmId", "id")
                        }
                    )
                    put(
                        "org",
                        buildJsonObject {
                            put("title", "org")
                            put("address", "addr")
                            put("addressKz", "addrKz")
                            put("inn", "inn")
                        }
                    )
                    put(
                        "pos",
                        buildJsonObject {
                            put("title", " ")
                            put("address", "")
                            put("addressKz", " ")
                            put("latitude", -1)
                            put("longitude", -2)
                        }
                    )
                }
            )
            put("ticketAds", 123)
        }
        val errs4 = validator.validate(s4)
        assertTrue(
            errs4.any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.service.regInfo.pos.title"
            }
        )
        assertTrue(
            errs4.any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.service.regInfo.pos.address"
            }
        )
        assertTrue(
            errs4.any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.service.regInfo.pos.addressKz"
            }
        )
        assertTrue(
            errs4.any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.service.regInfo.pos.latitude"
            }
        )
        assertTrue(
            errs4.any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.service.regInfo.pos.longitude"
            }
        )
        assertTrue(
            errs4.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.service.ticketAds" }
        )

        // 5. ticketAds is array with non-JsonObject elements
        val s5 = buildJsonObject {
            put(
                "regInfo",
                buildJsonObject {
                    put(
                        "kkm",
                        buildJsonObject {
                            put("fnsKkmId", "a")
                            put("serialNumber", "b")
                            put("kkmId", "c")
                        }
                    )
                    put(
                        "org",
                        buildJsonObject {
                            put("title", "a")
                            put("address", "b")
                            put("addressKz", "c")
                            put("inn", "d")
                        }
                    )
                }
            )
            put("ticketAds", buildJsonArray { add(123) })
        }
        val errs5 = validator.validate(s5)
        assertTrue(
            errs5.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.service.ticketAds[0]" }
        )

        // 6. ticketAds elements with invalid info/text
        val s6 = buildJsonObject {
            put(
                "regInfo",
                buildJsonObject {
                    put(
                        "kkm",
                        buildJsonObject {
                            put("fnsKkmId", "a")
                            put("serialNumber", "b")
                            put("kkmId", "c")
                        }
                    )
                    put(
                        "org",
                        buildJsonObject {
                            put("title", "a")
                            put("address", "b")
                            put("addressKz", "c")
                            put("inn", "d")
                        }
                    )
                }
            )
            put(
                "ticketAds",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put(
                                "info",
                                buildJsonObject {
                                    put("type", "")
                                    put("version", -1L)
                                }
                            )
                            put("text", " ")
                        }
                    )
                }
            )
        }
        val errs6 = validator.validate(s6)
        assertTrue(
            errs6.any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.service.ticketAds[0].info.type"
            }
        )
        assertTrue(
            errs6.any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.service.ticketAds[0].info.version"
            }
        )
        assertTrue(
            errs6.any {
                it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.service.ticketAds[0].text"
            }
        )
    }

    @Test
    fun testCommandValidatorRegistryUnregisteredCommand() {
        val reg = CommandValidatorRegistry(emptyMap())
        val errs = reg.validate(CommandType.COMMAND_TICKET, buildJsonObject {})
        assertTrue(errs.any { it.code == ErrorCode.COMMAND_UNSUPPORTED.name && it.path == "$.commandType" })
    }

    @Test
    fun testValidationErrorStandardMethods() {
        val err1 = ValidationError("c", "p", "ru", "kk", "en", mapOf("x" to "y"))
        val err2 = ValidationError("c", "p", "ru", "kk", "en", mapOf("x" to "y"))
        assertEquals(err1, err2)
        assertEquals(err1.hashCode(), err2.hashCode())
        assertEquals(
            "ValidationError(code=c, path=p, messageRu=ru, messageKk=kk, messageEn=en, params={x=y})",
            err1.toString()
        )
        val copied = err1.copy(code = "c2")
        assertEquals("c2", copied.code)
    }

    @Test
    fun testOfdCodecExceptionEmptyErrors() {
        val ex = OfdCodecException(emptyList())
        assertEquals("OFD codec error / Ошибка кодека ОФД / ОФД кодек қателігі", ex.message)
    }

    @Test
    fun testMoneyPlacementRequestBuilderExceptions() {
        val builder = MoneyPlacementRequestBuilder()

        // 1. Missing moneyPlacement
        assertFailsWith<IllegalArgumentException> {
            builder.build(buildJsonObject {})
        }

        // 2. Missing sum
        val p1 = buildJsonObject {
            put(
                "moneyPlacement",
                buildJsonObject {
                    put("dateTime", buildValidDateTimeJson())
                    put("operation", "MONEY_PLACEMENT_DEPOSIT")
                    put("operator", buildValidOperatorJson())
                }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            builder.build(p1)
        }

        // 3. Missing operator
        val p2 = buildJsonObject {
            put(
                "moneyPlacement",
                buildJsonObject {
                    put("dateTime", buildValidDateTimeJson())
                    put("operation", "MONEY_PLACEMENT_DEPOSIT")
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
        assertFailsWith<IllegalArgumentException> {
            builder.build(p2)
        }

        // 4. Test optional fields coverage (isOffline, frShiftNumber, printedDocumentNumber)
        val p3 = buildJsonObject {
            put(
                "moneyPlacement",
                buildJsonObject {
                    put("dateTime", buildValidDateTimeJson())
                    put("operation", "MONEY_PLACEMENT_DEPOSIT")
                    put(
                        "sum",
                        buildJsonObject {
                            put("bills", 100)
                            put("coins", 0)
                        }
                    )
                    put("operator", buildValidOperatorJson())
                    put("isOffline", true)
                    put("frShiftNumber", 5)
                    put("printedDocumentNumber", 10L)
                }
            )
        }
        val proto = builder.build(p3)
        assertTrue(proto.is_offline!!)
        assertEquals(5, proto.fr_shift_number)
        assertEquals(10L, proto.printed_document_number)
    }

    @Test
    fun testResponseDeserializerServiceAndTicket() {
        val ad = TicketAd(
            info = TicketAdInfo(
                type = TicketAdTypeEnum.TICKET_AD_INFO,
                version = 1L
            ),
            text = "Ad Text"
        )

        val kkm = KkmRegInfo(
            point_of_payment_number = "pop",
            terminal_number = "term",
            fns_kkm_id = "fns",
            serial_number = "serial",
            kkm_id = "kkmId"
        )

        val org = OrgRegInfo(
            title = "org",
            address = "addr",
            address_kz = "addrKz",
            inn = "inn",
            okved = "okved"
        )

        val pos = PosRegInfo(
            title = "pos",
            address = "posAddr",
            address_kz = "posAddrKz",
            latitude = 123,
            longitude = 456
        )

        val service = ServiceResponse(
            ticket_ads = listOf(ad),
            reg_info = ServiceResponse.RegInfo(
                kkm = kkm,
                org = org,
                pos = pos
            )
        )

        val ticket = TicketResponse(
            ticket_number = "777",
            qr_code = okio.ByteString.of(1, 2, 3)
        )

        val message = Response(
            command = CommandTypeEnum.COMMAND_TICKET,
            result = Result(result_code = 0),
            service = service,
            ticket = ticket
        )

        val bytes = Response.ADAPTER.encode(message)
        val json = KazakhtelecomV203ResponseDeserializer().deserialize(bytes)
        assertNotNull(json["service"])
        assertNotNull(json["ticket"])
        val qrBase64 = json["ticket"]?.jsonObject?.get("qrCodeBase64")?.jsonPrimitive?.content
        assertEquals(Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3)), qrBase64)
    }

    @Test
    fun testOfdCodecEncodeAndDecodeRemainingPaths() {
        val registry = DefaultRegistry.create()
        val codec = OfdCodec(registry)

        // 1. Encode with unsupported version/ofdId
        val badEnv = buildJsonObject {
            put("ofdId", "nonexistent")
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put("commandType", "COMMAND_TICKET")
            put(
                "header",
                buildJsonObject {
                    put("deviceId", 123)
                    put("token", 456)
                    put("reqNum", 1)
                }
            )
            put("payload", buildJsonObject {})
        }
        val res1 = codec.encode(badEnv)
        assertTrue(res1.isFailure)
        assertTrue(
            (res1.exceptionOrNull() as OfdCodecException).errors.any { it.code == ErrorCode.PROTOCOL_UNSUPPORTED.name }
        )

        // 2. Encode with messageType != REQUEST (e.g. RESPONSE)
        val badMsgType = buildJsonObject {
            put("ofdId", "kazakhtelecom")
            put("protocolVersion", "203")
            put("messageType", "RESPONSE")
            put("commandType", "COMMAND_TICKET")
            put(
                "header",
                buildJsonObject {
                    put("deviceId", 123)
                    put("token", 456)
                    put("reqNum", 1)
                }
            )
            put("payload", buildJsonObject {})
        }
        val res2 = codec.encode(badMsgType)
        assertTrue(res2.isFailure)
        assertTrue(
            (res2.exceptionOrNull() as OfdCodecException).errors.any {
                it.code == ErrorCode.ENCODE_UNSUPPORTED_MESSAGE_TYPE.name
            }
        )

        // 3. Decode with response validation errors
        val customRegistry = OfdRegistry()
        val mockHandler = OfdProtocolHandler(
            ofdId = "mockOfd",
            protocolVersion = "203",
            requestValidator = object : Validator {
                override fun validate(commandType: CommandType, json: JsonObject) = emptyList<ValidationError>()
            },
            requestSerializer = object : Serializer {
                override fun serialize(commandType: CommandType, json: JsonObject) = ByteArray(0)
            },
            responseValidator = object : Validator {
                override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
                    return listOf(ValidationError("TEST_VAL_ERR", "$.path", "ru", "kk", "en"))
                }
            },
            responseDeserializer = object : Deserializer {
                override fun deserialize(bytes: ByteArray): JsonObject {
                    return buildJsonObject {
                        put("commandType", "COMMAND_SYSTEM")
                    }
                }
            }
        )
        customRegistry.register(mockHandler)
        val codec2 = OfdCodec(customRegistry)
        val header = MessageHeader(
            appCode = HeaderConstants.APPCODE,
            protocolVersion = 203,
            size = 18,
            deviceId = 123,
            token = 456,
            reqNum = 1
        )
        val bytes = HeaderCodec.encode(header, 0)
        val res3 = codec2.decode(bytes)
        assertTrue(res3.isFailure)
        assertTrue((res3.exceptionOrNull() as OfdCodecException).errors.any { it.code == "TEST_VAL_ERR" })
    }

    @Test
    fun testResponseValidatorNomenclatureTaxesAndResultCode() {
        val validator = ResponseValidatorNomenclature()

        // 1. Result code != 0 returns early
        val r1 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 5) })
        }
        val errs1 = validator.validate(CommandType.COMMAND_NOMENCLATURE, r1)
        assertTrue(errs1.isEmpty())

        // 2. Missing nomenclature.result and invalid element.type (missing and non-string)
        val r2 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "nomenclature",
                buildJsonObject {
                    put("version", 1)
                    put(
                        "elements",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("type", 123)
                                    put("title", "T")
                                    put("id", 1L)
                                }
                            )
                            add(
                                buildJsonObject {
                                    put("title", "T")
                                    put("id", 1L)
                                }
                            )
                        }
                    )
                }
            )
        }
        val errs2 = validator.validate(CommandType.COMMAND_NOMENCLATURE, r2)
        assertTrue(
            errs2.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.nomenclature.result" }
        )
        assertTrue(
            errs2.any {
                it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.nomenclature.elements[0].type"
            }
        )
        assertTrue(
            errs2.any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.nomenclature.elements[1].type"
            }
        )

        // 3. Elements item taxes object is not JsonObject, and invalid tax fields
        val r3 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
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
                                    put("title", "T")
                                    put("id", 1L)
                                    put(
                                        "item",
                                        buildJsonObject {
                                            put(
                                                "taxes",
                                                buildJsonArray {
                                                    add(123)
                                                    add(
                                                        buildJsonObject {
                                                            put("taxationType", -1)
                                                            put("taxType", -1)
                                                            put("taxPercent", -1)
                                                        }
                                                    )
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
        val errs3 = validator.validate(CommandType.COMMAND_NOMENCLATURE, r3)
        val basePath = "$.payload.nomenclature.elements[0].item.taxes"
        assertTrue(errs3.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$basePath[0]" })
        assertTrue(errs3.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$basePath[1].taxationType" })
        assertTrue(errs3.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$basePath[1].taxType" })
        assertTrue(errs3.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$basePath[1].taxPercent" })
    }

    @Test
    fun testResponseValidatorTicketResultCodeNonZero() {
        val validator = ResponseValidatorTicket()

        // 1. resultCode != 0 and ticket present with invalid fields
        val r1 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 1) })
            put(
                "ticket",
                buildJsonObject {
                    put("ticketNumber", " ")
                    put("qrCodeBase64", " ")
                }
            )
        }
        val errs1 = validator.validate(CommandType.COMMAND_TICKET, r1)
        assertTrue(
            errs1.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.ticket.ticketNumber" }
        )
        assertTrue(
            errs1.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$.payload.ticket.qrCodeBase64" }
        )
    }

    @Test
    fun testResponseValidatorCloseShiftServiceInvalidType() {
        val validator = ResponseValidatorCloseShift()
        val r = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "report",
                buildJsonObject {
                    put("reportType", "REPORT_Z")
                    put(
                        "zxReport",
                        buildJsonObject {
                            put("dateTime", buildValidDateTimeJson())
                            put("shiftNumber", 1)
                            put(
                                "cashSum",
                                buildJsonObject {
                                    put("bills", 100)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "revenue",
                                buildJsonObject {
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 100)
                                            put("coins", 0)
                                        }
                                    )
                                    put("isNegative", false)
                                }
                            )
                            put("openShiftTime", buildValidDateTimeJson())
                        }
                    )
                }
            )
            put("service", 123)
        }
        val errs = validator.validate(CommandType.COMMAND_CLOSE_SHIFT, r)
        assertTrue(errs.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.service" })
    }

    @Test
    fun testCloseShiftRequestBuilderMissingOperator() {
        val builder = CloseShiftRequestBuilder()
        val p = buildJsonObject {
            put(
                "closeShift",
                buildJsonObject {
                    put("closeTime", buildValidDateTimeJson())
                    put("zReport", buildValidZReportJson())
                }
            )
        }
        val ex = assertFailsWith<IllegalArgumentException> {
            builder.build(p)
        }
        println("EXCEPTION MESSAGE: ${ex.message}")
    }

    @Test
    fun testJsonMessageMapperInvalidJsonType() {
        val (parsed, errors) = JsonMessageMapper.parseEnvelope(JsonPrimitive(123))
        assertNull(parsed)
        assertTrue(errors.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$" })
    }

    @Test
    fun testJsonMessageMapperMissingVersion() {
        val env = buildJsonObject {
            put("ofdId", "kazakhtelecom")
            put("messageType", "REQUEST")
            put("commandType", "COMMAND_SYSTEM")
            put(
                "header",
                buildJsonObject {
                    put("deviceId", 123)
                    put("token", 456)
                    put("reqNum", 1)
                }
            )
            put("payload", buildJsonObject {})
        }
        val (parsed, errors) = JsonMessageMapper.parseEnvelope(env)
        assertNull(parsed)
        assertTrue(errors.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.protocolVersion" })
    }

    @Test
    fun testRequestValidatorTicketExtensionOptionsAndParentTicket() {
        val validator = RequestValidatorTicket()

        val p = buildJsonObject {
            put("service", buildValidServiceJson())
            put(
                "ticket",
                buildJsonObject {
                    put("operation", "OPERATION_SELL_RETURN")
                    put("dateTime", buildValidDateTimeJson())
                    put("operator", buildValidOperatorJson())
                    put("items", buildJsonArray { add(buildValidItemJson()) })
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
                        }
                    )
                    put(
                        "extensionOptions",
                        buildJsonObject {
                            put("customerEmail", " ")
                            put("customerPhone", " ")
                            put("customerIinOrBin", " ")
                        }
                    )
                    put(
                        "parentTicket",
                        buildJsonObject {
                            put("parentTicketNumber", " ")
                            put("kgdKkmId", "")
                            put("parentTicketIsOffline", 123)
                        }
                    )
                }
            )
        }
        val errs = validator.validate(CommandType.COMMAND_TICKET, p)
        val pathOpt = "$.payload.ticket.extensionOptions"
        val pathParent = "$.payload.ticket.parentTicket"
        assertTrue(errs.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$pathOpt.customerEmail" })
        assertTrue(errs.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$pathOpt.customerPhone" })
        assertTrue(errs.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$pathOpt.customerIinOrBin" })
        assertTrue(
            errs.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$pathParent.parentTicketNumber" }
        )
        assertTrue(errs.any { it.code == ErrorCode.JSON_INVALID_VALUE.name && it.path == "$pathParent.kgdKkmId" })
        assertTrue(
            errs.any { it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$pathParent.parentTicketIsOffline" }
        )
    }

    @Test
    fun testAdditionalLineCoverage() {
        // 1. OfdCodec: Header decode errors (too short bytes) -> Line 105
        val codecDefault = OfdCodec(DefaultRegistry.create())
        val decodeShortRes = codecDefault.decode(ByteArray(5))
        assertTrue(decodeShortRes.isFailure)
        val exShort = decodeShortRes.exceptionOrNull() as OfdCodecException
        assertTrue(exShort.errors.any { it.code == ErrorCode.HEADER_TOO_SHORT.name })

        // 2. OfdCodec: Serialization failed with null message exception -> Line 73(partly)
        val customRegistry1 = OfdRegistry()
        val mockHandler1 = OfdProtocolHandler(
            ofdId = "mockOfd",
            protocolVersion = "203",
            requestValidator = object : Validator {
                override fun validate(commandType: CommandType, json: JsonObject) = emptyList<ValidationError>()
            },
            requestSerializer = object : Serializer {
                override fun serialize(commandType: CommandType, json: JsonObject): ByteArray {
                    throw RuntimeException(null as String?)
                }
            },
            responseValidator = object : Validator {
                override fun validate(commandType: CommandType, json: JsonObject) = emptyList<ValidationError>()
            },
            responseDeserializer = object : Deserializer {
                override fun deserialize(bytes: ByteArray) = buildJsonObject {}
            }
        )
        customRegistry1.register(mockHandler1)
        val codec1 = OfdCodec(customRegistry1)
        val envelope1 = buildJsonObject {
            put("ofdId", "mockOfd")
            put("protocolVersion", "203")
            put("messageType", "REQUEST")
            put("commandType", "COMMAND_SYSTEM")
            put(
                "header",
                buildJsonObject {
                    put("deviceId", 123)
                    put("token", 456)
                    put("reqNum", 1)
                }
            )
            put("payload", buildJsonObject {})
        }
        val res1 = codec1.encode(envelope1)
        assertTrue(res1.isFailure)
        val ex1 = res1.exceptionOrNull() as OfdCodecException
        assertTrue(
            ex1.errors.any {
                it.code == ErrorCode.SERIALIZATION_FAILED.name && it.params["reason"] == "RuntimeException"
            }
        )

        // 3. OfdCodec: Deserialization failed with null message exception -> Line 140(partly)
        val customRegistry2 = OfdRegistry()
        val mockHandler2 = OfdProtocolHandler(
            ofdId = "mockOfd",
            protocolVersion = "203",
            requestValidator = object : Validator {
                override fun validate(commandType: CommandType, json: JsonObject) = emptyList<ValidationError>()
            },
            requestSerializer = object : Serializer {
                override fun serialize(commandType: CommandType, json: JsonObject) = ByteArray(0)
            },
            responseValidator = object : Validator {
                override fun validate(commandType: CommandType, json: JsonObject) = emptyList<ValidationError>()
            },
            responseDeserializer = object : Deserializer {
                override fun deserialize(bytes: ByteArray): JsonObject {
                    throw RuntimeException(null as String?)
                }
            }
        )
        customRegistry2.register(mockHandler2)
        val codec2 = OfdCodec(customRegistry2)
        val header = MessageHeader(
            appCode = HeaderConstants.APPCODE,
            protocolVersion = 203,
            size = 18,
            deviceId = 123,
            token = 456,
            reqNum = 1
        )
        val bytes = HeaderCodec.encode(header, 0)
        val res2 = codec2.decode(bytes)
        assertTrue(res2.isFailure)
        val ex2 = res2.exceptionOrNull() as OfdCodecException
        assertTrue(
            ex2.errors.any {
                it.code == ErrorCode.DESERIALIZATION_FAILED.name && it.params["reason"] == "RuntimeException"
            }
        )

        // 4. ResponseValidatorNomenclature: resultCodeValue not primitive/integer, createdTime valid JsonObject -> Line 43(partly), 72(partly), 73
        val valNomenclature = ResponseValidatorNomenclature()
        val rNomenclature1 = buildJsonObject {
            put(
                "result",
                buildJsonObject {
                    put("resultCode", buildJsonObject {}) // not primitive
                }
            )
        }
        val errsNomenclature1 = valNomenclature.validate(CommandType.COMMAND_NOMENCLATURE, rNomenclature1)
        assertTrue(
            errsNomenclature1.any {
                it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.nomenclature"
            }
        )

        val rNomenclature2 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "nomenclature",
                buildJsonObject {
                    put("version", 1)
                    put("createdTime", buildValidDateTimeJson()) // valid JsonObject
                    put(
                        "result",
                        buildJsonObject {
                            put("code", 0)
                            put("name", "OK")
                        }
                    )
                }
            )
        }
        val errsNomenclature2 = valNomenclature.validate(CommandType.COMMAND_NOMENCLATURE, rNomenclature2)
        assertTrue(errsNomenclature2.isEmpty())

        // 5. ResponseValidatorMoneyPlacement: service as valid JsonObject -> Line 37(partly), 41, 42(partly)
        val valMoney = ResponseValidatorMoneyPlacement()
        val rMoney = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "service",
                buildJsonObject {
                    put("getRegInfo", true)
                    put(
                        "offlinePeriod",
                        buildJsonObject {
                            put("beginTime", buildValidDateTimeJson())
                            put("endTime", buildValidDateTimeJson())
                        }
                    )
                    put(
                        "securityStats",
                        buildJsonObject {
                            put(
                                "geoPosition",
                                buildJsonObject {
                                    put("latitude", 123)
                                    put("longitude", 456)
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
                                    put("fnsKkmId", "fns")
                                    put("serialNumber", "serial")
                                    put("kkmId", "kkmId")
                                }
                            )
                            put(
                                "org",
                                buildJsonObject {
                                    put("title", "org")
                                    put("address", "address")
                                    put("addressKz", "addressKz")
                                    put("inn", "inn")
                                    put("okved", "okved")
                                }
                            )
                        }
                    )
                }
            )
        }
        val errsMoney = valMoney.validate(CommandType.COMMAND_MONEY_PLACEMENT, rMoney)
        assertTrue(errsMoney.isEmpty())

        // 6. ResponseValidatorCloseShift: resultCodeValue/reportTypeValue not primitive, service as valid JsonObject -> Line 42(partly), 54(partly), 68
        val valCloseShift = ResponseValidatorCloseShift()
        val rCloseShift1 = buildJsonObject {
            put(
                "result",
                buildJsonObject {
                    put("resultCode", buildJsonObject {}) // not primitive
                }
            )
        }
        val errsCloseShift1 = valCloseShift.validate(CommandType.COMMAND_CLOSE_SHIFT, rCloseShift1)
        assertTrue(
            errsCloseShift1.any { it.code == ErrorCode.JSON_MISSING_FIELD.name && it.path == "$.payload.report" }
        )

        val rCloseShift2 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "report",
                buildJsonObject {
                    put("reportType", true) // primitive but not string
                }
            )
        }
        val errsCloseShift2 = valCloseShift.validate(CommandType.COMMAND_CLOSE_SHIFT, rCloseShift2)
        assertTrue(
            errsCloseShift2.any {
                it.code == ErrorCode.JSON_INVALID_TYPE.name && it.path == "$.payload.report.reportType"
            }
        )

        val rCloseShift3 = buildJsonObject {
            put("result", buildJsonObject { put("resultCode", 0) })
            put(
                "report",
                buildJsonObject {
                    put("reportType", "REPORT_Z")
                    put(
                        "zxReport",
                        buildJsonObject {
                            put("dateTime", buildValidDateTimeJson())
                            put("shiftNumber", 1)
                            put(
                                "cashSum",
                                buildJsonObject {
                                    put("bills", 10000)
                                    put("coins", 0)
                                }
                            )
                            put(
                                "revenue",
                                buildJsonObject {
                                    put(
                                        "sum",
                                        buildJsonObject {
                                            put("bills", 10000)
                                            put("coins", 0)
                                        }
                                    )
                                    put("isNegative", false)
                                }
                            )
                            put("openShiftTime", buildValidDateTimeJson())
                        }
                    )
                }
            )
            put(
                "service",
                buildJsonObject {
                    put("getRegInfo", true)
                    put(
                        "offlinePeriod",
                        buildJsonObject {
                            put("beginTime", buildValidDateTimeJson())
                            put("endTime", buildValidDateTimeJson())
                        }
                    )
                    put(
                        "securityStats",
                        buildJsonObject {
                            put(
                                "geoPosition",
                                buildJsonObject {
                                    put("latitude", 123)
                                    put("longitude", 456)
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
                                    put("fnsKkmId", "fns")
                                    put("serialNumber", "serial")
                                    put("kkmId", "kkmId")
                                }
                            )
                            put(
                                "org",
                                buildJsonObject {
                                    put("title", "org")
                                    put("address", "address")
                                    put("addressKz", "addressKz")
                                    put("inn", "inn")
                                    put("okved", "okved")
                                }
                            )
                        }
                    )
                }
            )
        }
        val errsCloseShift3 = valCloseShift.validate(CommandType.COMMAND_CLOSE_SHIFT, rCloseShift3)
        assertTrue(errsCloseShift3.isEmpty())

        // 7. CloseShiftRequestBuilder: operator is primitive -> Line 39(partly), 40
        val builderCloseShift = CloseShiftRequestBuilder()
        val pCloseShift = buildJsonObject {
            put(
                "closeShift",
                buildJsonObject {
                    put("closeTime", buildValidDateTimeJson())
                    put("zReport", buildValidZReportJson())
                    put("operator", 123) // primitive instead of JsonObject
                }
            )
        }
        assertFailsWith<IllegalArgumentException> {
            builderCloseShift.build(pCloseShift)
        }
    }
}
