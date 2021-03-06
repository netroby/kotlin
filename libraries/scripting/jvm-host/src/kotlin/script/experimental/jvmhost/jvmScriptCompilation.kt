/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.jvmhost

import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingEnvironment
import kotlin.script.experimental.jvmhost.impl.KJvmCompilerImpl
import kotlin.script.experimental.jvmhost.impl.withDefaults

interface CompiledJvmScriptsCache {
    fun get(script: ScriptSource, scriptCompilationConfiguration: ScriptCompilationConfiguration): CompiledScript<*>?
    fun store(compiledScript: CompiledScript<*>, scriptCompilationConfiguration: ScriptCompilationConfiguration)

    object NoCache : CompiledJvmScriptsCache {
        override fun get(
            script: ScriptSource, scriptCompilationConfiguration: ScriptCompilationConfiguration
        ): CompiledScript<*>? = null

        override fun store(
            compiledScript: CompiledScript<*>, scriptCompilationConfiguration: ScriptCompilationConfiguration
        ) {
        }
    }
}

open class JvmScriptCompiler(
    hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingEnvironment,
    val compilerProxy: KJvmCompilerProxy = KJvmCompilerImpl(hostConfiguration.withDefaults()),
    val cache: CompiledJvmScriptsCache = CompiledJvmScriptsCache.NoCache
) : ScriptCompiler {

    override suspend operator fun invoke(
        script: ScriptSource,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>> {
        val refineConfigurationFn = scriptCompilationConfiguration[ScriptCompilationConfiguration.refineConfigurationBeforeParsing]
        val refinedConfiguration =
            refineConfigurationFn?.handler?.invoke(ScriptConfigurationRefinementContext(script, scriptCompilationConfiguration))?.let {
                when (it) {
                    is ResultWithDiagnostics.Failure -> return it
                    is ResultWithDiagnostics.Success -> it.value
                }
            } ?: scriptCompilationConfiguration
        val cached = cache.get(script, refinedConfiguration)

        if (cached != null) return cached.asSuccess()

        return compilerProxy.compile(script, refinedConfiguration).also {
            if (it is ResultWithDiagnostics.Success) {
                cache.store(it.value, refinedConfiguration)
            }
        }
    }
}

interface KJvmCompilerProxy {
    fun compile(
        script: ScriptSource,
        scriptCompilationConfiguration: ScriptCompilationConfiguration
    ): ResultWithDiagnostics<CompiledScript<*>>
}

