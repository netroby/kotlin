package test

import kotlin.reflect.KClass

annotation class Anno(
    val klass: KClass<*>,
    val klasses: Array<KClass<*>>,
    val arrKlass: KClass<Array<String>>
)

@Anno(
    String::class,
    arrayOf(Int::class, String::class, Float::class),
    Array<String>::class
)
class Klass
