package top.kkoishi.proc.json

import top.kkoishi.proc.property.BuildFailedException
import top.kkoishi.proc.property.ClassSerializeProperties
import top.kkoishi.proc.property.LoaderException
import top.kkoishi.proc.property.TokenizeException
import java.io.IOException

@Suppress("MemberVisibilityCanBePrivate")
open class JsonProperties<T>(clz: Class<T>) : ClassSerializeProperties<T>(clz) {
    protected var parser: JsonParser? = null

    @Throws(IOException::class)
    override fun loadImpl(content: String) =
        if (parser != null) parser!!.reset(content) else this.parser = JsonParser(content)

    override fun instance(): T = JsonJavaBridge.cast(clz, parser!!.result())

    @Throws(BuildFailedException::class, LoaderException::class, TokenizeException::class)
    override fun parse() = parser!!.parse()

    fun getJsonObject(): JsonObject = parser!!.result()
}