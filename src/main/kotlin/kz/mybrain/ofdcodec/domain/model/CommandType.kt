package kz.mybrain.ofdcodec.domain.model

/**
 * Команды протокола обмена ККМ ↔ ОФД.
 */
enum class CommandType(val code: Int, val title: String) {
    COMMAND_SYSTEM(0, "Системный обмен"),
    COMMAND_TICKET(1, "Фискализация"),
    COMMAND_CLOSE_SHIFT(2, "Закрытие смены"),
    COMMAND_REPORT(3, "Запрос отчета"),
    COMMAND_NOMENCLATURE(4, "Запрос номенклатуры"),
    COMMAND_INFO(5, "Инициализация"),
    COMMAND_MONEY_PLACEMENT(6, "Внесение/снятие денег"),
    COMMAND_AUTH(8, "Авторизация"),
    COMMAND_RESERVED(127, "Зарезервировано");

    companion object {
        fun fromName(name: String): CommandType? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
