package top.kkoishi.proc.xml.convert

import top.kkoishi.proc.xml.convert.XmlJavaBridge.*

internal const val NUMBER: Int = 1
internal const val STRING: Int = 0
internal const val BOOLEAN: Int = 2
internal val INT_MAX = top.kkoishi.proc.json.INT_MAX

private val CLASS_REFLECT: Map<Class<*>, Int> = getClassReflect()

private fun getClassReflect(): HashMap<Class<*>, Int> = HashMap<Class<*>, Int>(44).apply {
    putAll(listOf(
        Pair(INTEGER_CLASS, NUMBER),
        Pair(LONG_CLASS, NUMBER),
        Pair(FLOAT_CLASS, NUMBER),
        Pair(DOUBLE_CLASS, NUMBER),
        Pair(SHORT_CLASS, NUMBER),
        Pair(BYTE_CLASS, NUMBER),
        Pair(BOOLEAN_CLASS, BOOLEAN),
        Pair(java.lang.String::class.java, STRING)
    ))
}

internal fun cast(xmlContent: String, clz: Class<*>): Any {
    when (CLASS_REFLECT.getOrDefault(clz, -1)) {
        NUMBER -> {

        }
        STRING -> {

        }
        BOOLEAN -> {

        }
        else -> {

        }
    }
}