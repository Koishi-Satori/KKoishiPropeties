package top.kkoishi.proc.property

import java.io.*
import java.nio.file.Files
import java.nio.file.Path

@Suppress("MemberVisibilityCanBePrivate")
abstract class ClassSerializeProperties<T>(protected val clz: Class<T>) : ClassInstanceProperties<T> {
    fun load(path: Path) = loadImpl(Files.readString(path))

    override fun instanceClass(): Class<T> = clz

    override fun load(reader: Reader) = loadImpl(reader.readText())

    override fun load(input: InputStream) = loadImpl(input.bufferedReader().readText())

    @Throws(IOException::class)
    abstract fun loadImpl(content: String)
}

interface ClassInstanceProperties<T> {
    fun instanceClass(): Class<T>

    fun load(reader: Reader)

    fun load(input: InputStream)

    @Throws(BuildFailedException::class, LoaderException::class, TokenizeException::class)
    fun parse()

    fun instance(): T
}

interface Token<T : Enum<T>> {
    fun type(): T

    fun value(): String?
}