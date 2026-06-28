package kz.mybrain.ofdcodec.infrastructure.util

/**
 * Чистая KMP-реализация контрольной суммы CRC32.
 * Использует предвычисленную таблицу для быстрого расчета CRC32 от массива байт.
 */
object Crc32 {
    private val TABLE = IntArray(256) { i ->
        var entry = i
        for (j in 0..7) {
            entry = if (entry and 1 != 0) {
                (entry ushr 1) xor -0x112320af // 0xEDB88320
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
            val index = (crc xor b.toInt()) and 0xFF
            crc = (crc ushr 8) xor TABLE[index]
        }
        return crc.toLong() and 0xFFFFFFFFL
    }
}
