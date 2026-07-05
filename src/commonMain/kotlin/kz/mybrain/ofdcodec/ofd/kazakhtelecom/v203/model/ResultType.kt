package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.model

/**
 * Коды статуса ответа сервера для текущего provider module 2.0.3.
 */
internal enum class ResultType(
    val code: Int,
    val title: String,
    val descriptionRu: String,
    val descriptionKz: String,
    val descriptionEn: String
) {
    RESULT_TYPE_OK(
        0,
        "RESULT_TYPE_OK",
        "Команда выполнена успешно. Аппарат работает в штатном режиме.",
        "Пәрмен сәтті орындалды. Құрылғы қалыпты режимде жұмыс істейді.",
        "Command executed successfully. The device operates normally."
    ),
    RESULT_TYPE_UNKNOWN_ID(
        1,
        "RESULT_TYPE_UNKNOWN_ID",
        "Неизвестный ID устройства. Аппарат не зарегистрирован в системе. Устройство должно заблокироваться.",
        "Құрылғының ID-і белгісіз. Құрылғы жүйеде тіркелмеген. Құрылғы бұғатталуы тиіс.",
        "Unknown device ID. The device is not registered in the system. The device must block itself."
    ),
    RESULT_TYPE_INVALID_TOKEN(
        2,
        "RESULT_TYPE_INVALID_TOKEN",
        "Неверный токен. Отправка данных невозможна, необходимо произвести сброс токена.",
        "Қате токен. Мәліметтерді жіберу мүмкін емес, токенді қайта орнату қажет.",
        "Invalid token. Data sending is impossible; the token must be reset."
    ),
    RESULT_TYPE_PROTOCOL_ERROR(
        3,
        "RESULT_TYPE_PROTOCOL_ERROR",
        "Ошибка протокола. Обратитесь в сервисную службу.",
        "Хаттама қатесі. Қызмет көрсету орталығына хабарласыңыз.",
        "Protocol error. Contact service support."
    ),
    RESULT_TYPE_UNKNOWN_COMMAND(
        4,
        "RESULT_TYPE_UNKNOWN_COMMAND",
        "Неизвестная команда. Обратитесь в сервисную службу.",
        "Белгісіз пәрмен. Қызмет көрсету орталығына хабарласыңыз.",
        "Unknown command. Contact service support."
    ),
    RESULT_TYPE_UNSUPPORTED_COMMAND(
        5,
        "RESULT_TYPE_UNSUPPORTED_COMMAND",
        "Команда не поддерживается сервером. Обратитесь в сервисную службу.",
        "Пәрменге сервер қолдау көрсетпейді. Қызмет көрсету орталығына хабарласыңыз.",
        "Command is not supported by the server. Contact service support."
    ),
    RESULT_TYPE_INVALID_CONFIGURATION(
        6,
        "RESULT_TYPE_INVALID_CONFIGURATION",
        "Неверные настройки устройства.",
        "Құрылғының қате баптаулары.",
        "Invalid device configuration."
    ),
    RESULT_TYPE_SSL_IS_NOT_ALLOWED(
        7,
        "RESULT_TYPE_SSL_IS_NOT_ALLOWED",
        "Использование SSL не разрешено. Подключите услугу или используйте открытый канал связи.",
        "SSL пайдалануға рұқсат етілмейді. Қызметті қосыңыз немесе ашық байланыс арнасын пайдаланыңыз.",
        "SSL is not allowed. Enable the service or use a non-encrypted channel."
    ),
    RESULT_TYPE_INVALID_REQUEST_NUMBER(
        8,
        "RESULT_TYPE_INVALID_REQUEST_NUMBER",
        "Неправильный номер запроса. REQNUM совпадает с предыдущим, но токен другой.",
        "Сұраныстың қате нөмірі. REQNUM алдыңғымен сәйкес келеді, бірақ токен басқа.",
        "Invalid request number. REQNUM matches the previous request but token differs."
    ),
    RESULT_TYPE_INVALID_RETRY_REQUEST(
        9,
        "RESULT_TYPE_INVALID_RETRY_REQUEST",
        "Неправильная повторная отправка. REQNUM и TOKEN те же, но команда отличается.",
        "Қате қайталанған сұраныс. REQNUM және TOKEN сәйкес келеді, бірақ пәрмен басқа.",
        "Invalid retry request. REQNUM and TOKEN match the previous request but command differs."
    ),
    RESULT_TYPE_OPEN_SHIFT_TIMEOUT_EXPIRED(
        11,
        "RESULT_TYPE_OPEN_SHIFT_TIMEOUT_EXPIRED",
        "Время открытой смены истекло. Сервер будет возвращать ошибку до закрытия смены.",
        "Ашық ауысым уақыты аяқталды. Ауысым жабылғанша сервер қате қайтарады.",
        "Open shift timeout expired. The server will return this error until the shift is closed."
    ),
    RESULT_TYPE_INVALID_LOGIN_PASSWORD(
        12,
        "RESULT_TYPE_INVALID_LOGIN_PASSWORD",
        "Неправильное имя или пароль (устаревший код).",
        "Қате логин немесе пароль (ескірген код).",
        "Invalid login or password (deprecated code)."
    ),
    RESULT_TYPE_INCORRECT_REQUEST_DATA(
        13,
        "RESULT_TYPE_INCORRECT_REQUEST_DATA",
        "Неверные входные данные. Исправьте данные и отправьте повторно.",
        "Қате кіріс мәліметтері. Мәліметтерді түзетіп, қайта жіберіңіз.",
        "Incorrect request data. Fix the data and resend."
    ),
    RESULT_TYPE_NOT_ENOUGH_CASH(
        14,
        "RESULT_TYPE_NOT_ENOUGH_CASH",
        "Недостаточно наличных для операции.",
        "Операция үшін қолма-қол ақша жеткіликті емес.",
        "Not enough cash for the operation."
    ),
    RESULT_TYPE_BLOCKED(
        15,
        "RESULT_TYPE_BLOCKED",
        "Касса заблокирована. Устройство должно перейти в режим блокировки.",
        "Касса бұғатталды. Құрылғы бұғаттау режиміне өтуі тиіс.",
        "Cash register is blocked. The device must enter a blocked state."
    ),
    RESULT_TYPE_SAME_TAXPAYER_AND_CUSTOMER(
        17,
        "RESULT_TYPE_SAME_TAXPAYER_AND_CUSTOMER",
        "Совпадает ИИН/БИН покупателя и продавца. Отправьте чек повторно.",
        "Сатып алушы мен сатушының ЖСН/БСН сәйкес келеді. Чекті қайта жіберіңіз.",
        "Taxpayer and customer IDs match. Resend the receipt."
    ),
    RESULT_TYPE_SERVICE_TEMPORARILY_UNAVAILABLE(
        254,
        "RESULT_TYPE_SERVICE_TEMPORARILY_UNAVAILABLE",
        "Сервис временно недоступен. Повторяйте попытки до таймаута, затем переходите в автономный режим.",
        "Қызмет уақытша қолжетімсіз. Таймаутқа дейін әрекетті қайталаңыз, содан кейін автономды режимге өтіңіз.",
        "Service is temporarily unavailable. Retry until timeout, then switch to offline mode."
    ),
    RESULT_TYPE_UNKNOWN_ERROR(
        255,
        "RESULT_TYPE_UNKNOWN_ERROR",
        "Неизвестная ошибка. Повторяйте попытки до таймаута, затем переходите в автономный режим.",
        "Белгісіз қате. Таймаутқа дейін әрекетті қайталаңыз, содан кейін автономды режимге өтіңіз.",
        "Unknown error. Retry until timeout, then switch to offline mode."
    );

    companion object {
        fun fromCode(code: Int): ResultType? = entries.firstOrNull { it.code == code }
    }
}
