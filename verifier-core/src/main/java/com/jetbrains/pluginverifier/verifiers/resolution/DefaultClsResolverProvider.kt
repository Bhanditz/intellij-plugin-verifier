package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.ResultHolder
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.graph.DepEdge
import com.jetbrains.pluginverifier.dependencies.graph.DepGraph2ApiGraphConverter
import com.jetbrains.pluginverifier.dependencies.graph.DepGraphBuilder
import com.jetbrains.pluginverifier.dependencies.graph.DepVertex
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.cache.ResourceCacheEntryResult
import org.jgrapht.DirectedGraph
import org.jgrapht.graph.DefaultDirectedGraph
import java.io.Closeable

/**
 * [ClsResolverProvider] that provides the [DefaultClsResolver].
 */
class DefaultClsResolverProvider(
    private val dependencyFinder: DependencyFinder,
    private val jdkDescriptorsCache: JdkDescriptorsCache,
    private val jdkPath: JdkPath,
    private val ideDescriptor: IdeDescriptor,
    private val externalClassesPackageFilter: PackageFilter
) : ClsResolverProvider {

  override fun provide(
      checkedPluginDetails: PluginDetails,
      resultHolder: ResultHolder
  ): ClsResolver {
    val pluginResolver = checkedPluginDetails.pluginClassesLocations.createPluginResolver()

    val depGraph: DirectedGraph<DepVertex, DepEdge> = DefaultDirectedGraph(DepEdge::class.java)
    return try {
      val apiGraph = buildDependenciesGraph(checkedPluginDetails.idePlugin, depGraph)
      resultHolder.dependenciesGraph = apiGraph
      resultHolder.addCycleWarningIfExists(apiGraph)
      provide(pluginResolver, depGraph)
    } catch (e: Throwable) {
      depGraph.vertexSet().forEach { it.dependencyResult.closeLogged() }
      throw e
    }
  }

  private fun provide(
      pluginResolver: Resolver,
      depGraph: DirectedGraph<DepVertex, DepEdge>
  ): ClsResolver {
    val dependenciesResults = depGraph.vertexSet().map { it.dependencyResult }
    val dependenciesResolver = createDependenciesResolver(depGraph)

    val jdkCacheEntry = jdkDescriptorsCache.getJdkResolver(jdkPath)
    return when (jdkCacheEntry) {
      is ResourceCacheEntryResult.Found -> {
        val jdkClassesResolver = jdkCacheEntry.resourceCacheEntry.resource.jdkClassesResolver
        val closeableResources = listOf<Closeable>(jdkCacheEntry.resourceCacheEntry) + dependenciesResults
        DefaultClsResolver(
            pluginResolver,
            dependenciesResolver,
            jdkClassesResolver,
            ideDescriptor.ideResolver,
            externalClassesPackageFilter,
            closeableResources
        )
      }
      is ResourceCacheEntryResult.Failed -> throw IllegalStateException("Unable to resolve JDK descriptor", jdkCacheEntry.error)
      is ResourceCacheEntryResult.NotFound -> throw IllegalStateException("Unable to find JDK $jdkPath: ${jdkCacheEntry.message}")
    }
  }

  private fun buildDependenciesGraph(
      plugin: IdePlugin,
      depGraph: DirectedGraph<DepVertex, DepEdge>
  ): DependenciesGraph {
    val start = DepVertex(plugin.pluginId!!, DependencyFinder.Result.FoundPlugin(plugin))
    DepGraphBuilder(dependencyFinder).buildDependenciesGraph(depGraph, start)
    return DepGraph2ApiGraphConverter(ideDescriptor.ideVersion).convert(depGraph, start)
  }

  private fun createDependenciesResolver(graph: DirectedGraph<DepVertex, DepEdge>): Resolver {
    val dependenciesResolvers = arrayListOf<Resolver>()
    dependenciesResolvers.closeOnException {
      for (depVertex in graph.vertexSet()) {
        val result = depVertex.dependencyResult
        if (result is DependencyFinder.Result.DetailsProvided) {
          val cacheResult = result.pluginDetailsCacheResult
          if (cacheResult is PluginDetailsCache.Result.Provided) {
            val pluginResolver = try {
              cacheResult.pluginDetails.pluginClassesLocations.createPluginResolver()
            } catch (ie: InterruptedException) {
              throw ie
            } catch (e: Exception) {
              continue
            }
            dependenciesResolvers.add(pluginResolver)
          }
        }
      }
      return UnionResolver.create(dependenciesResolvers)
    }
  }
}