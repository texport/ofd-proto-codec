package kz.mybrain.ofdcodec.infrastructure.util

/**
 * Чистая KMP-реализация контрольной суммы CRC32.
 * Использует предвычисленную таблицу для быстрого расчета CRC32 от массива байт.
 */
object Crc32 {
    private const val TABLE_SIZE = 256
    private const val NUM_BITS = 8
    private const val CRC_POLYNOMIAL = -0x112320af // 0xEDB88320
    private const val BYTE_MASK = 0xFF
    private const val MASK_32_BIT = 0xFFFFFFFFL

    private val TABLE = IntArray(TABLE_SIZE) { i ->
        var entry = i
        repeat(NUM_BITS) {
            entry = if (entry and 1 != 0) {
                (entry ushr 1) xor CRC_POLYNOMIAL
            } else {
                entry ushr 1
            }
        }
        entry
    }

    /**
     * Вычисляет CRC32-хеш для массива байтов.
     */
    fun calculate(bytes: ByteArray): Long {
        var crc = -1
        for (b in bytes) {
            val index = (crc xor b.toInt()) and BYTE_MASK
            crc = (crc ushr NUM_BITS) xor TABLE[index]
        }
        return crc.toLong() and MASK_32_BIT
    }
}
