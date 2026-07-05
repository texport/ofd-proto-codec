package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kz.kazakhtelecom.proto.v203.*
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.ServiceRequestBuilder

/**
 * Сборщик Request для COMMAND_AUTH.
 */
internal class CommandAuthRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()

    /**
     * Строит proto Request для COMMAND_AUTH на основе JSON payload.
     */
    override fun build(json: JsonObject): Request {
        val serviceRequest = serviceRequestBuilder.build(json)
        val authJson = json["auth"] as? JsonObject
            ?: throw IllegalArgumentException("Missing auth / Отсутствует блок auth / auth блогы жетіспейді")

        val loginPrimitive = authJson["login"] as? JsonPrimitive
            ?: throw IllegalArgumentException("Missing login in auth / Отсутствует login в auth / auth ішіндегі login жетіспейді")
        val login = loginPrimitive.content

        val passwordPrimitive = authJson["password"] as? JsonPrimitive
            ?: throw IllegalArgumentException("Missing password in auth / Отсутствует password в auth / auth ішіндегі password жетіспейді")
        val password = passwordPrimitive.content

        val authRequest = AuthRequest(
            login = login,
            password = password
        )

        return Request(
            command = CommandTypeEnum.COMMAND_AUTH,
            service = serviceRequest,
            auth = authRequest
        )
    }
}
