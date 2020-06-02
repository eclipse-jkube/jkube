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
package org.eclipse.jkube.generator.api;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import static org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy.SourceStrategy.kind;
import static org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy.SourceStrategy.name;
import static org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy.SourceStrategy.namespace;


/**
 * Helper class to encapsulate the selection of a base image
 *
 * @author roland
 */
public abstract class FromSelector {

    private final GeneratorContext context;

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

    protected abstract String getDockerBuildFrom();
    protected abstract String getS2iBuildFrom();
    protected abstract String getIstagFrom();

    public static class Default extends FromSelector {

        private final String upstreamDocker;
        private final String upstreamS2i;
        private final String upstreamIstag;

        public Default(GeneratorContext context, String prefix) {
            super(context);
            DefaultImageLookup lookup = new DefaultImageLookup(Default.class);

            this.upstreamDocker = lookup.getImageName(prefix + ".upstream.docker");
            this.upstreamS2i = lookup.getImageName(prefix + ".upstream.s2i");
            this.upstreamIstag = lookup.getImageName(prefix + ".upstream.istag");
        }

        @Override
        protected String getDockerBuildFrom() {
            return upstreamDocker;
        }

        @Override
        protected String getS2iBuildFrom() {
            return upstreamS2i;
        }

        protected String getIstagFrom() {
            return upstreamIstag;
        }
    }
}
