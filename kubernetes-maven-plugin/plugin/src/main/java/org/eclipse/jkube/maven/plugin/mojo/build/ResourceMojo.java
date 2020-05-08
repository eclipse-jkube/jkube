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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.ConfigHelper;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ImageConfigResolver;
import org.eclipse.jkube.kit.build.service.docker.helper.ImageNameFormatter;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.MavenUtil;
import org.eclipse.jkube.kit.common.util.ResourceClassifier;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.common.util.ValidationUtil;
import org.eclipse.jkube.kit.common.util.validator.ResourceValidator;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.MappingConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.profile.Profile;
import org.eclipse.jkube.kit.profile.ProfileUtil;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.kit.enricher.handler.HandlerHub;
import org.eclipse.jkube.maven.plugin.enricher.EnricherManager;
import org.eclipse.jkube.maven.plugin.generator.GeneratorManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;

import javax.validation.ConstraintViolationException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.eclipse.jkube.kit.common.ResourceFileType.yaml;
import static org.eclipse.jkube.kit.common.util.ResourceMojoUtil.DEFAULT_RESOURCE_LOCATION;
import static org.eclipse.jkube.kit.common.util.ResourceMojoUtil.useDekorate;
import static org.eclipse.jkube.maven.plugin.mojo.build.BuildMojo.CONTEXT_KEY_BUILD_TIMESTAMP;


/**
 * Generates or copies the Kubernetes JSON file and attaches it to the build so its
 * installed and released to maven repositories like other build artifacts.
 */
@Mojo(name = "resource", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ResourceMojo extends AbstractJKubeMojo {

    // Filename for holding the build timestamp
    public static final String DOCKER_BUILD_TIMESTAMP = "docker/build.timestamp";

    private static final String DOCKER_IMAGE_USER = "docker.image.user";
    /**
     * The generated kubernetes and openshift manifests
     */
    @Parameter(property = "jkube.targetDir", defaultValue = "${project.build.outputDirectory}/META-INF/jkube")
    protected File targetDir;

    @Component(role = MavenFileFilter.class, hint = "default")
    private MavenFileFilter mavenFileFilter;

    @Component
    private ImageConfigResolver imageConfigResolver;

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

    /**
     * The jkube working directory
     */
    @Parameter(property = "jkube.workDirOpenShiftOverride", defaultValue = "${project.build.directory}/jkube-openshift-override")
    private File workDirOpenShiftOverride;

    // Resource specific configuration for this plugin
    @Parameter
    private ResourceConfig resources;

    @Parameter(property = "jkube.mode")
    private RuntimeMode runtimeMode = RuntimeMode.DEFAULT;

    // Skip resource descriptors validation
    @Parameter(property = "jkube.skipResourceValidation", defaultValue = "false")
    private Boolean skipResourceValidation;

    // Determine if the plugin should stop when a validation error is encountered
    @Parameter(property = "jkube.failOnValidationError", defaultValue = "false")
    private Boolean failOnValidationError;

    // Reusing image configuration from d-m-p
    @Parameter
    private List<ImageConfiguration> images;

    @Parameter(property = "jkube.build.switchToDeployment", defaultValue = "false")
    private Boolean switchToDeployment;
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

    // Whether to use replica sets or replication controller. Could be configurable
    // but for now leave it hidden.
    private boolean useReplicaSet = true;

    // The image configuration after resolving and customization
    private List<ImageConfiguration> resolvedImages;

    // Mapping for kind filenames
    @Parameter
    private List<MappingConfig> mappings;

    // Services
    private HandlerHub handlerHub;

    /**
     * Namespace to use when accessing Kubernetes or OpenShift
     */
    @Parameter(property = "jkube.namespace")
    private String namespace;

    @Parameter(property = "jkube.sidecar", defaultValue = "false")
    private Boolean sidecar;

    @Parameter(property = "jkube.skipHealthCheck", defaultValue = "false")
    private Boolean skipHealthCheck;

    /**
     * The OpenShift deploy timeout in seconds:
     * See this issue for background of why for end users on slow wifi on their laptops
     * DeploymentConfigs usually barf: https://github.com/openshift/origin/issues/10531
     *
     * Please follow also the discussion at
     * <ul>
     *     <li>https://github.com/fabric8io/fabric8-maven-plugin/pull/944#discussion_r116962969</li>
     *     <li>https://github.com/fabric8io/fabric8-maven-plugin/pull/794</li>
     * </ul>
     * and the references within it for the reason of this ridiculous long default timeout
     * (in short: Its because Docker image download times are added to the deployment time, making
     * the default of 10 minutes quite unusable if multiple images are included in the deployment).
     */
    @Parameter(property = "jkube.openshift.deployTimeoutSeconds", defaultValue = "3600")
    private Long openshiftDeployTimeoutSeconds;

    /**
     * If set to true it would set the container image reference to "", this is done to handle weird
     * behavior of OpenShift 3.7 in which subsequent rollouts lead to ImagePullErr
     *
     * Please see discussion at
     * <ul>
     *     <li>https://github.com/openshift/origin/issues/18406</li>
     *     <li>https://github.com/fabric8io/fabric8-maven-plugin/issues/1130</li>
     * </ul>
     */
    @Parameter(property = "jkube.openshift.trimImageInContainerSpec", defaultValue = "false")
    private Boolean trimImageInContainerSpec;

    @Parameter(property = "jkube.openshift.generateRoute", defaultValue = "true")
    private Boolean generateRoute;

    @Parameter(property = "jkube.openshift.enableAutomaticTrigger", defaultValue = "true")
    private Boolean enableAutomaticTrigger;

    @Parameter(property = "jkube.openshift.imageChangeTrigger", defaultValue = "true")
    private Boolean enableImageChangeTrigger;

    @Parameter(property = "jkube.openshift.enrichAllWithImageChangeTrigger", defaultValue = "false")
    private Boolean erichAllWithImageChangeTrigger;

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

    @Component
    private MavenProjectHelper projectHelper;

    // resourceDir when environment has been applied
    private File realResourceDir;

    /**
     * Returns the Template if the list contains a single Template only otherwise returns null
     */
    protected static Template getSingletonTemplate(KubernetesList resources) {
        // if the list contains a single Template lets unwrap it
        if (resources != null) {
            List<HasMetadata> items = resources.getItems();
            if (items != null && items.size() == 1) {
                HasMetadata singleEntity = items.get(0);
                if (singleEntity instanceof Template) {
                    return (Template) singleEntity;
                }
            }
        }
        return null;
    }

    public static File writeResourcesIndividualAndComposite(KubernetesList resources, File resourceFileBase,
        ResourceFileType resourceFileType, KitLogger log) throws MojoExecutionException {

        // entity is object which will be sent to writeResource for openshift.yml
        // if generateRoute is false, this will be set to resources with new list
        // otherwise it will be set to resources with old list.
        Object entity = resources;

        // if the list contains a single Template lets unwrap it
        // in resources already new or old as per condition is set.
        // no need to worry about this for dropping Route.
        Template template = getSingletonTemplate(resources);
        if (template != null) {
            entity = template;
        }

        File file = writeResource(resourceFileBase, entity, resourceFileType);

        // write separate files, one for each resource item
        // resources passed to writeIndividualResources is also new one.
        writeIndividualResources(resources, resourceFileBase, resourceFileType, log);
        return file;
    }

    private static void writeIndividualResources(KubernetesList resources, File targetDir,
        ResourceFileType resourceFileType, KitLogger log) throws MojoExecutionException {
        for (HasMetadata item : resources.getItems()) {
            String name = KubernetesHelper.getName(item);
            if (StringUtils.isBlank(name)) {
                log.error("No name for generated item %s", item);
                continue;
            }
            String itemFile = KubernetesResourceUtil.getNameWithSuffix(name, item.getKind());

            // Here we are writing individual file for all the resources.
            File itemTarget = new File(targetDir, itemFile);
            writeResource(itemTarget, item, resourceFileType);
        }
    }

    private static File writeResource(File resourceFileBase, Object entity, ResourceFileType resourceFileType)
        throws MojoExecutionException {
        try {
            return ResourceUtil.save(resourceFileBase, entity, resourceFileType);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write resource to " + resourceFileBase + ". " + e, e);
        }
    }

    @Override
    protected boolean canExecute() {
        return super.canExecute() && !skipResource;
    }

    public void executeInternal() throws MojoExecutionException, MojoFailureException {
        if (useDekorate(project) && mergeWithDekorate) {
            log.info("Dekorate detected, merging JKube and Dekorate resources");
            System.setProperty("dekorate.input.dir", DEFAULT_RESOURCE_LOCATION);
            System.setProperty("dekorate.output.dir", DEFAULT_RESOURCE_LOCATION);
        } else if (useDekorate(project)) {
            log.info("Dekorate detected, delegating resource build");
            System.setProperty("dekorate.output.dir", DEFAULT_RESOURCE_LOCATION);
            return;
        }

        realResourceDir = ResourceUtil.getFinalResourceDir(resourceDir, environment);
        updateKindFilenameMappings();
        try {
            lateInit();
            // Resolve the Docker image build configuration
            resolvedImages = getResolvedImages(images, log);
            if (!skip && (!isPomProject() || hasJKubeDir())) {
                // Extract and generate resources which can be a mix of Kubernetes and OpenShift resources
                final PlatformMode platformMode = getPlatformMode();
                final ResourceClassifier resourceClassifier = getResourceClassifier();
                final KubernetesList resourceList = generateResources(platformMode, resolvedImages);
                File resourceClassifierDir = new File(this.targetDir, resourceClassifier.getValue());
                validateIfRequired(resourceClassifierDir, resourceClassifier);
                writeResources(resourceList, resourceClassifier);
            }
        } catch (IOException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Failed to generate kubernetes descriptor", e);
        }
    }

    protected PlatformMode getPlatformMode() {
        return PlatformMode.kubernetes;
    }

    protected ResourceClassifier getResourceClassifier() {
        return ResourceClassifier.KUBERNETES;
    }

    private void updateKindFilenameMappings() {
        if (mappings != null) {
            final Map<String, List<String>> mappingKindFilename = new HashMap<>();
            for (MappingConfig mappingConfig : this.mappings) {
                if (mappingConfig.isValid()) {
                    mappingKindFilename.put(mappingConfig.getKind(), Arrays.asList(mappingConfig.getFilenamesAsArray()));
                } else {
                    throw new IllegalArgumentException(String.format("Invalid mapping for Kind %s and Filename Types %s",
                        mappingConfig.getKind(), mappingConfig.getFilenameTypes()));
                }
            }
            KubernetesResourceUtil.updateKindFilenameMapper(mappingKindFilename);
        }
    }

    private void validateIfRequired(File resourceDir, ResourceClassifier classifier)
        throws MojoExecutionException, MojoFailureException {
        try {
            if (!skipResourceValidation) {
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
        runtimeMode = clusterAccess.resolveRuntimeMode(runtimeMode, log);
        if (runtimeMode.equals(RuntimeMode.openshift)) {
            Properties properties = project.getProperties();
            if (!properties.contains(DOCKER_IMAGE_USER)) {
                String namespace = this.namespace != null && !this.namespace.isEmpty() ?
                        this.namespace: clusterAccess.getNamespace();
                log.info("Using docker image name of namespace: " + namespace);
                properties.setProperty(DOCKER_IMAGE_USER, namespace);
            }
            if (!properties.contains(RuntimeMode.FABRIC8_EFFECTIVE_PLATFORM_MODE)) {
                properties.setProperty(RuntimeMode.FABRIC8_EFFECTIVE_PLATFORM_MODE, runtimeMode.toString());
            }
        }
    }

    private KubernetesList generateResources(PlatformMode platformMode, List<ImageConfiguration> images)
        throws IOException, MojoExecutionException, DependencyResolutionRequiredException {

        if (namespace != null && !namespace.isEmpty()) {
            resources = ResourceConfig.toBuilder(resources).namespace(namespace).build();
        }
        // Manager for calling enrichers.
        JavaProject jkubeProject = MavenUtil.convertMavenProjectToJKubeProject(project, session);
        JKubeEnricherContext.JKubeEnricherContextBuilder ctxBuilder = JKubeEnricherContext.builder()
                .project(jkubeProject)
                .processorConfig(extractEnricherConfig())
                .settings(MavenUtil.getRegistryServerFromMavenSettings(settings))
                .properties(jkubeProject.getProperties())
                .resources(resources)
                .images(resolvedImages)
                .log(log);

        EnricherManager enricherManager = new EnricherManager(ctxBuilder.build(),
            MavenUtil.getCompileClasspathElementsIfRequested(project, useProjectClasspath));

        // Generate all resources from the main resource directory, configuration and create them accordingly
        KubernetesListBuilder builder = generateAppResources(platformMode, images, enricherManager);

        // Add resources found in subdirectories of resourceDir, with a certain profile
        // applied
        addProfiledResourcesFromSubdirectories(platformMode, builder, realResourceDir, enricherManager);
        return builder.build();
    }

    private void addProfiledResourcesFromSubdirectories(PlatformMode platformMode, KubernetesListBuilder builder, File resourceDir,
        EnricherManager enricherManager) throws IOException, MojoExecutionException {
        File[] profileDirs = resourceDir.listFiles(File::isDirectory);
        if (profileDirs != null) {
            for (File profileDir : profileDirs) {
                Profile foundProfile = ProfileUtil.findProfile(profileDir.getName(), resourceDir);
                ProcessorConfig enricherConfig = foundProfile.getEnricherConfig();
                File[] resourceFiles = KubernetesResourceUtil.listResourceFragments(profileDir);
                if (resourceFiles.length > 0) {
                    KubernetesListBuilder profileBuilder = readResourceFragments(platformMode, resourceFiles);
                    enricherManager.createDefaultResources(platformMode, enricherConfig, profileBuilder);
                    enricherManager.enrich(platformMode, enricherConfig, profileBuilder);
                    KubernetesList profileItems = profileBuilder.build();
                    for (HasMetadata item : profileItems.getItems()) {
                        builder.addToItems(item);
                    }
                }
            }
        }
    }

    private KubernetesListBuilder generateAppResources(PlatformMode platformMode, List<ImageConfiguration> images, EnricherManager enricherManager)
        throws IOException, MojoExecutionException {
        try {
            KubernetesListBuilder builder = processResourceFragments(platformMode);

            // Create default resources for app resources only
            enricherManager.createDefaultResources(platformMode, builder);

            // Enrich descriptors
            enricherManager.enrich(platformMode, builder);

            return builder;
        } catch (ConstraintViolationException e) {
            String message = ValidationUtil.createValidationMessage(e.getConstraintViolations());
            log.error("ConstraintViolationException: %s", message);
            throw new MojoExecutionException(message, e);
        }
    }

    private KubernetesListBuilder processResourceFragments(PlatformMode platformMode) throws IOException, MojoExecutionException {
        File[] resourceFiles = KubernetesResourceUtil.listResourceFragments(realResourceDir, resources !=null ? resources.getRemotes() : null, log);
        KubernetesListBuilder builder;

        // Add resource files found in the jkube directory
        if (resourceFiles != null && resourceFiles.length > 0) {
            log.info("using resource templates from %s", realResourceDir);
            builder = readResourceFragments(platformMode, resourceFiles);
        } else {
            builder = new KubernetesListBuilder();
        }
        return builder;
    }

    private KubernetesListBuilder readResourceFragments(PlatformMode platformMode, File[] resourceFiles) throws IOException, MojoExecutionException {
        KubernetesListBuilder builder;
        String defaultName = JKubeProjectUtil.createDefaultResourceName(project.getArtifactId());
        builder = KubernetesResourceUtil.readResourceFragmentsFrom(
            platformMode,
            KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING,
            defaultName,
            mavenFilterFiles(resourceFiles, this.workDir));
        return builder;
    }

    private ProcessorConfig extractEnricherConfig() throws IOException {
        return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.ENRICHER_CONFIG, profile, realResourceDir, enricher);
    }

    private ProcessorConfig extractGeneratorConfig() throws IOException {
        return ProfileUtil.blendProfileWithConfiguration(ProfileUtil.GENERATOR_CONFIG, profile, realResourceDir, generator);
    }

    // ==================================================================================

    private List<ImageConfiguration> getResolvedImages(List<ImageConfiguration> images, final KitLogger log)
        throws MojoExecutionException, DependencyResolutionRequiredException {
        List<ImageConfiguration> ret;
        JavaProject jkubeProject = MavenUtil.convertMavenProjectToJKubeProject(project, session);
        ret = ConfigHelper.resolveImages(
            log,
            images,
            (ImageConfiguration image) -> imageConfigResolver.resolve(image, jkubeProject),
            null,  // no filter on image name yet (TODO: Maybe add this, too ?)
                (List<ImageConfiguration> configs) -> {
                    try {
                        GeneratorContext ctx = GeneratorContext.builder()
                                .config(extractGeneratorConfig())
                                .project(jkubeProject)
                                .runtimeMode(runtimeMode)
                                .logger(log)
                                .strategy(OpenShiftBuildStrategy.docker)
                                .useProjectClasspath(useProjectClasspath)
                                .build();
                        return GeneratorManager.generate(configs, ctx, true);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Cannot extract generator: " + e, e);
                    }
            });

        Date now = getBuildReferenceDate();
        storeReferenceDateInPluginContext(now);
        String minimalApiVersion = ConfigHelper.initAndValidate(ret, null /* no minimal api version */,
            new ImageNameFormatter(MavenUtil.convertMavenProjectToJKubeProject(project, session), now), log);
        return ret;
    }

    private void storeReferenceDateInPluginContext(Date now) {
        Map<String, Object> pluginContext = getPluginContext();
        pluginContext.put(CONTEXT_KEY_BUILD_TIMESTAMP, now);
    }

    // get a reference date
    private Date getBuildReferenceDate() throws MojoExecutionException {
        // Pick up an existing build date created by k8s:build previously
        File tsFile = new File(project.getBuild().getDirectory(), DOCKER_BUILD_TIMESTAMP);
        if (!tsFile.exists()) {
            return new Date();
        }
        try {
            return EnvUtil.loadTimestamp(tsFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read timestamp from " + tsFile, e);
        }
    }

    private File[] mavenFilterFiles(File[] resourceFiles, File outDir) throws MojoExecutionException {
        if (!outDir.exists()) {
            if (!outDir.mkdirs()) {
                throw new MojoExecutionException("Cannot create working dir " + outDir);
            }
        }
        File[] ret = new File[resourceFiles.length];
        int i = 0;
        for (File resource : resourceFiles) {
            File targetFile = new File(outDir, resource.getName());
            try {
                mavenFileFilter.copyFile(resource, targetFile, true,
                    project, null, false, "utf8", session);
                ret[i++] = targetFile;
            } catch (MavenFilteringException exp) {
                throw new MojoExecutionException(
                    String.format("Cannot filter %s to %s", resource, targetFile), exp);
            }
        }
        return ret;
    }

    private boolean hasJKubeDir() {
        return realResourceDir.isDirectory();
    }

    private boolean isPomProject() {
        return "pom".equals(project.getPackaging());
    }

    protected void writeResources(KubernetesList resources, ResourceClassifier classifier)
        throws MojoExecutionException {
        // write kubernetes.yml / openshift.yml
        File resourceFileBase = new File(this.targetDir, classifier.getValue());

        File file =
            writeResourcesIndividualAndComposite(resources, resourceFileBase, this.resourceFileType, log);
        // Resolve template placeholders
        if (classifier == ResourceClassifier.KUBERNETES) {
            resolveTemplateVariablesIfAny(resources);
        }

        KubernetesHelper.resolveTemplateVariablesIfAny(resources, this.targetDir);

        // Attach it to the Maven reactor so that it will also get deployed
        projectHelper.attachArtifact(project, this.resourceFileType.getArtifactType(), classifier.getValue(), file);
    }

    private void resolveTemplateVariablesIfAny(KubernetesList resources) throws MojoExecutionException {
        Template template = findTemplate(resources);
        if (template != null) {
            List<io.fabric8.openshift.api.model.Parameter> parameters = template.getParameters();
            if (parameters == null || parameters.isEmpty()) {
                return;
            }
            File kubernetesYaml = new File(this.targetDir, "kubernetes.yml");
            resolveTemplateVariables(parameters, kubernetesYaml);
            File kubernetesResourceDir = new File(this.targetDir, "kubernetes");
            File[] kubernetesResources = kubernetesResourceDir.listFiles((dir, filename) -> filename.endsWith(".yml"));
            if (kubernetesResources != null) {
                for (File kubernetesResource : kubernetesResources) {
                    resolveTemplateVariables(parameters, kubernetesResource);
                }
            }
        }
    }

    private void resolveTemplateVariables(List<io.fabric8.openshift.api.model.Parameter> parameters, File kubernetesYaml) throws MojoExecutionException {
        String text;
        try {
            text = FileUtils.readFileToString(kubernetesYaml, Charset.defaultCharset());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to load " + kubernetesYaml + " so we can replace template expressions " +e, e);
        }
        String original = text;
        for (io.fabric8.openshift.api.model.Parameter parameter : parameters) {
            String from = "${" + parameter.getName() + "}";
            String to = parameter.getValue();
            text = text.replace(from, to);
        }
        if (!original.equals(text)) {
            try {
                FileUtils.writeStringToFile(kubernetesYaml, text, Charset.defaultCharset());
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to save " + kubernetesYaml + " after replacing template expressions " +e, e);
            }
        }
    }

    private Template findTemplate(KubernetesList resources) {
        return (Template) resources.getItems().stream()
          .filter(template -> template instanceof Template)
          .findFirst()
          .orElse(null);
    }

}
