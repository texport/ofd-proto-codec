package kz.mybrain.ofdcodec.domain.model

/**
 * Коды ошибок с локализованными сообщениями (RU/EN).
 */
enum class ErrorCode(val ru: String, val en: String) {
    JSON_MISSING_FIELD("Отсутствует обязательное поле: {field}", "Missing required field: {field}"),
    JSON_INVALID_TYPE("Неверный тип поля: {field}", "Invalid type for field: {field}"),
    JSON_INVALID_VALUE("Неверное значение поля: {field}", "Invalid value for field: {field}"),
    PROTOCOL_UNSUPPORTED("Неподдерживаемые параметры протокола: ОФД={ofdId}, версия={version}", "Unsupported protocol parameters: OFD={ofdId}, version={version}"),
    HEADER_TOO_SHORT("Сообщение короче заголовка", "Message is too short for header"),
    HEADER_INVALID_APPCODE("Неверный APPCODE", "Invalid APPCODE"),
    HEADER_INVALID_VERSION_FORMAT("Неверный формат версии протокола", "Invalid protocol version format"),
    HEADER_INVALID_SIZE("Неверный размер в заголовке", "Invalid size in header"),
    MESSAGE_UNDETERMINED_OFD("Невозможно определить ОФД по сообщению", "Cannot determine OFD id for decode"),
    ENCODE_UNSUPPORTED_MESSAGE_TYPE("Кодирование поддерживается только для REQUEST", "Encoding is supported only for REQUEST messages"),
    COMMAND_UNSUPPORTED("Неподдерживаемая команда: {command}", "Unsupported command: {command}"),
    SERIALIZATION_FAILED("Ошибка сериализации payload: {reason}", "Payload serialization failed: {reason}"),
    DESERIALIZATION_FAILED("Ошибка десериализации payload: {reason}", "Payload deserialization failed: {reason}")
}
