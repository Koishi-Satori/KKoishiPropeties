package top.kkoishi.proc.json

import top.kkoishi.proc.json.JsonJavaBridge.*
import top.kkoishi.proc.property.LoaderException
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.collections.HashMap

typealias Token = JsonParser.Token
typealias Type = JsonParser.JsonType
typealias TokenList = JsonParser.TokenList

/**
 * The reflection type map of java types which can be converted to basic type.
 */
internal val TYPE_MAP: Map<Class<*>, Type> = HashMap<Class<*>, Type>(48).apply {
    putAll(
        listOf(
            Pair(INTEGER_CLASS, Type.NUMBER),
            Pair(java.lang.Integer::class.java, Type.NUMBER),
            Pair(LONG_CLASS, Type.NUMBER),
            Pair(java.lang.Long::class.java, Type.NUMBER),
            Pair(FLOAT_CLASS, Type.NUMBER),
            Pair(java.lang.Float::class.java, Type.NUMBER),
            Pair(DOUBLE_CLASS, Type.NUMBER),
            Pair(java.lang.Double::class.java, Type.NUMBER),
            Pair(SHORT_CLASS, Type.NUMBER),
            Pair(java.lang.Short::class.java, Type.NUMBER),
            Pair(BYTE_CLASS, Type.NUMBER),
            Pair(java.lang.Byte::class.java, Type.NUMBER),
            Pair(java.lang.Number::class.java, Type.NUMBER),
            Pair(Number::class.java, Type.NUMBER),
            Pair(BigDecimal::class.java, Type.NUMBER),
            Pair(BigInteger::class.java, Type.NUMBER),
            Pair(BOOLEAN_CLASS, Type.BOOLEAN),
            Pair(java.lang.Boolean::class.java, Type.BOOLEAN),
            Pair(java.lang.CharSequence::class.java, Type.STRING),
            Pair(java.lang.String::class.java, Type.STRING),
            Pair(java.lang.StringBuilder::class.java, Type.STRING),
            Pair(java.lang.StringBuffer::class.java, Type.STRING)
        )
    )
}

internal operator fun Token.component1(): Type = this.type

internal operator fun Token.component2(): String? = this.value

class JsonJavaConvertException : LoaderException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

@Suppress("MemberVisibilityCanBePrivate")
open class JsonConvertor(jsonObject: JsonObject) {
    protected var tokens = TokenList()
    protected val result: StringBuilder = StringBuilder()

    fun tokens(): TokenList {
        return tokens
    }

    init {
        buildObject(jsonObject)
    }

    protected fun buildObject(jsonObject: JsonObject) {
        tokens.add(Token(Type.OBJ_BEGIN, null))
        //parse inner data
        val end: Int = jsonObject.data.size - 1
        for ((index, datum) in jsonObject.data.withIndex()) {
            buildEntry(datum)
            if (index == end) break
            tokens.add(Token(Type.SEP_COMMA, null))
        }
        tokens.add(Token(Type.OBJ_END, null))
    }

    protected fun buildEntry(entry: Pair<String, Any?>) {
        tokens.add(Token(Type.STRING, entry.first))
        tokens.add(Token(Type.SEP_ENTRY, null))
        buildAny(entry.second)
    }

    protected fun buildAny(any: Any?) {
        if (any == null) {
            tokens.add(Token(Type.NULL, null))
        } else {
            when (any) {
                is JsonObject -> {
                    buildObject(any)
                }
                is Array<*> -> {
                    buildArray(any)
                }
                else -> {
                    val type: Type =
                        TYPE_MAP[any.javaClass] ?: throw JsonJavaConvertException("The type of $any is illegal.")
                    when (type) {
                        Type.NUMBER -> tokens.add(Token(type, (any as Number).toString()))
                        Type.BOOLEAN -> tokens.add(Token(type, if (any as Boolean) "0" else "1"))
                        Type.STRING -> tokens.add(Token(type, any.toString()))
                        else -> throw JsonJavaConvertException("This should not happen.")
                    }
                }
            }
        }
    }

    protected fun buildArray(array: Array<*>) {
        tokens.add(Token(Type.ARRAY_BEGIN, null))
        for (value in array) {
            buildAny(value)
        }
        tokens.add(Token(Type.ARRAY_END, null))
    }

    fun convert() {
        while (!tokens.isEmpty) {
            val (type, value) = tokens.remove()
            when (type) {
                Type.OBJ_BEGIN -> result.append('{')
                Type.OBJ_END -> result.append('}')
                Type.STRING -> result.append('"').append(value).append('"')
                Type.SEP_ENTRY -> result.append(": ")
                Type.NUMBER -> result.append(value)
                Type.SEP_COMMA -> result.append(", ")
                Type.BOOLEAN -> result.append(value == "0")
                Type.NULL -> result.append("null")
                Type.ARRAY_BEGIN -> {
                    result.append('[')
                    convertArray()
                }
                else -> throw JsonJavaConvertException()
            }
        }
    }

    protected fun convertArray () {
        var mappingObject = false
        while (!tokens.isEmpty) {
            val (type, value) = tokens.remove()
            when (type) {
                Type.OBJ_BEGIN -> {
                    result.append('{')
                    mappingObject = true
                }
                Type.OBJ_END -> {
                    result.append('}')
                    mappingObject = false
                }
                Type.STRING -> result.append('"').append(value).append('"')
                Type.SEP_ENTRY -> result.append(": ")
                Type.NUMBER -> result.append(value)
                Type.SEP_COMMA -> result.append(", ")
                Type.BOOLEAN -> result.append(value == "0")
                Type.NULL -> result.append("null")
                Type.ARRAY_BEGIN -> {
                    result.append('[')
                    convertArray()
                }
                Type.ARRAY_END -> {
                    result.deleteAtReverse(0).deleteAtReverse(0)
                    result.append(']')
                    return
                }
            }
            if (!mappingObject)
                result.append(", ")
        }
    }

    fun result(): StringBuilder {
        return result
    }

    fun reset(jsonObject: JsonObject) {
        result.clear()
        tokens.clear()
        tokens = TokenList()
        buildObject(jsonObject)
    }
}
