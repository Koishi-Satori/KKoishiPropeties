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

internal const val BLACK = true
internal const val RED = false

class XmlStoreMap : Map<String, Any?> {
    private var root: Node? = null

    internal data class Node(
        var index: Int,
        var parent: Node?,
        var left: Node?,
        var right: Node?,
        var color: Boolean = BLACK,
    )

    private fun insert(index: Int) {
        if (root == null) {
            root = Node(index, null, null, null)
        } else {
            var pointer = root
            var parent: Node?
            var cmp: Boolean
            do {
                parent = pointer
                if (index < pointer!!.index) {
                    pointer = pointer.left
                } else if (index > pointer.index) {
                    pointer = pointer.right
                } else {
                    pointer.index = index
                    return
                }
                cmp = index < pointer!!.index
            } while (pointer != null)
            link(index, parent!!, cmp)
        }
    }

    private fun link(index: Int, root: Node, addToLeft: Boolean) {
        val nNode = Node(index, root, null, null)
        if (addToLeft) {
            root.left = nNode
        } else {
            root.right = nNode
        }
        fixTree(nNode)
        ++size
    }

    private fun fixTree(node: Node?) {
        var x = node
        setColor(x, RED)

        while (x != null && x != root && x.color == RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                val y = rightOf(parentOf(parentOf(x)))
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK)
                    setColor(y, BLACK)
                    setColor(parentOf(parentOf(x)), RED)
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x)
                        rotateLeft(x)
                    }
                    setColor(parentOf(x), BLACK)
                    setColor(parentOf(parentOf(x)), RED)
                    rotateRight(parentOf(parentOf(x)))
                }
            } else {
                val y = leftOf(parentOf(parentOf(x)))
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK)
                    setColor(y, BLACK)
                    setColor(parentOf(parentOf(x)), RED)
                    x = parentOf(parentOf(x))
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x)
                        rotateRight(x)
                    }
                    setColor(parentOf(x), BLACK)
                    setColor(parentOf(parentOf(x)), RED)
                    rotateLeft(parentOf(parentOf(x)))
                }
            }
        }
        root!!.color = BLACK
    }

    private fun rotateRight(x: Node?) {
        TODO("Not yet implemented")
    }

    private fun rotateLeft(x: Node?) {
        TODO("Not yet implemented")
    }

    companion object {
        internal fun parentOf(x: Node?): Node? = x?.parent
        internal fun leftOf(x: Node?): Node? = x?.left
        internal fun rightOf(x: Node?): Node? = x?.right
        internal fun colorOf(x: Node?): Boolean = x?.color ?: BLACK
        internal fun setColor(x: Node?, color: Boolean) {
            if (x != null) x.color = color
        }
    }

    override val entries: Set<Map.Entry<String, Any?>>
        get() = TODO("Not yet implemented")
    override val keys: Set<String>
        get() = TODO("Not yet implemented")
    override var size: Int = 0
    override val values: Collection<Any?>
        get() = TODO("Not yet implemented")

    override fun containsKey(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: String): Any? {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }
}

open class XmlSkipMap : Map<String, Any?> {
    protected data class PointerNode(
        val hash: Int,
        val prev: PointerNode?,
        val next: PointerNode?,
        val down: PointerNode?,
    )

    protected var elements: Array<XmlStore> = Array(0) { XmlStore("null", null) }

    protected data class SkipSet(private var root: PointerNode? = null) : MutableSet<Int> {
        private fun link(root: PointerNode, element: Int) {

        }

        override var size: Int = 0

        override fun contains(element: Int): Boolean {
            TODO("Not yet implemented")
        }

        override fun containsAll(elements: Collection<Int>): Boolean {
            TODO("Not yet implemented")
        }

        override fun isEmpty(): Boolean {
            TODO("Not yet implemented")
        }

        override fun iterator(): MutableIterator<Int> {
            TODO("Not yet implemented")
        }

        override fun add(element: Int): Boolean {
            if (root == null) {
                root = PointerNode(element, null, null, null)
            } else {
                link(root!!, element)
            }
            ++size
            return true
        }

        override fun addAll(elements: Collection<Int>): Boolean {
            TODO("Not yet implemented")
        }

        override fun clear() {
            TODO("Not yet implemented")
        }

        override fun remove(element: Int): Boolean {
            TODO("Not yet implemented")
        }

        override fun removeAll(elements: Collection<Int>): Boolean {
            TODO("Not yet implemented")
        }

        override fun retainAll(elements: Collection<Int>): Boolean {
            TODO("Not yet implemented")
        }
    }

    init {

    }

    override val entries: Set<Map.Entry<String, Any?>>
        get() = TODO("Not yet implemented")
    override val keys: Set<String>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<Any?>
        get() = TODO("Not yet implemented")

    override fun containsKey(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: String): Any? {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }
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