package kz.mybrain.ofdcodec.application

import kz.mybrain.ofdcodec.domain.registry.OfdRegistry
import kz.mybrain.ofdcodec.ofd.bfd.v204.BfdV204Module
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.KazakhtelecomV203Module

/**
 * Реестр по умолчанию с базовыми регистрациями.
 */
object DefaultRegistry {
    /** Идентификатор ОФД БФД. Формат провода 2.0.3 у него общий с Казахтелекомом. */
    const val BFD_OFD_ID: String = "bfd"

    fun create(): OfdRegistry {
        val registry = OfdRegistry()
        KazakhtelecomV203Module.register(registry)
        // БФД говорит по обеим версиям. Формат провода 2.0.3 у него общий
        // с Казахтелекомом, поэтому используется тот же модуль под своим
        // идентификатором; на 2.0.4 у БФД собственный модуль.
        KazakhtelecomV203Module.register(registry, BFD_OFD_ID)
        BfdV204Module.register(registry)
        return registry
    }
}
