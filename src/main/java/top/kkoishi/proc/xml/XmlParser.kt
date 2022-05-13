package top.kkoishi.proc.xml

import top.kkoishi.proc.property.Token

open class XmlParser {
    data class Token(val type: XmlType, val value: String?) : top.kkoishi.proc.property.Token<XmlType> {
        override fun type(): XmlType = type

        override fun value(): String? = value
    }

    enum class XmlType {

    }
}