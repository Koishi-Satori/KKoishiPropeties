package top.kkoishi.proc.xml

import top.kkoishi.proc.json.TargetClass
import top.kkoishi.proc.property.BuildFailedException
import top.kkoishi.proc.property.TokenizeException

/**
 * This annotation in top.kkoishi.proc.json has the same function here.
 */
typealias XmlTargetClass = TargetClass

fun StringBuilder.deleteReverse(start: Int, end: Int): StringBuilder {
    val len = this.length - 1
    return this.deleteRange(len - end, len - start)
}

class XmlBuildFailedException : BuildFailedException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

class XmlSyntaxException : TokenizeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

abstract class XmlParser<T>(protected var rest: Iterator<Char>) {
    protected val tokens = TokenList()
    protected val stack: ArrayDeque<String> = ArrayDeque(8)
    protected val buf: StringBuilder = StringBuilder()

    constructor(xmlContent: String) : this(xmlContent.toCharArray().iterator())

    @Throws(TokenizeException::class, BuildFailedException::class)
    abstract fun parse()

    abstract fun build(): T

    protected open fun lookForward() = this.rest.next()

    protected fun hasMore() = this.rest.hasNext()

    protected fun stackEmpty() = this.stack.isEmpty()

    protected fun clearBuf() = this.buf.clear()

    protected fun appendChar(ch: Char): java.lang.StringBuilder = this.buf.append(ch)

    protected fun addToken(token: Token) = this.tokens.add(token)

    protected fun removeToken() = this.tokens.remove()

    protected fun push(content: String) = this.stack.addLast(content)

    protected fun pop() = this.stack.removeLast()

    open fun reset(xmlContent: String) = this.reset(xmlContent.toCharArray().iterator())

    open fun reset(rest: Iterator<Char>) {
        this.rest = rest
        this.buf.clear()
        this.stack.clear()
        this.tokens.reset()
    }

    protected fun tokensEmpty() = tokens.isEmpty()

    data class Token(val type: XmlType, val value: String?) : top.kkoishi.proc.property.Token<XmlType> {
        override fun type(): XmlType = type

        override fun value(): String? = value
    }

    enum class XmlType {
        LEFT_ELE,
        RIGHT_ELE,
        ELEMENT,
        TEXT,
        COMMENT,
        DOC_DESC
    }

    @Suppress("MemberVisibilityCanBePrivate")
    class TokenList {
        private var elements: Array<Token?> = Array(0) { null }
        private var head: Int = 0

        fun add(token: Token) {
            grow(1)
            elements[elements.size - 1] = token
        }

        operator fun plus(tokenList: TokenList) {
            val aSize = tokenList.elements.size - tokenList.head
            grow(aSize)
            for (index in (1.until(aSize))) {
                elements[elements.size - index] = tokenList.remove()
            }
            tokenList.clear()
        }

        private fun grow(length: Int) {
            val cpy = elements
            elements = Array(elements.size + length) { null }
            System.arraycopy(cpy, 0, elements, 0, cpy.size)
        }

        fun remove(): Token {
            if (elements.size == head) {
                throw NoSuchElementException("The token array is empty.")
            }
            val oldVal = elements[head]
            elements[head++] = null
            return oldVal!!
        }

        fun clear() {
            for (index in (head.until(elements.size))) {
                elements[index] = null
            }
            head = 0
        }

        internal fun reset() {
            head = 0
            elements = Array(0) { null }
        }

        fun isEmpty(): Boolean = elements.size - head <= 0

        override fun toString(): String {
            if (isEmpty()) {
                return "TokenList[]"
            }
            val sb = StringBuilder("TokenList[")
            for (index in (head.until(elements.size - 1))) {
                sb.append(elements[index]).append(", ")
            }
            return sb.append(elements[elements.size - 1]).append(']').toString()
        }
    }
}