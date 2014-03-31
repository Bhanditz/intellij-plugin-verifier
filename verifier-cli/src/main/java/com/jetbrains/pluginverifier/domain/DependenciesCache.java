package com.jetbrains.pluginverifier.domain;

import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import com.jetbrains.pluginverifier.resolvers.CombiningResolver;
import com.jetbrains.pluginverifier.resolvers.Resolver;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DependenciesCache {
  private static DependenciesCache ourInstance = new DependenciesCache();

  private static final Set<IdeaPlugin> DEP_CALC_MARKER = new HashSet<IdeaPlugin>();

  public static DependenciesCache getInstance() {
    return ourInstance;
  }

  private final WeakHashMap<Idea, WeakHashMap<IdeaPlugin, PluginDependenciesDescriptor>> map = new WeakHashMap<Idea, WeakHashMap<IdeaPlugin, PluginDependenciesDescriptor>>();

  private DependenciesCache() {
  }

  private PluginDependenciesDescriptor getPluginDependenciesDescriptor(Idea ide, IdeaPlugin plugin) {
    WeakHashMap<IdeaPlugin, PluginDependenciesDescriptor> m = map.get(ide);
    if (m == null) {
      m = new WeakHashMap<IdeaPlugin, PluginDependenciesDescriptor>();
      map.put(ide, m);
    }

    PluginDependenciesDescriptor descr = m.get(plugin);
    if (descr == null) {
      descr = new PluginDependenciesDescriptor();
      m.put(plugin, descr);
    }

    return descr;
  }

  public Resolver getResolver(Idea ide, IdeaPlugin plugin) {
    PluginDependenciesDescriptor descr = getPluginDependenciesDescriptor(ide, plugin);
    if (descr.myResolver == null) {
      List<Resolver> resolvers = new ArrayList<Resolver>();

      resolvers.add(plugin.getClassPool());
      resolvers.add(ide.getResolver());

      for (IdeaPlugin dep : getDependenciesWithTransitive(ide, plugin)) {
        ClassPool pluginClassPool = dep.getPluginClassPool();
        if (!pluginClassPool.isEmpty()) {
          resolvers.add(pluginClassPool);
        }

        ClassPool libraryClassPool = dep.getLibraryClassPool();
        if (!libraryClassPool.isEmpty()) {
          resolvers.add(libraryClassPool);
        }
      }

      descr.myResolver = CombiningResolver.union(resolvers);
    }

    return descr.myResolver;
  }

  public Set<IdeaPlugin> getDependenciesWithTransitive(Idea ide, IdeaPlugin plugin) {
    PluginDependenciesDescriptor descriptor = getPluginDependenciesDescriptor(ide, plugin);

    Set<IdeaPlugin> res = descriptor.dependenciesWithTransitive;
    if (res == DEP_CALC_MARKER) throw new RuntimeException("Cyclic plugin dependencies");

    if (descriptor.dependenciesWithTransitive == null) {
      descriptor.dependenciesWithTransitive = DEP_CALC_MARKER;

      try {
        res = new HashSet<IdeaPlugin>();

        for (PluginDependency pluginDependency : plugin.getDependencies()) {
          IdeaPlugin depPlugin = ide.getBundledPlugin(pluginDependency.getId());
          if (depPlugin == null) {
            try {
              UpdateInfo updateInfo = RepositoryManager.getInstance().findPlugin(ide.getVersion(), pluginDependency.getId());
              if (updateInfo != null) {
                File pluginZip = RepositoryManager.getInstance().getOrLoadUpdate(updateInfo);
                depPlugin = PluginCache.getInstance().getPlugin(pluginZip);
              }
            }
            catch (IOException e) {
              e.printStackTrace();
            }
          }

          if (depPlugin != null && res.add(depPlugin)) {
            res.addAll(getDependenciesWithTransitive(ide, depPlugin));
          }
        }

        descriptor.dependenciesWithTransitive = res;
      }
      finally {
        if (descriptor.dependenciesWithTransitive == DEP_CALC_MARKER) {
          descriptor.dependenciesWithTransitive = null;
        }
      }
    }

    return res;
  }

  private static class PluginDependenciesDescriptor {
    public Resolver myResolver;

    public Set<IdeaPlugin> dependenciesWithTransitive;
  }
}