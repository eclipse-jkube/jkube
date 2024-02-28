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
package org.eclipse.jkube.generator.api.support;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.Generator;
import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.build.api.helper.DockerFileUtil;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.kit.common.util.GitUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.image.build.util.BuildLabelAnnotations;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

/**
 * @author roland
 */
public abstract class BaseGenerator implements Generator {

    private static final String LABEL_SCHEMA_VERSION = "1.0";
    private static final String GIT_REMOTE = "origin";

    private final GeneratorContext context;

    @Getter
    private final String name;
    private final GeneratorConfig config;
    protected final PrefixedLogger log;
    private final FromSelector fromSelector;

    @AllArgsConstructor
    enum Config implements Configs.Config {
        // The image name
        NAME("name", null),

        // The alias to use (default to the generator name)
        ALIAS("alias", null),

        // whether the generator should always add to already existing image configurations
        ADD("add", "false"),

        // Base image
        FROM("from", null),

        // Base image mode (only relevant for OpenShift)
        FROM_MODE("fromMode", null),

        // Optional registry
        REGISTRY("registry", null),

        // Tags
        TAGS("tags", null);

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    public BaseGenerator(GeneratorContext context, String name) {
        this(context, name, null);
    }

    public BaseGenerator(GeneratorContext context, String name, FromSelector fromSelector) {
        this.context = context;
        this.name = name;
        this.fromSelector = fromSelector;
        this.config = new GeneratorConfig(context.getProject().getProperties(), getName(), context.getConfig());
        this.log = new PrefixedLogger(name, context.getLogger());
    }

    protected JavaProject getProject() {
        return context.getProject();
    }


    public GeneratorContext getContext() {
        return context;
    }

    public GeneratorConfig getGeneratorConfig() {
        return config;
    }

    protected String getConfig(Configs.Config key) {
        return config.get(key);
    }

    protected String getConfig(Configs.Config key, String defaultVal) {
        return config.get(key, defaultVal);
    }

    protected String getConfigWithFallback(Config key, String fallbackPropertyKey, String defaultVal) {
        return config.getWithFallback(key, fallbackPropertyKey, defaultVal);
    }

    // Get 'from' as configured without any default and image stream tag handling
    protected String getFromAsConfigured() {
        return getConfigWithFallback(Config.FROM, "jkube.generator.from", null);
    }

    /**
     * Add the base image either from configuration or from a given selector
     *
     * @param builder for the build image configuration to add the from to.
     */
    protected void addFrom(BuildConfiguration.BuildConfigurationBuilder builder) {
        String fromMode = getConfigWithFallback(Config.FROM_MODE, "jkube.generator.fromMode", "docker");
        String from = getFromAsConfigured();
        if ("docker".equalsIgnoreCase(fromMode)) {
            String fromImage = from;
            if (fromImage == null) {
                fromImage = fromSelector != null ? fromSelector.getFrom() : null;
            }
            builder.from(fromImage);
            log.info("Using Docker image %s as base / builder", fromImage);
        } else if ("istag".equalsIgnoreCase(fromMode)) {
            Map<String, String> fromExt = new HashMap<>();
            if (from != null) {
                ImageName iName = new ImageName(from);
                // user/project is considered to be the namespace
                String tag = iName.getTag();
                if (StringUtils.isBlank(tag)) {
                    tag = "latest";
                }
                fromExt.put(JKubeBuildStrategy.SourceStrategy.name.key(), iName.getSimpleName() + ":" + tag);
                if (iName.inferUser() != null) {
                    fromExt.put(JKubeBuildStrategy.SourceStrategy.namespace.key(), iName.inferUser());
                }
                fromExt.put(JKubeBuildStrategy.SourceStrategy.kind.key(), "ImageStreamTag");
            } else {
                fromExt = fromSelector != null ? fromSelector.getImageStreamTagFromExt() : null;
            }
            if (fromExt != null) {
                String namespace = fromExt.get(JKubeBuildStrategy.SourceStrategy.namespace.key());
                if (namespace != null) {
                    log.info("Using ImageStreamTag '%s' from namespace '%s' as builder image",
                             fromExt.get(JKubeBuildStrategy.SourceStrategy.name.key()), namespace);
                } else {
                    log.info("Using ImageStreamTag '%s' as builder image",
                             fromExt.get(JKubeBuildStrategy.SourceStrategy.name.key()));
                }
                builder.fromExt(fromExt);
            }
        } else {
            throw new IllegalArgumentException(String.format("Invalid 'fromMode' in generator configuration for '%s'", getName()));
        }
    }

    /**
     * Get Image name with a standard default
     *
     * @return Docker image name which is never null
     */
    protected String getImageName() {
        if (getContext().getRuntimeMode() == RuntimeMode.OPENSHIFT) {
            return getConfigWithFallback(Config.NAME, "jkube.generator.name", "%a:%l");
        } else {
            return getConfigWithFallback(Config.NAME, "jkube.generator.name", "%g/%a:%l");
        }
    }

    /**
     * Get the docker registry where the image should be located.
     * It returns null in OpenShift mode.
     *
     * @return The docker registry if configured
     */
    protected String getRegistry() {
        if (getContext().getRuntimeMode() == RuntimeMode.OPENSHIFT &&
            getContext().getStrategy() == JKubeBuildStrategy.s2i) {
            return null;
        }
        return getConfigWithFallback(Config.REGISTRY, "jkube.generator.registry", null);
    }

    /**
     * Get alias name with the generator name as default
     * @return an alias which is never null;
     */
    protected String getAlias() {
        return getConfigWithFallback(Config.ALIAS, "jkube.generator.alias", getName());
    }

    protected boolean shouldAddGeneratedImageConfiguration(List<ImageConfiguration> configs) {
        if (getProject() != null && getProject().getBaseDirectory() != null && getProject().getBaseDirectory().exists()
              && DockerFileUtil.isSimpleDockerFileMode(getContext().getProject().getBaseDirectory())) {
            return false;
        }
        if (containsBuildConfiguration(configs)) {
            return Boolean.parseBoolean(getConfigWithFallback(Config.ADD, "jkube.generator.add", "false"));
        }
        return true;
    }

    protected void addLatestTagIfSnapshot(BuildConfiguration.BuildConfigurationBuilder buildBuilder) {
        if (getProject().getVersion().endsWith("-SNAPSHOT")) {
            buildBuilder.tags(Collections.singletonList("latest"));
        }
    }

    protected void addTagsFromConfig(BuildConfiguration.BuildConfigurationBuilder buildConfigurationBuilder) {
        String commaSeparatedTags = getConfigWithFallback(Config.TAGS, "jkube.generator.tags", null);
        if (StringUtils.isNotBlank(commaSeparatedTags)) {
            List<String> tags = Arrays.stream(commaSeparatedTags.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
            buildConfigurationBuilder.tags(tags);
        }
    }

    private boolean containsBuildConfiguration(List<ImageConfiguration> configs) {
        for (ImageConfiguration imageConfig : configs) {
            if (imageConfig.getBuildConfiguration() != null) {
                return true;
            }
        }
        return false;
    }

    protected void addSchemaLabels(BuildConfiguration.BuildConfigurationBuilder buildBuilder, PrefixedLogger log) {
        final JavaProject project = getProject();
        String docURL = project.getDocumentationUrl();
        Map<String, String> labels = new HashMap<>();

        labels.put(BuildLabelAnnotations.BUILD_DATE.value(), getProject().getBuildDate().format(DateTimeFormatter.ISO_DATE));
        labels.put(BuildLabelAnnotations.NAME.value(), project.getName());
        labels.put(BuildLabelAnnotations.DESCRIPTION.value(), project.getDescription());
        if (docURL != null) {
            labels.put(BuildLabelAnnotations.USAGE.value(), docURL);
        }
        if (project.getSite() != null) {
            labels.put(BuildLabelAnnotations.URL.value(), project.getSite());
        }
        if (project.getOrganizationName() != null && !project.getOrganizationName().isEmpty()) {
            labels.put(BuildLabelAnnotations.VENDOR.value(), project.getOrganizationName());
        }
        labels.put(BuildLabelAnnotations.VERSION.value(), project.getVersion());
        labels.put(BuildLabelAnnotations.SCHEMA_VERSION.value(), LABEL_SCHEMA_VERSION);

        try {
            Repository repository = GitUtil.getGitRepository(project.getBaseDirectory());
            if (repository != null) {
                String commitID = GitUtil.getGitCommitId(repository);
                labels.put(BuildLabelAnnotations.VCS_REF.value(), commitID);
                String gitRemoteUrl = repository.getConfig().getString("remote", GIT_REMOTE, "url");
                if (gitRemoteUrl != null) {
                    labels.put(BuildLabelAnnotations.VCS_URL.value(), gitRemoteUrl);
                } else {
                    log.verbose("Could not detect any git remote");
                }
            }
        } catch (IOException | GitAPIException | NullPointerException e) {
            log.error("Cannot extract Git information: " + e, e);
        } finally {
            buildBuilder.labels(labels);
        }
    }

}
