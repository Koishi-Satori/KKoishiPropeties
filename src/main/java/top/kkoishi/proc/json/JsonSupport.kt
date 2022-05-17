package top.kkoishi.proc.json

import top.kkoishi.proc.json.JsonJavaBridge.*
import sun.misc.Unsafe
import top.kkoishi.proc.property.BuildFailedException
import top.kkoishi.proc.property.TokenizeException
import java.lang.NumberFormatException
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.collections.HashSet

val INT_MAX = BigInteger("7fffffff", 16)
val INT_MIN = BigInteger("80000000", 16)
val LONG_MAX = BigInteger("9223372036854775807")
val LONG_MIN = BigInteger("-9223372036854775808")
val DOUBLE_MAX = BigDecimal(Double.MAX_VALUE)
val DOUBLE_MIN = BigDecimal(Double.MIN_VALUE)
val NUMBER_MAP = HashSet<Char>(listOf('-', '_', '.', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'))
internal val UNSAFE: Unsafe = accessUnsafe()
val BASIC_TYPES = accessBasicTypes()

internal fun accessBasicTypes(): HashSet<Class<*>> = HashSet<Class<*>>(24).apply {
    addAll(
        listOf(
            INTEGER_CLASS,
            java.lang.Integer::class.java,
            LONG_CLASS,
            java.lang.Long::class.java,
            FLOAT_CLASS,
            java.lang.Float::class.java,
            DOUBLE_CLASS,
            java.lang.Double::class.java,
            SHORT_CLASS,
            java.lang.Short::class.java,
            BYTE_CLASS,
            java.lang.Byte::class.java,
            java.lang.String::class.java,
            java.lang.StringBuilder::class.java,
            java.lang.StringBuffer::class.java
        )
    )
}

internal fun accessUnsafe(): Unsafe {
    val f = Unsafe::class.java.getDeclaredField("theUnsafe")
    f.isAccessible = true
    return f.get(null) as Unsafe
}

internal fun java.lang.StringBuilder.clear() = this.delete(0, this.length)

internal fun java.lang.StringBuilder.deleteAtReverse(index: Int) = this.deleteAt(this.length - index - 1)

@Throws(JsonSyntaxException::class, UnexpectedJsonException::class)
internal fun JsonParser.block() {
    if (!rest.hasNext() && stack.isEmpty()) return
    when (this.lookForward) {
        '{' -> {
            this.builder.tokens.add(JsonParser.Token(JsonParser.JsonType.OBJ_BEGIN, null))
            this.stack.addLast('{')
        }
        '}' -> {
            if (this.stack.isEmpty()) {
                throw JsonSyntaxException("The parser meet '}' token, but there is not matched '{' token in the stack.")
            }
            if (stack.removeLast() == '{') {
                this.builder.tokens.add(JsonParser.Token(JsonParser.JsonType.OBJ_END, null))
            } else throw UnexpectedJsonException("Expected get the token '{', but got ${this.lookForward}")
        }
        else -> throw UnexpectedJsonException("The token ${this.lookForward} should not appear here.")
    }
    if (!rest.hasNext() && stack.isEmpty()) return
    this.lookForward()
    this.jump()
}

@Throws(BuildFailedException::class)
internal fun JsonParser.key() {
    if (!rest.hasNext() && stack.isEmpty()) return
    lookForward()
    while (true) {
        if (!this.rest.hasNext()) throw BuildFailedException("The entry key is not closed.")
        if (this.lookForward == '\\') {
            lookForward()
            this.buf.append('\\').append(this.lookForward)
        } else if (this.lookForward == '"') {
            lookForward()
            break
        } else this.buf.append(this.lookForward)
        lookForward()
    }
    this.builder.tokens.add(JsonParser.Token(JsonParser.JsonType.STRING, this.buf.toString()))
    this.buf.clear()
    this.jump()
}

@Throws(JsonSyntaxException::class)
internal fun JsonParser.value() {
    if (!rest.hasNext() && stack.isEmpty()) return
    while (true) {
        if (!this.rest.hasNext()) throw BuildFailedException("The entry key is not closed.")
        when (this.lookForward) {
            ' ', '\r', '\n', '\t' -> continue
            '"' -> {
                lookForward()
                break
            }
            '\\' -> {
                lookForward()
                this.buf.append('\\').append(this.lookForward)
            }
            '{' -> {
                block()
                break
            }
            '[' -> {
                array()
                break
            }
            else -> this.buf.append(this.lookForward)
        }
        lookForward()
    }
    this.builder.tokens.add(JsonParser.Token(JsonParser.JsonType.STRING, this.buf.toString()))
    this.buf.clear()
    this.jump()
}

@Throws(JsonSyntaxException::class)
internal fun JsonParser.nil() {
    if (lookForward == 'n') {
        (2 downTo 0).forEach { _ ->
            lookForward()
            this.buf.append(lookForward)
        }
        if (this.buf.toString() == "null") {
            this.builder.tokens.add(JsonParser.Token(JsonParser.JsonType.NULL, null))
            this.buf.clear()
            lookForward()
        } else throw JsonSyntaxException()
    } else throw JsonSyntaxException()
}

@Throws(JsonSyntaxException::class)
internal fun JsonParser.bool() {
    ((if (lookForward == 't') 2 else 3) downTo 0).forEach { _ ->
        lookForward()
        this.buf.append(lookForward)
    }
    when (this.buf.toString()) {
        "true" -> this.builder.tokens.add(JsonParser.Token(JsonParser.JsonType.BOOLEAN, "1"))
        "false" -> this.builder.tokens.add(JsonParser.Token(JsonParser.JsonType.BOOLEAN, "0"))
        else -> throw JsonSyntaxException()
    }
    this.buf.clear()
    lookForward()
}

@Throws(JsonSyntaxException::class)
internal fun JsonParser.numberValue() {
    if (!rest.hasNext() && stack.isEmpty()) return
    while (true) {
        when (this.lookForward) {
            ',', '{', '[', ' ', '\n', '\r', '\t' -> break
            else -> {
                if (NUMBER_MAP.contains(lookForward)) {
                    this.buf.append(lookForward)
                } else throw JsonSyntaxException()
            }
        }
        lookForward()
    }
    this.builder.tokens.add(JsonParser.Token(JsonParser.JsonType.NUMBER, this.buf.toString()))
    this.buf.clear()
    this.jump()
}

@Throws(JsonSyntaxException::class)
internal fun JsonParser.sep() {
    if (!rest.hasNext() && stack.isEmpty()) return
    lookForward()
    when (this.lookForward) {
        '"' -> this.key()
        ':' -> {
            this.builder.tokens.add(JsonParser.Token(JsonParser.JsonType.SEP_ENTRY, null))
            this.value()
        }
        ',' -> {
            this.builder.tokens.add(JsonParser.Token(JsonParser.JsonType.SEP_COMMA, null))
            this.sep()
        }
        ' ', '\r', '\n', '\t', '{', '[' -> {
            this.lookForward()
            this.jump()
        }
        't', 'f' -> {
            this.buf.clear()
            this.buf.append(lookForward)
            bool()
            this.jump()
        }
        'n' -> {
            this.buf.clear()
            this.buf.append(lookForward)
            nil()
            this.jump()
        }
        else -> {
            if (NUMBER_MAP.contains(this.lookForward)) {
                this.numberValue()
            } else {
                throw JsonSyntaxException()
            }
        }
    }
}

internal fun JsonParser.array() {
    if (!rest.hasNext() && stack.isEmpty()) return
    lookForward()
    this.builder.tokens.add(JsonParser.Token(JsonParser.JsonType.ARRAY_BEGIN, null))
    this.stack.addLast('[')
    jump()
}

class UnexpectedJsonException : BuildFailedException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

class JsonSyntaxException : TokenizeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

class Entry(var key: String, var value: Any?) {
    fun cast2Pair(): Pair<String, Any?> {
        return Pair(key, value)
    }
}

/**
 * Mark how to decode json object to java class instance.
 * And this is also used in top.kkoishi.proc.xml package.
 *
 * @param classNames the possible class names' list.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
@MustBeDocumented
annotation class TargetClass(vararg val classNames: String)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
@MustBeDocumented
annotation class ArrayClass(vararg val classNames: String)

@Throws(ClassNotFoundException::class)
fun TargetClass.accessClass(): Array<Class<*>?> {
    val result: Array<Class<*>?> = Array(classNames.size) { null }
    for ((index, value) in classNames.withIndex()) {
        result[index] = Class.forName(value)
    }
    return result
}

internal fun getStringIterator(content: String): Iterator<Char> = content.toCharArray().iterator()

@Throws(java.lang.ClassCastException::class, NumberFormatException::class)
@Suppress("UNCHECKED_CAST")
internal fun <T> jsonTokenCast(token: JsonParser.Token): T {
    return when (token.type()) {
        JsonParser.JsonType.STRING -> token.value() as T
        JsonParser.JsonType.NUMBER -> {
            if (token.value()!!.contains('.')) {
                val decimal = BigDecimal(token.value())
                if (token.value()!!.elementAt(0) != '-') {
                    if (decimal > DOUBLE_MAX) decimal else decimal.toDouble()
                } else {
                    if (decimal < DOUBLE_MIN) decimal else decimal.toDouble()
                }
            } else {
                val integer = BigInteger(token.value()!!)
                if (token.value()!!.elementAt(0) != '-') {
                    if (integer > INT_MAX) {
                        if (integer > LONG_MAX) integer else token.value()!!.toLong()
                    } else token.value()!!.toInt()
                } else {
                    if (integer < INT_MIN) {
                        if (integer < LONG_MIN) integer else token.value()!!.toLong()
                    } else token.value()!!.toInt()
                }
            } as T
        }
        JsonParser.JsonType.BOOLEAN -> {
            (token.value() == "0") as T
        }
        else -> throw ClassCastException("The token type ${token.type()} cannot be force casted.")
    }
}
