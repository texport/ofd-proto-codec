package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kz.kazakhtelecom.proto.v203.Auth
import kz.kazakhtelecom.proto.v203.Message
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.service.ServiceRequestBuilder

/**
 * Сборщик Request для COMMAND_AUTH.
 */
class CommandAuthRequestBuilder : CommandRequestBuilder {
    private val serviceRequestBuilder = ServiceRequestBuilder()

    /**
     * Строит proto Request для COMMAND_AUTH на основе JSON payload.
     */
    override fun build(json: JsonObject): Message.Request {
        val serviceRequest = serviceRequestBuilder.build(json)
        val authJson = json["auth"] as? JsonObject
            ?: throw IllegalArgumentException("Missing auth / Отсутствует блок auth / auth блогы жетіспейді")

        val login = (authJson["login"] as? JsonPrimitive)?.content
            ?: throw IllegalArgumentException("Missing login in auth / Отсутствует login в auth / auth ішіндегі login жетіспейді")
        val password = (authJson["password"] as? JsonPrimitive)?.content
            ?: throw IllegalArgumentException("Missing password in auth / Отсутствует password в auth / auth ішіндегі password жетіспейді")

        val authRequest = Auth.AuthRequest.newBuilder()
            .setLogin(login)
            .setPassword(password)
            .build()

        return Message.Request.newBuilder()
            .setCommand(Message.CommandTypeEnum.COMMAND_AUTH)
            .setService(serviceRequest)
            .setAuth(authRequest)
            .build()
    }
}
