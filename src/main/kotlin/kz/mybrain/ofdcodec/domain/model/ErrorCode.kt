package kz.mybrain.ofdcodec.domain.model

/**
 * Коды ошибок с локализованными сообщениями (RU/EN).
 */
enum class ErrorCode(val ru: String, val kk: String, val en: String) {
    JSON_MISSING_FIELD(
        "Отсутствует обязательное поле: {field}",
        "Міндетті өріс жетіспейді: {field}",
        "Missing required field: {field}"
    ),
    JSON_INVALID_TYPE(
        "Неверный тип поля: {field}",
        "Өріс түрі қате: {field}",
        "Invalid type for field: {field}"
    ),
    JSON_INVALID_VALUE(
        "Неверное значение поля: {field}",
        "Өріс мәні қате: {field}",
        "Invalid value for field: {field}"
    ),
    PROTOCOL_UNSUPPORTED(
        "Неподдерживаемые параметры протокола: ОФД={ofdId}, версия={version}",
        "Хаттама параметрлері қолданылмайды: ОФД={ofdId}, нұсқасы={version}",
        "Unsupported protocol parameters: OFD={ofdId}, version={version}"
    ),
    HEADER_TOO_SHORT(
        "Сообщение короче заголовка",
        "Хабарлама тақырыптан қысқа",
        "Message is too short for header"
    ),
    HEADER_INVALID_APPCODE(
        "Неверный APPCODE",
        "Қате APPCODE",
        "Invalid APPCODE"
    ),
    HEADER_INVALID_VERSION_FORMAT(
        "Неверный формат версии протокола",
        "Хаттама нұсқасының форматы қате",
        "Invalid protocol version format"
    ),
    HEADER_INVALID_SIZE(
        "Неверный размер в заголовке",
        "Тақырыптағы өлшем қате",
        "Invalid size in header"
    ),
    MESSAGE_UNDETERMINED_OFD(
        "Невозможно определить ОФД по сообщению",
        "Хабарлама бойынша ОФД анықтау мүмкін емес",
        "Cannot determine OFD id for decode"
    ),
    ENCODE_UNSUPPORTED_MESSAGE_TYPE(
        "Кодирование поддерживается только для REQUEST",
        "Кодтау тек REQUEST үшін қолданылады",
        "Encoding is supported only for REQUEST messages"
    ),
    COMMAND_UNSUPPORTED(
        "Неподдерживаемая команда: {command}",
        "Қолданылмайтын пәрмен: {command}",
        "Unsupported command: {command}"
    ),
    SERIALIZATION_FAILED(
        "Ошибка сериализации payload: {reason}",
        "Payload сериялау қатесі: {reason}",
        "Payload serialization failed: {reason}"
    ),
    DESERIALIZATION_FAILED(
        "Ошибка десериализации payload: {reason}",
        "Payload десериялау қатесі: {reason}",
        "Payload deserialization failed: {reason}"
    )
}
