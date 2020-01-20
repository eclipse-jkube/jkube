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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorMode;
import org.eclipse.jkube.kit.build.service.docker.BuildService;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.build.service.docker.WatchService;
import org.eclipse.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.OpenshiftHelper;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JkubeServiceHub;
import org.eclipse.jkube.kit.profile.ProfileUtil;
import org.eclipse.jkube.maven.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.maven.plugin.generator.GeneratorManager;
import org.eclipse.jkube.maven.plugin.mojo.build.AbstractDockerMojo;
import org.eclipse.jkube.maven.plugin.watcher.WatcherManager;
import org.eclipse.jkube.watcher.api.WatcherContext;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.eclipse.jkube.maven.plugin.mojo.build.ApplyMojo.DEFAULT_KUBERNETES_MANIFEST;
import static org.eclipse.jkube.maven.plugin.mojo.build.ApplyMojo.DEFAULT_OPENSHIFT_MANIFEST;


// TODO: Similar to the DebugMojo the WatchMojo should scale down any deployment to 1 replica (or ensure that its running only with one replica)
// The WatchEnricher has been removed since the enrichment shouldn't know anything about the mode running and should
// always create the same resources

/**
 * Used to automatically rebuild Docker images and restart containers in case of updates.
 */
@Mojo(name = "watch", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(goal = "deploy")
public class WatchMojo extends AbstractDockerMojo {

    @Parameter
    ProcessorConfig generator;

    /**
     * To skip over the execution of the goal
     */
    @Parameter(property = "jkube.skip", defaultValue = "false")
    protected boolean skip;
    /**
     * The generated kubernetes YAML file
     */
    @Parameter(property = "jkube.kubernetesManifest", defaultValue = DEFAULT_KUBERNETES_MANIFEST)
    private File kubernetesManifest;
    /**
     * The generated openshift YAML file
     */
    @Parameter(property = "jkube.openshiftManifest", defaultValue = DEFAULT_OPENSHIFT_MANIFEST)
    private File openshiftManifest;
    /**
     * Whether to perform a Kubernetes build (i.e. against a vanilla Docker daemon) or
     * an OpenShift build (with a Docker build against the OpenShift API server.
     */
    @Parameter(property = "jkube.mode")
    private RuntimeMode mode = RuntimeMode.auto;
    /**
     * OpenShift build mode when an OpenShift build is performed.
     * Can be either "s2i" for an s2i binary build mode or "docker" for a binary
     * docker mode.
     */
    @Parameter(property = "jkube.build.strategy")
    private OpenShiftBuildStrategy buildStrategy = OpenShiftBuildStrategy.s2i;

    @Parameter
    protected ClusterConfiguration access;

    /**
     * Watcher specific options. This is a generic prefix where the keys have the form
     * <code>&lt;watcher-prefix&gt;-&lt;option&gt;</code>.
     */
    @Parameter
    private ProcessorConfig watcher;

    /**
     * Should we use the project's compile-time classpath to scan for additional enrichers/generators?
     */
    @Parameter(property = "jkube.useProjectClasspath", defaultValue = "false")
    private boolean useProjectClasspath = false;

    /**
     * Profile to use. A profile contains the enrichers and generators to
     * use as well as their configuration. Profiles are looked up
     * in the classpath and can be provided as yaml files.
     *
     * However, any given enricher and or generator configuration overrides
     * the information provided by a profile.
     */
    @Parameter(property = "jkube.profile")
    private String profile;

    /**
     * Folder where to find project specific files, e.g a custom profile
     */
    @Parameter(property = "jkube.resourceDir", defaultValue = "${basedir}/src/main/jkube")
    private File resourceDir;

    /**
     * Environment name where resources are placed. For example, if you set this property to dev and resourceDir is the default one, Plugin will look at src/main/jkube/dev
     */
    @Parameter(property = "jkube.environment")
    private String environment;

    // Whether to use color
    @Parameter(property = "jkube.useColor", defaultValue = "true")
    protected boolean useColor;

    // For verbose output
    @Parameter(property = "jkube.verbose", defaultValue = "false")
    protected String verbose;

    @Component
    protected RepositorySystem repositorySystem;

    private ClusterAccess clusterAccess;
    private KubernetesClient kubernetes;
    private ServiceHub hub;

    @Override
    public void contextualize(Context context) throws ContextException {
        authConfigFactory = new AuthConfigFactory((PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY));
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            return;
        }

        log = new AnsiLogger(getLog(), useColor, verbose, !settings.getInteractiveMode(), getLogPrefix());
        clusterAccess = new ClusterAccess(getClusterConfiguration());
        kubernetes = clusterAccess.createDefaultClient(log);

        if(clusterAccess.resolveRuntimeMode(mode, log).equals(RuntimeMode.kubernetes)) {
            super.execute();
        } else {
            executeInternal(null);
        }
    }

    protected ClusterConfiguration getClusterConfiguration() {
        if(access == null) {
            access = new ClusterConfiguration.Builder().build();
        }
        final ClusterConfiguration.Builder clusterConfigurationBuilder = new ClusterConfiguration.Builder(access);

        return clusterConfigurationBuilder.from(System.getProperties())
            .from(project.getProperties()).build();
    }

    @Override
    protected synchronized void executeInternal(ServiceHub hub) throws MojoExecutionException {
        if(hub != null) {
            this.hub = hub;
        }

        URL masterUrl = kubernetes.getMasterUrl();
        KubernetesResourceUtil.validateKubernetesMasterUrl(masterUrl);

        File manifest;
        boolean isOpenshift = OpenshiftHelper.isOpenShift(kubernetes);
        if (isOpenshift) {
            manifest = openshiftManifest;
        } else {
            manifest = kubernetesManifest;
        }

        try {
            Set<HasMetadata> resources = KubernetesResourceUtil.loadResources(manifest);
            WatcherContext context = getWatcherContext();

            WatcherManager.watch(getResolvedImages(), resources, context);

        } catch (KubernetesClientException ex) {
            KubernetesResourceUtil.handleKubernetesClientException(ex, this.log);
        } catch (Exception ex) {
            throw new MojoExecutionException("An error has occurred while while trying to watch the resources", ex);
        }

    }

    public WatcherContext getWatcherContext() throws MojoExecutionException {
        try {
            BuildService.BuildContext buildContext = getBuildContext();
            WatchService.WatchContext watchContext = hub != null ? getWatchContext(hub) : null;

            return new WatcherContext.Builder()
                    .serviceHub(hub)
                    .buildContext(buildContext)
                    .watchContext(watchContext)
                    .config(extractWatcherConfig())
                    .logger(log)
                    .newPodLogger(createLogger("[[C]][NEW][[C]] "))
                    .oldPodLogger(createLogger("[[R]][OLD][[R]] "))
                    .mode(mode)
                    .project(MavenUtil.convertMavenProjectToJkubeProject(project))
                    .useProjectClasspath(useProjectClasspath)
                    .clusterConfiguration(getClusterConfiguration())
                    .kubernetesClient(kubernetes)
                    .fabric8ServiceHub(getJkubeServiceHub())
                    .build();
        } catch(IOException | DependencyResolutionRequiredException exception) {
            throw new MojoExecutionException(exception.getMessage());
        }
    }

    protected JkubeServiceHub getJkubeServiceHub() {
        return new JkubeServiceHub.Builder()
                .log(log)
                .clusterAccess(clusterAccess)
                .dockerServiceHub(hub)
                .platformMode(mode)
                .repositorySystem(repositorySystem)
                .mavenProject(project)
                .build();
    }

    @Override
    public List<ImageConfiguration> customizeConfig(List<ImageConfiguration> configs) {
        try {
            JkubeServiceHub serviceHub = getJkubeServiceHub();
            GeneratorContext ctx = new GeneratorContext.Builder()
                    .config(extractGeneratorConfig())
                    .project(MavenUtil.convertMavenProjectToJkubeProject(project))
                    .logger(log)
                    .runtimeMode(mode)
                    .strategy(buildStrategy)
                    .useProjectClasspath(useProjectClasspath)
                    .artifactResolver(serviceHub.getArtifactResolverService())
                    .generatorMode(GeneratorMode.WATCH)
                    .build();
            return GeneratorManager.generate(configs, ctx, false);
        } catch (MojoExecutionException e) {
            throw new IllegalArgumentException("Cannot extract generator config: " + e, e);
        } catch (DependencyResolutionRequiredException de) {
            throw new IllegalArgumentException("Instructed to use project classpath, but cannot. Continuing build if we can: ", de);
        }
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
        return new AnsiLogger(getLog(), useColor, verbose, !settings.getInteractiveMode(), "k8s:" + prefix);
    }

    @Override
    protected String getLogPrefix() {
        return "k8s: ";
    }

    protected WatchService.WatchContext getWatchContext(ServiceHub hub) throws IOException {
        return new WatchService.WatchContext.Builder()
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
                .pomLabel(getGavLabel())
                .mojoParameters(createMojoParameters())
                .follow(follow())
                .showLogs(showLogs())
                .serviceHubFactory(serviceHubFactory)
                .hub(hub)
                .dispatcher(getLogDispatcher(hub))
                .build();
    }

}
