/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jkube.generator.api;

import io.jkube.kit.common.KitLogger;
import io.jkube.kit.common.service.ArtifactResolverService;
import io.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import io.jkube.kit.config.resource.RuntimeMode;
import io.jkube.kit.config.resource.ProcessorConfig;
import org.apache.maven.project.MavenProject;

/**
 * @author roland
 * @since 15/05/16
 */
public class GeneratorContext {
    private MavenProject project;
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

    public MavenProject getProject() {
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

        public Builder project(MavenProject project) {
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
