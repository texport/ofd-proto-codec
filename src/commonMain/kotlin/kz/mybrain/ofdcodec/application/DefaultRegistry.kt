package kz.mybrain.ofdcodec.application

import kz.mybrain.ofdcodec.domain.registry.OfdRegistry
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.KazakhtelecomV203Module

/**
 * Реестр по умолчанию с базовыми регистрациями.
 */
object DefaultRegistry {
    fun create(): OfdRegistry {
        val registry = OfdRegistry()
        KazakhtelecomV203Module.register(registry)
        return registry
    }
}
