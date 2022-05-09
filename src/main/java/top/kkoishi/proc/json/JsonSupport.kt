package top.kkoishi.proc.json

import sun.misc.Unsafe
import top.kkoishi.proc.property.BuildFailedException
import top.kkoishi.proc.property.TokenizeException
import java.math.BigInteger
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

val INT_MAX = BigInteger("7fffffff", 16)
val INT_MIN = BigInteger("80000000", 16)
val LONG_MAX = BigInteger("9223372036854775807")
val LONG_MIN = BigInteger("-9223372036854775808")
val NUMBER_MAP = HashSet<Char>(listOf('-', '_', '.', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'))
val UNSAFE: Unsafe = accessUnsafe()

internal fun accessUnsafe(): Unsafe {
    val f = Unsafe::class.java.getDeclaredField("theUnsafe")
    f.isAccessible = true
    return f.get(null) as Unsafe
}

internal fun java.lang.StringBuilder.clear() {
    this.delete(0, this.length)
}

internal fun java.lang.StringBuilder.deleteAtReverse(index: Int) {
    this.deleteAt(this.length - index - 1)
}

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
        ' ', '\r', '\n', '\t' -> {
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
            } else throw JsonSyntaxException()
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

internal data class JsonBuilderInfo<T>(private val clz: Class<T>, private val fieldsInfo: ArrayList<JsonBuilderInfo<Any?>>?)

internal fun getStringIterator(content: String): Iterator<Char> {
    return content.toCharArray().iterator()
}

@Throws(java.lang.ClassCastException::class)
@Suppress("UNCHECKED_CAST")
internal fun <T> jsonTokenCast(token: JsonParser.Token): T {
    return when (token.type) {
        JsonParser.JsonType.STRING -> token.value as T
        JsonParser.JsonType.NUMBER -> {
            val integer = BigInteger(token.value)
            if (token.value.elementAt(0) == '-') {
                if (integer > INT_MAX) {
                    if (integer > LONG_MAX) integer else token.value.toLong()
                } else token.value.toInt()
            } else {
                if (integer < INT_MIN) {
                    if (integer < LONG_MIN) integer else token.value.toLong()
                } else token.value.toInt()
            } as T
        }
        JsonParser.JsonType.BOOLEAN -> {
            (token.value == "0") as T
        }
        else -> throw ClassCastException("The token type ${token.type} cannot be force casted.")
    }
}

class BplusTree<T>(private var comparator: Comparator<T>) {
    private class Node<T>() {
        var value: T? = null
        var elementAmount: Int = 0
        var children: Array<Node<T>?>? = Array(5) { null }
        var isLeaf: Boolean = false

        constructor(value: T?) : this() {
            this.value = value
        }
    }

    private var root: Node<T>? = null

    private fun insert0(node: Node<T>, value: T?) {
        if (node.value == null) {
            node.value = value
            node.children = null
            val refNode = Node(value)
            refNode.isLeaf = true
            return
        } else {
            node.children = Array(5) { null }
            node.children!![node.elementAmount++] = node
            var cursor: Node<T>? = node
            while (!cursor!!.isLeaf) {
                for (index in cursor!!.children!!.indices) {
                    if (comparator.compare(value, node.children!![index]!!.value) < 0) {
                        cursor = cursor.children!![index]
                        break
                    }
                    if (index == node.children!!.size - 1) {
                        cursor = cursor.children!![index + 1]
                        break
                    }
                }
            }
            if (cursor.elementAmount < 5) {
                insertVal(cursor, value)
                cursor.children!![cursor.elementAmount] = cursor.children!![cursor.elementAmount - 1]
                cursor.children!![cursor.elementAmount - 1] = null
            } else {
                split(cursor, value)
            }
        }
    }

    /**
     * Find the position to insert the value to the node array
     * and insert the value to the node array.
     *
     * @param node The node to insert the value to.
     * @param value The value to insert.
     */
    private fun insertVal(node: Node<T>, value: T?) {
        for (index in node.children!!.indices) {
            if (comparator.compare(value, node.children!![index]!!.value) < 0) {
                node.children!![index] = node.children!![index - 1]
                node.children!![index - 1] = null
                break
            }
        }
        node.children!![node.elementAmount] = Node(value)
        node.elementAmount++
    }

    /**
     * Split the node to two nodes.
     * Two split cases:
     * a.After the leaf node is split, the middle node will be the parent node of the two new nodes.
     * b.Or invoke <code>insertInternal</code> method.
     */
    private fun split(node: Node<T>, value: T?) {
        var leftNode: Node<T> = Node()
        var rightNode: Node<T> = Node()
        insertVal(node, value)
        TODO("Not finished yet.")

    }
}