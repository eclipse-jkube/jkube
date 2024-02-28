/*
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
package org.eclipse.jkube.maven.plugin.mojo.build;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorManager;
import org.eclipse.jkube.kit.build.core.GavLabel;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImagePullManager;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.build.service.docker.DockerServiceHub;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.build.service.docker.helper.ConfigHelper;
import org.eclipse.jkube.kit.build.service.docker.config.DockerMachineConfiguration;
import org.eclipse.jkube.kit.config.image.WatchMode;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.image.build.RegistryAuthConfiguration;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.enricher.api.DefaultEnricherManager;
import org.eclipse.jkube.kit.profile.ProfileUtil;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.eclipse.jkube.maven.plugin.mojo.KitLoggerProvider;
import org.fusesource.jansi.Ansi;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import static org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory.DockerAccessContext.DEFAULT_MAX_CONNECTIONS;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestamp;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestampFile;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.updateResourceConfigNamespace;
import static org.eclipse.jkube.maven.plugin.mojo.build.AbstractJKubeMojo.DEFAULT_LOG_PREFIX;

public abstract class AbstractDockerMojo extends AbstractMojo
    implements ConfigHelper.Customizer, Contextualizable, KitLoggerProvider {

    public static final String CONTEXT_KEY_LOG_DISPATCHER = "CONTEXT_KEY_DOCKER_LOG_DISPATCHER";

    // Key under which the build timestamp is stored so that other mojos can reuse it
    public static final String CONTEXT_KEY_BUILD_TIMESTAMP = "CONTEXT_KEY_BUILD_TIMESTAMP";

    // Filename for holding the build timestamp
    public static final String DOCKER_BUILD_TIMESTAMP = "docker/build.timestamp";

    @Parameter
    protected ClusterConfiguration access;

    // Current maven project
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    protected MojoExecution mojoExecution;

    // For verbose output
    @Parameter(property = "jkube.docker.verbose", defaultValue = "false")
    protected String verbose;

    // The date format to use when putting out logs
    @Parameter(property = "jkube.docker.logDate")
    protected String logDate;

    // Log to stdout regardless if log files are configured or not
    @Parameter(property = "jkube.docker.logStdout", defaultValue = "false")
    protected boolean logStdout;

    /**
     * URL to docker daemon
     */
    @Parameter(property = "jkube.docker.host")
    protected String dockerHost;

    @Parameter(property = "jkube.docker.certPath")
    protected String certPath;

    // Docker-machine configuration
    @Parameter
    protected DockerMachineConfiguration machine;

    /**
     * Whether the usage of docker machine should be skipped completely
     */
    @Parameter(property = "jkube.docker.skip.machine", defaultValue = "false")
    protected boolean skipMachine;

    // maximum connection to use in parallel for connecting the docker host
    @Parameter(property = "jkube.docker.maxConnections", defaultValue = "" + DEFAULT_MAX_CONNECTIONS)
    protected int maxConnections;

    // Whether to use color
    @Parameter(property = "jkube.useColor", defaultValue = "true")
    protected boolean useColor;

    /**
     * Whether to restrict operation to a single image. This can be either
     * the image or an alias name. It can also be comma separated list.
     * This parameter has to be set via the command line s system property.
     */
    @Parameter(property = "jkube.image.filter")
    protected String filter;

    // Images resolved with external image resolvers and hooks for subclass to
    // mangle the image configurations.
    protected List<ImageConfiguration> resolvedImages;

    // Settings holding authentication info
    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    @Parameter(property = "jkube.build.source.dir", defaultValue="src/main/docker")
    protected String sourceDirectory;

    @Parameter(property = "jkube.build.target.dir", defaultValue="target/docker")
    protected String outputDirectory;

    @Parameter(property = "jkube.docker.autoPull")
    protected String autoPull;

    @Parameter(property = "jkube.docker.imagePullPolicy")
    protected String imagePullPolicy;

    @Parameter(property = "jkube.docker.pull.registry")
    protected String pullRegistry;

    /**
     * Build mode when build is performed.
     * Can be either "s2i" for an s2i binary build mode (in case of OpenShift) or
     * "docker" for a binary docker mode or
     * "jib" for binary jib build
     */
    @Parameter(property = "jkube.build.strategy")
    protected JKubeBuildStrategy buildStrategy;

    /**
     * Profile to use. A profile contains the enrichers and generators to
     * use as well as their configuration. Profiles are looked up
     * in the classpath and can be provided as yaml files.
     * <p>
     * However, any given enricher and or generator configuration overrides
     * the information provided by a profile.
     */
    @Parameter(property = "jkube.profile")
    protected String profile;

    // Handler for external configurations
    @Component
    protected ImageConfigResolver imageConfigResolver;

    /**
     * Skip extended authentication
     */
    @Parameter(property = "jkube.docker.skip.extendedAuth", defaultValue = "false")
    protected boolean skipExtendedAuth;

    @Parameter
    protected Map<String, String> buildArgs;

    // Authentication information
    @Parameter
    protected RegistryAuthConfiguration authConfig;

    // Default registry to use if no registry is specified
    @Parameter(property = "jkube.docker.registry")
    protected String registry;

    /**
     * How to recreate the build config and/or image stream created by the build.
     * Only in effect when <code>mode == openshift</code> or mode is <code>auto</code>
     * and openshift is detected. If not set, existing
     * build config will not be recreated.
     * <p>
     * The possible values are:
     *
     * <ul>
     * <li><strong>buildConfig</strong> or <strong>bc</strong> :
     * Only the build config is recreated</li>
     * <li><strong>imageStream</strong> or <strong>is</strong> :
     * Only the image stream is recreated</li>
     * <li><strong>all</strong> : Both, build config and image stream are recreated</li>
     * <li><strong>none</strong> : Neither build config nor image stream is recreated</li>
     * </ul>
     */
    @Parameter(property = "jkube.build.recreate", defaultValue = "none")
    protected String buildRecreate;

    @Component
    protected MavenProjectHelper projectHelper;

    @Component
    protected DockerAccessFactory dockerAccessFactory;

    /**
     * Image configurations configured directly.
     */
    @Parameter
    protected List<ImageConfiguration> images;

    @Parameter(property = "jkube.skip.build", defaultValue = "false")
    protected boolean skipBuild;

    // To skip over the execution of the goal
    @Parameter(property = "jkube.skip", defaultValue = "false")
    protected boolean skip;

    @Parameter(property = "jkube.skip.build.pom")
    protected Boolean skipBuildPom;

    /**
     * Generator specific options. This is a generic prefix where the keys have the form
     * <code>&lt;generator-prefix&gt;-&lt;option&gt;</code>.
     */
    @Parameter
    protected ProcessorConfig generator;

    /**
     * While creating a BuildConfig, By default, if the builder image specified in the
     * build configuration is available locally on the node, that image will be used.
     * <p>
     * ForcePull to override the local image and refresh it from the registry to which the image stream points.
     */
    @Parameter(property = "jkube.build.forcePull", defaultValue = "false")
    protected boolean forcePull = false;

    /**
     * Should we use the project's compile-time classpath to scan for additional enrichers/generators?
     */
    @Parameter(property = "jkube.useProjectClasspath", defaultValue = "false")
    protected boolean useProjectClasspath = false;

    /**
     * Resource config for getting annotation and labels to be applied to enriched build objects
     */
    @Parameter
    protected ResourceConfig resources;

    /**
     * Enrichers used for enricher build objects
     */
    @Parameter
    protected ProcessorConfig enricher;

    /**
     * Folder where to find project specific files, e.g a custom profile
     */
    @Parameter(property = "jkube.resourceDir", defaultValue = "${basedir}/src/main/jkube")
    protected File resourceDir;

    /**
     * Environment name where resources are placed. For example, if you set this property to dev and resourceDir is the default one, Plugin will look at src/main/jkube/dev
     */
    @Parameter(property = "jkube.environment")
    protected String environment;

    // Handler dealing with authentication credentials
    protected AuthConfigFactory authConfigFactory;

    protected KitLogger log;

    // Access for creating OpenShift binary builds
    protected ClusterAccess clusterAccess;

    // The JKube service hub
    protected JKubeServiceHub jkubeServiceHub;

    // Mode which is resolved, also when 'auto' is set
    protected RuntimeMode runtimeMode;

    protected PlexusContainer plexusContainer;

    /**
     * Watching mode for rebuilding images
     */
    @Parameter(property = "jkube.watch.mode", defaultValue = "both")
    protected WatchMode watchMode;

    @Parameter(property = "jkube.watch.interval", defaultValue = "5000")
    protected int watchInterval;

    @Parameter(property = "jkube.watch.postGoal")
    protected String watchPostGoal;

    @Parameter(property = "jkube.watch.postExec")
    protected String watchPostExec;

    /**
     * Namespace to use when accessing Kubernetes or OpenShift
     */
    @Parameter(property = "jkube.namespace")
    protected String namespace;

    @Parameter(property = "jkube.offline", defaultValue = "false")
    protected boolean offline;

    protected JavaProject javaProject;

    @Override
    public void contextualize(Context context) throws ContextException {
        plexusContainer = ((PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY));
    }

    @Override
    public KitLogger getKitLogger() {
        return log;
    }

    public RuntimeMode getConfiguredRuntimeMode() {
        return RuntimeMode.KUBERNETES;
    }

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        init();
        if (shouldSkip()) {
            log.info("`%s` goal is skipped.", mojoExecution.getMojoDescriptor().getFullGoalName());
            return;
        }
        doExecute();
    }

    protected void init() {
        log = new AnsiLogger(getLog(), useColorForLogging(), verbose, !settings.getInteractiveMode(), getLogPrefix());
        authConfigFactory = new AuthConfigFactory(log);
        imageConfigResolver.setLog(log);
        clusterAccess = new ClusterAccess(initClusterConfiguration());
        runtimeMode = getConfiguredRuntimeMode();
    }

    protected boolean shouldSkip() {
        return skip;
    }

    protected void doExecute() throws MojoExecutionException {
        final boolean ansiRestore = Ansi.isEnabled();
        try {
            DockerAccess dockerAccess = null;
            try {
                javaProject = MavenUtil.convertMavenProjectToJKubeProject(project, session);
                resources = updateResourceConfigNamespace(namespace, resources);
                // The 'real' images configuration to use (configured images + externally resolved images)
                if (isDockerAccessRequired()) {
                    DockerAccessFactory.DockerAccessContext dockerAccessContext = getDockerAccessContext();
                    dockerAccess = dockerAccessFactory.createDockerAccess(dockerAccessContext);
                }
                jkubeServiceHub = JKubeServiceHub.builder()
                    .log(log)
                    .configuration(initJKubeConfiguration())
                    .clusterAccess(clusterAccess)
                    .platformMode(getConfiguredRuntimeMode())
                    .dockerServiceHub(DockerServiceHub.newInstance(log, dockerAccess))
                    .buildServiceConfig(buildServiceConfigBuilder().build())
                    .offline(offline)
                    .build();
                resolvedImages = ConfigHelper.initImageConfiguration(getBuildTimestamp(getPluginContext(), CONTEXT_KEY_BUILD_TIMESTAMP, project.getBuild().getDirectory(), DOCKER_BUILD_TIMESTAMP), images, imageConfigResolver, log, filter, this, jkubeServiceHub.getConfiguration());
                executeInternal();
            } catch (IOException | DependencyResolutionRequiredException exp) {
                logException(exp);
                throw new MojoExecutionException(exp.getMessage());
            } catch (MojoExecutionException exp) {
                logException(exp);
                throw exp;
            } finally {
                Optional.ofNullable(jkubeServiceHub).ifPresent(JKubeServiceHub::close);
            }
        } finally {
            Ansi.setEnabled(ansiRestore);
        }
    }

    /**
     * Hook for subclass for doing the real job
     */
    protected abstract void executeInternal() throws IOException, MojoExecutionException;

    protected JKubeConfiguration initJKubeConfiguration() throws DependencyResolutionRequiredException {
        ConfigHelper.validateExternalPropertyActivation(javaProject, images);
        return JKubeConfiguration.builder()
            .project(MavenUtil.convertMavenProjectToJKubeProject(project, session))
            .sourceDirectory(sourceDirectory)
            .outputDirectory(outputDirectory)
            .reactorProjects(Collections.singletonList(javaProject))
            .buildArgs(buildArgs)
            .registryConfig(getRegistryConfig(pullRegistry))
            .build();
    }


    protected RegistryConfig getRegistryConfig(String specificRegistry) {
        return RegistryConfig.builder()
                .settings(MavenUtil.getRegistryServerFromMavenSettings(settings))
                .authConfig(authConfig != null ? authConfig.toMap() : null)
                .skipExtendedAuth(skipExtendedAuth)
                .registry(specificRegistry != null ? specificRegistry : registry)
                .passwordDecryptionMethod(password -> {
                    try {
                        // Done by reflection since I have classloader issues otherwise
                        if (plexusContainer != null) {
                            Object secDispatcher = plexusContainer.lookup(SecDispatcher.ROLE, "maven");
                            Method method = secDispatcher.getClass().getMethod("decrypt", String.class);
                            return (String) method.invoke(secDispatcher, password);
                        } else {
                            return password;
                        }
                    } catch (ComponentLookupException e) {
                        throw new RuntimeException("Error looking security dispatcher",e);
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException("Cannot decrypt password: " + e.getCause(),e);
                    }
                }).build();
    }


    protected void logException(Exception exp) {
        if (exp.getCause() != null) {
            log.error("%s [%s]", exp.getMessage(), exp.getCause().getMessage());
        } else {
            log.error("%s", exp.getMessage());
        }
    }

    protected DockerAccessFactory.DockerAccessContext getDockerAccessContext() {
        return DockerAccessFactory.DockerAccessContext.builder()
                .dockerHost(dockerHost)
                .certPath(certPath)
                .machine(machine)
                .maxConnections(maxConnections)
                .projectProperties(project.getProperties())
                .skipMachine(skipMachine)
                .log(log)
                .build();
    }

    /**
     * Get all images to use. Can be restricted via -Djkube.image.filter to pick a one or more images.
     * The values are taken as comma separated list.
     *
     * @return list of image configuration to be use. Can be empty but never null.
     */
    protected List<ImageConfiguration> getResolvedImages() {
        return resolvedImages;
    }

    protected boolean isDockerAccessRequired() {
        return !getJKubeBuildStrategy().equals(JKubeBuildStrategy.jib);// True in case of kubernetes maven plugin
    }

    void executeBuildGoal() throws MojoExecutionException {
        if (skipBuild) {
            return;
        }
        buildAndTag(getResolvedImages());
    }

    protected boolean shouldSkipBecauseOfPomPackaging() {
        if (!project.getPackaging().equals("pom")) {
            // No pom packaging
            return false;
        }
        if (skipBuildPom != null) {
            // If configured take the config option
            return skipBuildPom;
        }

        // Not specified: Skip if no image with build configured, otherwise don't skip
        for (ImageConfiguration image : getResolvedImages()) {
            if (image.getBuildConfiguration() != null) {
                return false;
            }
        }
        return true;
    }

    private void buildAndTag(List<ImageConfiguration> imageConfigs)
            throws MojoExecutionException {

        try {
            // TODO need to refactor d-m-p to avoid this call
            EnvUtil.storeTimestamp(getBuildTimestampFile(project.getBuild().getDirectory(), DOCKER_BUILD_TIMESTAMP),
                    getBuildTimestamp(getPluginContext(), CONTEXT_KEY_BUILD_TIMESTAMP, project.getBuild().getDirectory(), DOCKER_BUILD_TIMESTAMP));

            jkubeServiceHub.getBuildService().build(imageConfigs.toArray(new ImageConfiguration[0]));

        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to execute the build", ex);
        }
    }

    protected BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder() {
        return BuildServiceConfig.builder()
                .buildRecreateMode(BuildRecreateMode.fromParameter(buildRecreate))
                .jKubeBuildStrategy(getJKubeBuildStrategy())
                .forcePull(forcePull)
                .imagePullManager(ImagePullManager.createImagePullManager(imagePullPolicy, autoPull, project.getProperties()))
                .buildDirectory(project.getBuild().getDirectory())
                .resourceConfig(resources)
                .resourceDir(resourceDir)
                .attacher((classifier, destFile) -> {
                    if (destFile.exists()) {
                        projectHelper.attachArtifact(project, "yml", classifier, destFile);
                    }
                })
                .enricherTask(builder -> {
              DefaultEnricherManager enricherManager = new DefaultEnricherManager(getEnricherContext(),
                useProjectClasspath ? javaProject.getCompileClassPathElements() : Collections.emptyList());
                    enricherManager.enrich(PlatformMode.kubernetes, builder);
                    enricherManager.enrich(PlatformMode.openshift, builder);
                });
    }

    /**
     * Customization hook called by the base plugin.
     *
     * @param configs configuration to customize
     * @return the configuration customized by our generators.
     */
    public List<ImageConfiguration> customizeConfig(List<ImageConfiguration> configs) {
        log.info("Building Docker image");
        try {
            return GeneratorManager.generate(configs, generatorContextBuilder().build(), false);
        } catch (DependencyResolutionRequiredException de) {
            throw new IllegalArgumentException("Instructed to use project classpath, but cannot. Continuing build if we can: ", de);
        }
    }

    protected String getLogPrefix() {
        return DEFAULT_LOG_PREFIX;
    }

    protected JKubeBuildStrategy getJKubeBuildStrategy() {
        if (buildStrategy != null) {
            return buildStrategy;
        }
        return JKubeBuildStrategy.docker;
    }

    // ==================================================================================================

    // Get enricher context
    public EnricherContext getEnricherContext() throws DependencyResolutionRequiredException {
        return JKubeEnricherContext.builder()
                .project(MavenUtil.convertMavenProjectToJKubeProject(project, session))
                .processorConfig(extractEnricherConfig())
                .images(getResolvedImages())
                .resources(resources)
                .log(log)
                .build();
    }

    // Get generator context
    protected GeneratorContext.GeneratorContextBuilder generatorContextBuilder() throws DependencyResolutionRequiredException {
        return GeneratorContext.builder()
                .config(extractGeneratorConfig())
                .project(MavenUtil.convertMavenProjectToJKubeProject(project, session))
                .logger(log)
                .runtimeMode(runtimeMode)
                .useProjectClasspath(useProjectClasspath);
    }

    // Get generator config
    protected ProcessorConfig extractGeneratorConfig() {
        try {
            return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.GENERATOR_CONFIG, profile, ResourceUtil.getFinalResourceDirs(resourceDir, environment), generator);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot extract generator config: " + e, e);
        }
    }

    // Get enricher config
    protected ProcessorConfig extractEnricherConfig() {
        try {
            return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.ENRICHER_CONFIG, profile, ResourceUtil.getFinalResourceDirs(resourceDir, environment), enricher);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot extract enricher config: " + e, e);
        }
    }

    /**
     * Determine whether to enable colorized log messages
     * @return true if log statements should be colorized
     */
    protected boolean useColorForLogging() {
        return useColor && MessageUtils.isColorEnabled()
                && !(EnvUtil.isWindows() && !MavenUtil.isMaven350OrLater(session));
    }

    protected ClusterConfiguration initClusterConfiguration() {
        return ClusterConfiguration.from(access, System.getProperties(), project.getProperties()).build();
    }

    protected GavLabel getGavLabel() {
        // Label used for this run
        return new GavLabel(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }

}
