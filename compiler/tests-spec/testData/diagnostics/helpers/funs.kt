fun _fun(value_1: List<Int>, value_2: List<Int>): Int {
    return value_1[0] + value_2[1]
}

fun _fun(value_1: List<Int>): Int {
    return value_1[0] + value_2[1]
}

fun _fun(): Int {
    return Any().hashCode().toInt()
}

fun _funWithAnyArg(value_1: Any): Int {
    return value_1.hashCode()
}
