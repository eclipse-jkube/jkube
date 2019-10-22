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
package io.jkube.generator.api;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import io.jkube.kit.config.resource.RuntimeMode;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

import static io.jkube.kit.config.image.build.OpenShiftBuildStrategy.SourceStrategy.kind;
import static io.jkube.kit.config.image.build.OpenShiftBuildStrategy.SourceStrategy.name;
import static io.jkube.kit.config.image.build.OpenShiftBuildStrategy.SourceStrategy.namespace;


/**
 * Helper class to encapsulate the selection of a base image
 *
 * @author roland
 * @since 12/08/16
 */
public abstract class FromSelector {

    private final GeneratorContext context;

    private final Pattern REDHAT_VERSION_PATTERN = Pattern.compile("^.*\\.(redhat|fuse)-.*$");

    public FromSelector(GeneratorContext context) {
        this.context = context;
    }

    public String getFrom() {
        RuntimeMode mode = context.getRuntimeMode();
        OpenShiftBuildStrategy strategy = context.getStrategy();
        if (mode == RuntimeMode.openshift && strategy == OpenShiftBuildStrategy.s2i) {
            return getS2iBuildFrom();
        } else {
            return getDockerBuildFrom();
        }
    }

    public Map<String, String> getImageStreamTagFromExt() {
        Map<String, String> ret = new HashMap<>();
        ret.put(kind.key(), "ImageStreamTag");
        ret.put(namespace.key(), "openshift");
        ret.put(name.key(), getIstagFrom());
        return ret;
    }

    abstract protected String getDockerBuildFrom();
    abstract protected String getS2iBuildFrom();
    abstract protected String getIstagFrom();

    public boolean isRedHat() {
        MavenProject project = context.getProject();
        Plugin plugin = project.getPlugin("io.jkube:jkube-maven-plugin");
        if (plugin == null) {
            // This plugin might be repackaged.
            plugin = project.getPlugin("org.jboss.redhat-fuse:jkube-maven-plugin");
        }
        if (plugin == null) {
            // Can happen if not configured in a build section but only in a dependency management section
            return false;
        }
        String version = plugin.getVersion();
        return REDHAT_VERSION_PATTERN.matcher(version).matches();
    }

    public static class Default extends FromSelector {

        private final String upstreamDocker;
        private final String upstreamS2i;
        private final String redhatDocker;
        private final String redhatS2i;
        private final String redhatIstag;
        private final String upstreamIstag;

        public Default(GeneratorContext context, String prefix) {
            super(context);
            DefaultImageLookup lookup = new DefaultImageLookup(Default.class);

            this.upstreamDocker = lookup.getImageName(prefix + ".upstream.docker");
            this.upstreamS2i = lookup.getImageName(prefix + ".upstream.s2i");
            this.upstreamIstag = lookup.getImageName(prefix + ".upstream.istag");

            this.redhatDocker = lookup.getImageName(prefix + ".redhat.docker");
            this.redhatS2i = lookup.getImageName(prefix + ".redhat.s2i");
            this.redhatIstag = lookup.getImageName(prefix + ".redhat.istag");
        }

        @Override
        protected String getDockerBuildFrom() {
            return isRedHat() ? redhatDocker : upstreamDocker;
        }

        @Override
        protected String getS2iBuildFrom() {
            return isRedHat() ? redhatS2i : upstreamS2i;
        }

        protected String getIstagFrom() {
            return isRedHat() ? redhatIstag : upstreamIstag;
        }
    }
}
