/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec

import org.jetbrains.kotlin.spec.validators.LinkedSpecTestValidator
import org.jetbrains.kotlin.spec.validators.NotLinkedSpecTestValidator
import org.jetbrains.kotlin.spec.validators.SpecTestLinkedType
import org.jetbrains.kotlin.spec.validators.TestArea
import java.io.File
import java.util.regex.Matcher

open class SpecTestsStatElement(
    val parent: SpecTestsStatElement?,
    val type: SpecTestsStatElementType
) {
    val elements: MutableMap<Any, SpecTestsStatElement> = mutableMapOf()
    var number = 0
    fun increment() {
        number++
    }
}

enum class SpecTestsStatElementType {
    TYPE,
    CATEGORY,
    PARAGRAPH,
    SECTION,
    AREA
}

object TestsStatisticCollector {
    private const val TEST_DATA_DIR = "./compiler/tests-spec/testData"

    private fun incrementStatCounters(baseStatElement: SpecTestsStatElement, elementTypes: List<Pair<SpecTestsStatElementType, Any>>) {
        var currentStatElement = baseStatElement

        baseStatElement.increment()

        for ((elementType, value) in elementTypes) {
            currentStatElement = currentStatElement.run {
                elements.computeIfAbsent(value) {
                    SpecTestsStatElement(currentStatElement, elementType)
                }.apply { increment() }
            }
        }
    }

    fun collect(testLinkedType: SpecTestLinkedType): Map<TestArea, SpecTestsStatElement> {
        val statistic = mutableMapOf<TestArea, SpecTestsStatElement>()

        for (specTestArea in TestArea.values()) {
            val specTestsPath = "$TEST_DATA_DIR/${specTestArea.name.toLowerCase()}"

            statistic[specTestArea] = SpecTestsStatElement(null, SpecTestsStatElementType.AREA)

            File(specTestsPath).walkTopDown().forEach areaTests@{
                if (!it.isFile || it.extension != "kt") return@areaTests

                val testInfoMatcher = when (testLinkedType) {
                    SpecTestLinkedType.LINKED -> LinkedSpecTestValidator
                    SpecTestLinkedType.NOT_LINKED -> NotLinkedSpecTestValidator
                }.getPathPattern().matcher(it.path)

                if (!testInfoMatcher.find()) return@areaTests

                incrementStatCounters(
                    statistic[specTestArea]!!,
                    when (testLinkedType) {
                        SpecTestLinkedType.LINKED -> getStatElementsByLinkedTests(testInfoMatcher)
                        SpecTestLinkedType.NOT_LINKED -> getStatElementsByNotLinkedTests(testInfoMatcher)
                    }
                )
            }
        }

        return statistic
    }

    private fun getStatElementsByLinkedTests(testInfoMatcher: Matcher) = listOf(
        SpecTestsStatElementType.SECTION to testInfoMatcher.group("sectionName"),
        SpecTestsStatElementType.PARAGRAPH to testInfoMatcher.group("paragraphNumber").toInt(),
        SpecTestsStatElementType.TYPE to testInfoMatcher.group("testType")
    )

    private fun getStatElementsByNotLinkedTests(testInfoMatcher: Matcher) =
        mutableListOf(
            SpecTestsStatElementType.SECTION to testInfoMatcher.group("sectionName")
        ).apply {
            addAll(testInfoMatcher.group("categories").split("/").map { SpecTestsStatElementType.CATEGORY to it })
            add(SpecTestsStatElementType.TYPE to testInfoMatcher.group("testType"))
        }
}