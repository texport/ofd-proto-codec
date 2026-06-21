package kz.mybrain.ofdcodec

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kz.kazakhtelecom.proto.v203.Auth
import kz.kazakhtelecom.proto.v203.Common
import kz.kazakhtelecom.proto.v203.Message
import kz.mybrain.ofdcodec.application.DefaultRegistry
import kz.mybrain.ofdcodec.application.OfdCodec
import kz.mybrain.ofdcodec.domain.model.MessageHeader
import kz.mybrain.ofdcodec.domain.model.OfdCodecException
import kz.mybrain.ofdcodec.infrastructure.header.HeaderCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Тесты для основного фасада OfdCodec, проверяющие команду COMMAND_AUTH
 * и локализованные сообщения об ошибках на трех языках.
 */
class OfdCodecTest {

    private fun buildBaseRequestJson(payloadFields: String): String {
        return """
            {
              "ofdId": "kazakhtelecom",
              "protocolVersion": "203",
              "messageType": "REQUEST",
              "commandType": "COMMAND_AUTH",
              "header": {
                "deviceId": 201873,
                "token": 0,
                "reqNum": 1
              },
              "payload": {
                "service": {
                  "getRegInfo": true,
                  "offlinePeriod": {
                    "beginTime": {
                      "date": { "year": 2024, "month": 9, "day": 1 },
                      "time": { "hour": 10, "minute": 30, "second": 0 }
                    },
                    "endTime": {
                      "date": { "year": 2024, "month": 9, "day": 1 },
                      "time": { "hour": 10, "minute": 40, "second": 0 }
                    }
                  },
                  "securityStats": {
                    "geoPosition": {
                      "latitude": 432156,
                      "longitude": 765432,
                      "source": "CELL"
                    }
                  },
                  "regInfo": {
                    "kkm": {
                      "fnsKkmId": "391827192812",
                      "serialNumber": "5465434234",
                      "kkmId": "201873"
                    },
                    "org": {
                      "title": "ИП МИЧКА ПАВЕЛ АНДРЕЕВИЧ",
                      "address": "обл. Павлодарская, Ауэзова 88",
                      "addressKz": "Республика Қазақстан, обл. Павлодарская, қ. Екібастұз, Ауэзова 88",
                      "inn": "960624350642",
                      "okved": "47301"
                    }
                  }
                }
                ${if (payloadFields.isNotEmpty()) ", $payloadFields" else ""}
              }
            }
        """.trimIndent()
    }

    @Test
    fun shouldEncodeCommandAuth() {
        val requestJsonString = buildBaseRequestJson(
            """
            "auth": {
              "login": "my_login",
              "password": "my_password"
            }
            """.trimIndent()
        )
        val requestJson = Json.parseToJsonElement(requestJsonString)

        val codec = OfdCodec(DefaultRegistry.create())
        val result = codec.encode(requestJson)

        assertTrue(result.isSuccess, "Encoding should be successful: ${result.exceptionOrNull()?.message}")
        val output = result.getOrNull()
        assertNotNull(output)

        val size = output["size"]?.jsonPrimitive?.content?.toInt()
        val messageBase64 = output["messageBase64"]?.jsonPrimitive?.content
        assertNotNull(size)
        assertNotNull(messageBase64)
        assertTrue(size > 18)

        val bytes = java.util.Base64.getDecoder().decode(messageBase64)
        assertEquals(size, bytes.size)

        // Извлекаем payload и проверяем proto-структуру
        val payloadBytes = bytes.copyOfRange(18, bytes.size)
        val protoRequest = Message.Request.parseFrom(payloadBytes)
        assertEquals(Message.CommandTypeEnum.COMMAND_AUTH, protoRequest.command)
        assertEquals("my_login", protoRequest.auth.login)
        assertEquals("my_password", protoRequest.auth.password)
    }

    @Test
    fun shouldDecodeCommandAuthSuccessResponse() {
        val authResponse = Auth.AuthResponse.newBuilder()
            .setResult(Auth.AuthResponse.ResultTypeEnum.RESULT_TYPE_OK)
            .setOperatorCode(123)
            .setOperatorName("Иван Иванов")
            .addRoles(Common.UserRoleEnum.USER_ROLE_ADMINISTRATOR)
            .addRoles(Common.UserRoleEnum.USER_ROLE_PAYMASTER)
            .build()

        val protoResponse = Message.Response.newBuilder()
            .setCommand(Message.CommandTypeEnum.COMMAND_AUTH)
            .setResult(
                Message.Result.newBuilder()
                    .setResultCode(0)
                    .setResultText("Success")
                    .build()
            )
            .setAuth(authResponse)
            .build()

        val payloadBytes = protoResponse.toByteArray()
        val header = MessageHeader(
            appCode = 0x81A2,
            protocolVersion = 203,
            size = 0,
            deviceId = 201873,
            token = 123456L,
            reqNum = 1
        )
        val headerBytes = HeaderCodec.encode(header, payloadBytes.size)
        val messageBytes = ByteArray(headerBytes.size + payloadBytes.size)
        System.arraycopy(headerBytes, 0, messageBytes, 0, headerBytes.size)
        System.arraycopy(payloadBytes, 0, messageBytes, headerBytes.size, payloadBytes.size)

        val codec = OfdCodec(DefaultRegistry.create())
        val result = codec.decode(messageBytes)

        assertTrue(result.isSuccess, "Decoding should be successful: ${result.exceptionOrNull()?.message}")
        val envelope = result.getOrNull()
        assertNotNull(envelope)

        assertEquals("kazakhtelecom", envelope["ofdId"]?.jsonPrimitive?.content)
        assertEquals("203", envelope["protocolVersion"]?.jsonPrimitive?.content)
        assertEquals("RESPONSE", envelope["messageType"]?.jsonPrimitive?.content)
        assertEquals("COMMAND_AUTH", envelope["commandType"]?.jsonPrimitive?.content)

        val payload = envelope["payload"]?.jsonObject
        assertNotNull(payload)
        assertEquals("RESULT_TYPE_OK", payload["auth"]?.jsonObject?.get("result")?.jsonPrimitive?.content)
        assertEquals("123", payload["auth"]?.jsonObject?.get("operatorCode")?.jsonPrimitive?.content)
        assertEquals("Иван Иванов", payload["auth"]?.jsonObject?.get("operatorName")?.jsonPrimitive?.content)

        val roles = payload["auth"]?.jsonObject?.get("roles")?.jsonArray
        assertNotNull(roles)
        assertEquals(2, roles.size)
        assertEquals("USER_ROLE_ADMINISTRATOR", roles[0].jsonPrimitive.content)
        assertEquals("USER_ROLE_PAYMASTER", roles[1].jsonPrimitive.content)
    }

    @Test
    fun shouldFailValidationOnMissingOrInvalidFields() {
        // Missing auth block completely
        val requestJsonMissingAuth = Json.parseToJsonElement(buildBaseRequestJson(""))

        val codec = OfdCodec(DefaultRegistry.create())
        val result = codec.encode(requestJsonMissingAuth)

        assertTrue(result.isFailure, "Encoding should fail due to missing auth block")
        val exception = result.exceptionOrNull()
        assertTrue(exception is OfdCodecException)

        val message = exception.message
        assertNotNull(message)
        assertTrue(message.contains("Missing required field: payload.auth"))
        assertTrue(message.contains("Отсутствует обязательное поле: payload.auth"))
        assertTrue(message.contains("Міндетті өріс жетіспейді: payload.auth"))

        // Missing login in auth
        val requestJsonMissingLogin = Json.parseToJsonElement(
            buildBaseRequestJson(
                """
                "auth": {
                  "password": "my_password"
                }
                """.trimIndent()
            )
        )

        val result2 = codec.encode(requestJsonMissingLogin)
        assertTrue(result2.isFailure)
        val exception2 = result2.exceptionOrNull() as OfdCodecException
        val message2 = exception2.message
        assertNotNull(message2)
        assertTrue(message2.contains("Missing required field: payload.auth.login"))
        assertTrue(message2.contains("Отсутствует обязательное поле: payload.auth.login"))
        assertTrue(message2.contains("Міндетті өріс жетіспейді: payload.auth.login"))
    }

    @Test
    fun shouldThrowTrilingualExceptionInBuilderDirectly() {
        val builder = kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.codec.command.CommandAuthRequestBuilder()

        // Test missing auth block
        val ex1 = kotlin.test.assertFailsWith<IllegalArgumentException> {
            builder.build(buildJsonObject { })
        }
        assertTrue(ex1.message!!.contains("Missing auth"))
        assertTrue(ex1.message!!.contains("Отсутствует блок auth"))
        assertTrue(ex1.message!!.contains("auth блогы жетіспейді"))

        // Test missing login in auth
        val ex2 = kotlin.test.assertFailsWith<IllegalArgumentException> {
            builder.build(
                buildJsonObject {
                    put(
                        "auth",
                        buildJsonObject {
                            put("password", "123")
                        }
                    )
                }
            )
        }
        assertTrue(ex2.message!!.contains("Missing login in auth"))
        assertTrue(ex2.message!!.contains("Отсутствует login в auth"))
        assertTrue(ex2.message!!.contains("auth ішіндегі login жетіспейді"))
    }
}
