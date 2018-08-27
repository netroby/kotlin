package test

import kotlin.reflect.KClass

annotation class Anno(
    val klass: KClass<*>,
    val klasses: Array<KClass<*>>
)

@Anno(
    String::class,
    arrayOf(Int::class, String::class, Float::class)
)
class Klass
