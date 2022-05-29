package top.kkoishi.proc.xml.convert

import top.kkoishi.proc.property.BuildFailedException
import top.kkoishi.proc.xml.convert.XmlJavaBridge.*
import top.kkoishi.proc.xml.dom.XmlDocTree
import top.kkoishi.proc.xml.dom.XmlNodeImpl
import java.lang.StringBuilder
import java.math.BigDecimal
import java.math.BigInteger

internal const val NUMBER: Int = 1
internal const val STRING: Int = 0
internal const val BOOLEAN: Int = 2
internal val INT_MAX = top.kkoishi.proc.json.INT_MAX
internal val INT_MIN = top.kkoishi.proc.json.INT_MIN
internal val LONG_MAX = top.kkoishi.proc.json.LONG_MAX
internal val LONG_MIN = top.kkoishi.proc.json.LONG_MIN
internal val DOUBLE_MAX = top.kkoishi.proc.json.DOUBLE_MAX
internal val DOUBLE_MIN = top.kkoishi.proc.json.DOUBLE_MIN
internal val CLASS_REFLECT: Map<Class<*>, Int> = getClassReflect()
internal val CLASS_NIL_VALUES: Map<Class<*>, Any?> = HashMap<Class<*>, Any?>(44).apply {
    putAll(
        listOf(
            Pair(SHORT_CLASS, 0),
            Pair(java.lang.Short::class.java, 0),
            Pair(INTEGER_CLASS, 0),
            Pair(java.lang.Integer::class.java, 0),
            Pair(LONG_CLASS, 0),
            Pair(java.lang.Long::class.java, 0),
            Pair(FLOAT_CLASS, 0.0f),
            Pair(java.lang.Float::class.java, 0.0f),
            Pair(DOUBLE_CLASS, 0.0),
            Pair(java.lang.Double::class.java, 0.0),
            Pair(BYTE_CLASS, 0x00.toByte()),
            Pair(java.lang.Byte::class.java, 0x00.toByte()),
            Pair(BOOLEAN_CLASS, false),
            Pair(java.lang.Boolean::class.java, false),
            Pair(java.lang.String::class.java, null),
            Pair(StringBuilder::class.java, null),
            Pair(StringBuffer::class.java, null),
        )
    )
}

object XmlJavaBridgeKt {
    internal fun getRootNode(dom: XmlDocTree): XmlNodeImpl {
        dom.root.children.forEach {
            if (it is XmlNodeImpl) {
                return it
            }
        }
        throw BuildFailedException("There is no root node in the DOM instance $dom")
    }

    internal fun isBasicType (clz: Class<*>): Boolean = CLASS_REFLECT.containsKey(clz)

    internal fun basicTypeNilValue(clz: Class<*>): Any? = CLASS_NIL_VALUES.getOrDefault(clz, null)
}

private fun getClassReflect(): HashMap<Class<*>, Int> = HashMap<Class<*>, Int>(44).apply {
    putAll(
        listOf(
            Pair(SHORT_CLASS, NUMBER),
            Pair(java.lang.Short::class.java, NUMBER),
            Pair(INTEGER_CLASS, NUMBER),
            Pair(java.lang.Integer::class.java, NUMBER),
            Pair(LONG_CLASS, NUMBER),
            Pair(java.lang.Long::class.java, NUMBER),
            Pair(FLOAT_CLASS, NUMBER),
            Pair(java.lang.Float::class.java, NUMBER),
            Pair(DOUBLE_CLASS, NUMBER),
            Pair(java.lang.Double::class.java, NUMBER),
            Pair(BYTE_CLASS, NUMBER),
            Pair(java.lang.Byte::class.java, NUMBER),
            Pair(BOOLEAN_CLASS, BOOLEAN),
            Pair(java.lang.Boolean::class.java, BOOLEAN),
            Pair(java.lang.String::class.java, STRING),
            Pair(StringBuilder::class.java, STRING),
            Pair(StringBuffer::class.java, STRING),
        )
    )
}

internal fun cast(xmlContent: String, clz: Class<*>): Any {
    when (CLASS_REFLECT.getOrDefault(clz, -1)) {
        NUMBER -> return if (xmlContent.contains('.')) {
            val dec = BigDecimal(xmlContent)
            if (xmlContent[0] != '-')
                if (dec > DOUBLE_MIN) dec else dec.toDouble()
            else
                if (dec < DOUBLE_MAX) dec else dec.toDouble()
        } else {
            val int = BigInteger(xmlContent)
            if (xmlContent[0] != '-')
                if (int > INT_MIN) int.toInt() else if (int > LONG_MIN) int.toLong() else int
            else
                if (int < INT_MAX) int.toInt() else if (int < LONG_MAX) int.toLong() else int
        }
        STRING -> return xmlContent
        BOOLEAN -> return xmlContent != "0"
        else -> throw BuildFailedException("The XML Content$xmlContent can not be casted to an instance of $clz.")
    }
}