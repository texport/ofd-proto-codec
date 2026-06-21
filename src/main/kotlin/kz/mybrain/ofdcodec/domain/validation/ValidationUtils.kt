package kz.mybrain.ofdcodec.domain.validation

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kz.mybrain.ofdcodec.domain.model.ErrorCode
import kz.mybrain.ofdcodec.domain.model.ErrorFactory
import kz.mybrain.ofdcodec.domain.model.ValidationError

/**
 * Общие проверки для бизнес-валидаций.
 */
object ValidationUtils {

    /**
     * Формирует ошибку отсутствующего поля.
     */
    fun missingField(path: String): ValidationError {
        return ErrorFactory.error(
            ErrorCode.JSON_MISSING_FIELD,
            path,
            mapOf("field" to path.removePrefix("$."))
        )
    }

    /**
     * Формирует ошибку неверного типа.
     */
    fun invalidType(path: String): ValidationError {
        return ErrorFactory.error(
            ErrorCode.JSON_INVALID_TYPE,
            path,
            mapOf("field" to path.removePrefix("$."))
        )
    }

    /**
     * Формирует ошибку неверного значения.
     */
    fun invalidValue(path: String): ValidationError {
        return ErrorFactory.error(
            ErrorCode.JSON_INVALID_VALUE,
            path,
            mapOf("field" to path.removePrefix("$."))
        )
    }

    /**
     * Проверяет, что поле является объектом.
     */
    fun requireObject(
        json: JsonObject,
        key: String,
        path: String,
        errors: MutableList<ValidationError>
    ): JsonObject? {
        val element = json[key]
        if (element == null) {
            errors.add(missingField(path))
            return null
        }
        val obj = element as? JsonObject
        if (obj == null) {
            errors.add(invalidType(path))
        }
        return obj
    }

    /**
     * Проверяет, что поле является массивом.
     */
    fun requireArray(
        json: JsonObject,
        key: String,
        path: String,
        errors: MutableList<ValidationError>
    ): JsonArray? {
        val element = json[key]
        if (element == null) {
            errors.add(missingField(path))
            return null
        }
        val array = element as? JsonArray
        if (array == null) {
            errors.add(invalidType(path))
        }
        return array
    }

    /**
     * Проверяет обязательную строку и непустое значение после trim.
     */
    fun requireNonBlankString(json: JsonObject, key: String, path: String, errors: MutableList<ValidationError>) {
        val element = json[key] as? JsonPrimitive
        if (element == null) {
            errors.add(missingField(path))
            return
        }
        if (!element.isString) {
            errors.add(invalidType(path))
            return
        }
        if (element.content.isBlank()) {
            errors.add(invalidValue(path))
        }
    }

    /**
     * Проверяет опциональную строку: если поле присутствует, оно не должно быть пустым после trim.
     */
    fun optionalNonBlankString(json: JsonObject, key: String, path: String, errors: MutableList<ValidationError>) {
        val element = json[key] ?: return
        val primitive = element as? JsonPrimitive
        if (primitive == null || !primitive.isString) {
            errors.add(invalidType(path))
            return
        }
        if (primitive.content.isBlank()) {
            errors.add(invalidValue(path))
        }
    }

    /**
     * Проверяет обязательный boolean.
     */
    fun requireBoolean(json: JsonObject, key: String, path: String, errors: MutableList<ValidationError>) {
        val element = json[key] as? JsonPrimitive
        if (element == null) {
            errors.add(missingField(path))
            return
        }
        if (element.booleanOrNull == null) {
            errors.add(invalidType(path))
        }
    }

    /**
     * Проверяет целое значение и диапазон.
     */
    fun requireIntInRange(
        json: JsonObject,
        key: String,
        min: Int,
        max: Int,
        path: String,
        errors: MutableList<ValidationError>
    ) {
        val element = json[key] as? JsonPrimitive
        val value = element?.intOrNull
        if (element == null) {
            errors.add(missingField(path))
            return
        }
        if (value == null) {
            errors.add(invalidType(path))
            return
        }
        if (value !in min..max) {
            errors.add(invalidValue(path))
        }
    }

    /**
     * Проверяет long значение и диапазон.
     */
    fun requireLongInRange(
        json: JsonObject,
        key: String,
        min: Long,
        max: Long,
        path: String,
        errors: MutableList<ValidationError>
    ) {
        val element = json[key] as? JsonPrimitive
        val value = element?.longOrNull
        if (element == null) {
            errors.add(missingField(path))
            return
        }
        if (value == null) {
            errors.add(invalidType(path))
            return
        }
        if (value !in min..max) {
            errors.add(invalidValue(path))
        }
    }

    /**
     * Валидирует список элементов JsonArray с помощью переданной функции валидации.
     */
    fun validateList(
        container: JsonObject,
        key: String,
        path: String,
        errors: MutableList<ValidationError>,
        validateItem: (item: JsonObject, itemPath: String) -> List<ValidationError>
    ) {
        val element = container[key] ?: return
        val array = element as? JsonArray
        if (array == null) {
            errors.add(invalidType(path))
            return
        }
        array.forEachIndexed { index, item ->
            val itemPath = "$path[$index]"
            val obj = item as? JsonObject
            if (obj == null) {
                errors.add(invalidType(itemPath))
            } else {
                errors.addAll(validateItem(obj, itemPath))
            }
        }
    }

    /**
     * Валидирует значение по ключу, проверяя, что оно принадлежит разрешенному множеству значений.
     */
    fun validateEnum(
        container: JsonObject,
        key: String,
        path: String,
        allowed: Set<String>
    ): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val element = container[key] as? JsonPrimitive
        if (element == null) {
            errors.add(missingField(path))
            return errors
        }
        if (!element.isString) {
            errors.add(invalidType(path))
            return errors
        }
        val value = element.content
        if (value !in allowed) {
            errors.add(invalidValue(path))
        }
        return errors
    }
}
