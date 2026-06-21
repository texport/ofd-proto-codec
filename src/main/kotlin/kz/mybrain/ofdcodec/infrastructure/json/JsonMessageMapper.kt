package kz.mybrain.ofdcodec.infrastructure.json

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ErrorCode
import kz.mybrain.ofdcodec.domain.model.ErrorFactory
import kz.mybrain.ofdcodec.domain.model.HeaderConstants
import kz.mybrain.ofdcodec.domain.model.MessageType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.infrastructure.util.ProtocolVersion

/**
 * Поля заголовка, полученные из JSON-конверта.
 */
data class HeaderFields(
    val deviceId: Long,
    val token: Long,
    val reqNum: Int,
    val appCode: Int = HeaderConstants.APPCODE
)

/**
 * Результат разбора входного JSON-конверта.
 */
data class ParsedEnvelope(
    val ofdId: String,
    val protocolVersionText: String,
    val protocolVersion: Int,
    val messageType: MessageType,
    val commandType: CommandType,
    val header: HeaderFields,
    val payload: JsonObject
)

/**
 * Разбор JSON-конверта и извлечение обязательных полей.
 * Ожидаемая структура:
 * - ofdId: String
 * - protocolVersion: String (например "203")
 * - messageType: REQUEST/RESPONSE
 * - commandType: COMMAND_*
 * - header: { deviceId, token, reqNum }
 * - payload: { ... }
 *
 * Поле header.size не требуется при кодировании, вычисляется автоматически.
 */
object JsonMessageMapper {
    fun parseEnvelope(
        json: JsonElement
    ): Pair<ParsedEnvelope?, List<ValidationError>> {
        if (json !is JsonObject) {
            return null to listOf(ErrorFactory.error(ErrorCode.JSON_INVALID_TYPE, "$", mapOf("field" to "$")))
        }

        val errors = mutableListOf<ValidationError>()
        val ofdId = readString(json, JsonKeys.OFD_ID, errors)
        val protocolVersionText = readString(json, JsonKeys.PROTOCOL_VERSION, errors)
        val messageType = readMessageType(json, errors)
        val commandType = readCommandType(json, errors)
        val header = readHeader(json, errors)
        val payload = readPayloadObject(json, errors)

        val versionNumber = if (protocolVersionText != null) {
            ProtocolVersion.parseNumeric(protocolVersionText)
        } else {
            null
        }
        if (protocolVersionText != null && versionNumber == null) {
            errors.add(
                ErrorFactory.error(
                    ErrorCode.HEADER_INVALID_VERSION_FORMAT,
                    "$.${JsonKeys.PROTOCOL_VERSION}",
                    mapOf("field" to JsonKeys.PROTOCOL_VERSION)
                )
            )
        }

        if (errors.isNotEmpty()) {
            return null to errors
        }

        return ParsedEnvelope(
            ofdId = ofdId!!,
            protocolVersionText = protocolVersionText!!,
            protocolVersion = versionNumber!!,
            messageType = messageType!!,
            commandType = commandType!!,
            header = header!!,
            payload = payload!!
        ) to emptyList()
    }

    private fun readHeader(
        json: JsonObject,
        errors: MutableList<ValidationError>
    ): HeaderFields? {
        val headerElement = json[JsonKeys.HEADER]
        if (headerElement !is JsonObject) {
            errors.add(
                ErrorFactory.error(
                    ErrorCode.JSON_INVALID_TYPE,
                    "$.${JsonKeys.HEADER}",
                    mapOf("field" to JsonKeys.HEADER)
                )
            )
            return null
        }

        val deviceId = readLong(headerElement, JsonKeys.DEVICE_ID, errors)
        val token = readLong(headerElement, JsonKeys.TOKEN, errors)
        val reqNum = readReqNumInt(headerElement, errors)
        if (errors.isNotEmpty()) {
            return null
        }

        return HeaderFields(
            deviceId = deviceId!!,
            token = token!!,
            reqNum = reqNum!!,
            appCode = HeaderConstants.APPCODE
        )
    }

    private fun readMessageType(
        json: JsonObject,
        errors: MutableList<ValidationError>
    ): MessageType? {
        val value = readString(json, JsonKeys.MESSAGE_TYPE, errors) ?: return null
        return try {
            MessageType.valueOf(value.uppercase())
        } catch (_: IllegalArgumentException) {
            errors.add(
                ErrorFactory.error(
                    ErrorCode.JSON_INVALID_VALUE,
                    "$.${JsonKeys.MESSAGE_TYPE}",
                    mapOf("field" to JsonKeys.MESSAGE_TYPE)
                )
            )
            null
        }
    }

    private fun readCommandType(
        json: JsonObject,
        errors: MutableList<ValidationError>
    ): CommandType? {
        val value = readString(json, JsonKeys.COMMAND_TYPE, errors) ?: return null
        val command = CommandType.fromName(value)
        if (command == null) {
            errors.add(
                ErrorFactory.error(
                    ErrorCode.COMMAND_UNSUPPORTED,
                    "$.${JsonKeys.COMMAND_TYPE}",
                    mapOf("command" to value)
                )
            )
        }
        return command
    }

    private fun readPayloadObject(
        json: JsonObject,
        errors: MutableList<ValidationError>
    ): JsonObject? {
        val key = JsonKeys.PAYLOAD
        val element = json[key]
        if (element == null) {
            errors.add(
                ErrorFactory.error(
                    ErrorCode.JSON_MISSING_FIELD,
                    "$.$key",
                    mapOf("field" to key)
                )
            )
            return null
        }
        if (element !is JsonObject) {
            errors.add(
                ErrorFactory.error(
                    ErrorCode.JSON_INVALID_TYPE,
                    "$.$key",
                    mapOf("field" to key)
                )
            )
            return null
        }
        return element
    }

    private fun readString(
        json: JsonObject,
        key: String,
        errors: MutableList<ValidationError>
    ): String? {
        val element = json[key]
        if (element == null) {
            errors.add(
                ErrorFactory.error(
                    ErrorCode.JSON_MISSING_FIELD,
                    "$.$key",
                    mapOf("field" to key)
                )
            )
            return null
        }
        if (element !is JsonPrimitive || !element.isString) {
            errors.add(
                ErrorFactory.error(
                    ErrorCode.JSON_INVALID_TYPE,
                    "$.$key",
                    mapOf("field" to key)
                )
            )
            return null
        }
        return element.content
    }

    private fun readLong(
        json: JsonObject,
        key: String,
        errors: MutableList<ValidationError>
    ): Long? {
        val element = json[key]
        if (element == null) {
            errors.add(
                ErrorFactory.error(
                    ErrorCode.JSON_MISSING_FIELD,
                    "$.$key",
                    mapOf("field" to key)
                )
            )
            return null
        }
        if (element !is JsonPrimitive || element.isString) {
            errors.add(
                ErrorFactory.error(
                    ErrorCode.JSON_INVALID_TYPE,
                    "$.$key",
                    mapOf("field" to key)
                )
            )
            return null
        }
        return element.longOrNull ?: run {
            errors.add(
                ErrorFactory.error(
                    ErrorCode.JSON_INVALID_VALUE,
                    "$.$key",
                    mapOf("field" to key)
                )
            )
            null
        }
    }

    private fun readReqNumInt(
        json: JsonObject,
        errors: MutableList<ValidationError>
    ): Int? {
        val value = readLong(json, JsonKeys.REQ_NUM, errors) ?: return null
        return value.toInt()
    }
}
