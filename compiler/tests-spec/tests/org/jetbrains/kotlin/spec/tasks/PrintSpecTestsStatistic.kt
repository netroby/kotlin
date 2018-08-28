/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.tasks

import org.jetbrains.kotlin.spec.SpecTestsStatElement
import org.jetbrains.kotlin.spec.TestsStatisticCollector
import org.jetbrains.kotlin.spec.validators.SpecTestLinkedType
import org.jetbrains.kotlin.spec.validators.TestType

fun linkedSpecTestsPrint() {
    println("SPEC TESTS STATISTIC")
    println("--------------------------------------------------")

    val statistic = TestsStatisticCollector.collect(SpecTestLinkedType.LINKED)

    for ((areaName, areaElement) in statistic) {
        println("$areaName: ${areaElement.number} tests")
        for ((sectionName, sectionElement) in areaElement.elements) {
            println("  $sectionName: ${sectionElement.number} tests")
            for ((paragraphName, paragraphElement) in sectionElement.elements) {
                val testsStatByType = mutableListOf<String>()
                for ((typeName, typeElement) in paragraphElement.elements) {
                    testsStatByType.add("$typeName: ${typeElement.number}")
                }
                println("    PARAGRAPH $paragraphName: ${paragraphElement.number} tests (${testsStatByType.joinToString(", ")})")
            }
        }
    }
}

fun notLinkedSpecTestsCategoryPrint(elements: Map<Any, SpecTestsStatElement>, level: Int = 0) {
    for ((name, element) in elements) {
        print("   ")
        for (i in 0..level) print("  ")
        print("$name: ${element.number} tests")
        val isTypeChildElements = element.elements[TestType.POSITIVE.type] != null || element.elements[TestType.NEGATIVE.type] != null
        if (isTypeChildElements) {
            val testsStatByType = mutableListOf<String>()

            for ((typeName, typeElement) in element.elements) {
                testsStatByType.add("$typeName: ${typeElement.number}")
            }
            print(" (${testsStatByType.joinToString(", ")})")
            println()
        } else if (element.elements.isNotEmpty()) {
            println()
            notLinkedSpecTestsCategoryPrint(element.elements, level + 1)
        }
    }
}

fun notLinkedSpecTestsPrint() {
    println("NOT LINKED SPEC TESTS STATISTIC")
    println("--------------------------------------------------")

    val statistic = TestsStatisticCollector.collect(SpecTestLinkedType.NOT_LINKED)

    for ((areaName, areaElement) in statistic) {
        println("$areaName: ${areaElement.number} tests")
        for ((sectionName, sectionElement) in areaElement.elements) {
            println("  $sectionName: ${sectionElement.number} tests")
            notLinkedSpecTestsCategoryPrint(sectionElement.elements)
        }
    }
}

fun main(args: Array<String>) {
    println("==================================================")
    linkedSpecTestsPrint()
    println("==================================================")
    notLinkedSpecTestsPrint()
    println("==================================================")
}