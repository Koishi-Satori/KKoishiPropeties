package top.kkoishi.proc.json

import java.lang.reflect.Field
import kotlin.reflect.KClass

private val basicTypeMap: HashMap<out Any, JavaType> = initBasicTypeMap()

private fun initBasicTypeMap(): HashMap<Class<*>, JavaType> {
    val map: HashMap<Class<*>, JavaType> = HashMap(28)
    map[Int::class.java] = JavaType.INT
    map[Long::class.java] = JavaType.LONG
    map[Float::class.java] = JavaType.FLOAT
    map[Double::class.java] = JavaType.DOUBLE
    map[Byte::class.java] = JavaType.BYTE
    map[Short::class.java] = JavaType.SHORT
    map[Boolean::class.java] = JavaType.BOOL
    map[String::class.java] = JavaType.STRING
    return map
}

class JsonEncoder<T>(@Suppress("CanBeParameter") private val clz: KClass<Class<T>>) {
    private var jsonObject: JsonObject
    private var fields: Array<Field>

    init {
        if (clz.java.isArray) {
            throw IllegalArgumentException("Please use ${JsonArrayEncoder::class} to encode array!")
        }
        val c = clz::class
        fields = c.java.declaredFields
        jsonObject = JsonObject(fields.size)
    }
}

class JsonArrayEncoder(private val array: Array<Any?>) {
    private var javaObject: JavaObject? = null

    fun parse() {
        val arr = JavaArray(Array(array.size) { null })
        array.forEachIndexed { index, it -> arr[index] = parse(it) }
    }

    @JvmName("parse0")
    private fun parse(o: Any?): JavaObject? = o?.cast2javaObject()

    @JvmName("result")
    fun getResult(): JavaObject? = javaObject
}

internal fun Any.cast2javaObject(): JavaObject {
    val clz = this::class
    return if (clz.java.isArray) javaArrayCastImpl() else if (basicTypeMap.containsKey(clz.java)) javaBasicCastImpl() else directlyCast()
}

private fun Any.javaArrayCastImpl(): JavaArray {
    val arr = JavaArray(Array((this as Array<out Any?>).size) { null })
    for ((index, any) in this.withIndex()) {
        arr[index] = any?.cast2javaObject()
    }
    return arr
}

private fun Any.javaBasicCastImpl(): JavaBasicType = JavaBasicType(basicTypeMap[this::class.java], this)

private fun Any.directlyCast(): JavaObjectImpl {
    val clz = this::class
    val fields: ArrayList<Field> = ArrayList(clz.java.declaredFields.toList())
    val superClass = clz.java.superclass
    if (superClass != null && superClass != Any::class.java) {
        fields.addAll(accessFields(superClass))
    }
    val entries: ArrayList<Pair<Field, Any?>> = ArrayList(fields.size)
    fields.forEach { f ->
        f.isAccessible = true
        entries.add(Pair(f, f.get(this)))
    }
    return JavaObjectImpl(entries)
}

private fun accessFields(clz: Class<in Nothing>): ArrayList<Field> {
    val fields: ArrayList<Field> = ArrayList(clz.declaredFields.toList())
    val superClass = clz.superclass
    if (superClass != null && superClass != Any::class.java) {
        fields.addAll(accessFields(superClass))
    }
    return fields
}

private operator fun JavaArray.set(index: Int, value: JavaObject?) {
    this.data[index] = value
}

interface JavaObject {
    fun isArray(): Boolean {
        return false
    }

    fun accessClass(): KClass<out Any>

    fun value(): Any?

    fun type(): JavaType
}

class JavaBasicType(private val type: JavaType?, private val value: Any) : JavaObject {
    override fun accessClass(): KClass<out Any> {
        return value::class
    }

    override fun value(): Any = value

    override fun type(): JavaType = type!!
}

class JavaObjectImpl(value: Any?) : JavaObject {
    private val clz: KClass<out Any> = if (value == null) Any::class else value::class

    @Suppress("UNCHECKED_CAST")
    private val entries: ArrayList<Pair<Field, Any?>>? =
        if (value == null) null else value as ArrayList<Pair<Field, Any?>>?

    override fun accessClass(): KClass<out Any> {
        return clz
    }

    override fun value(): Any? {
        return entries
    }

    override fun type(): JavaType {
        return JavaType.CLASS
    }
}

class JavaArray(array: Array<JavaObject?>) : JavaObject {
    internal val data: Array<JavaObject?> = array

    override fun isArray(): Boolean {
        return true
    }

    override fun accessClass(): KClass<out Any> {
        return data::class
    }

    override fun value(): Any? {
        return data
    }

    override fun type(): JavaType {
        return JavaType.ARRAY
    }
}

enum class JavaType {
    INT, LONG, FLOAT, DOUBLE, BOOL, BYTE, SHORT, STRING, CLASS, ARRAY
}