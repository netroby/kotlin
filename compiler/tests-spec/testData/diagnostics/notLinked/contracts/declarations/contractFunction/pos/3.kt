// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: Contracts
 CATEGORY: declarations, contractFunction
 NUMBER: 3
 DESCRIPTION: Contract function usage before declaration it
 */

import kotlin.internal.contracts.*

fun case_1_1(x: Any?) {
    if (case_1_2(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun case_2_1(x: Number?) {
    case_2_2(x)
    println(<!DEBUG_INFO_SMARTCAST!>x<!>.toByte())
}

fun case_1_2(x: Any?): Boolean {
    contract { returns(true) implies (x is String) }
    return x is String
}

fun case_2_2(x: Any?) {
    contract { returns() implies(x != null) }
    if (x == null) throw Exception()
}


