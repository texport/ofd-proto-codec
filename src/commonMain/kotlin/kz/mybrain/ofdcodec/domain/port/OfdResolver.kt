package kz.mybrain.ofdcodec.domain.port

import kz.mybrain.ofdcodec.domain.model.MessageHeader
import kz.mybrain.ofdcodec.domain.registry.OfdRegistry

/**
 * Определение ОФД при декодировании ответа.
 *
 * Используется [OfdCodec] для выбора подходящего обработчика протокола на основе заголовка сообщения
 * и/или содержимого полезной бинарной нагрузки.
 */
fun interface OfdResolver {
    /**
     * Определяет уникальный строковый идентификатор ОФД (ofdId) для входящего сообщения.
     *
     * @param header Раскодированный заголовок сообщения.
     * @param payload Сырые бинарные данные полезной нагрузки сообщения.
     * @param registry Реестр зарегистрированных протоколов ОФД.
     * @return Уникальный строковый идентификатор ОФД (например, "kazakhtelecom"), или null, если ОФД не удалось определить.
     */
    fun resolve(header: MessageHeader, payload: ByteArray, registry: OfdRegistry): String?
}
