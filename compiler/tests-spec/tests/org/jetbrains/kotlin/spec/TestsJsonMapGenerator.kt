/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

object TestsJsonMapGenerator {
    val testDataDir = "./testData"
    val outDir = "./out"
    val outFilename = "testsMap.json"

    val specTestAreas = listOf("diagnostics", "psi", "codegen")
    val integerRegex = "[1-9]\\d*"
    val sectionFolderRegex = "s-(?<sectionNumber>(?:$integerRegex)(?:\\.$integerRegex)*)_(?<sectionName>[\\w-]+)"
    val testPathRegex =
        "^.*?/(?<testArea>${specTestAreas.joinToString("|")})/$sectionFolderRegex/p-(?<paragraphNumber>$integerRegex)/(?<testType>pos|neg)/(?<sentenceNumber>$integerRegex)\\.(?<testNumber>$integerRegex)\\.kt$"
    val testUnexpectedBehaviour = "(?:\\s*(?<unexpectedBehaviour>UNEXPECTED BEHAVIOUR)\n)"
    val testIssues = "(?:\\s*ISSUES:\\s*(?<issues>(KT-[1-9]\\d*)(,\\s*KT-[1-9]\\d*)*)\n)"
    val testContentRegex =
        "\\/\\*\\s+KOTLIN (?<testArea>DIAGNOSTICS|PSI|CODEGEN) SPEC TEST \\((?<testType>POSITIVE|NEGATIVE)\\)\n\\s+SECTION: (?<sectionNumber>(?:$integerRegex)(?:\\.$integerRegex)*)\\s*(?<sectionName>.*?)\n\\s+PARAGRAPH:\\s*(?<paragraphNumber>$integerRegex)\n\\s+SENTENCE:\\s*\\[(?<sentenceNumber>$integerRegex)\\]\\s*(?<sentence>.*?)\n\\s+NUMBER:\\s*(?<testNumber>$integerRegex)\n\\s+DESCRIPTION:\\s*(?<description>.*?)\n$testUnexpectedBehaviour?$testIssues?\\s*[\\s\\S]*?\\*\\/"
    val testCaseInfo =
        "(?:(?:\\/\\*\n\\s*)|(?:\\/\\/\\s*))CASE DESCRIPTION:\\s*(?<description>.*?)\n$testUnexpectedBehaviour?$testIssues?(\\s*\\*\\/)?"

    val stringListType = object : TypeToken<List<String>>() {}.getType()

    fun addJsonIfNotExist(element: JsonObject, key: String): JsonObject {
        if (!element.has(key)) element.add(key, JsonObject())
        return element.get(key).asJsonObject
    }

    fun addInfoToTestElement(testElement: JsonObject, testElementInfoMatcher: Matcher): JsonObject {
        val unexpectedBehaviour = testElementInfoMatcher.group("unexpectedBehaviour") != null
        val issues = testElementInfoMatcher.group("issues")?.split(Regex(",\\s*"))

        testElement.addProperty("description", testElementInfoMatcher.group("description"))
        if (unexpectedBehaviour) testElement.addProperty("unexpectedBehaviour", unexpectedBehaviour)
        if (issues !== null) testElement.add("issues", Gson().toJsonTree(issues, stringListType))

        return testElement
    }

    fun getTestCasesInfo(testCaseInfoMatcher: Matcher, testInfoMatcher: Matcher): JsonArray {
        val testCases = JsonArray()

        while (testCaseInfoMatcher.find()) {
            testCases.add(addInfoToTestElement(JsonObject(), testCaseInfoMatcher))
        }

        return testCases
    }

    fun generate() {
        val testsMap = JsonObject()
        val gson = Gson()

        File(testDataDir).walkTopDown().forEach {
            val testInfoByPathMatcher = Pattern.compile(testPathRegex).matcher(it.path)
            if (!testInfoByPathMatcher.find()) return@forEach

            val sectionElement = addJsonIfNotExist(testsMap, testInfoByPathMatcher.group("sectionName"))
            val paragraphElement = addJsonIfNotExist(sectionElement, testInfoByPathMatcher.group("paragraphNumber"))
            val sentenceElement = addJsonIfNotExist(paragraphElement, testInfoByPathMatcher.group("sentenceNumber"))
            val testAreaElement = addJsonIfNotExist(sentenceElement, testInfoByPathMatcher.group("testArea"))
            val testTypeElement = addJsonIfNotExist(testAreaElement, testInfoByPathMatcher.group("testType"))
            val testNumberElement = addJsonIfNotExist(testTypeElement, testInfoByPathMatcher.group("testNumber"))

            val testFileContent = File(it.path).readText()
            val testInfoByContentMatcher = Pattern.compile(testContentRegex).matcher(testFileContent)
            val testCaseInfoMatcher = Pattern.compile(testCaseInfo).matcher(testFileContent)

            println(testContentRegex)
            println(testFileContent)
            testInfoByContentMatcher.find()

            addInfoToTestElement(testNumberElement, testInfoByContentMatcher)

            val testCases = getTestCasesInfo(testCaseInfoMatcher, testInfoByContentMatcher)
            if (testCases.size() != 0) testNumberElement.add("cases", testCases)
        }

        File(outDir).mkdir()
        File("$outDir/$outFilename").writeText(testsMap.toString())
    }
}