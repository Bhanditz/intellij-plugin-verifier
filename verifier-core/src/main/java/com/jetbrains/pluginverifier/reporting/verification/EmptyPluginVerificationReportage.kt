package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.misc.impossible
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning

object EmptyPluginVerificationReportage : PluginVerificationReportage {
  override val plugin: PluginCoordinate
    get() = impossible()

  override val ideVersion: IdeVersion
    get() = impossible()

  override fun logVerificationStarted() = Unit

  override fun logVerificationFinished() = Unit

  override fun logDependencyGraph(dependenciesGraph: DependenciesGraph) = Unit

  override fun logNewProblemDetected(problem: Problem) = Unit

  override fun logNewWarningDetected(warning: Warning) = Unit

  override fun logProgress(completed: Double) = Unit

  override fun logVerdict(verdict: Verdict) = Unit

  override fun logProblemIgnored(problem: Problem, reason: String) = Unit

  override fun logDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) = Unit

  override fun close() = Unit
}