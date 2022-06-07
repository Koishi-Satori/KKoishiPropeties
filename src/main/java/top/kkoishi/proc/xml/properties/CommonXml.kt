package top.kkoishi.proc.xml.properties

import top.kkoishi.proc.json.accessUnsafe
import top.kkoishi.proc.property.AbstractPropertiesLoader
import top.kkoishi.proc.property.TokenizeException
import top.kkoishi.proc.xml.XmlParser
import top.kkoishi.proc.xml.XmlSyntaxException
import top.kkoishi.proc.xml.properties.CommonXml.toXml
import java.util.*

internal object CommonXml {
    val INTEGER_CLASS = Int::class.javaPrimitiveType
    val LONG_CLASS = Long::class.javaPrimitiveType
    val FLOAT_CLASS = Float::class.javaPrimitiveType
    val DOUBLE_CLASS = Double::class.javaPrimitiveType
    val SHORT_CLASS = Short::class.javaPrimitiveType
    val BYTE_CLASS = Byte::class.javaPrimitiveType
    val BOOLEAN_CLASS = Boolean::class.javaPrimitiveType

    internal val sharedUnsafe = accessUnsafe()

    internal fun Any?.toXml(): String = this?.toString() ?: ""
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


class XmlStoreMap : HashMap<String, Any> {
    constructor(storeType: XmlStoreType, initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor) {
        this.storeType = storeType
    }

    constructor(storeType: XmlStoreType, initialCapacity: Int) : super(initialCapacity) {
        this.storeType = storeType
    }

    constructor(storeType: XmlStoreType) : super() {
        this.storeType = storeType
    }

    constructor(storeType: XmlStoreType, m: MutableMap<out String, out Any>?) : super(m) {
        this.storeType = storeType
    }

    companion object {
        fun getInstance(data: Map<String, Any>, storeType: XmlStoreType = XmlStoreType.AS_ELEMENT): XmlStoreMap {
            val xsm = XmlStoreMap(storeType, data.size)
            xsm.putAll(data)
            return xsm
        }

        fun getInstance(
            data: Iterator<Pair<String, Any>>,
            storeType: XmlStoreType = XmlStoreType.AS_ELEMENT,
            initSize: Int = 32,
        ): XmlStoreMap {
            val xsm = XmlStoreMap(storeType, initSize)
            for ((key, value) in data) {
                xsm[key] = value
            }
            return xsm
        }

        fun getInstance(
            data: Iterable<Pair<String, Any>>,
            storeType: XmlStoreType = XmlStoreType.AS_ELEMENT,
            initSize: Int = 32,
        ) =
            getInstance(data.iterator(), storeType, initSize)

        fun getInstance(
            data: Collection<Pair<String, Any>>,
            storeType: XmlStoreType = XmlStoreType.AS_ELEMENT,
            initSize: Int = 4 * data.size,
        ): XmlStoreMap {
            val xsm = XmlStoreMap(storeType, initSize)
            for ((key, value) in data) {
                xsm[key] = value
            }
            return xsm
        }

        fun getInstance(
            data: Enumeration<Pair<String, Any>>,
            storeType: XmlStoreType = XmlStoreType.AS_ELEMENT,
            initSize: Int = 32,
        ): XmlStoreMap {
            val xsm = XmlStoreMap(storeType, initSize)
            for ((key, value) in data) {
                xsm[key] = value
            }
            return xsm
        }
    }

    var storeType: XmlStoreType
    var rootName = "root"

    operator fun minusAssign(key: String) {
        remove(key)
    }

    operator fun plusAssign(entry: Pair<String, Any>) {
        put(entry.first, entry.second)
    }

    operator fun plusAssign(entry: Map.Entry<String, Any>) {
        put(entry.key, entry.value)
    }

    fun toXml(): String {
        val buffer = StringBuilder("<").append(this.rootName).append(">\n")
        when (this.storeType) {
            XmlStoreType.AS_ENTRY -> {
                for ((key, value) in this) {
                    buffer.append("<data ").append(key).append("=\"").append(value.toXml()).append("\">\n")
                }
            }
            XmlStoreType.AS_ELEMENT -> {
                for ((key, value) in this) {
                    buffer.append('<').append(key).append('>').append(value.toXml()).append("</").append(key)
                        .append(">\n")
                }
            }
        }
        buffer.append("</").append(this.rootName).append('>')
        return buffer.toString()
    }

    override fun toString() = toXml()
}

internal class InnerParser(storeType: XmlStoreType) : XmlParser<XmlStoreMap>(EMPTY_ITERATOR) {
    internal val map = XmlStoreMap(storeType)
    private var lookForward: Char = '\u0001'
    private var commit: String = ""

    internal companion object {
        internal class CharIteratorImpl : Iterator<Char> {
            override fun hasNext(): Boolean = throw UnsupportedOperationException()

            override fun next(): Char = throw UnsupportedOperationException()
        }

        internal val EMPTY_ITERATOR = CharIteratorImpl()
    }

    override fun parse() {
        if (rest == EMPTY_ITERATOR) {
            throw TokenizeException("The input char collection is empty!")
        }
        lookForward()
        jump()
    }

    override fun build(): XmlStoreMap {
        TODO("Not yet implemented")
    }

    override fun lookForward(): Char {
        lookForward = super.lookForward()
        return lookForward
    }

    private fun jump() {
        if (stackEmpty() && !hasMore()) {
            return
        }
        while (true) {
            when (lookForward) {
                '<' -> {
                    if (!hasMore()) {
                        throw XmlSyntaxException("Meet EOF while Syntax Analysing:There still contains not-completed-element in this xml document!")
                    }
                    when (lookForward()) {
                        '?' -> {
                            docDesc()
                            return
                        }
                        '!' -> {
                            if (!hasMore()) {
                                throw XmlSyntaxException("Meet EOF while Syntax Analysing:There still contains not-completed-element in this xml document!")
                            }
                            if (lookForward() == '-') {
                                comment()
                            } else if (lookForward == '[') {
                                ignore()
                            } else {
                                throw XmlSyntaxException("Error xml token:Expect '<![CDATA[' or '<!--', but got different token.")
                            }
                            return
                        }
                        '/' -> {
                            endElement()
                            return
                        }
                        else -> {
                            element()
                            return
                        }
                    }
                }
                ' ', '\r', '\n', '\t' -> {
                    // nothing to do.
                }
                else -> {
                    text()
                    return
                }
            }
            lookForward()
        }
    }

    private fun docDesc() {
        while (hasMore()) {
            if (lookForward() == '?') {
                if (lookForward() == '>') {
                    if (!hasMore()) {
                        throw XmlSyntaxException("Meet EOF while Syntax Analysing:There is no root element in this xml document!")
                    }
                    tokens.add(Token(XmlType.DOC_DESC, buf.toString()))
                    buf.clear()
                    lookForward()
                    jump()
                    return
                } else {
                    buf.append(lookForward)
                }
            } else {
                buf.append(lookForward)
            }
        }
        throw XmlSyntaxException("Meet EOF while Syntax Analysing:There is no root element in this xml document!")
    }

    private fun comment() {
        var count = 0
        if (!hasMore()) {
            throw XmlSyntaxException("Meet EOF while Syntax Analysing:The xml comment element is not ended!")
        }
        if (lookForward() == '-') {
            comment_def@ while (hasMore()) {
                if (lookForward() == '-') {
                    ++count
                    lookForward()
                    while (hasMore()) {
                        if (lookForward == '-') {
                            ++count
                        } else if (lookForward == '>') {
                            if (count == 2) {
                                tokens.add(Token(XmlType.COMMENT, buf.toString()))
                                buf.clear()
                                lookForward()
                                jump()
                                return
                            } else {
                                if (count > 2) {
                                    throw XmlSyntaxException()
                                } else {
                                    count = 0
                                    buf.append('-').append('>')
                                }
                            }
                        } else {
                            if (count <= 1) {
                                count = 0
                                buf.append('-')
                            } else {
                                throw XmlSyntaxException()
                            }
                            buf.append(lookForward)
                            continue@comment_def
                        }
                        lookForward()
                    }
                } else if (lookForward == '>') {
                    if (count == 2) {
                        tokens.add(Token(XmlType.COMMENT, buf.toString()))
                        buf.clear()
                        lookForward()
                        jump()
                        return
                    }
                } else {
                    buf.append(lookForward)
                }
            }
        }
        throw XmlSyntaxException()
    }

    private fun ignore() {
        for (i in 0..5) {
            if (!hasMore()) {
                throw XmlSyntaxException("Meet EOF while Syntax Analysing:The xml ignore element is not completed yet.")
            }
            buf.append(lookForward())
        }
        if ("CDATA[" == buf.toString()) {
            //right format
            while (hasMore()) {
                if (lookForward() == ']') {
                    if (super.lookForward() != ']') {
                        throw XmlSyntaxException("Meet EOF while Syntax Analysing:The xml ignore element is not completed yet.")
                    }
                    if (super.lookForward() != '>') {
                        throw XmlSyntaxException("Meet EOF while Syntax Analysing:The xml ignore element is not completed yet.")
                    }
                    lookForward()
                    jump()
                    return
                }
            }
        }
        throw XmlSyntaxException("Syntax Error:The format of xml parser-ignore element should be like '<![CDATA[xxx]]>'!")
    }

    private fun endElement() {
        while (hasMore()) {
            if (lookForward() == '>') {
                if (stack.removeLast() == buf.toString()) {
                    tokens.add(Token(XmlType.RIGHT_ELE, buf.toString()))
                    buf.clear()
                    if (stack.isNotEmpty()) {
                        if (!hasMore()) {
                            throw XmlSyntaxException("Meet EOF while Syntax Analysing:There still contains not-completed-element in this xml document!")
                        }
                        lookForward()
                        jump()
                    }
                } else {
                    throw XmlSyntaxException("The end element name is $buf, but there is no matched element in the stack.")
                }
            } else
                buf.append(lookForward)
        }
    }

    private fun getElementName(`in`: String): String {
        val pos = `in`.indexOf(' ')
        return if (pos != -1) `in`.substring(0, pos) else `in`
    }

    private fun element() {
        buf.append(lookForward)
        var indexString = false
        while (hasMore()) {
            lookForward()
            if (indexString) {
                if (lookForward == '"') {
                    indexString = false
                }
                buf.append(lookForward)
            } else if (lookForward == '"') {
                indexString = true
                buf.append('"')
            } else if (lookForward == '/') {
                if (!hasMore()) {
                    throw XmlSyntaxException("Meet EOF while Syntax Analysing:The final element is not finished!")
                }
                if (lookForward() == '>') {
                    tokens.add(Token(XmlType.LEFT_ELE, buf.toString()))
                    stack.addLast(getElementName(buf.toString()))
                    buf.clear()
                    lookForward()
                    jump()
                    return
                }
            } else {
                buf.append(lookForward)
            }
        }
    }

    private fun text() {
        buf.append(lookForward)
        while (hasMore()) {
            when (lookForward()) {
                '<', '\r', '\n' -> {
                    if (buf.isNotEmpty()) {
                        tokens.add(Token(XmlType.TEXT, buf.toString()))
                        buf.clear()
                    }
                    jump()
                    return
                }
                else -> buf.append(lookForward)
            }
        }
    }

    fun commit() = commit

    fun clear() = reset(EMPTY_ITERATOR)
}

open class XmlProperties(storeType: XmlStoreType = XmlStoreType.AS_ELEMENT) : AbstractPropertiesLoader<String, Any>() {
    private val parser = InnerParser(storeType)

    fun setType(storeType: XmlStoreType): XmlStoreType {
        val oldType = parser.map.storeType
        parser.map.storeType = storeType
        return oldType
    }

    fun type() = parser.map.storeType

    override fun loadImpl(noCommit: String?) {
        parser.reset(noCommit?.iterator() ?: InnerParser.EMPTY_ITERATOR)
        parser.parse()
        putAll(parser.build())
        commit = parser.commit()
    }

    override fun prepare(): String {
        TODO("Not yet implemented")
    }

    override fun removeCommit(`in`: String?): String? = `in`

    override fun clear() {
        super.clear()
        parser.clear()
    }
}