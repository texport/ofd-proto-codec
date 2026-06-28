package kz.mybrain.ofdcodec

import java.nio.file.Files
import java.nio.file.Path

/**
 * Хранилище токена для сетевых тестов.
 *
 * Позволяет обновлять токен между тестами, если сервер его меняет.
 */
object TestTokenProvider {
    private val tokenFile: Path = Path.of("build", "tmp", "ofd-test-token.txt")

    /**
     * Возвращает актуальный токен из файла или окружения.
     */
    fun current(): Long? {
        val envToken = (System.getenv("OFD_TEST_TOKEN") ?: System.getProperty("OFD_TEST_TOKEN"))?.toLongOrNull()
        if (Files.exists(tokenFile)) {
            val value = Files.readString(tokenFile).trim()
            if (value.isNotBlank()) {
                return value.toLongOrNull()
            }
        }
        if (envToken != null) {
            update(envToken)
            return envToken
        }
        return null
    }

    /**
     * Сохраняет новый токен для последующих тестов.
     */
    fun update(token: Long) {
        Files.createDirectories(tokenFile.parent)
        Files.writeString(tokenFile, token.toString())
    }
}
