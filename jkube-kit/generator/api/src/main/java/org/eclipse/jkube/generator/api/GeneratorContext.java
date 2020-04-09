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

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.service.ArtifactResolverService;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;

/**
 * @author roland
 * @since 15/05/16
 */
public class GeneratorContext {
    private JavaProject project;
    private ProcessorConfig config;
    private KitLogger logger;
    private RuntimeMode runtimeMode;
    private OpenShiftBuildStrategy strategy;

    private boolean useProjectClasspath;
    private boolean prePackagePhase;
    private ArtifactResolverService artifactResolver;

    private GeneratorMode generatorMode = GeneratorMode.BUILD;

    private GeneratorContext() {
    }

    public JavaProject getProject() {
        return project;
    }

    public ProcessorConfig getConfig() {
        return config;
    }

    public KitLogger getLogger() {
        return logger;
    }

    public RuntimeMode getRuntimeMode() {
        return runtimeMode;
    }

    public OpenShiftBuildStrategy getStrategy() {
        return strategy;
    }


    public GeneratorMode getGeneratorMode() {
        return generatorMode;
    }

    public ArtifactResolverService getArtifactResolver() {
        return artifactResolver;
    }

    public boolean isUseProjectClasspath() {
        return useProjectClasspath;
    }

    public boolean isPrePackagePhase() {
        return prePackagePhase;
    }

    // ========================================================================

    public static class Builder {

        private GeneratorContext ctx = new GeneratorContext();

        public Builder config(ProcessorConfig config) {
            ctx.config = config;
            return this;
        }

        public Builder project(JavaProject project) {
            ctx.project = project;
            return this;
        }

        public Builder generatorMode(GeneratorMode generatorMode) {
            ctx.generatorMode = generatorMode;
            return this;
        }

        public Builder logger(KitLogger logger) {
            ctx.logger = logger;
            return this;
        }

        public Builder runtimeMode(RuntimeMode mode) {
            ctx.runtimeMode = mode;
            return this;
        }

        public Builder strategy(OpenShiftBuildStrategy strategy) {
            ctx.strategy = strategy;
            return this;
        }

        public Builder useProjectClasspath(boolean useProjectClasspath) {
            ctx.useProjectClasspath = useProjectClasspath;
            return this;
        }

        public Builder prePackagePhase(boolean prePackagePhase) {
            ctx.prePackagePhase = prePackagePhase;
            return this;
        }

        public Builder artifactResolver(ArtifactResolverService artifactResolver) {
            ctx.artifactResolver = artifactResolver;
            return this;
        }

        public GeneratorContext build() {
            return ctx;
        }
    }
}
