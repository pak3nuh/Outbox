package io.github.pak3nuh.util

@JvmInline
value class MetadataKey(private val key: String) {
    operator fun plus(other: String): MetadataKey = MetadataKey("$key.$other")

    operator fun plus(other: MetadataKey): MetadataKey = MetadataKey("$key.${other.key}")

    fun withValue(value: String): Pair<String, String> = Pair(key, value)

    override fun toString(): String {
        return key
    }

    companion object {
        val outbox: MetadataKey = MetadataKey("io.github.pak3nuh.messaging.outbox")
    }
}
