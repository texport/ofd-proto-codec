package kz.mybrain.ofdcodec

/**
 * Единая точка получения номера запроса для сетевых тестов.
 *
 * Позволяет запускать все интеграционные тесты одним набором параметров,
 * не сталкиваясь с повторяющимися REQNUM.
 */
object TestReqNum {
    /**
     * Возвращает базовое значение REQNUM из окружения/системных свойств.
     *
     * Если OFD_TEST_REQNUM_BASE не задан, используется безопасное значение по умолчанию.
     */
    fun base(): Int {
        return (System.getenv("OFD_TEST_REQNUM_BASE") ?: System.getProperty("OFD_TEST_REQNUM_BASE"))
            ?.toIntOrNull()
            ?: 1000
    }

    /**
     * Возвращает итоговый REQNUM как base + offset.
     */
    fun value(offset: Int): Int = base() + offset
}
