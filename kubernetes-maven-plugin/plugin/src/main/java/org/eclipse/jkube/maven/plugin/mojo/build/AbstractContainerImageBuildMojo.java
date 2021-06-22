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
package org.eclipse.jkube.maven.plugin.mojo.build;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.build.api.helper.DockerFileUtil;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.PlexusContainerHelper;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.image.build.JKubeConfiguration;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImagePullManager;
import org.eclipse.jkube.kit.config.image.RegistryConfig;
import org.eclipse.jkube.kit.build.service.docker.ServiceHubFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpecFactory;
import org.eclipse.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.build.service.docker.helper.ConfigHelper;
import org.eclipse.jkube.kit.build.service.docker.config.DockerMachineConfiguration;
import org.eclipse.jkube.kit.config.image.WatchMode;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.build.service.docker.helper.ContainerNamingUtil;
import org.eclipse.jkube.kit.build.service.docker.helper.ImageNameFormatter;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.AnsiLogger;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.image.build.RegistryAuthConfiguration;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.maven.plugin.enricher.DefaultEnricherManager;
import org.eclipse.jkube.maven.plugin.generator.GeneratorManager;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.eclipse.jkube.maven.plugin.mojo.KitLoggerProvider;

import static org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory.DockerAccessContext.DEFAULT_MAX_CONNECTIONS;
import static org.eclipse.jkube.kit.build.service.docker.ImagePullManager.CacheStore.getSessionCacheStore;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestamp;

public abstract class AbstractContainerImageBuildMojo extends AbstractJKubeMojo
    implements ConfigHelper.Customizer, Contextualizable, KitLoggerProvider {

    public static final String DOCKER_EXTRA_DIR = "docker-extra";

    // Key holding the log dispatcher
    public static final String CONTEXT_KEY_LOG_DISPATCHER = "CONTEXT_KEY_DOCKER_LOG_DISPATCHER";

    // Key under which the build timestamp is stored so that other mojos can reuse it
    public static final String CONTEXT_KEY_BUILD_TIMESTAMP = "CONTEXT_KEY_BUILD_TIMESTAMP";

    // Filename for holding the build timestamp
    public static final String DOCKER_BUILD_TIMESTAMP = "docker/build.timestamp";

    // Handler for external configurations
    @Component
    public ImageConfigResolver imageConfigResolver;

    /**
     * Image configurations configured directly.
     */
    @Parameter
    public List<ImageConfiguration> images;

    @Parameter(property = "jkube.docker.apiVersion")
    protected String apiVersion;

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
    public ServiceHubFactory serviceHubFactory;

    @Component
    protected DockerAccessFactory dockerAccessFactory;

    @Parameter(property = "jkube.skip.build", defaultValue = "false")
    protected boolean skipBuild;

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

    protected String minimalApiVersion;

    protected PlexusContainer plexusContainer;

    /**
     * Watching mode for rebuilding images
     */
    @Parameter(property = "jkube.watch.mode", defaultValue = "both")
    protected WatchMode watchMode;

    @Parameter(property = "jkube.watch.interval", defaultValue = "5000")
    protected int watchInterval;

    @Parameter(property = "jkube.watch.keepRunning", defaultValue = "false")
    protected boolean keepRunning;

    @Parameter(property = "jkube.watch.postGoal")
    protected String watchPostGoal;

    @Parameter(property = "jkube.watch.postExec")
    protected String watchPostExec;

    // Whether to keep the containers afters stopping (start/watch/stop)
    @Parameter(property = "jkube.watch.keepContainer", defaultValue = "false")
    protected boolean keepContainer;

    // Whether to remove volumes when removing the container (start/watch/stop)
    @Parameter(property = "jkube.watch.removeVolumes", defaultValue = "false")
    protected boolean removeVolumes;

    @Parameter(property = "jkube.watch.follow", defaultValue = "false")
    protected boolean watchFollow;

    @Parameter(property = "jkube.watch.showLogs")
    protected String watchShowLogs;
    /**
     * Naming pattern for how to name containers when started
     */
    @Parameter(property = "jkube.watch.containerNamePattern")
    protected String containerNamePattern = ContainerNamingUtil.DEFAULT_CONTAINER_NAME_PATTERN;

    /**
     * Whether to create the customs networks (user-defined bridge networks) before starting automatically
     */
    @Parameter(property = "jkube.watch.autoCreateCustomNetworks", defaultValue = "false")
    protected boolean autoCreateCustomNetworks;

    @Override
    public void contextualize(Context context) throws ContextException {
        plexusContainer = ((PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY));
    }

    @Override
    public void init() throws DependencyResolutionRequiredException {
        super.init();
        authConfigFactory = new AuthConfigFactory(log);
        imageConfigResolver.setLog(log);
        this.minimalApiVersion = initImageConfiguration(getBuildTimestamp(getPluginContext(), CONTEXT_KEY_BUILD_TIMESTAMP, project.getBuild().getDirectory(), DOCKER_BUILD_TIMESTAMP));
    }

    @Override
    protected JKubeServiceHub.JKubeServiceHubBuilder initJKubeServiceHubBuilder(JavaProject javaProject) {
        LogOutputSpecFactory logSpecFactory = new LogOutputSpecFactory(useColor, logStdout, logDate);
        // The 'real' images configuration to use (configured images + externally resolved images)
        DockerAccess dockerAccess = null;
        if (isDockerAccessRequired()) {
            DockerAccessFactory.DockerAccessContext dockerAccessContext = DockerAccessFactory.DockerAccessContext.getDockerAccessContext(dockerHost,
                    certPath, machine, maxConnections, minimalApiVersion, project.getProperties(), skipMachine, log);
            dockerAccess = dockerAccessFactory.createDockerAccess(dockerAccessContext);
        }
        return super.initJKubeServiceHubBuilder(javaProject)
                .dockerServiceHub(serviceHubFactory.createServiceHub(dockerAccess, log, logSpecFactory))
                .buildServiceConfig(buildServiceConfigBuilder().build());
    }

    @Override
    public JKubeConfiguration initJKubeConfiguration(JavaProject javaProject) {
        ConfigHelper.validateExternalPropertyActivation(javaProject, images);
        return JKubeConfiguration.getJKubeConfiguration(javaProject, sourceDirectory, outputDirectory, buildArgs, getRegistryConfig(pullRegistry));
    }

    @Override
    protected KitLogger createLogger(String prefix) {
        return new AnsiLogger(getLog(), useColorForLogging(), verbose, !settings.getInteractiveMode(), getLogPrefix());
    }

    protected RegistryConfig getRegistryConfig(String specificRegistry) {
        PlexusContainerHelper plexusContainerHelper = new PlexusContainerHelper(plexusContainer);
        return RegistryConfig.getRegistryConfig(specificRegistry, MavenUtil.getRegistryServerFromMavenSettings(settings),
                authConfig != null ? authConfig.toMap() : null, skipExtendedAuth, registry,
                plexusContainerHelper::decryptString);
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
        return !getJKubeBuildStrategy().getLabel().equalsIgnoreCase("jib");// True in case of kubernetes maven plugin
    }

    protected BuildServiceConfig.BuildServiceConfigBuilder buildServiceConfigBuilder() {
        return BuildServiceConfig.getBuildServiceConfigBuilder(BuildRecreateMode.fromParameter(buildRecreate), getJKubeBuildStrategy(),
                forcePull, getImagePullManager(imagePullPolicy, autoPull), project.getBuild().getDirectory(),
                (classifier, destFile) -> attachArtifacts(project, projectHelper, ResourceFileType.yaml, classifier, destFile),
                builder -> enricherTask(builder, new DefaultEnricherManager(getEnricherContext(), MavenUtil.getCompileClasspathElementsIfRequested(project, useProjectClasspath))), resources, resourceDir);
    }

    /**
     * Customization hook called by the base plugin.
     *
     * @param configs configuration to customize
     * @return the configuration customized by our generators.
     */
    public List<ImageConfiguration> customizeConfig(List<ImageConfiguration> configs) {
        log.info("Running in [[B]]%s[[B]] mode", runtimeMode.getLabel());
        if (runtimeMode != RuntimeMode.OPENSHIFT) {
            log.info("Building Docker image in [[B]]Kubernetes[[B]] mode");
        }
        try {
            return GeneratorManager.generate(configs, generatorContextBuilder().build(), false);
        } catch (DependencyResolutionRequiredException de) {
            throw new IllegalArgumentException("Instructed to use project classpath, but cannot. Continuing build if we can: ", de);
        }
    }

    public JKubeBuildStrategy getJKubeBuildStrategy() {
        if (buildStrategy != null) {
            return buildStrategy;
        }
        return JKubeBuildStrategy.docker;
    }

    // ==================================================================================================

    // Get enricher context
    public EnricherContext getEnricherContext() throws DependencyResolutionRequiredException {
        return JKubeEnricherContext.getEnricherContext(MavenUtil.convertMavenProjectToJKubeProject(project, session),
                extractEnricherConfig(profile, ResourceUtil.getFinalResourceDir(resourceDir, environment), enricher),
                getResolvedImages(), resources, log).build();
    }

    // Get generator context
    protected GeneratorContext.GeneratorContextBuilder generatorContextBuilder() throws DependencyResolutionRequiredException {
        return GeneratorContext.generatorContextBuilder(extractGeneratorConfig(profile, ResourceUtil.getFinalResourceDir(resourceDir, environment), generator),
                MavenUtil.convertMavenProjectToJKubeProject(project, session),
                log, runtimeMode, useProjectClasspath, jkubeServiceHub.getArtifactResolverService(), null);
    }

    public ImagePullManager getImagePullManager(String imagePullPolicy, String autoPull) {
        return new ImagePullManager(getSessionCacheStore(session.getUserProperties()), imagePullPolicy, autoPull);
    }

    // Resolve and customize image configuration
    protected String initImageConfiguration(Date buildTimeStamp) throws DependencyResolutionRequiredException {
        // Resolve images
        JavaProject jkubeProject = MavenUtil.convertMavenProjectToJKubeProject(project, session);
        resolvedImages = ConfigHelper.resolveImages(
                log,
                images,                  // Unresolved images
                (ImageConfiguration image) -> imageConfigResolver.resolve(image, jkubeProject),
                filter,                   // A filter which image to process
                this);                     // customizer (can be overwritten by a subclass)

        // Check for simple Dockerfile mode
        DockerFileUtil.checkIfDockerfileModeAndImageConfigs(project.getBasedir(), resolvedImages, MavenUtil.getPropertiesWithSystemOverrides(project).getProperty("jkube.image.name"));

        // Initialize configuration and detect minimal API version
        return ConfigHelper.initAndValidate(resolvedImages, apiVersion, new ImageNameFormatter(jkubeProject, buildTimeStamp));
    }

}
