package kz.mybrain.ofdcodec.domain.registry

/**
 * Реестр обработчиков по ОФД и версии протокола.
 */
class OfdRegistry {
    private val handlers = mutableMapOf<String, MutableMap<String, OfdProtocolHandler>>()

    /**
     * Регистрирует обработчик для пары (ОФД, версия протокола).
     */
    fun register(handler: OfdProtocolHandler) {
        val byVersion = handlers.getOrPut(handler.ofdId) { mutableMapOf() }
        byVersion[handler.protocolVersion] = handler
    }

    /**
     * Возвращает обработчик для ОФД и версии, если он зарегистрирован.
     */
    fun find(ofdId: String, protocolVersion: String): OfdProtocolHandler? {
        return handlers[ofdId]?.get(protocolVersion)
    }

    /**
     * Список поддерживаемых версий для заданного ОФД.
     */
    @Suppress("unused")
    fun supportedVersions(ofdId: String): Set<String> {
        return handlers[ofdId]?.keys ?: emptySet()
    }

    /**
     * Список всех зарегистрированных ОФД.
     */
    @Suppress("unused")
    fun ofdIds(): Set<String> = handlers.keys
}
