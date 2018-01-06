package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import java.net.URL

/**
 * Created by Sergey.Patrikeev
 */
object EmptyPublicPluginRepository : PluginRepository {
  override val repositoryURL: URL = URL("http://example.com")

  override fun getAllPlugins(): List<PluginInfo> = emptyList()

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> = emptyList()

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? = null

  override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<PluginInfo> = emptyList()

  override fun getAllVersionsOfPlugin(pluginId: String): List<PluginInfo> = emptyList()

  override fun downloadPluginFile(pluginInfo: PluginInfo): FileRepositoryResult = FileRepositoryResult.NotFound("")

  override fun getPluginInfoById(updateId: Int): UpdateInfo? = null

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? = null

}