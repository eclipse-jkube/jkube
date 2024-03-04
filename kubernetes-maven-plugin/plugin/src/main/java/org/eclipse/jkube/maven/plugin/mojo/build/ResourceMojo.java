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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.validation.ConstraintViolationException;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorManager;
import org.eclipse.jkube.kit.build.api.helper.ImageConfigResolver;
import org.eclipse.jkube.kit.build.api.helper.ConfigHelper;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.common.util.validator.ResourceValidator;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.MappingConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.enricher.api.DefaultEnricherManager;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.profile.ProfileUtil;

import io.fabric8.kubernetes.api.model.KubernetesList;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;

import static org.eclipse.jkube.kit.build.api.helper.ImageNameFormatter.DOCKER_IMAGE_USER;
import static org.eclipse.jkube.kit.common.util.BuildReferenceDateUtil.getBuildTimestamp;
import static org.eclipse.jkube.kit.common.util.DekorateUtil.DEFAULT_RESOURCE_LOCATION;
import static org.eclipse.jkube.kit.common.util.DekorateUtil.useDekorate;
import static org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceFragments.updateKindFilenameMappings;
import static org.eclipse.jkube.maven.plugin.mojo.build.BuildMojo.CONTEXT_KEY_BUILD_TIMESTAMP;


/**
 * Generates or copies the Kubernetes JSON file and attaches it to the build so its
 * installed and released to maven repositories like other build artifacts.
 */
@Mojo(name = "resource", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ResourceMojo extends AbstractJKubeMojo {

    // Filename for holding the build timestamp
    public static final String DOCKER_BUILD_TIMESTAMP = "docker/build.timestamp";

    @Component
    protected ImageConfigResolver imageConfigResolver;

    /**
     * Should we use the project's compile-time classpath to scan for additional enrichers/generators?
     */
    @Parameter(property = "jkube.useProjectClasspath", defaultValue = "false")
    private boolean useProjectClasspath = false;


    // Skip resource descriptors validation
    @Parameter(property = "jkube.skipResourceValidation", defaultValue = "false")
    protected Boolean skipResourceValidation;

    // Determine if the plugin should stop when a validation error is encountered
    @Parameter(property = "jkube.failOnValidationError", defaultValue = "false")
    protected Boolean failOnValidationError;

    // Reusing image configuration from d-m-p
    @Parameter
    protected List<ImageConfiguration> images;

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
     * Enricher specific configuration passed on to the discovered Enrichers.
     */

    @Parameter
    private ProcessorConfig enricher;

    /**
     * Configuration passed to generators
     */
    @Parameter
    private ProcessorConfig generator;

    // Whether to use replica sets or replication controller. Could be configurable
    // but for now leave it hidden.
    private boolean useReplicaSet = true;

    // The image configuration after resolving and customization
    protected List<ImageConfiguration> resolvedImages;

    // Mapping for kind filenames
    @Parameter
    private List<MappingConfig> mappings;

    @Parameter(property = "jkube.skip.resource", defaultValue = "false")
    protected boolean skipResource;


    // When resource generation is delegated to Dekorate, should JKube resources be merged with Dekorate's
    @Parameter(property = "jkube.mergeWithDekorate", defaultValue = "false")
    private Boolean mergeWithDekorate;


    @Component
    protected MavenProjectHelper projectHelper;


    @Override
    protected boolean shouldSkip() {
        return super.shouldSkip() || skipResource;
    }

    @Override
    public void executeInternal() throws MojoExecutionException, MojoFailureException {
        if (useDekorate(javaProject) && mergeWithDekorate) {
            log.info("Dekorate detected, merging JKube and Dekorate resources");
            System.setProperty("dekorate.input.dir", DEFAULT_RESOURCE_LOCATION);
            System.setProperty("dekorate.output.dir", DEFAULT_RESOURCE_LOCATION);
        } else if (useDekorate(javaProject)) {
            log.info("Dekorate detected, delegating resource build");
            System.setProperty("dekorate.output.dir", DEFAULT_RESOURCE_LOCATION);
            return;
        }

        updateKindFilenameMappings(mappings);
        try {
            lateInit();
            // Resolve the Docker image build configuration
            resolvedImages = getResolvedImages(images, log);
            if (!skip && (!isPomProject() || hasJKubeDir())) {
                // Extract and generate resources which can be a mix of Kubernetes and OpenShift resources
                final ResourceClassifier resourceClassifier = getResourceClassifier();
                final KubernetesList resourceList = generateResources();
                final File resourceClassifierDir = new File(this.targetDir, resourceClassifier.getValue());
                final File artifact = jkubeServiceHub.getResourceService().writeResources(resourceList, resourceClassifier, log);
                validateIfRequired(resourceClassifierDir, resourceClassifier);
                // Attach it to the Maven reactor so that it will also get deployed
                projectHelper.attachArtifact(project,
                  jkubeServiceHub.getResourceServiceConfig().getResourceFileType().getArtifactType(),
                  resourceClassifier.getValue(), artifact);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate kubernetes descriptor", e);
        }
    }

    @Override
    protected RuntimeMode getRuntimeMode() {
        return RuntimeMode.KUBERNETES;
    }

    protected PlatformMode getPlatformMode() {
        return PlatformMode.kubernetes;
    }

    protected ResourceClassifier getResourceClassifier() {
        return ResourceClassifier.KUBERNETES;
    }

    private void validateIfRequired(File resourceDir, ResourceClassifier classifier)
        throws MojoExecutionException, MojoFailureException {
        try {
            if (!skipResourceValidation) {
                log.verbose("Validating resources");
                new ResourceValidator(resourceDir, classifier, log).validate();
            }
        } catch (ConstraintViolationException e) {
            if (failOnValidationError) {
                log.error("[[R]]" + e.getMessage() + "[[R]]");
                log.error("[[R]]use \"mvn -Djkube.skipResourceValidation=true\" option to skip the validation[[R]]");
                throw new MojoFailureException("Failed to generate kubernetes descriptor");
            } else {
                log.warn("[[Y]]" + e.getMessage() + "[[Y]]");
            }
        } catch (Exception e) {
            if (failOnValidationError) {
                throw new MojoExecutionException("Failed to validate resources", e);
            } else {
                log.warn("Failed to validate resources: %s", e.getMessage());
            }
        }
    }

    private void lateInit() {
        RuntimeMode runtimeMode = getRuntimeMode();
        jkubeServiceHub.setPlatformMode(runtimeMode);
        if (runtimeMode.equals(RuntimeMode.OPENSHIFT)) {
            Properties properties = javaProject.getProperties();
            if (!properties.contains(DOCKER_IMAGE_USER)) {
                String namespaceToBeUsed = this.namespace != null && !this.namespace.isEmpty() ?
                        this.namespace: clusterAccess.getNamespace();
                log.info("Using docker image name of namespace: " + namespaceToBeUsed);
                properties.setProperty(DOCKER_IMAGE_USER, namespaceToBeUsed);
            }
            if (!properties.contains(RuntimeMode.JKUBE_EFFECTIVE_PLATFORM_MODE)) {
                properties.setProperty(RuntimeMode.JKUBE_EFFECTIVE_PLATFORM_MODE, runtimeMode.toString());
            }
        }
    }

    private KubernetesList generateResources() throws IOException {
        log.verbose("Generating resources");
        JKubeEnricherContext.JKubeEnricherContextBuilder ctxBuilder = JKubeEnricherContext.builder()
                .jKubeBuildStrategy(buildStrategy)
                .project(javaProject)
                .processorConfig(extractEnricherConfig())
                .settings(MavenUtil.getRegistryServerFromMavenSettings(settings))
                .resources(resources)
                .images(resolvedImages)
                .log(log);

        DefaultEnricherManager enricherManager = new DefaultEnricherManager(ctxBuilder.build(),
          useProjectClasspath ? javaProject.getCompileClassPathElements() : Collections.emptyList());

        return jkubeServiceHub.getResourceService().generateResources(getPlatformMode(), enricherManager, log);
    }

    private ProcessorConfig extractEnricherConfig() throws IOException {
        return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.ENRICHER_CONFIG, profile,
          jkubeServiceHub.getResourceServiceConfig().getResourceDirs(), enricher);
    }

    private ProcessorConfig extractGeneratorConfig() throws IOException {
        return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.GENERATOR_CONFIG, profile,
          jkubeServiceHub.getResourceServiceConfig().getResourceDirs(), generator);
    }

    // ==================================================================================

    private List<ImageConfiguration> getResolvedImages(List<ImageConfiguration> images, final KitLogger log)
        throws IOException {
      return ConfigHelper.initImageConfiguration(
          getBuildTimestamp(getPluginContext(), CONTEXT_KEY_BUILD_TIMESTAMP, project.getBuild().getDirectory(),
              DOCKER_BUILD_TIMESTAMP),
          images, imageConfigResolver,
          log,
          null, // no filter on image name yet (TODO: Maybe add this, too ?)
          configs -> {
            try {
              GeneratorContext ctx = GeneratorContext.builder()
                  .config(extractGeneratorConfig())
                  .project(javaProject)
                  .runtimeMode(getRuntimeMode())
                  .logger(log)
                  .strategy(JKubeBuildStrategy.docker)
                  .useProjectClasspath(useProjectClasspath)
                  .build();
              return GeneratorManager.generate(configs, ctx, true);
            } catch (Exception e) {
              throw new IllegalArgumentException("Cannot extract generator: " + e, e);
            }
          },
          jkubeServiceHub.getConfiguration());
    }

    private boolean hasJKubeDir() {
        final List<File> realResourceDirs = jkubeServiceHub.getResourceServiceConfig().getResourceDirs();
        return !realResourceDirs.isEmpty() && realResourceDirs.get(0).isDirectory();
    }

    private boolean isPomProject() {
        return "pom".equals(project.getPackaging());
    }

}
