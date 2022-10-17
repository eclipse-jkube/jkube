/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.gradle.plugin.task;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;
import org.eclipse.jkube.kit.build.core.GavLabel;
import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogDispatcher;
import org.eclipse.jkube.kit.build.service.docker.watch.WatchContext;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.kit.profile.ProfileUtil;
import org.eclipse.jkube.watcher.api.WatcherContext;
import org.eclipse.jkube.watcher.api.WatcherManager;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestamp;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.applicableNamespace;
import static org.eclipse.jkube.kit.config.service.kubernetes.SummaryServiceUtil.handleExceptionAndSummary;
import static org.eclipse.jkube.kit.config.service.kubernetes.SummaryServiceUtil.printSummary;

public class KubernetesWatchTask extends AbstractJKubeTask {
  @Inject
  public KubernetesWatchTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    setDescription("Used to automatically rebuild Docker images and restart containers in case of updates.");
  }

  @Override
  protected JKubeServiceHub.JKubeServiceHubBuilder initJKubeServiceHubBuilder() {
    return super.initJKubeServiceHubBuilder()
        .dockerServiceHub(DockerServiceHub.newInstance(kitLogger, TaskUtil.initDockerAccess(kubernetesExtension, kitLogger),
            logOutputSpecFactory))
        .buildServiceConfig(TaskUtil.buildServiceConfigBuilder(kubernetesExtension).build());
  }

  @Override
  public void run() {
    try (KubernetesClient kubernetesClient = jKubeServiceHub.getClient()) {
      URL masterUrl = kubernetesClient.getMasterUrl();
      KubernetesResourceUtil.validateKubernetesMasterUrl(masterUrl);

      try {
        List<HasMetadata> resources = KubernetesHelper.loadResources(getManifest(kubernetesClient));
        WatcherContext context = createWatcherContext();

        WatcherManager.watch(resolvedImages,
            applicableNamespace(null, kubernetesExtension.getNamespaceOrNull(), kubernetesExtension.resources, clusterAccess),
            resources,
            context);
      } catch (KubernetesClientException kubernetesClientException) {
        IllegalStateException illegalStateException = KubernetesResourceUtil.handleKubernetesClientException(kubernetesClientException, kitLogger, jKubeServiceHub.getSummaryService());
        printSummary(jKubeServiceHub);
        throw illegalStateException;
      } catch (Exception ioException) {
        handleExceptionAndSummary(jKubeServiceHub, ioException);
        throw new IllegalStateException("An error has occurred while while trying to watch the resources", ioException);
      }
    }
  }

  private WatcherContext createWatcherContext() throws IOException {
    WatchContext watchContext = jKubeServiceHub.getDockerServiceHub() != null ? getWatchContext() : null;
    return WatcherContext.builder()
        .buildContext(jKubeServiceHub.getConfiguration())
        .watchContext(watchContext)
        .config(extractWatcherConfig())
        .logger(kitLogger)
        .newPodLogger(createLogger("[[C]][NEW][[C]] "))
        .oldPodLogger(createLogger("[[R]][OLD][[R]] "))
        .useProjectClasspath(kubernetesExtension.getUseProjectClassPathOrDefault())
        .jKubeServiceHub(jKubeServiceHub)
        .build();
  }

  private ProcessorConfig extractWatcherConfig() {
    try {
      return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.WATCHER_CONFIG, kubernetesExtension.getProfileOrNull(), ResourceUtil.getFinalResourceDirs(kubernetesExtension.getResourceSourceDirectoryOrDefault(), kubernetesExtension.getResourceEnvironmentOrNull()), kubernetesExtension.watcher);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot extract watcher config: " + e, e);
    }
  }

  private WatchContext getWatchContext() throws IOException {
    final DockerServiceHub hub = jKubeServiceHub.getDockerServiceHub();
    return WatchContext.builder()
        .watchInterval(kubernetesExtension.getWatchIntervalOrDefault())
        .watchMode(kubernetesExtension.getWatchModeOrDefault())
        .watchPostExec(kubernetesExtension.getWatchPostExecOrNull())
        .autoCreateCustomNetworks(kubernetesExtension.getWatchAutoCreateCustomNetworksOrDefault())
        .keepContainer(kubernetesExtension.getWatchKeepContainerOrDefault())
        .keepRunning(kubernetesExtension.getWatchKeepRunningOrDefault())
        .removeVolumes(kubernetesExtension.getWatchRemoveVolumesOrDefault())
        .containerNamePattern(kubernetesExtension.getWatchContainerNamePatternOrDefault())
        .buildTimestamp(getBuildTimestamp(null, null, kubernetesExtension.javaProject.getBuildDirectory().getAbsolutePath(), DOCKER_BUILD_TIMESTAMP))
        .gavLabel(new GavLabel(kubernetesExtension.javaProject.getGroupId(), kubernetesExtension.javaProject.getArtifactId(), kubernetesExtension.javaProject.getVersion()))
        .buildContext(jKubeServiceHub.getConfiguration())
        .follow(kubernetesExtension.getWatchFollowOrDefault())
        .showLogs(kubernetesExtension.getWatchShowLogsOrNull())
        .logOutputSpecFactory(logOutputSpecFactory)
        .hub(hub)
        .dispatcher(new LogDispatcher(hub.getDockerAccess()))
        .build();
  }
}
