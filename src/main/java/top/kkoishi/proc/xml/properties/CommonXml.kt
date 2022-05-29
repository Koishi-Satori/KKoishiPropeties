package top.kkoishi.proc.xml.properties

import top.kkoishi.proc.json.accessUnsafe

object CommonXml {
    internal val sharedUnsafe = accessUnsafe()
}

enum class XmlStoreType {
    /**
     * Store xml_store as entry of xml element,
     * like:&lt;data key="value"/&gt;
     */
    AS_ENTRY,

    /**
     * Store xml_store as independent xml element,
     * like:&lt;key&gt;value&lt;/key&gt;
     */
    AS_ELEMENT
}

data class XmlStore(
    override val key: String,
    override var value: Any?,
    var storeType: XmlStoreType = XmlStoreType.AS_ELEMENT,
) :
    Map.Entry<String, Any?> {
    companion object {
        fun <T> XmlStore.cast(clz: Class<T>): XmlStore {
            if (this.value != null) {
                this.value = clz.cast(this.value)
            }
            return this
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> XmlStore.get(): T {
            return this.value as T
        }

        fun <T> XmlStore.get(clz: Class<T>): T {
            return clz.cast(this.value)
        }
    }

    @Suppress("LiftReturnOrAssignment")
    fun toXml(title: String?): String = when (storeType) {
        XmlStoreType.AS_ELEMENT -> "<$key>$value</$key>"
        XmlStoreType.AS_ENTRY -> "<${title ?: "data"} $key=\"$value\"/>"
    }
}