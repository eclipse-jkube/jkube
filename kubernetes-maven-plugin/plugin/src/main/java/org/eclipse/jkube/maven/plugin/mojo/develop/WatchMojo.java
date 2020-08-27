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
package org.eclipse.jkube.maven.plugin.mojo.develop;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorMode;
import org.eclipse.jkube.kit.config.image.build.JKubeConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.build.service.docker.WatchService;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.profile.ProfileUtil;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.maven.plugin.mojo.ManifestProvider;
import org.eclipse.jkube.maven.plugin.mojo.build.AbstractDockerMojo;
import org.eclipse.jkube.maven.plugin.watcher.WatcherManager;
import org.eclipse.jkube.watcher.api.WatcherContext;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static org.eclipse.jkube.maven.plugin.mojo.build.ApplyMojo.DEFAULT_KUBERNETES_MANIFEST;


// TODO: Similar to the DebugMojo the WatchMojo should scale down any deployment to 1 replica (or ensure that its running only with one replica)
// The WatchEnricher has been removed since the enrichment shouldn't know anything about the mode running and should
// always create the same resources

/**
 * Used to automatically rebuild Docker images and restart containers in case of updates.
 */
@Mojo(name = "watch", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(goal = "deploy")
public class WatchMojo extends AbstractDockerMojo implements ManifestProvider {

    /**
     * The generated kubernetes YAML file
     */
    @Parameter(property = "jkube.kubernetesManifest", defaultValue = DEFAULT_KUBERNETES_MANIFEST)
    private File kubernetesManifest;

    /**
     * Watcher specific options. This is a generic prefix where the keys have the form
     * <code>&lt;watcher-prefix&gt;-&lt;option&gt;</code>.
     */
    @Parameter
    private ProcessorConfig watcher;

    private KubernetesClient kubernetesClient;

    @Override
    public File getKubernetesManifest() {
        return kubernetesManifest;
    }

    @Override
    protected void init() {
        super.init();
        kubernetesClient = clusterAccess.createDefaultClient();
    }

    @Override
    public void executeInternal() throws MojoExecutionException {
        URL masterUrl = kubernetesClient.getMasterUrl();
        KubernetesResourceUtil.validateKubernetesMasterUrl(masterUrl);

        try {
            Set<HasMetadata> resources = KubernetesResourceUtil.loadResources(getManifest(kubernetesClient));
            WatcherContext context = getWatcherContext();

            WatcherManager.watch(getResolvedImages(), resources, context);

        } catch (KubernetesClientException ex) {
            KubernetesResourceUtil.handleKubernetesClientException(ex, this.log);
        } catch (Exception ex) {
            throw new MojoExecutionException("An error has occurred while while trying to watch the resources", ex);
        }
    }

    private WatcherContext getWatcherContext() throws MojoExecutionException {
        try {
            JKubeConfiguration buildContext = initJKubeConfiguration();
            WatchService.WatchContext watchContext = jkubeServiceHub.getDockerServiceHub() != null ? getWatchContext() : null;

            return WatcherContext.builder()
                    .buildContext(buildContext)
                    .watchContext(watchContext)
                    .config(extractWatcherConfig())
                    .logger(log)
                    .newPodLogger(createLogger("[[C]][NEW][[C]] "))
                    .oldPodLogger(createLogger("[[R]][OLD][[R]] "))
                    .useProjectClasspath(useProjectClasspath)
                    .jKubeServiceHub(jkubeServiceHub)
                    .build();
        } catch (IOException exception) {
            throw new MojoExecutionException(exception.getMessage());
        } catch (DependencyResolutionRequiredException dependencyException) {
            throw new MojoExecutionException("Instructed to use project classpath, but cannot. Continuing build if we can: " + dependencyException.getMessage());
        }
    }

    @Override
    protected GeneratorContext.GeneratorContextBuilder generatorContextBuilder() throws DependencyResolutionRequiredException {
        return GeneratorContext.builder()
            .config(extractGeneratorConfig())
            .project(MavenUtil.convertMavenProjectToJKubeProject(project, session))
            .logger(log)
            .runtimeMode(getConfiguredRuntimeMode())
            .useProjectClasspath(useProjectClasspath)
            .artifactResolver(jkubeServiceHub.getArtifactResolverService())
            .generatorMode(GeneratorMode.WATCH);
    }

    // Get watcher config
    private ProcessorConfig extractWatcherConfig() {
        try {
            return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.WATCHER_CONFIG, profile, ResourceUtil.getFinalResourceDir(resourceDir, environment), watcher);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot extract watcher config: " + e, e);
        }
    }

    protected KitLogger createLogger(String prefix) {
        return new AnsiLogger(getLog(), useColor, verbose, !settings.getInteractiveMode(), getLogPrefix() + prefix);
    }

    protected WatchService.WatchContext getWatchContext() throws IOException, DependencyResolutionRequiredException {
        final ServiceHub hub = jkubeServiceHub.getDockerServiceHub();
        return WatchService.WatchContext.builder()
                .watchInterval(watchInterval)
                .watchMode(watchMode)
                .watchPostGoal(watchPostGoal)
                .watchPostExec(watchPostExec)
                .autoCreateCustomNetworks(autoCreateCustomNetworks)
                .keepContainer(keepContainer)
                .keepRunning(keepRunning)
                .removeVolumes(removeVolumes)
                .containerNamePattern(containerNamePattern)
                .buildTimestamp(getBuildTimestamp())
                .gavLabel(getGavLabel())
                .buildContext(initJKubeConfiguration())
                .follow(watchFollow)
                .showLogs(watchShowLogs)
                .serviceHubFactory(serviceHubFactory)
                .hub(hub)
                .dispatcher(getLogDispatcher(hub))
                .build();
    }

}
