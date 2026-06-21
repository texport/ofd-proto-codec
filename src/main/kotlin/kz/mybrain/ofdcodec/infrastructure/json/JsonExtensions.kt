package kz.mybrain.ofdcodec.infrastructure.json

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

fun JsonObject.readString(key: String): String? {
    val element = this[key] as? JsonPrimitive ?: return null
    return if (element.isString) element.content else null
}

fun JsonObject.readStringRequired(key: String): String {
    return readString(key) ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
}

fun JsonObject.readInt(key: String): Int? {
    val element = this[key] as? JsonPrimitive ?: return null
    return element.intOrNull
}

fun JsonObject.readIntRequired(key: String): Int {
    return readInt(key) ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
}

fun JsonObject.readLong(key: String): Long? {
    val element = this[key] as? JsonPrimitive ?: return null
    return element.longOrNull
}

fun JsonObject.readLongRequired(key: String): Long {
    return readLong(key) ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
}

fun JsonObject.readBool(key: String): Boolean? {
    val element = this[key] as? JsonPrimitive ?: return null
    return element.booleanOrNull
}

fun JsonObject.readBoolRequired(key: String): Boolean {
    return readBool(key) ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
}

fun JsonObject.readObject(key: String): JsonObject? {
    return this[key] as? JsonObject
}

fun JsonObject.readObjectRequired(key: String): JsonObject {
    return readObject(key) ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
}

fun JsonObject.readArray(key: String): JsonArray? {
    return this[key] as? JsonArray
}

fun JsonObject.readArrayRequired(key: String): JsonArray {
    return readArray(key) ?: throw IllegalArgumentException("Missing $key / Отсутствует $key / $key өрісі жетіспейді")
}

fun JsonObject.readObjectList(key: String): List<JsonObject>? {
    val array = this[key] as? JsonArray ?: return null
    return array.filterIsInstance<JsonObject>()
}

fun JsonElement.requireObject(key: String): JsonObject {
    return this as? JsonObject ?: throw IllegalArgumentException("Invalid $key element / Неверный элемент $key / $key элементі қате")
}

fun JsonElement.readStringElement(): String {
    val primitive = this as? JsonPrimitive
        ?: throw IllegalArgumentException("Invalid list value / Неверное значение списка / Тізім мәні қате")
    require(primitive.isString) { "Invalid list value / Неверное значение списка / Тізім мәні қате" }
    return primitive.content
}
