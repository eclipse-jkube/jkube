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
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.build.service.docker.helper.ConfigHelper;
import org.eclipse.jkube.kit.build.service.docker.helper.ImageNameFormatter;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.LazyBuilder;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.MappingConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.config.service.ResourceServiceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.kit.resource.service.DefaultResourceService;
import org.eclipse.jkube.maven.plugin.enricher.DefaultEnricherManager;
import org.eclipse.jkube.maven.plugin.generator.GeneratorManager;

import io.fabric8.kubernetes.api.model.KubernetesList;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenFileFilter;

import static org.eclipse.jkube.kit.build.api.helper.DockerFileUtil.checkIfDockerfileModeAndImageConfigs;
import static org.eclipse.jkube.kit.common.ResourceFileType.yaml;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildReferenceDate;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.storeReferenceDateInPluginContext;
import static org.eclipse.jkube.kit.common.util.MavenFileFilterHelper.mavenFilterFiles;
import static org.eclipse.jkube.kit.common.util.ResourceMojoUtil.DEFAULT_RESOURCE_LOCATION;
import static org.eclipse.jkube.kit.common.util.ResourceMojoUtil.useDekorate;
import static org.eclipse.jkube.kit.common.util.ValidationUtil.validateIfRequired;
import static org.eclipse.jkube.maven.plugin.mojo.build.BuildMojo.CONTEXT_KEY_BUILD_TIMESTAMP;

/**
 * Generates or copies the Kubernetes JSON file and attaches it to the build so its
 * installed and released to maven repositories like other build artifacts.
 */
@Mojo(name = "resource", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ResourceMojo extends AbstractJKubeMojo {

    // Filename for holding the build timestamp
    public static final String DOCKER_BUILD_TIMESTAMP = "docker/build.timestamp";

    protected static final String DOCKER_IMAGE_USER = "docker.image.user";
    /**
     * The generated kubernetes and openshift manifests
     */
    @Parameter(property = "jkube.targetDir", defaultValue = "${project.build.outputDirectory}/META-INF/jkube")
    protected File targetDir;

    @Component(role = MavenFileFilter.class, hint = "default")
    private MavenFileFilter mavenFileFilter;

    @Component
    ImageConfigResolver imageConfigResolver;

    /**
     * Folder where to find project specific files
     */
    @Parameter(property = "jkube.resourceDir", defaultValue = "${basedir}/src/main/jkube")
    private File resourceDir;

    /**
     * Environment name where resources are placed. For example, if you set this property to dev and resourceDir is the default one, plugin will look at src/main/jkube/dev
     * Same applies for resourceDirOpenShiftOverride property.
     */
    @Parameter(property = "jkube.environment")
    private String environment;

    /**
     * Should we use the project's compile-time classpath to scan for additional enrichers/generators?
     */
    @Parameter(property = "jkube.useProjectClasspath", defaultValue = "false")
    private boolean useProjectClasspath = false;

    /**
     * The jkube working directory
     */
    @Parameter(property = "jkube.workDir", defaultValue = "${project.build.directory}/jkube")
    private File workDir;

    // Skip resource descriptors validation
    @Parameter(property = "jkube.skipResourceValidation", defaultValue = "false")
    Boolean skipResourceValidation;

    // Determine if the plugin should stop when a validation error is encountered
    @Parameter(property = "jkube.failOnValidationError", defaultValue = "false")
    Boolean failOnValidationError;

    // Reusing image configuration from d-m-p
    @Parameter
    List<ImageConfiguration> images;

    /**
     * Profile to use. A profile contains the enrichers and generators to
     * use as well as their configuration. Profiles are looked up
     * in the classpath and can be provided as yaml files.
     * <p>
     * However, any given enricher and or generator configuration overrides
     * the information provided by a profile.
     */
    @Parameter(property = "jkube.profile")
    private String profile;

    /**
     * Enricher specific configuration configuration given through
     * to the various enrichers.
     */

    @Parameter
    private ProcessorConfig enricher;

    /**
     * Configuration passed to generators
     */
    @Parameter
    private ProcessorConfig generator;

    // The image configuration after resolving and customization
    private List<ImageConfiguration> resolvedImages;

    // Mapping for kind filenames
    @Parameter
    private List<MappingConfig> mappings;

    /**
     * Namespace to use when accessing Kubernetes or OpenShift
     */
    @Parameter(property = "jkube.namespace")
    String namespace;

    @Parameter(property = "jkube.skip.resource", defaultValue = "false")
    protected boolean skipResource;

    /**
     * The artifact type for attaching the generated resource file to the project.
     * Can be either 'json' or 'yaml'
     */
    @Parameter(property = "jkube.resourceType")
    private ResourceFileType resourceFileType = yaml;

    // When resource generation is delegated to Dekorate, should JKube resources be merged with Dekorate's
    @Parameter(property = "jkube.mergeWithDekorate", defaultValue = "false")
    private Boolean mergeWithDekorate;

    @Parameter(property="jkube.interpolateTemplateParameters", defaultValue = "true")
    Boolean interpolateTemplateParameters;

    @Component
    MavenProjectHelper projectHelper;

    // resourceDir when environment has been applied
    private File realResourceDir;

    @Override
    protected boolean canExecute() {
        return super.canExecute() && !skipResource;
    }

    @Override
    public void executeInternal() throws MojoExecutionException {
        if (useDekorate(project) && mergeWithDekorate) {
            log.info("Dekorate detected, merging JKube and Dekorate resources");
            System.setProperty("dekorate.input.dir", DEFAULT_RESOURCE_LOCATION);
            System.setProperty("dekorate.output.dir", DEFAULT_RESOURCE_LOCATION);
        } else if (useDekorate(project)) {
            log.info("Dekorate detected, delegating resource build");
            System.setProperty("dekorate.output.dir", DEFAULT_RESOURCE_LOCATION);
            return;
        }

        KubernetesResourceUtil.updateKindFilenameMapper(MappingConfig.getKindFilenameMappings(mappings));
        try {
            lateInit();
            // Resolve the Docker image build configuration
            resolvedImages = getResolvedImages(images, log);
            if (!skip && (!isPomProject() || hasJKubeDir())) {
                // Extract and generate resources which can be a mix of Kubernetes and OpenShift resources
                final ResourceClassifier resourceClassifier = getResourceClassifier();
                final KubernetesList resourceList = generateResources();
                final File resourceClassifierDir = new File(this.targetDir, resourceClassifier.getValue());
                validateIfRequired(resourceClassifierDir, resourceClassifier, log, skipResourceValidation, failOnValidationError);
                final File artifact = jkubeServiceHub.getResourceService().writeResources(resourceList, resourceClassifier, log);
                // Attach it to the Maven reactor so that it will also get deployed
                attachArtifacts(project, projectHelper, resourceFileType, resourceClassifier.getValue(), artifact);
            }
        } catch (IOException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Failed to generate kubernetes descriptor", e);
        }
    }

    @Override
    public RuntimeMode getRuntimeMode() {
        return RuntimeMode.KUBERNETES;
    }

    @Override
    protected JKubeServiceHub.JKubeServiceHubBuilder initJKubeServiceHubBuilder(JavaProject javaProject) {
        realResourceDir = ResourceUtil.getFinalResourceDir(resourceDir, environment);
        if (namespace != null && !namespace.isEmpty()) {
            resources = ResourceConfig.toBuilder(resources).namespace(namespace).build();
        }
        final ResourceServiceConfig resourceServiceConfig = ResourceServiceConfig.getResourceServiceConfig(javaProject,
                realResourceDir, targetDir, resourceFileType, resources, resourceFiles -> mavenFilterFiles(mavenFileFilter, project, session, resourceFiles, workDir), interpolateTemplateParameters);
        return super.initJKubeServiceHubBuilder(javaProject)
            .resourceService(new LazyBuilder<>(() -> new DefaultResourceService(resourceServiceConfig)));
    }

    protected PlatformMode getPlatformMode() {
        return PlatformMode.kubernetes;
    }

    protected ResourceClassifier getResourceClassifier() {
        return ResourceClassifier.KUBERNETES;
    }

    protected void lateInit() {
        RuntimeMode runtimeMode = getRuntimeMode();
        jkubeServiceHub.setPlatformMode(runtimeMode);
    }

    private KubernetesList generateResources()
        throws IOException, DependencyResolutionRequiredException {

        JKubeEnricherContext.JKubeEnricherContextBuilder ctxBuilder = JKubeEnricherContext.getEnricherContext(MavenUtil.convertMavenProjectToJKubeProject(project, session),
                extractEnricherConfig(profile, realResourceDir, enricher), resolvedImages, resources, log);
        ctxBuilder.settings(MavenUtil.getRegistryServerFromMavenSettings(settings));
        DefaultEnricherManager enricherManager = new DefaultEnricherManager(ctxBuilder.build(),
            MavenUtil.getCompileClasspathElementsIfRequested(project, useProjectClasspath));

        return jkubeServiceHub.getResourceService().generateResources(getPlatformMode(), enricherManager, log);
    }

    // ==================================================================================

    private List<ImageConfiguration> getResolvedImages(List<ImageConfiguration> images, final KitLogger log)
        throws DependencyResolutionRequiredException {
        List<ImageConfiguration> ret;
        JavaProject jkubeProject = MavenUtil.convertMavenProjectToJKubeProject(project, session);
        ret = ConfigHelper.resolveImages(
                log,
                images,
                (ImageConfiguration image) -> imageConfigResolver.resolve(image, jkubeProject),
                null,  // no filter on image name yet (TODO: Maybe add this, too ?)
                (List<ImageConfiguration> configs) -> customizeImages(configs, extractGeneratorConfig(profile, realResourceDir, generator), jkubeProject, getRuntimeMode(), log, JKubeBuildStrategy.docker, useProjectClasspath));

        Date now = getBuildReferenceDate(project.getBuild().getDirectory(), DOCKER_BUILD_TIMESTAMP);
        storeReferenceDateInPluginContext(now, getPluginContext(), CONTEXT_KEY_BUILD_TIMESTAMP);
        // Check for simple Dockerfile mode
        checkIfDockerfileModeAndImageConfigs(project.getBasedir(), ret, MavenUtil.getPropertiesWithSystemOverrides(project).getProperty("jkube.image.name"));

        ConfigHelper.initAndValidate(ret, null /* no minimal api version */,
            new ImageNameFormatter(MavenUtil.convertMavenProjectToJKubeProject(project, session), now));
        return ret;
    }

    private boolean hasJKubeDir() {
        return realResourceDir.isDirectory();
    }

    private boolean isPomProject() {
        return "pom".equals(project.getPackaging());
    }

    static List<ImageConfiguration> customizeImages(List<ImageConfiguration> configs, ProcessorConfig generatorConfig,
                                                    JavaProject jkubeProject, RuntimeMode runtimeMode, KitLogger log,
                                                    JKubeBuildStrategy buildStrategy, boolean useProjectClasspath) {
        try {
            GeneratorContext ctx = GeneratorContext.generatorContextBuilder(generatorConfig, jkubeProject,
                    log, runtimeMode, useProjectClasspath, null, buildStrategy).build();
            return GeneratorManager.generate(configs, ctx, true);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot extract generator: " + e, e);
        }
    }
}
