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

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.build.maven.GavLabel;
import org.eclipse.jkube.kit.build.maven.MavenBuildContext;
import org.eclipse.jkube.kit.build.service.docker.BuildService;
import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImagePullManager;
import org.eclipse.jkube.kit.build.service.docker.RegistryService;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.build.service.docker.ServiceHubFactory;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogDispatcher;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpecFactory;
import org.eclipse.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.build.service.docker.config.ConfigHelper;
import org.eclipse.jkube.kit.build.service.docker.config.DockerMachineConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.WatchMode;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.build.service.docker.helper.AnsiLogger;
import org.eclipse.jkube.kit.build.service.docker.helper.ContainerNamingUtil;
import org.eclipse.jkube.kit.build.service.docker.helper.ImageNameFormatter;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.image.build.RegistryAuthConfiguration;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JkubeServiceHub;
import org.eclipse.jkube.kit.profile.ProfileUtil;
import org.eclipse.jkube.maven.enricher.api.EnricherContext;
import org.eclipse.jkube.maven.enricher.api.MavenEnricherContext;
import org.eclipse.jkube.maven.plugin.generator.GeneratorManager;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenReaderFilter;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.fusesource.jansi.Ansi;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractDockerMojo extends AbstractMojo implements ConfigHelper.Customizer, Contextualizable {
    public static final String DMP_PLUGIN_DESCRIPTOR = "META-INF/maven/org.eclipse.jkube/k8s-plugin";
    public static final String DOCKER_EXTRA_DIR = "docker-extra";

    // Key for indicating that a "start" goal has run
    public static final String CONTEXT_KEY_START_CALLED = "CONTEXT_KEY_DOCKER_START_CALLED";

    // Key holding the log dispatcher
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

    @Parameter(property = "docker.apiVersion")
    protected String apiVersion;

    // For verbose output
    @Parameter(property = "docker.verbose", defaultValue = "false")
    protected String verbose;

    // The date format to use when putting out logs
    @Parameter(property = "docker.logDate")
    protected String logDate;

    // Log to stdout regardless if log files are configured or not
    @Parameter(property = "docker.logStdout", defaultValue = "false")
    protected boolean logStdout;

    /**
     * URL to docker daemon
     */
    @Parameter(property = "docker.host")
    protected String dockerHost;

    @Parameter(property = "docker.certPath")
    protected String certPath;

    // Docker-machine configuration
    @Parameter
    protected DockerMachineConfiguration machine;

    /**
     * Whether the usage of docker machine should be skipped competely
     */
    @Parameter(property = "docker.skip.machine", defaultValue = "false")
    protected boolean skipMachine;

    // maximum connection to use in parallel for connecting the docker host
    @Parameter(property = "docker.maxConnections", defaultValue = "100")
    protected int maxConnections;

    // Whether to use color
    @Parameter(property = "docker.useColor", defaultValue = "true")
    protected boolean useColor;

    /**
     * Whether to restrict operation to a single image. This can be either
     * the image or an alias name. It can also be comma separated list.
     * This parameter has to be set via the command line s system property.
     */
    @Parameter(property = "docker.filter")
    protected String filter;

    // Images resolved with external image resolvers and hooks for subclass to
    // mangle the image configurations.
    protected List<ImageConfiguration> resolvedImages;

    // Settings holding authentication info
    @Parameter(defaultValue = "${settings}", readonly = true)
    protected Settings settings;

    @Parameter(property = "docker.source.dir", defaultValue="src/main/docker")
    protected String sourceDirectory;

    @Parameter(property = "docker.target.dir", defaultValue="target/docker")
    protected String outputDirectory;

    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    protected List<MavenProject> reactorProjects;

    @Parameter(property = "docker.autoPull")
    protected String autoPull;

    @Parameter(property = "docker.imagePullPolicy")
    protected String imagePullPolicy;

    @Parameter(property = "docker.pull.registry")
    protected String pullRegistry;

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
    @Parameter(property = "docker.skip.extendedAuth", defaultValue = "false")
    protected boolean skipExtendedAuth;

    @Parameter
    protected MavenArchiveConfiguration archive;

    @Component
    protected MavenFileFilter mavenFileFilter;

    @Component
    protected MavenReaderFilter mavenFilterReader;

    @Parameter
    protected Map<String, String> buildArgs;

    // Authentication information
    @Parameter
    protected RegistryAuthConfiguration authConfig;

    // Default registry to use if no registry is specified
    @Parameter(property = "docker.registry")
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
    protected RepositorySystem repositorySystem;

    @Component
    protected ServiceHubFactory serviceHubFactory;

    @Component
    protected DockerAccessFactory dockerAccessFactory;

    /**
     * Image configurations configured directly.
     */
    @Parameter
    protected List<ImageConfiguration> images;

    @Parameter(property = "docker.skip.build", defaultValue = "false")
    protected boolean skipBuild;

    /**
     * OpenShift build mode when an OpenShift build is performed.
     * Can be either "s2i" for an s2i binary build mode or "docker" for a binary
     * docker mode.
     */
    @Parameter(property = "jkube.build.strategy")
    protected OpenShiftBuildStrategy buildStrategy = OpenShiftBuildStrategy.s2i;

    /**
     * The name of pullSecret to be used to pull the base image in case pulling from a protected
     * registry which requires authentication.
     */
    @Parameter(property = "jkube.build.pullSecret", defaultValue = "pullsecret-jkube")
    protected String openshiftPullSecret;

    // To skip over the execution of the goal
    @Parameter(property = "jkube.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * Whether to perform a Kubernetes build (i.e. against a vanilla Docker daemon) or
     * an OpenShift build (with a Docker build against the OpenShift API server.
     */
    @Parameter(property = "jkube.mode")
    protected RuntimeMode mode = RuntimeMode.DEFAULT;

    @Parameter(property = "jkube.skip.build.pom")
    protected Boolean skipBuildPom;

    /**
     * The S2I binary builder BuildConfig name suffix appended to the image name to avoid
     * clashing with the underlying BuildConfig for the Jenkins pipeline
     */
    @Parameter(property = "jkube.s2i.buildNameSuffix", defaultValue = "-s2i")
    protected String s2iBuildNameSuffix;

    /**
     * Generator specific options. This is a generic prefix where the keys have the form
     * <code>&lt;generator-prefix&gt;-&lt;option&gt;</code>.
     */
    @Parameter
    protected ProcessorConfig generator;

    /**
     * Allow the ImageStream used in the S2I binary build to be used in standard
     * Kubernetes resources such as Deployment or StatefulSet.
     */
    @Parameter(property = "jkube.s2i.imageStreamLookupPolicyLocal", defaultValue = "true")
    protected boolean s2iImageStreamLookupPolicyLocal = true;

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

    protected String minimalApiVersion;

    // Access for creating OpenShift binary builds
    protected ClusterAccess clusterAccess;

    // The Jkube service hub
    protected JkubeServiceHub jkubeServiceHub;

    // Mode which is resolved, also when 'auto' is set
    protected RuntimeMode runtimeMode;

    /**
     * Watching mode for rebuilding images
     */
    @Parameter(property = "docker.watchMode", defaultValue = "both")
    protected WatchMode watchMode;

    @Parameter(property = "docker.watchInterval", defaultValue = "5000")
    protected int watchInterval;

    @Parameter(property = "docker.keepRunning", defaultValue = "false")
    protected boolean keepRunning;

    @Parameter(property = "docker.watchPostGoal")
    protected String watchPostGoal;

    @Parameter(property = "docker.watchPostExec")
    protected String watchPostExec;

    // Whether to keep the containers afters stopping (start/watch/stop)
    @Parameter(property = "docker.keepContainer", defaultValue = "false")
    protected boolean keepContainer;

    // Whether to remove volumes when removing the container (start/watch/stop)
    @Parameter(property = "docker.removeVolumes", defaultValue = "false")
    protected boolean removeVolumes;

    /**
     * Naming pattern for how to name containers when started
     */
    @Parameter(property = "docker.containerNamePattern")
    protected String containerNamePattern = ContainerNamingUtil.DEFAULT_CONTAINER_NAME_PATTERN;

    /**
     * Whether to create the customs networks (user-defined bridge networks) before starting automatically
     */
    @Parameter(property = "docker.autoCreateCustomNetworks", defaultValue = "false")
    protected boolean autoCreateCustomNetworks;

    @Override
    public void contextualize(Context context) throws ContextException {
        authConfigFactory = new AuthConfigFactory((PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY));
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!skip) {
            boolean ansiRestore = Ansi.isEnabled();
            log = new AnsiLogger(getLog(), useColorForLogging(), verbose, !settings.getInteractiveMode(), getLogPrefix());

            try {
                authConfigFactory.setLog(log);
                imageConfigResolver.setLog(log);

                LogOutputSpecFactory logSpecFactory = new LogOutputSpecFactory(useColor, logStdout, logDate);

                ConfigHelper.validateExternalPropertyActivation(project, images);

                DockerAccess access = null;
                try {
                    // The 'real' images configuration to use (configured images + externally resolved images)
                    this.minimalApiVersion = initImageConfiguration(getBuildTimestamp());
                    if (isDockerAccessRequired()) {
                        DockerAccessFactory.DockerAccessContext dockerAccessContext = getDockerAccessContext();
                        access = dockerAccessFactory.createDockerAccess(dockerAccessContext);
                    }
                    ServiceHub serviceHub = serviceHubFactory.createServiceHub(project, session, access, log, logSpecFactory);
                    executeInternal(serviceHub);
                } catch (IOException exp) {
                    logException(exp);
                    throw new MojoExecutionException(exp.getMessage());
                } catch (MojoExecutionException exp) {
                    logException(exp);
                    throw exp;
                } finally {
                    if (access != null) {
                        access.shutdown();
                    }
                }
            } finally {
                Ansi.setEnabled(ansiRestore);
            }
        }
    }

    /**
     * Hook for subclass for doing the real job
     *
     * @param serviceHub context for accessing backends
     */
    protected abstract void executeInternal(ServiceHub serviceHub)
            throws IOException, MojoExecutionException;

    protected BuildService.BuildContext getBuildContext() throws MojoExecutionException {
        return new BuildService.BuildContext.Builder()
                .buildArgs(buildArgs)
                .mojoParameters(createMojoParameters())
                .registryConfig(getRegistryConfig(pullRegistry))
                .build();
    }

    protected MavenBuildContext createMojoParameters() {
        return new MavenBuildContext.Builder()
                .session(session)
                .project(project)
                .mavenFileFilter(mavenFileFilter)
                .mavenReaderFilter(mavenFilterReader)
                .settings(settings)
                .sourceDirectory(sourceDirectory)
                .outputDirectory(outputDirectory)
                .reactorProjects(reactorProjects)
                .archiveConfiguration(archive)
                .build();
    }

    // Get the reference date for the build. By default this is picked up
    // from an existing build date file. If this does not exist, the current date is used.
    protected Date getReferenceDate() throws IOException {
        Date referenceDate = EnvUtil.loadTimestamp(getBuildTimestampFile());
        return referenceDate != null ? referenceDate : new Date();
    }

    // used for storing a timestamp
    protected File getBuildTimestampFile() {
        return new File(project.getBuild().getDirectory(), DOCKER_BUILD_TIMESTAMP);
    }

    /**
     * Helper method to process an ImageConfiguration.
     *
     * @param hub          ServiceHub
     * @param aImageConfig ImageConfiguration that would be forwarded to build and tag
     * @throws DockerAccessException
     * @throws MojoExecutionException
     */
    protected void processImageConfig(ServiceHub hub, ImageConfiguration aImageConfig) throws IOException, MojoExecutionException {
        BuildConfiguration buildConfig = aImageConfig.getBuildConfiguration();

        if (buildConfig != null) {
            if (buildConfig.getSkip()) {
                log.info("%s : Skipped building", aImageConfig.getDescription());
            } else {
                buildAndTag(hub, aImageConfig);
            }
        }
    }

    protected File getAndEnsureOutputDirectory() {
        File outputDir = new File(new File(project.getBuild().getDirectory()), DOCKER_EXTRA_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        return outputDir;
    }

    /**
     * Get the current build timestamp. this has either already been created by a previous
     * call or a new current date is created
     * @return timestamp to use
     */
    protected synchronized Date getBuildTimestamp() throws IOException {
        Date now = (Date) getPluginContext().get(CONTEXT_KEY_BUILD_TIMESTAMP);
        if (now == null) {
            now = getReferenceDate();
            getPluginContext().put(CONTEXT_KEY_BUILD_TIMESTAMP,now);
        }
        return now;
    }

    protected void processDmpPluginDescription(URL pluginDesc, File outputDir) throws IOException {
        String line = null;
        try (LineNumberReader reader =
                     new LineNumberReader(new InputStreamReader(pluginDesc.openStream(), "UTF8"))) {
            line = reader.readLine();
            while (line != null) {
                if (line.matches("^\\s*#")) {
                    // Skip comments
                    continue;
                }
                callBuildPlugin(outputDir, line);
                line = reader.readLine();
            }
        } catch (ClassNotFoundException e) {
            // Not declared as dependency, so just ignoring ...
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.verbose("Found dmp-plugin %s but could not be called : %s",
                    line,
                    e.getMessage());
        }
    }

    protected RegistryService.RegistryConfig getRegistryConfig(String specificRegistry) throws MojoExecutionException {
        return new RegistryService.RegistryConfig.Builder()
                .settings(settings)
                .authConfig(authConfig != null ? authConfig.toMap() : null)
                .authConfigFactory(authConfigFactory)
                .skipExtendedAuth(skipExtendedAuth)
                .registry(specificRegistry != null ? specificRegistry : registry)
                .build();
    }

    protected void callBuildPlugin(File outputDir, String buildPluginClass) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class buildPlugin = Class.forName(buildPluginClass);
        try {
            Method method = buildPlugin.getMethod("addExtraFiles", File.class);
            method.invoke(null, outputDir);
            log.info("Extra files from %s extracted", buildPluginClass);
        } catch (NoSuchMethodException exp) {
            log.verbose("Build plugin %s does not support 'addExtraFiles' method", buildPluginClass);
        }
    }





    protected ImageConfiguration createSimpleDockerfileConfig(File dockerFile) {
        // No configured name, so create one from maven GAV
        String name = MavenUtil.getPropertiesWithSystemOverrides(project).getProperty("docker.name");
        if (name == null) {
            // Default name group/artifact:version (or 'latest' if SNAPSHOT)
            name = "%g/%a:%l";
        }

        BuildConfiguration buildConfig =
                new BuildConfiguration.Builder()
                        .dockerFile(dockerFile.getPath())
                        .build();

        return new ImageConfiguration.Builder()
                .name(name)
                .buildConfig(buildConfig)
                .build();
    }

    protected ImageConfiguration addSimpleDockerfileConfig(ImageConfiguration image, File dockerfile) {
        BuildConfiguration buildConfig =
                new BuildConfiguration.Builder()
                        .dockerFile(dockerfile.getPath())
                        .build();
        return new ImageConfiguration.Builder(image).buildConfig(buildConfig).build();
    }

    protected void logException(Exception exp) {
        if (exp.getCause() != null) {
            log.error("%s [%s]", exp.getMessage(), exp.getCause().getMessage());
        } else {
            log.error("%s", exp.getMessage());
        }
    }

    protected DockerAccessFactory.DockerAccessContext getDockerAccessContext() {
        return new DockerAccessFactory.DockerAccessContext.Builder()
                .dockerHost(dockerHost)
                .certPath(certPath)
                .machine(machine)
                .maxConnections(maxConnections)
                .minimalApiVersion(minimalApiVersion)
                .projectProperties(project.getProperties())
                .skipMachine(skipMachine)
                .log(log)
                .build();
    }

    /**
     * Get all images to use. Can be restricted via -Ddocker.filter to pick a one or more images.
     * The values are taken as comma separated list.
     *
     * @return list of image configuration to be use. Can be empty but never null.
     */
    protected List<ImageConfiguration> getResolvedImages() {
        return resolvedImages;
    }

    protected boolean isDockerAccessRequired() {
        return true; // True in case of kubernetes maven plugin
    }

    protected void executeBuildGoal(ServiceHub hub) throws IOException, MojoExecutionException {
        if (skipBuild) {
            return;
        }

        // Check for build plugins
        executeBuildPlugins();

        // Iterate over all the ImageConfigurations and process one by one
        for (ImageConfiguration imageConfig : getResolvedImages()) {
            processImageConfig(hub, imageConfig);
        }
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

    protected void buildAndTag(ServiceHub hub, ImageConfiguration imageConfig)
            throws MojoExecutionException, DockerAccessException {

        try {
            // TODO need to refactor d-m-p to avoid this call
            EnvUtil.storeTimestamp(getBuildTimestampFile(), getBuildTimestamp());

            jkubeServiceHub.getBuildService().build(imageConfig);

        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to execute the build", ex);
        }
    }

    protected org.eclipse.jkube.kit.config.service.BuildService.BuildServiceConfig getBuildServiceConfig() throws MojoExecutionException {
        return new org.eclipse.jkube.kit.config.service.BuildService.BuildServiceConfig.Builder()
                .dockerBuildContext(getBuildContext())
                .dockerMavenBuildContext(createMojoParameters())
                .buildRecreateMode(BuildRecreateMode.fromParameter(buildRecreate))
                .openshiftBuildStrategy(buildStrategy)
                .openshiftPullSecret(openshiftPullSecret)
                .s2iBuildNameSuffix(s2iBuildNameSuffix)
                .s2iImageStreamLookupPolicyLocal(s2iImageStreamLookupPolicyLocal)
                .forcePullEnabled(forcePull)
                .imagePullManager(getImagePullManager(imagePullPolicy, autoPull))
                .buildDirectory(project.getBuild().getDirectory())
                .attacher((classifier, destFile) -> {
                    if (destFile.exists()) {
                        projectHelper.attachArtifact(project, "yml", classifier, destFile);
                    }
                })
                .build();
    }

    /**
     * Customization hook called by the base plugin.
     *
     * @param configs configuration to customize
     * @return the configuration customized by our generators.
     */
    public List<ImageConfiguration> customizeConfig(List<ImageConfiguration> configs) {
        runtimeMode = clusterAccess.resolveRuntimeMode(mode, log);
        log.info("Running in [[B]]%s[[B]] mode", runtimeMode.getLabel());
        if (runtimeMode == RuntimeMode.openshift) {
            log.info("Using [[B]]OpenShift[[B]] build with strategy [[B]]%s[[B]]", buildStrategy.getLabel());
        } else {
            log.info("Building Docker image in [[B]]Kubernetes[[B]] mode");
        }

        try {
            return GeneratorManager.generate(configs, getGeneratorContext(), false);
        } catch (MojoExecutionException e) {
            throw new IllegalArgumentException("Cannot extract generator config: " + e, e);
        }
    }

    protected String getLogPrefix() {
        return "k8s: ";
    }

    // ==================================================================================================

    // Get generator context
    protected GeneratorContext getGeneratorContext() {
        return new GeneratorContext.Builder()
                .config(extractGeneratorConfig())
                .project(project)
                .logger(log)
                .runtimeMode(runtimeMode)
                .strategy(buildStrategy)
                .useProjectClasspath(useProjectClasspath)
                .artifactResolver(getJkubeServiceHub().getArtifactResolverService())
                .build();
    }

    protected JkubeServiceHub getJkubeServiceHub() {
        return new JkubeServiceHub.Builder()
                .log(log)
                .clusterAccess(clusterAccess)
                .platformMode(mode)
                .repositorySystem(repositorySystem)
                .mavenProject(project)
                .build();
    }

    // Get generator config
    protected ProcessorConfig extractGeneratorConfig() {
        try {
            return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.GENERATOR_CONFIG, profile, ResourceUtil.getFinalResourceDir(resourceDir, environment), generator);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot extract generator config: " + e, e);
        }
    }

    // Get enricher context
    public EnricherContext getEnricherContext() {
        return new MavenEnricherContext.Builder()
                .project(project)
                .properties(project.getProperties())
                .session(session)
                .config(extractEnricherConfig())
                .images(getResolvedImages())
                .resources(resources)
                .log(log)
                .build();
    }

    // Get enricher config
    protected ProcessorConfig extractEnricherConfig() {
        try {
            return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.ENRICHER_CONFIG, profile, ResourceUtil.getFinalResourceDir(resourceDir, environment), enricher);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot extract enricher config: " + e, e);
        }
    }

    public ImagePullManager getImagePullManager(String imagePullPolicy, String autoPull) {
        return new ImagePullManager(getSessionCacheStore(), imagePullPolicy, autoPull);
    }

    protected ImagePullManager.CacheStore getSessionCacheStore() {
        return new ImagePullManager.CacheStore() {
            @Override
            public String get(String key) {
                Properties userProperties = session.getUserProperties();
                return userProperties.getProperty(key);
            }

            @Override
            public void put(String key, String value) {
                Properties userProperties = session.getUserProperties();
                userProperties.setProperty(key, value);
            }
        };
    }

    // check for a run-java.sh dependency an extract the script to target/ if found
    protected void executeBuildPlugins() {
        try {
            Enumeration<URL> dmpPlugins = Thread.currentThread().getContextClassLoader().getResources(DMP_PLUGIN_DESCRIPTOR);
            while (dmpPlugins.hasMoreElements()) {

                URL dmpPlugin = dmpPlugins.nextElement();
                File outputDir = getAndEnsureOutputDirectory();
                processDmpPluginDescription(dmpPlugin, outputDir);
            }
        } catch (IOException e) {
            log.error("Cannot load dmp-plugins from %s", DMP_PLUGIN_DESCRIPTOR);
        }
    }

    // Resolve and customize image configuration
    protected String initImageConfiguration(Date buildTimeStamp)  {
        // Resolve images
        resolvedImages = ConfigHelper.resolveImages(
                log,
                images,                  // Unresolved images
                (ImageConfiguration image) -> imageConfigResolver.resolve(image, project, session),
                filter,                   // A filter which image to process
                this);                     // customizer (can be overwritten by a subclass)

        // Check for simple Dockerfile mode
        File topDockerfile = new File(project.getBasedir(),"Dockerfile");
        if (topDockerfile.exists()) {
            if (resolvedImages.isEmpty()) {
                resolvedImages.add(createSimpleDockerfileConfig(topDockerfile));
            } else if (resolvedImages.size() == 1 && resolvedImages.get(0).getBuildConfiguration() == null) {
                resolvedImages.set(0, addSimpleDockerfileConfig(resolvedImages.get(0), topDockerfile));
            }
        }

        // Initialize configuration and detect minimal API version
        return ConfigHelper.initAndValidate(resolvedImages, apiVersion, new ImageNameFormatter(project, buildTimeStamp), log);
    }

    /**
     * Determine whether to enable colorized log messages
     * @return true if log statements should be colorized
     */
    protected boolean useColorForLogging() {
        return useColor && MessageUtils.isColorEnabled()
                && !(EnvUtil.isWindows() && !MavenUtil.isMaven350OrLater(session));
    }

    protected ClusterConfiguration getClusterConfiguration() {
        final ClusterConfiguration.Builder clusterConfigurationBuilder = new ClusterConfiguration.Builder(access);

        return clusterConfigurationBuilder.from(System.getProperties())
                .from(project.getProperties()).build();
    }

    protected GavLabel getGavLabel() {
        // Label used for this run
        return new GavLabel(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }

    protected String showLogs() {
        return System.getProperty("docker.showLogs");
    }

    protected boolean follow() {
        return Boolean.valueOf(System.getProperty("docker.follow", "false"));
    }

    protected LogDispatcher getLogDispatcher(ServiceHub hub) {
        LogDispatcher dispatcher = (LogDispatcher) getPluginContext().get(CONTEXT_KEY_LOG_DISPATCHER);
        if (dispatcher == null) {
            dispatcher = new LogDispatcher(hub.getDockerAccess());
            getPluginContext().put(CONTEXT_KEY_LOG_DISPATCHER, dispatcher);
        }
        return dispatcher;
    }

}
