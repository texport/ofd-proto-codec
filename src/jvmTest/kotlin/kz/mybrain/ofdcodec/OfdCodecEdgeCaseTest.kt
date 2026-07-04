package kz.mybrain.ofdcodec

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.ErrorCode
import kz.mybrain.ofdcodec.domain.model.HeaderConstants
import kz.mybrain.ofdcodec.domain.model.MessageHeader
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kz.mybrain.ofdcodec.infrastructure.header.HeaderCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OfdCodecEdgeCaseTest {
    private val codec = OfdCodec(DefaultRegistry.create())

    @Test
    fun encodeFailsWhenReqNumIsNegative() {
        val result = codec.encode(validRequest(reqNum = JsonPrimitive(-1)))

        assertFailureCode(result, ErrorCode.JSON_INVALID_VALUE)
    }

    @Test
    fun encodeFailsWhenReqNumExceedsUnsignedShort() {
        val result = codec.encode(validRequest(reqNum = JsonPrimitive(65_536)))

        assertFailureCode(result, ErrorCode.JSON_INVALID_VALUE)
    }

    @Test
    fun encodeAcceptsMinimumReqNum() {
        val result = codec.encode(validRequest(reqNum = JsonPrimitive(0)))

        assertTrue(result.isSuccess, "Expected reqNum=0 to be encoded")
    }

    @Test
    fun encodeAcceptsMaximumReqNum() {
        val result = codec.encode(validRequest(reqNum = JsonPrimitive(65_535)))

        assertTrue(result.isSuccess, "Expected reqNum=65535 to be encoded")
    }

    @Test
    fun encodeFailsWhenReqNumIsString() {
        val result = codec.encode(validRequest(reqNum = JsonPrimitive("1")))

        assertFailureCode(result, ErrorCode.JSON_INVALID_TYPE)
    }

    @Test
    fun encodeFailsWhenReqNumIsDecimal() {
        val result = codec.encode(validRequest(reqNum = JsonPrimitive(1.5)))

        assertFailureCode(result, ErrorCode.JSON_INVALID_VALUE)
    }

    @Test
    fun encodeFailsWhenHeaderIsMissing() {
        val result = codec.encode(validRequest(includeHeader = false))

        assertFailureCode(result, ErrorCode.JSON_INVALID_TYPE)
    }

    @Test
    fun encodeFailsWhenHeaderIsArray() {
        val result = codec.encode(validRequest(header = JsonArray(emptyList())))

        assertFailureCode(result, ErrorCode.JSON_INVALID_TYPE)
    }

    @Test
    fun encodeFailsWhenPayloadIsMissing() {
        val result = codec.encode(validRequest(includePayload = false))

        assertFailureCode(result, ErrorCode.JSON_MISSING_FIELD)
    }

    @Test
    fun encodeFailsWhenPayloadIsArray() {
        val result = codec.encode(validRequest(payload = JsonArray(emptyList())))

        assertFailureCode(result, ErrorCode.JSON_INVALID_TYPE)
    }

    @Test
    fun encodeFailsWhenEnvelopeIsNotObject() {
        val result = codec.encode(JsonArray(emptyList()))

        assertFailureCode(result, ErrorCode.JSON_INVALID_TYPE)
    }

    @Test
    fun encodeFailsWhenProtocolVersionIsBlank() {
        val result = codec.encode(validRequest(protocolVersion = JsonPrimitive("")))

        assertFailureCode(result, ErrorCode.HEADER_INVALID_VERSION_FORMAT)
    }

    @Test
    fun encodeFailsWhenProtocolVersionContainsDots() {
        val result = codec.encode(validRequest(protocolVersion = JsonPrimitive("2.0.3")))

        assertFailureCode(result, ErrorCode.HEADER_INVALID_VERSION_FORMAT)
    }

    @Test
    fun encodeFailsWhenProtocolVersionIsUnsupported() {
        val result = codec.encode(validRequest(protocolVersion = JsonPrimitive("204")))

        assertFailureCode(result, ErrorCode.PROTOCOL_UNSUPPORTED)
    }

    @Test
    fun encodeFailsWhenOfdIsUnsupported() {
        val result = codec.encode(validRequest(ofdId = JsonPrimitive("other-ofd")))

        assertFailureCode(result, ErrorCode.PROTOCOL_UNSUPPORTED)
    }

    @Test
    fun encodeFailsWhenTryingToEncodeResponseEnvelope() {
        val result = codec.encode(validRequest(messageType = JsonPrimitive("RESPONSE")))

        assertFailureCode(result, ErrorCode.ENCODE_UNSUPPORTED_MESSAGE_TYPE)
    }

    @Test
    fun encodeFailsWhenCommandTypeIsUnknown() {
        val result = codec.encode(validRequest(commandType = JsonPrimitive("COMMAND_UNKNOWN")))

        assertFailureCode(result, ErrorCode.COMMAND_UNSUPPORTED)
    }

    @Test
    fun encodeAcceptsLowercaseCommandType() {
        val result = codec.encode(validRequest(commandType = JsonPrimitive("command_auth")))

        assertTrue(result.isSuccess, "Expected lowercase commandType to be normalized")
    }

    @Test
    fun encodeAcceptsLowercaseMessageType() {
        val result = codec.encode(validRequest(messageType = JsonPrimitive("request")))

        assertTrue(result.isSuccess, "Expected lowercase messageType to be normalized")
    }

    @Test
    fun encodeAccumulatesMultipleEnvelopeErrors() {
        val result = codec.encode(buildJsonObject {})
        val exception = assertCodecException(result)

        assertTrue(exception.errors.size >= 5, "Expected several envelope errors, got ${exception.errors}")
    }

    @Test
    fun decodeFailsWhenMessageIsShorterThanHeader() {
        val result = codec.decode(ByteArray(HeaderConstants.HEADER_SIZE - 1))

        assertFailureCode(result, ErrorCode.HEADER_TOO_SHORT)
    }

    @Test
    fun decodeFailsWhenAppCodeIsInvalid() {
        val bytes = validHeaderBytes(payloadSize = 0)
        bytes[0] = 0
        bytes[1] = 0

        val result = codec.decode(bytes)

        assertFailureCode(result, ErrorCode.HEADER_INVALID_APPCODE)
    }

    @Test
    fun decodeFailsWhenHeaderProtocolVersionFormatIsInvalid() {
        val bytes = validHeaderBytes(payloadSize = 0)
        bytes[2] = 0
        bytes[3] = 0

        val result = codec.decode(bytes)

        assertFailureCode(result, ErrorCode.HEADER_INVALID_VERSION_FORMAT)
    }

    @Test
    fun decodeFailsWhenHeaderSizeIsSmallerThanHeader() {
        val bytes = validHeaderBytes(payloadSize = 0)
        writeHeaderSize(bytes, HeaderConstants.HEADER_SIZE - 1L)

        val result = codec.decode(bytes)

        assertFailureCode(result, ErrorCode.HEADER_INVALID_SIZE)
    }

    @Test
    fun decodeFailsWhenHeaderSizeIsGreaterThanActualBytes() {
        val bytes = validHeaderBytes(payloadSize = 0)
        writeHeaderSize(bytes, HeaderConstants.HEADER_SIZE + 1L)

        val result = codec.decode(bytes)

        assertFailureCode(result, ErrorCode.HEADER_INVALID_SIZE)
    }

    @Test
    fun decodeFailsWhenProtocolVersionIsNotRegistered() {
        val bytes = validHeaderBytes(payloadSize = 0, protocolVersion = 204)

        val result = codec.decode(bytes)

        assertFailureCode(result, ErrorCode.PROTOCOL_UNSUPPORTED)
    }

    @Test
    fun decodeFailsWhenPayloadCannotBeDeserialized() {
        val payload = byteArrayOf(1, 2, 3, 4)
        val header = HeaderCodec.encode(
            MessageHeader(
                appCode = HeaderConstants.APPCODE,
                protocolVersion = 203,
                size = 0,
                deviceId = 201_873,
                token = 0,
                reqNum = 1
            ),
            payload.size
        )
        val message = header + payload

        val result = codec.decode(message)

        assertFailureCode(result, ErrorCode.DESERIALIZATION_FAILED)
    }

    private fun validRequest(
        ofdId: JsonElement = JsonPrimitive("kazakhtelecom"),
        protocolVersion: JsonElement = JsonPrimitive("203"),
        messageType: JsonElement = JsonPrimitive("REQUEST"),
        commandType: JsonElement = JsonPrimitive("COMMAND_AUTH"),
        reqNum: JsonElement = JsonPrimitive(1),
        includeHeader: Boolean = true,
        header: JsonElement = header(reqNum),
        includePayload: Boolean = true,
        payload: JsonElement = authPayload()
    ): JsonObject {
        return buildJsonObject {
            put("ofdId", ofdId)
            put("protocolVersion", protocolVersion)
            put("messageType", messageType)
            put("commandType", commandType)
            if (includeHeader) {
                put("header", header)
            }
            if (includePayload) {
                put("payload", payload)
            }
        }
    }

    private fun header(reqNum: JsonElement): JsonObject {
        return buildJsonObject {
            put("deviceId", JsonPrimitive(201_873))
            put("token", JsonPrimitive(0))
            put("reqNum", reqNum)
        }
    }

    private fun authPayload(): JsonObject {
        return buildJsonObject {
            put("service", validService())
            put(
                "auth",
                buildJsonObject {
                    put("login", "my_login")
                    put("password", "my_password")
                }
            )
        }
    }

    private fun validService(): JsonObject {
        return buildJsonObject {
            put("getRegInfo", true)
            put(
                "offlinePeriod",
                buildJsonObject {
                    put("beginTime", dateTime())
                    put("endTime", dateTime())
                }
            )
            put(
                "securityStats",
                buildJsonObject {
                    put(
                        "geoPosition",
                        buildJsonObject {
                            put("latitude", 432_156)
                            put("longitude", 765_432)
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
    }

    private fun dateTime(): JsonObject {
        return buildJsonObject {
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
    }

    private fun validHeaderBytes(payloadSize: Int, protocolVersion: Int = 203): ByteArray {
        return HeaderCodec.encode(
            MessageHeader(
                appCode = HeaderConstants.APPCODE,
                protocolVersion = protocolVersion,
                size = 0,
                deviceId = 201_873,
                token = 0,
                reqNum = 1
            ),
            payloadSize
        )
    }

    private fun writeHeaderSize(bytes: ByteArray, value: Long) {
        bytes[HEADER_SIZE_OFFSET] = (value and 0xFF).toByte()
        bytes[HEADER_SIZE_OFFSET + 1] = ((value shr 8) and 0xFF).toByte()
        bytes[HEADER_SIZE_OFFSET + 2] = ((value shr 16) and 0xFF).toByte()
        bytes[HEADER_SIZE_OFFSET + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun assertFailureCode(result: Result<*>, code: ErrorCode) {
        val exception = assertCodecException(result)
        assertTrue(
            exception.errors.any { it.code == code.name },
            "Expected ${code.name}, got ${exception.errors}"
        )
    }

    private fun assertCodecException(result: Result<*>): OfdCodecException {
        assertTrue(result.isFailure, "Expected failure")
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is OfdCodecException, "Expected OfdCodecException, got ${exception::class}")
        assertEquals(true, exception.errors.isNotEmpty())
        return exception
    }

    private companion object {
        const val HEADER_SIZE_OFFSET = 4
    }
}
