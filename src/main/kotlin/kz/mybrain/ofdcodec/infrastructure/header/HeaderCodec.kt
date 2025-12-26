package kz.mybrain.ofdcodec.infrastructure.header

import kz.mybrain.ofdcodec.domain.model.ErrorCode
import kz.mybrain.ofdcodec.domain.model.ErrorFactory
import kz.mybrain.ofdcodec.domain.model.HeaderConstants
import kz.mybrain.ofdcodec.domain.model.MessageHeader
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.infrastructure.util.ProtocolVersion

object HeaderCodec {
    /**
     * Читает заголовок сообщения (little-endian).
     */
    fun decode(bytes: ByteArray): HeaderDecodeResult {
        if (bytes.size < HeaderConstants.HEADER_SIZE) {
            return HeaderDecodeResult.Errors(
                listOf(ErrorFactory.error(ErrorCode.HEADER_TOO_SHORT, "$"))
            )
        }

        val appCode = readU16(bytes, 0)
        val version = readU16(bytes, 2)
        val size = readU32(bytes, 4)
        val deviceId = readU32(bytes, 8)
        val token = readU32(bytes, 12)
        val reqNum = readU16(bytes, 16)

        val errors = mutableListOf<ValidationError>()
        if (appCode != HeaderConstants.APPCODE) {
            errors.add(ErrorFactory.error(ErrorCode.HEADER_INVALID_APPCODE, "$.header.appCode"))
        }
        if (!ProtocolVersion.isValidNumericVersion(version)) {
            errors.add(ErrorFactory.error(ErrorCode.HEADER_INVALID_VERSION_FORMAT, "$.header.protocolVersion"))
        }
        if (size <= 0L) {
            errors.add(ErrorFactory.error(ErrorCode.HEADER_INVALID_SIZE, "$.header.size"))
        } else if (size < HeaderConstants.HEADER_SIZE.toLong()) {
            errors.add(ErrorFactory.error(ErrorCode.HEADER_INVALID_SIZE, "$.header.size"))
        }

        if (errors.isNotEmpty()) {
            return HeaderDecodeResult.Errors(errors)
        }

        val header = MessageHeader(
            appCode = appCode,
            protocolVersion = version,
            size = size,
            deviceId = deviceId,
            token = token,
            reqNum = reqNum
        )
        return HeaderDecodeResult.Success(header)
    }

    /**
     * Собирает заголовок сообщения и подставляет общий размер.
     */
    fun encode(header: MessageHeader, payloadSize: Int): ByteArray {
        val totalSize = HeaderConstants.HEADER_SIZE.toLong() + payloadSize.toLong()
        val normalized = header.copy(appCode = HeaderConstants.APPCODE, size = totalSize)
        val bytes = ByteArray(HeaderConstants.HEADER_SIZE)
        writeU16(bytes, 0, normalized.appCode)
        writeU16(bytes, 2, normalized.protocolVersion)
        writeU32(bytes, 4, normalized.size)
        writeU32(bytes, 8, normalized.deviceId)
        writeU32(bytes, 12, normalized.token)
        writeU16(bytes, 16, normalized.reqNum)
        return bytes
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int {
        val b0 = bytes[offset].toInt() and 0xFF
        val b1 = bytes[offset + 1].toInt() and 0xFF
        return b0 or (b1 shl 8)
    }

    private fun readU32(bytes: ByteArray, offset: Int): Long {
        val b0 = bytes[offset].toLong() and 0xFF
        val b1 = bytes[offset + 1].toLong() and 0xFF
        val b2 = bytes[offset + 2].toLong() and 0xFF
        val b3 = bytes[offset + 3].toLong() and 0xFF
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun writeU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun writeU32(bytes: ByteArray, offset: Int, value: Long) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((value shr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }
}
