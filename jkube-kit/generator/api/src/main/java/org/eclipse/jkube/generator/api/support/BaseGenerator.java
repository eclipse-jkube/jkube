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
package org.eclipse.jkube.generator.api.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.Generator;
import org.eclipse.jkube.generator.api.GeneratorConfig;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;

/**
 * @author roland
 * @since 15/05/16
 */
abstract public class BaseGenerator implements Generator {

    private final GeneratorContext context;
    private final String name;
    private final GeneratorConfig config;
    protected final PrefixedLogger log;
    private final FromSelector fromSelector;


    private enum Config implements Configs.Key {
        // The image name
        name,

        // The alias to use (default to the generator name)
        alias,

        // whether the generator should always add to already existing image configurationws
        add {{d = "false"; }},

        // Base image
        from,

        // Base image mode (only relevant for OpenShift)
        fromMode,

        // Optional registry
        registry;

        public String def() { return d; } protected String d;
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

    protected MavenProject getProject() {
        return context.getProject();
    }

    public String getName() {
        return name;
    }

    public GeneratorContext getContext() {
        return context;
    }

    protected String getConfig(Configs.Key key) {
        return config.get(key);
    }

    protected String getConfig(Configs.Key key, String defaultVal) {
        return config.get(key, defaultVal);
    }

    // Get 'from' as configured without any default and image stream tag handling
    protected String getFromAsConfigured() {
        return getConfigWithFallback(Config.from, "jkube.generator.from", null);
    }

    /**
     * Add the base image either from configuration or from a given selector
     *
     * @param builder for the build image configuration to add the from to.
     */
    protected void addFrom(BuildConfiguration.Builder builder) {
        String fromMode = getConfigWithFallback(Config.fromMode, "jkube.generator.fromMode", getFromModeDefault(context.getRuntimeMode()));
        String from = getConfigWithFallback(Config.from, "jkube.generator.from", null);
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
                fromExt.put(OpenShiftBuildStrategy.SourceStrategy.name.key(), iName.getSimpleName() + ":" + tag);
                if (iName.getUser() != null) {
                    fromExt.put(OpenShiftBuildStrategy.SourceStrategy.namespace.key(), iName.getUser());
                }
                fromExt.put(OpenShiftBuildStrategy.SourceStrategy.kind.key(), "ImageStreamTag");
            } else {
                fromExt = fromSelector != null ? fromSelector.getImageStreamTagFromExt() : null;
            }
            if (fromExt != null) {
                String namespace = fromExt.get(OpenShiftBuildStrategy.SourceStrategy.namespace.key());
                if (namespace != null) {
                    log.info("Using ImageStreamTag '%s' from namespace '%s' as builder image",
                             fromExt.get(OpenShiftBuildStrategy.SourceStrategy.name.key()), namespace);
                } else {
                    log.info("Using ImageStreamTag '%s' as builder image",
                             fromExt.get(OpenShiftBuildStrategy.SourceStrategy.name.key()));
                }
                builder.fromExt(fromExt);
            }
        } else {
            throw new IllegalArgumentException(String.format("Invalid 'fromMode' in generator configuration for '%s'", getName()));
        }
    }

    // Use "istag" as default for "redhat" versions of this plugin
    private String getFromModeDefault(RuntimeMode mode) {
        if (mode == RuntimeMode.openshift && fromSelector != null && fromSelector.isRedHat()) {
            return "istag";
        } else {
            return "docker";
        }
    }

    /**
     * Get Image name with a standard default
     *
     * @return Docker image name which is never null
     */
    protected String getImageName() {
        if (RuntimeMode.isOpenShiftMode(getProject().getProperties())) {
            return getConfigWithFallback(Config.name, "jkube.generator.name", "%a:%l");
        } else {
            return getConfigWithFallback(Config.name, "jkube.generator.name", "%g/%a:%l");
        }
    }

    /**
     * Get the docker registry where the image should be located.
     * It returns null in Openshift mode.
     *
     * @return The docker registry if configured
     */
    protected String getRegistry() {
        if (!RuntimeMode.isOpenShiftMode(getProject().getProperties())) {
            return getConfigWithFallback(Config.registry, "jkube.generator.registry", null);
        }

        return null;
    }

    /**
     * Get alias name with the generator name as default
     * @return an alias which is never null;
     */
    protected String getAlias() {
        return getConfigWithFallback(Config.alias, "jkube.generator.alias", getName());
    }

    protected boolean shouldAddImageConfiguration(List<ImageConfiguration> configs) {
        return !containsBuildConfiguration(configs) || Configs.asBoolean(getConfig(Config.add));
    }

    protected String getConfigWithFallback(Config name, String key, String defaultVal) {
        String value = getConfig(name);
        if (value == null) {
            value = Configs.getSystemPropertyWithMavenPropertyAsFallback(getProject().getProperties(), key);
        }
        return value != null ? value : defaultVal;
    }

    protected void addLatestTagIfSnapshot(BuildConfiguration.Builder buildBuilder) {
        MavenProject project = getProject();
        if (project.getVersion().endsWith("-SNAPSHOT")) {
            buildBuilder.tags(Collections.singletonList("latest"));
        }
    }

    private boolean containsBuildConfiguration(List<ImageConfiguration> configs) {
        for (ImageConfiguration config : configs) {
            if (config.getBuildConfiguration() != null) {
                return true;
            }
        }
        return false;
    }
}
