package kz.mybrain.ofdcodec.application

import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ErrorCode
import kz.mybrain.ofdcodec.domain.model.ErrorFactory
import kz.mybrain.ofdcodec.domain.model.MessageType
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.registry.OfdRegistry
import kz.mybrain.ofdcodec.domain.port.OfdResolver
import kz.mybrain.ofdcodec.infrastructure.header.HeaderCodec
import kz.mybrain.ofdcodec.domain.model.HeaderConstants
import kz.mybrain.ofdcodec.infrastructure.header.HeaderDecodeResult
import kz.mybrain.ofdcodec.domain.model.MessageHeader
import kz.mybrain.ofdcodec.infrastructure.json.JsonEnvelopeBuilder
import kz.mybrain.ofdcodec.infrastructure.json.JsonKeys
import kz.mybrain.ofdcodec.infrastructure.json.JsonMessageMapper
import kz.mybrain.ofdcodec.infrastructure.util.ProtocolVersion
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Base64

/**
 * Фасад библиотеки: кодирование запросов и декодирование ответов.
 */
class OfdCodec(
    private val registry: OfdRegistry,
    private val ofdResolver: OfdResolver = defaultResolver()
) {
    /**
     * Кодирование запроса от кассы (REQUEST) в байтовое сообщение.
     * В случае ошибок возвращает Result.failure с OfdCodecException.
     *
     * Результат: JSON с полями size и messageBase64.
     */
    fun encode(json: JsonElement): Result<JsonObject> {
        val (parsed, errors) = JsonMessageMapper.parseEnvelope(json)
        if (errors.isNotEmpty()) {
            return Result.failure(OfdCodecException(errors))
        }

        val handler = registry.find(parsed!!.ofdId, parsed.protocolVersionText)
        if (handler == null) {
            val error = ErrorFactory.error(
                ErrorCode.PROTOCOL_UNSUPPORTED,
                "$.${JsonKeys.PROTOCOL_VERSION}",
                mapOf("ofdId" to parsed.ofdId, "version" to parsed.protocolVersionText)
            )
            return Result.failure(OfdCodecException(listOf(error)))
        }

        if (parsed.messageType != MessageType.REQUEST) {
            return Result.failure(
                OfdCodecException(listOf(ErrorFactory.error(ErrorCode.ENCODE_UNSUPPORTED_MESSAGE_TYPE, "$.messageType")))
            )
        }

        val validationErrors = when (parsed.messageType) {
            MessageType.REQUEST -> handler.requestValidator.validate(parsed.commandType, parsed.payload)
            MessageType.RESPONSE -> handler.responseValidator.validate(parsed.commandType, parsed.payload)
        }
        if (validationErrors.isNotEmpty()) {
            return Result.failure(OfdCodecException(validationErrors))
        }

        val payloadBytes = try {
            handler.requestSerializer.serialize(parsed.commandType, parsed.payload)
        } catch (ex: Exception) {
            val error = ErrorFactory.error(
                ErrorCode.SERIALIZATION_FAILED,
                "$.${JsonKeys.PAYLOAD}",
                mapOf("reason" to (ex.message ?: ex::class.simpleName.orEmpty()))
            )
            return Result.failure(OfdCodecException(listOf(error)))
        }

        val header = MessageHeader(
            appCode = parsed.header.appCode,
            protocolVersion = parsed.protocolVersion,
            size = 0,
            deviceId = parsed.header.deviceId,
            token = parsed.header.token,
            reqNum = parsed.header.reqNum
        )
        val headerBytes = HeaderCodec.encode(header, payloadBytes.size)
        val message = ByteArray(headerBytes.size + payloadBytes.size)
        System.arraycopy(headerBytes, 0, message, 0, headerBytes.size)
        System.arraycopy(payloadBytes, 0, message, headerBytes.size, payloadBytes.size)

        val response = buildJsonObject {
            put(JsonKeys.SIZE, JsonPrimitive(message.size))
            put(JsonKeys.MESSAGE_BASE64, JsonPrimitive(Base64.getEncoder().encodeToString(message)))
        }
        return Result.success(response)
    }

    /**
     * Декодирование ответа от ОФД в JSON-представление.
     * В случае ошибок возвращает Result.failure с OfdCodecException.
     */
    fun decode(bytes: ByteArray): Result<JsonObject> {
        val headerResult = HeaderCodec.decode(bytes)
        if (headerResult is HeaderDecodeResult.Errors) {
            return Result.failure(OfdCodecException(headerResult.errors))
        }

        val header = (headerResult as HeaderDecodeResult.Success).header
        if (header.size > bytes.size.toLong()) {
            return Result.failure(
                OfdCodecException(listOf(ErrorFactory.error(ErrorCode.HEADER_INVALID_SIZE, "$.header.size")))
            )
        }
        val payloadBytes = bytes.copyOfRange(
            HeaderConstants.HEADER_SIZE,
            header.size.toInt().coerceAtMost(bytes.size)
        )
        val ofdId = ofdResolver.resolve(header, payloadBytes, registry)
        if (ofdId == null) {
            return Result.failure(
                OfdCodecException(listOf(ErrorFactory.error(ErrorCode.MESSAGE_UNDETERMINED_OFD, "$.ofdId")))
            )
        }

        val protocolVersionText = ProtocolVersion.toNumericString(header.protocolVersion)
        val handler = registry.find(ofdId, protocolVersionText)
        if (handler == null) {
            val error = ErrorFactory.error(
                ErrorCode.PROTOCOL_UNSUPPORTED,
                "$.${JsonKeys.PROTOCOL_VERSION}",
                mapOf("ofdId" to ofdId, "version" to protocolVersionText)
            )
            return Result.failure(OfdCodecException(listOf(error)))
        }

        val payload = try {
            handler.responseDeserializer.deserialize(payloadBytes)
        } catch (ex: Exception) {
            val error = ErrorFactory.error(
                ErrorCode.DESERIALIZATION_FAILED,
                "$.${JsonKeys.PAYLOAD}",
                mapOf("reason" to (ex.message ?: ex::class.simpleName.orEmpty()))
            )
            return Result.failure(OfdCodecException(listOf(error)))
        }
        val (commandType, commandError) = extractCommandType(payload)
        if (commandError != null) {
            return Result.failure(OfdCodecException(listOf(commandError)))
        }
        val validationErrors = handler.responseValidator.validate(commandType, payload)
        if (validationErrors.isNotEmpty()) {
            return Result.failure(OfdCodecException(validationErrors))
        }
        val envelope = JsonEnvelopeBuilder.build(ofdId, MessageType.RESPONSE, commandType, header, payload)
        return Result.success(envelope)
    }

    private fun extractCommandType(payload: JsonObject): Pair<CommandType, ValidationError?> {
        val element = payload[JsonKeys.COMMAND_TYPE] ?: return Pair(
            CommandType.COMMAND_RESERVED,
            ErrorFactory.error(
                ErrorCode.JSON_MISSING_FIELD,
                "$.${JsonKeys.COMMAND_TYPE}",
                mapOf("field" to JsonKeys.COMMAND_TYPE)
            )
        )
        if (element !is JsonPrimitive || !element.isString) {
            return Pair(
                CommandType.COMMAND_RESERVED,
                ErrorFactory.error(
                    ErrorCode.JSON_INVALID_TYPE,
                    "$.${JsonKeys.COMMAND_TYPE}",
                    mapOf("field" to JsonKeys.COMMAND_TYPE)
                )
            )
        }
        val command = CommandType.fromName(element.content)
        if (command == null) {
            return Pair(
                CommandType.COMMAND_RESERVED,
                ErrorFactory.error(
                    ErrorCode.COMMAND_UNSUPPORTED,
                    "$.${JsonKeys.COMMAND_TYPE}",
                    mapOf("command" to element.content)
                )
            )
        }
        return Pair(command, null)
    }

    companion object {
        private fun defaultResolver(): OfdResolver {
            return OfdResolver { _, _, registry ->
                val ofdIds = registry.ofdIds()
                if (ofdIds.size == 1) ofdIds.first() else null
            }
        }
    }
}
