package com.jetbrains.pluginverifier.tasks.deprecatedUsages

import com.jetbrains.pluginverifier.misc.pluralize
import com.jetbrains.pluginverifier.misc.pluralizeWithNumber
import com.jetbrains.pluginverifier.output.OutputOptions
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.results.deprecated.formatUsageLocation
import com.jetbrains.pluginverifier.results.deprecated.locationType
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.tasks.TaskResult
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

/**
 * @author Sergey Patrikeev
 */
class DeprecatedUsagesResultPrinter(val outputOptions: OutputOptions, val pluginRepository: PluginRepository) : TaskResultPrinter {

  override fun printResults(taskResult: TaskResult) {
    val deprecatedUsagesResult = taskResult as DeprecatedUsagesResult
    if (outputOptions.needTeamCityLog) {
      val teamCityLog = TeamCityLog(System.out)
      with(deprecatedUsagesResult) {
        val deprecatedIdeApiToPluginUsages = hashMapOf<Location, MutableMap<PluginInfo, Int>>()
        for ((plugin, pluginUsages) in pluginDeprecatedUsages) {
          pluginUsages
              .asSequence()
              .filter { it.deprecatedElement in deprecatedIdeApiElements }
              .forEach {
                deprecatedIdeApiToPluginUsages
                    .getOrPut(it.deprecatedElement, { hashMapOf() })
                    .compute(plugin, { _, c -> (c ?: 0) + 1 })
              }
        }

        /**
         * Print the "Mostly used IU-172.1331 deprecated API" tab like this:
         *   Class org.jetbrains.Old is used in 3 plugins:
         *     org.plugin.one:1.0 (3 usages)
         *     org.plugin.two:2.0 (2 usages)
         *     org.plugin.three:3.0 (1 usage)
         *     <limited to ten mostly using plugins>
         *
         *   Method org.jetbrains.Dep.oldMethod() : void is used in 2 plugins:
         *     org.plugin.one:1.0 (2 usages)
         *     org.plugin.two:2.0 (1 usage)
         */
        if (deprecatedIdeApiToPluginUsages.isNotEmpty()) {
          val testName = "(Mostly used $ideVersion deprecated API)"
          teamCityLog.testStarted(testName).use {
            val sortedByNumberOfPlugins = deprecatedIdeApiToPluginUsages.toList()
                .sortedWith(compareByDescending<Pair<Location, MutableMap<PluginInfo, Int>>> { it.second.size }.thenBy { it.first.locationType })
            val fullTestMessage = buildString {
              for ((deprecatedApiElement, pluginToUsagesNumber) in sortedByNumberOfPlugins) {
                append(deprecatedApiElement.locationType.capitalize())
                append(" " + deprecatedApiElement.formatUsageLocation())
                appendln(" is used in ${pluginToUsagesNumber.size} " + "plugin".pluralize(pluginToUsagesNumber.size) + ":")
                val sortedByNumberOfUsages = pluginToUsagesNumber.toList()
                    .sortedWith(compareByDescending<Pair<PluginInfo, Int>> { it.second }.thenBy { it.first.pluginId })
                for ((plugin, usagesNumber) in sortedByNumberOfUsages) {
                  append("  ")
                  append("${plugin.pluginId} ${plugin.version}")
                  appendln(" ($usagesNumber " + "usage".pluralize(usagesNumber) + ")")
                }
                appendln()
              }
            }
            teamCityLog.testStdErr(testName, fullTestMessage)
            teamCityLog.testFailed(testName, "There " + "is".pluralize(deprecatedIdeApiToPluginUsages.size) + " ${deprecatedIdeApiToPluginUsages.size} deprecated API " + "element".pluralize(deprecatedIdeApiToPluginUsages.size) + " in $ideVersion used in checked plugins", "")
          }
        }

        /**
         * Print the "Unused IU-172.1331 deprecated API" tab like this:
         * There are 2 deprecated API classes in IU-172.1331 unused in checked plugins:
         *   Class org.jetbrains.Unused
         *   Class org.jetbrains.SuperOldClass
         *
         * There is 1  deprecated API method in IU-172.1331:
         *   Method org.jetbrains.Unused.unusedMethod
         */
        val unusedIdeDeprecatedElements = deprecatedIdeApiElements - deprecatedIdeApiToPluginUsages.keys
        val unusedIdeApiElementsNumber = unusedIdeDeprecatedElements.size
        if (unusedIdeDeprecatedElements.isNotEmpty()) {
          val testName = "(Unused $ideVersion deprecated API elements)"
          teamCityLog.testStarted(testName).use {
            val fullTestMessage = buildString {
              for ((locationType, unusedApiElementsWithType) in unusedIdeDeprecatedElements.groupBy { it.locationType }) {
                appendln("There " + "is".pluralizeWithNumber(unusedApiElementsWithType.size) + " deprecated API " + locationType.pluralize(unusedApiElementsWithType.size) + " in $ideVersion unused in checked plugins:")
                val formattedUnusedUsages = unusedApiElementsWithType.map { it.formatUsageLocation() }.sorted()
                for (unusedElement in formattedUnusedUsages) {
                  append("  ")
                  appendln(unusedElement)
                }
                appendln()
              }
            }
            teamCityLog.testStdErr(testName, fullTestMessage)
            teamCityLog.testFailed(testName, "There " + "is".pluralize(unusedIdeApiElementsNumber) + " $unusedIdeApiElementsNumber deprecated API " +
                "element".pluralize(unusedIdeApiElementsNumber) + " in $ideVersion unused in checked plugins.\n" +
                "You can explore concrete usages' details by looking into the verification-results " +
                "directory of a specific plugin (see the build artifacts), or via the Find External Usages action of the API Watcher plugin", ""
            )
          }
        }

        val allPluginsHavingDeprecatedApiUsages = deprecatedIdeApiToPluginUsages.values.flatMap { it.keys }.distinct()
        val numberOfPlugins = allPluginsHavingDeprecatedApiUsages.size
        /**
         * Print the "List of 2 plugins being checked"
         *   org.plugin.one 1.0
         *   com.plugin.two 2.0
         */
        if (allPluginsHavingDeprecatedApiUsages.isNotEmpty()) {
          val testName = "(List of $numberOfPlugins " + "plugin".pluralize(numberOfPlugins) + " being checked)"
          teamCityLog.testStarted(testName).use {
            val fullTestMessage = buildString {
              allPluginsHavingDeprecatedApiUsages
                  .sortedBy { it.pluginId }
                  .forEach {
                    val overviewUrl = pluginRepository.getPluginOverviewUrl(it)?.let { " ($it)" } ?: ""
                    appendln("%-50s %-25s%s".format(it.pluginId, it.version, overviewUrl))
                  }
            }
            teamCityLog.testStdErr(testName, fullTestMessage)
            teamCityLog.testFailed(testName, "There " + "is".pluralize(numberOfPlugins) + " $numberOfPlugins " + "plugin".pluralize(numberOfPlugins) + " checked", "")
          }
        }

        if (deprecatedIdeApiToPluginUsages.isNotEmpty() || unusedIdeDeprecatedElements.isNotEmpty()) {
          teamCityLog.buildStatusFailure(
              buildString {
                append("In $ideVersion found ${deprecatedIdeApiElements.size} deprecated API " + "element".pluralize(deprecatedIdeApiElements.size) + ": ")
                append("${deprecatedIdeApiToPluginUsages.size} " + "is".pluralize(deprecatedIdeApiToPluginUsages.size) + " used in $numberOfPlugins checked " + "plugin".pluralize(numberOfPlugins))
                append(" and $unusedIdeApiElementsNumber " + "is".pluralize(unusedIdeApiElementsNumber) + " unused in " + "this".pluralize(numberOfPlugins) + " " + "plugin".pluralize(numberOfPlugins))
              }
          )
        } else {
          teamCityLog.buildStatusSuccess("API of $ideVersion is up to date with $allPluginsHavingDeprecatedApiUsages " + "plugin".pluralize(numberOfPlugins) + " being checked")
        }
      }

    }
  }

}