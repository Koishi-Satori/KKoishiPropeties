@file:Suppress("MemberVisibilityCanBePrivate")

package top.kkoishi.proc.xml.dom

import top.kkoishi.proc.xml.XmlParser
import top.kkoishi.proc.xml.XmlSyntaxException

internal class XmlDom(private val tokens: XmlParser.TokenList) {
    private val dom: XmlDocTree = XmlDocTree()
    fun build() {
        val res = build0()!!
        while (!res.isEmpty()) dom.root.children.addLast(res.removeFirst())
    }

    fun build0(): ArrayDeque<AbstractXmlNode>? {
        if (tokens.isEmpty()) {
            return null
        }
        val xmlNodes: ArrayDeque<AbstractXmlNode> = ArrayDeque(8)
        while (!tokens.isEmpty()) {
            val token = tokens.remove()
            when (token.type) {
                XmlParser.XmlType.COMMENT -> xmlNodes.addLast(XmlLeafNode(XmlComment(token.value)))
                XmlParser.XmlType.DOC_DESC -> xmlNodes.addLast(XmlLeafNode(getElementContent(token.value!!).cast()))
                XmlParser.XmlType.ELEMENT -> xmlNodes.addLast(XmlLeafNode(getElementContent(token.value!!)))
                XmlParser.XmlType.LEFT_ELE -> {
                    val xmlNode = XmlNodeImpl(getElementContent(token.value!!))
                    val res = build0()
                    while (!res!!.isEmpty()) xmlNode.children.addLast(res.removeFirst())
                    xmlNodes.addLast(xmlNode)
                }
                XmlParser.XmlType.RIGHT_ELE -> return xmlNodes
                XmlParser.XmlType.TEXT -> xmlNodes.addLast(XmlLeafNode(token.value!!))
            }
        }
        return xmlNodes
    }

    fun dom() = dom
}

//fun main() = println(getElementContent("xml version=\"1.0\""))

@Throws(XmlSyntaxException::class)
public fun getElementContent(elementContent: String): XmlElementInfoDesc {
    val firstSpace = elementContent.indexOf(' ')
    if (firstSpace == -1) {
        return XmlElementInfoDesc(elementContent)
    }
    val docDesc = XmlElementInfoDesc(elementContent.substring(0, firstSpace))
    var index = -1
    var pos = firstSpace + 1
    var indexValue = false
    val keyTemp: StringBuilder = StringBuilder()
    val rest = elementContent.substring(firstSpace + 1).toCharArray().iterator()
    while (rest.hasNext()) {
        when (val c = rest.nextChar()) {
            ' ', '\n', '\r', '\t' -> {
                ++pos
                continue
            }
            '=' -> {
                if (keyTemp.isEmpty()) {
                    throw XmlSyntaxException("Syntax Error:The entry is not finished!")
                }
                indexValue = true
            }
            '"' -> {
                if (indexValue) {
                    if (index != -1) {
                        indexValue = false
                        docDesc.entries.addLast(Entry(keyTemp.toString(), elementContent.substring(index, pos)))
                        index = -1
                        keyTemp.clear()
                    } else {
                        index = pos + 1
                    }
                } else throw XmlSyntaxException("Syntax Error:The entry has error format!")
            }
            else -> {
                if (!indexValue) {
                    keyTemp.append(c)
                }
            }
        }
        ++pos
    }
    return docDesc
}

internal fun XmlElementInfoDesc.cast(): XmlDocInfoDesc {
    val info = XmlDocInfoDesc(this.title)
    while (!this.entries.isEmpty()) {
        info.entries.addLast(this.entries.removeFirst())
    }
    return info
}

data class Entry(var key: String, var value: String)

data class XmlElementInfoDesc(var title: String) {
    val entries: ArrayDeque<Entry> = ArrayDeque(2)

    override fun toString(): String {
        return "XmlElementInfoDesc(title='$title', entries=$entries)"
    }

}

data class XmlDocInfoDesc(var elementName: String) {
    val entries: ArrayDeque<Entry> = ArrayDeque(0)

    override fun toString(): String {
        return "XmlDocInfoDesc(elementName='$elementName', entries=$entries)"
    }

}

data class XmlComment(var comment: String?)

sealed class AbstractXmlNode(protected var value: Any?) {
    abstract fun hasChildren(): Boolean

    fun value() = this.value

    override fun toString(): String {
        return "AbstractXmlNode(value=$value)"
    }

}

class XmlLeafNode(value: Any) : AbstractXmlNode(value) {
    override fun hasChildren(): Boolean = false
}

class XmlNodeImpl(value: Any?) : AbstractXmlNode(value) {
    override fun hasChildren(): Boolean = true

    internal val children: ArrayDeque<AbstractXmlNode> = ArrayDeque(8)

    fun children(): ArrayDeque<AbstractXmlNode> = children

    fun add(node: AbstractXmlNode) = children.addLast(node)

    override fun toString(): String {
        return "XmlNodeImpl(value=$value, children=$children)"
    }


}

data class XmlDocTree(val root: XmlNodeImpl = XmlNodeImpl(null))