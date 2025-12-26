package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.model

/**
 * Коды статуса ответа сервера для протокола Казахтелеком 2.0.3.
 */
enum class ResultType(
    val code: Int,
    val title: String,
    val descriptionRu: String,
    val descriptionEn: String
) {
    RESULT_TYPE_OK(
        0,
        "RESULT_TYPE_OK",
        "Команда выполнена успешно. Аппарат работает в штатном режиме.",
        "Command executed successfully. The device operates normally."
    ),
    RESULT_TYPE_UNKNOWN_ID(
        1,
        "RESULT_TYPE_UNKNOWN_ID",
        "Неизвестный ID устройства. Аппарат не зарегистрирован в системе. Устройство должно заблокироваться.",
        "Unknown device ID. The device is not registered in the system. The device must block itself."
    ),
    RESULT_TYPE_INVALID_TOKEN(
        2,
        "RESULT_TYPE_INVALID_TOKEN",
        "Неверный токен. Отправка данных невозможна, необходимо произвести сброс токена.",
        "Invalid token. Data sending is impossible; the token must be reset."
    ),
    RESULT_TYPE_PROTOCOL_ERROR(
        3,
        "RESULT_TYPE_PROTOCOL_ERROR",
        "Ошибка протокола. Обратитесь в сервисную службу.",
        "Protocol error. Contact service support."
    ),
    RESULT_TYPE_UNKNOWN_COMMAND(
        4,
        "RESULT_TYPE_UNKNOWN_COMMAND",
        "Неизвестная команда. Обратитесь в сервисную службу.",
        "Unknown command. Contact service support."
    ),
    RESULT_TYPE_UNSUPPORTED_COMMAND(
        5,
        "RESULT_TYPE_UNSUPPORTED_COMMAND",
        "Команда не поддерживается сервером. Обратитесь в сервисную службу.",
        "Command is not supported by the server. Contact service support."
    ),
    RESULT_TYPE_INVALID_CONFIGURATION(
        6,
        "RESULT_TYPE_INVALID_CONFIGURATION",
        "Неверные настройки устройства.",
        "Invalid device configuration."
    ),
    RESULT_TYPE_SSL_IS_NOT_ALLOWED(
        7,
        "RESULT_TYPE_SSL_IS_NOT_ALLOWED",
        "Использование SSL не разрешено. Подключите услугу или используйте открытый канал связи.",
        "SSL is not allowed. Enable the service or use a non-encrypted channel."
    ),
    RESULT_TYPE_INVALID_REQUEST_NUMBER(
        8,
        "RESULT_TYPE_INVALID_REQUEST_NUMBER",
        "Неправильный номер запроса. REQNUM совпадает с предыдущим, но токен другой.",
        "Invalid request number. REQNUM matches the previous request but token differs."
    ),
    RESULT_TYPE_INVALID_RETRY_REQUEST(
        9,
        "RESULT_TYPE_INVALID_RETRY_REQUEST",
        "Неправильная повторная отправка. REQNUM и TOKEN те же, но команда отличается.",
        "Invalid retry request. REQNUM and TOKEN match the previous request but command differs."
    ),
    RESULT_TYPE_OPEN_SHIFT_TIMEOUT_EXPIRED(
        11,
        "RESULT_TYPE_OPEN_SHIFT_TIMEOUT_EXPIRED",
        "Время открытой смены истекло. Сервер будет возвращать ошибку до закрытия смены.",
        "Open shift timeout expired. The server will return this error until the shift is closed."
    ),
    RESULT_TYPE_INVALID_LOGIN_PASSWORD(
        12,
        "RESULT_TYPE_INVALID_LOGIN_PASSWORD",
        "Неправильное имя или пароль (устаревший код).",
        "Invalid login or password (deprecated code)."
    ),
    RESULT_TYPE_INCORRECT_REQUEST_DATA(
        13,
        "RESULT_TYPE_INCORRECT_REQUEST_DATA",
        "Неверные входные данные. Исправьте данные и отправьте повторно.",
        "Incorrect request data. Fix the data and resend."
    ),
    RESULT_TYPE_NOT_ENOUGH_CASH(
        14,
        "RESULT_TYPE_NOT_ENOUGH_CASH",
        "Недостаточно наличных для операции.",
        "Not enough cash for the operation."
    ),
    RESULT_TYPE_BLOCKED(
        15,
        "RESULT_TYPE_BLOCKED",
        "Касса заблокирована. Устройство должно перейти в режим блокировки.",
        "Cash register is blocked. The device must enter a blocked state."
    ),
    RESULT_TYPE_SAME_TAXPAYER_AND_CUSTOMER(
        17,
        "RESULT_TYPE_SAME_TAXPAYER_AND_CUSTOMER",
        "Совпадает ИИН/БИН покупателя и продавца. Отправьте чек повторно.",
        "Taxpayer and customer IDs match. Resend the receipt."
    ),
    RESULT_TYPE_SERVICE_TEMPORARILY_UNAVAILABLE(
        254,
        "RESULT_TYPE_SERVICE_TEMPORARILY_UNAVAILABLE",
        "Сервис временно недоступен. Повторяйте попытки до таймаута, затем переходите в автономный режим.",
        "Service is temporarily unavailable. Retry until timeout, then switch to offline mode."
    ),
    RESULT_TYPE_UNKNOWN_ERROR(
        255,
        "RESULT_TYPE_UNKNOWN_ERROR",
        "Неизвестная ошибка. Повторяйте попытки до таймаута, затем переходите в автономный режим.",
        "Unknown error. Retry until timeout, then switch to offline mode."
    );

    companion object {
        fun fromCode(code: Int): ResultType? = entries.firstOrNull { it.code == code }
    }
}
