package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParameters


class CheckTrunkApiParams(pluginsSet: PluginsSet,
                          val jdkPath: JdkPath,
                          val trunkIde: IdeDescriptor,
                          val releaseIde: IdeDescriptor,
                          val externalClassesPackageFilter: PackageFilter,
                          val problemsFilters: List<ProblemsFilter>,
                          val jetBrainsPluginIds: List<String>,
                          private val deleteReleaseIdeOnExit: Boolean,
                          private val releaseIdeFile: FileLock,
                          val releaseLocalPluginsRepository: LocalPluginRepository?,
                          val trunkLocalPluginsRepository: LocalPluginRepository?) : TaskParameters(pluginsSet) {
  override val presentableText: String
    get() = """
      |Trunk IDE        : $trunkIde
      |Release IDE      : $releaseIde
      |JDK              : $jdkPath
    """.trimMargin()

  override fun close() {
    trunkIde.closeLogged()
    releaseIde.closeLogged()
    releaseIdeFile.release()
    if (deleteReleaseIdeOnExit) {
      releaseIdeFile.file.deleteLogged()
    }
  }

}