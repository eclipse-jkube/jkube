/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.maven.plugin.mojo.develop;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.jshift.generator.api.GeneratorContext;
import io.jshift.generator.api.GeneratorMode;
import io.jshift.kit.build.service.docker.BuildService;
import io.jshift.kit.build.service.docker.ImageConfiguration;
import io.jshift.kit.build.service.docker.ServiceHub;
import io.jshift.kit.build.service.docker.WatchService;
import io.jshift.kit.build.service.docker.auth.AuthConfigFactory;
import io.jshift.kit.build.service.docker.helper.AnsiLogger;
import io.jshift.kit.common.KitLogger;
import io.jshift.kit.common.util.OpenshiftHelper;
import io.jshift.kit.common.util.ResourceUtil;
import io.jshift.kit.config.access.ClusterAccess;
import io.jshift.kit.config.access.ClusterConfiguration;
import io.jshift.kit.config.image.build.OpenShiftBuildStrategy;
import io.jshift.kit.config.resource.ProcessorConfig;
import io.jshift.kit.config.resource.RuntimeMode;
import io.jshift.kit.config.service.JshiftServiceHub;
import io.jshift.kit.profile.ProfileUtil;
import io.jshift.maven.enricher.api.util.KubernetesResourceUtil;
import io.jshift.maven.plugin.generator.GeneratorManager;
import io.jshift.maven.plugin.mojo.build.AbstractDockerMojo;
import io.jshift.maven.plugin.watcher.WatcherManager;
import io.jshift.watcher.api.WatcherContext;
import org.apache.maven.plugin.AbstractMojo;
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

import static io.jshift.maven.plugin.mojo.build.ApplyMojo.DEFAULT_KUBERNETES_MANIFEST;
import static io.jshift.maven.plugin.mojo.build.ApplyMojo.DEFAULT_OPENSHIFT_MANIFEST;


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
    @Parameter(property = "jshift.skip", defaultValue = "false")
    protected boolean skip;
    /**
     * The generated kubernetes YAML file
     */
    @Parameter(property = "jshift.kubernetesManifest", defaultValue = DEFAULT_KUBERNETES_MANIFEST)
    private File kubernetesManifest;
    /**
     * The generated openshift YAML file
     */
    @Parameter(property = "jshift.openshiftManifest", defaultValue = DEFAULT_OPENSHIFT_MANIFEST)
    private File openshiftManifest;
    /**
     * Whether to perform a Kubernetes build (i.e. against a vanilla Docker daemon) or
     * an OpenShift build (with a Docker build against the OpenShift API server.
     */
    @Parameter(property = "jshift.mode")
    private RuntimeMode mode = RuntimeMode.auto;
    /**
     * OpenShift build mode when an OpenShift build is performed.
     * Can be either "s2i" for an s2i binary build mode or "docker" for a binary
     * docker mode.
     */
    @Parameter(property = "jshift.build.strategy")
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
    @Parameter(property = "jshift.useProjectClasspath", defaultValue = "false")
    private boolean useProjectClasspath = false;

    /**
     * Profile to use. A profile contains the enrichers and generators to
     * use as well as their configuration. Profiles are looked up
     * in the classpath and can be provided as yaml files.
     *
     * However, any given enricher and or generator configuration overrides
     * the information provided by a profile.
     */
    @Parameter(property = "jshift.profile")
    private String profile;

    /**
     * Folder where to find project specific files, e.g a custom profile
     */
    @Parameter(property = "jshift.resourceDir", defaultValue = "${basedir}/src/main/jshift")
    private File resourceDir;

    /**
     * Environment name where resources are placed. For example, if you set this property to dev and resourceDir is the default one, Plugin will look at src/main/jshift/dev
     */
    @Parameter(property = "jshift.environment")
    private String environment;

    // Whether to use color
    @Parameter(property = "jshift.useColor", defaultValue = "true")
    protected boolean useColor;

    // For verbose output
    @Parameter(property = "jshift.verbose", defaultValue = "false")
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
                    .project(project)
                    .useProjectClasspath(useProjectClasspath)
                    .clusterConfiguration(getClusterConfiguration())
                    .kubernetesClient(kubernetes)
                    .fabric8ServiceHub(getJshiftServiceHub())
                    .build();
        } catch(IOException exception) {
            throw new MojoExecutionException(exception.getMessage());
        }
    }

    protected JshiftServiceHub getJshiftServiceHub() {
        return new JshiftServiceHub.Builder()
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
            JshiftServiceHub serviceHub = getJshiftServiceHub();
            GeneratorContext ctx = new GeneratorContext.Builder()
                    .config(extractGeneratorConfig())
                    .project(project)
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
