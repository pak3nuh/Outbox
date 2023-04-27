package io.github.pak3nuh.messaging.outbox

data class Entry(val key: ByteArray, val value: ByteArray, val id: String = "", val metadata: Map<String, String> = emptyMap()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Entry

        if (!key.contentEquals(other.key)) return false
        if (!value.contentEquals(other.value)) return false
        if (id != other.id) return false
        return metadata == other.metadata
    }

    override fun hashCode(): Int {
        var result = key.contentHashCode()
        result = 31 * result + value.contentHashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }

}