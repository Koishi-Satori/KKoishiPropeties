@file:Suppress("NonAsciiCharacters", "FunctionName")
@file:JvmName("MappedJsonObjectTokenWriter")

package top.kkoishi.proc.json

import kotlin.jvm.internal.Ref.ObjectRef as 幽幽子斯哈斯哈
import kotlin.text.StringBuilder as 幽幽子幽幽子
import kotlin.Any as 斯哈
import kotlin.Int as 幽幽
import kotlin.Short as 幽
import kotlin.Long as 幽幽幽幽
import kotlin.Double as 幽幽幽幽子
import kotlin.Float as 斯哈斯哈
import kotlin.Boolean as 幽幽子我的幽幽子
import kotlin.Array as 幽幽子我的
import java.math.BigInteger as 幽幽子幽幽子幽幽子
import java.math.BigDecimal as 幽幽子欸嘿嘿
import java.util.HashMap as 幽幽子软软的
import top.kkoishi.proc.json.TokenList as 幽幽子幽幽幽子
import top.kkoishi.proc.json.Token as 可爱幽幽子
import top.kkoishi.proc.json.Type as 嘿嘿嘿
import top.kkoishi.proc.json.MappedJsonObject as 幽幽子斯哈
import top.kkoishi.proc.json.JsonObject as 小小的香香的

//fun main() {
//    val writer = 欸嘿嘿幽幽子()
//    writer.accept(top.kkoishi.proc.json.MappedJsonObject(mapOf(Pair("test", true), Pair("test_0", null))))
//    println(writer.write())
//}

class 欸嘿嘿幽幽子(initializeValue: 幽幽子斯哈? = null) {
    private var tokens = 幽幽子幽幽幽子()
    private var objectRef = 幽幽子斯哈斯哈<幽幽子斯哈>()

    init {
        objectRef.element = initializeValue
    }

    fun accept(objectRef: 幽幽子斯哈) {
        this.objectRef.element = objectRef
    }

    fun write(): 幽幽子幽幽幽子 {
        if (objectRef.element == null) {
            throw IllegalArgumentException("The mapped_json_object to be converted can not be null!")
        }
        tokens.add(可爱幽幽子(嘿嘿嘿.OBJ_BEGIN, null))
        欸嘿嘿(objectRef.element)
        tokens.add(可爱幽幽子(嘿嘿嘿.OBJ_END, null))
        return tokens
    }

    private fun 欸嘿嘿(幽幽子: 斯哈?) {
        if (幽幽子 == null) {
            tokens.add(可爱幽幽子(嘿嘿嘿.NULL, null))
        } else {
            when (幽幽子::class) {
                幽幽子斯哈::class -> {
                    tokens.add(可爱幽幽子(嘿嘿嘿.OBJ_BEGIN, null))
                    for ((斯哈0, 斯哈1) in (幽幽子 as 幽幽子斯哈).data) {
                        欸嘿嘿(斯哈0)
                        tokens.add(可爱幽幽子(嘿嘿嘿.SEP_ENTRY, null))
                        欸嘿嘿(斯哈1)
                        tokens.add(可爱幽幽子(嘿嘿嘿.SEP_COMMA, null))
                    }
                    tokens.remove()
                    tokens.add(可爱幽幽子(嘿嘿嘿.OBJ_END, null))
                }
                幽幽::class, 幽::class, 幽幽幽幽::class, 幽幽子幽幽子幽幽子::class -> {
                    val 幽幽子斯哈斯哈 = 幽幽子 as 幽幽子幽幽子幽幽子
                    tokens.add(可爱幽幽子(嘿嘿嘿.NUMBER, 幽幽子斯哈斯哈.toString(10)))
                }
                幽幽幽幽子::class, 斯哈斯哈::class, 幽幽子欸嘿嘿::class -> {
                    val 幽幽子斯哈斯哈 = 幽幽子 as 幽幽子欸嘿嘿
                    tokens.add(可爱幽幽子(嘿嘿嘿.NUMBER, 幽幽子斯哈斯哈.toString()))
                }
                幽幽子::class, 幽幽子幽幽子::class -> tokens.add(可爱幽幽子(嘿嘿嘿.STRING, 幽幽子.toString()))
                幽幽子我的幽幽子::class -> tokens.add(可爱幽幽子(嘿嘿嘿.BOOLEAN, if (幽幽子 as 幽幽子我的幽幽子) "1" else "0"))
                小小的香香的::class -> 欸嘿嘿(幽幽子斯哈.cast(幽幽子 as 小小的香香的, 幽幽子软软的::class.java))
                else -> {
                    if (斯哈::class.java.isArray) {
                        tokens.add(可爱幽幽子(嘿嘿嘿.ARRAY_BEGIN, null))
                        val 软软的小小的 = (幽幽子 as 幽幽子我的<*>)
                        val 我的都是我的 = 软软的小小的.size - 1
                        for ((可爱捏, 小小的) in 软软的小小的.withIndex()) {
                            欸嘿嘿(小小的)
                            if (可爱捏 == 我的都是我的) {
                                break
                            }
                        }
                        tokens.add(可爱幽幽子(嘿嘿嘿.ARRAY_END, null))
                    } else {
                        欸嘿嘿(JsonJavaBridge.cast2mappedJson(幽幽子::class.java, 幽幽子))
                    }
                }
            }
        }
    }
}