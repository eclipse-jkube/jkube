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
package io.jkube.kit.config.service;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.jkube.kit.build.maven.MavenBuildContext;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.build.service.docker.ImagePullManager;
import io.jkube.kit.build.service.docker.helper.Task;
import io.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import io.jkube.kit.config.resource.BuildRecreateMode;

import java.io.File;

/**
 * @author nicola
 * @since 17/02/2017
 */
public interface BuildService {

    /**
     * Builds the given image using the specified configuration.
     *
     * @param imageConfig the image to build
     */
    void build(ImageConfiguration imageConfig) throws JkubeServiceException;

    /**
     * Post processing step called after all images has been build
     * @param config build configuration
     */
    void postProcess(BuildServiceConfig config);

    /**
     * Class to hold configuration parameters for the building service.
     */
    class BuildServiceConfig {

        private io.jkube.kit.build.service.docker.BuildService.BuildContext dockerBuildContext;

        private MavenBuildContext dockerMojoParameters;

        private BuildRecreateMode buildRecreateMode;

        private OpenShiftBuildStrategy openshiftBuildStrategy;

        private boolean forcePull;

        private String s2iBuildNameSuffix;

        private String openshiftPullSecret;

        private Task<KubernetesListBuilder> enricherTask;

        private String buildDirectory;

        private Attacher attacher;

        private ImagePullManager imagePullManager;

        private boolean s2iImageStreamLookupPolicyLocal;

        public BuildServiceConfig() {
        }

        public io.jkube.kit.build.service.docker.BuildService.BuildContext getDockerBuildContext() {
            return dockerBuildContext;
        }

        public MavenBuildContext getDockerMavenContext() {
            return dockerMojoParameters;
        }

        public BuildRecreateMode getBuildRecreateMode() {
            return buildRecreateMode;
        }

        public OpenShiftBuildStrategy getOpenshiftBuildStrategy() {
            return openshiftBuildStrategy;
        }

        public String getS2iBuildNameSuffix() {
            return s2iBuildNameSuffix;
        }

        public String getOpenshiftPullSecret() {
            return openshiftPullSecret;
        }

        public Task<KubernetesListBuilder> getEnricherTask() {
            return enricherTask;
        }

        public String getBuildDirectory() {
            return buildDirectory;
        }

        public Object getArtifactId() {
            return dockerMojoParameters.getProject().getArtifactId();
        }

        public ImagePullManager getImagePullManager() { return imagePullManager; }

        public boolean isS2iImageStreamLookupPolicyLocal() {
            return s2iImageStreamLookupPolicyLocal;
        }

        public boolean isForcePullEnabled() {
            return forcePull;
        }

        public void attachArtifact(String classifier, File destFile) {
            if (attacher != null) {
                attacher.attach(classifier, destFile);
            }
        }

        public static class Builder {
            private BuildServiceConfig config;

            public Builder() {
                this.config = new BuildServiceConfig();
            }

            public Builder(BuildServiceConfig config) {
                this.config = config;
            }

            public Builder dockerBuildContext(io.jkube.kit.build.service.docker.BuildService.BuildContext dockerBuildContext) {
                config.dockerBuildContext = dockerBuildContext;
                return this;
            }

            public Builder dockerMavenBuildContext(MavenBuildContext dockerMojoParameters) {
                config.dockerMojoParameters = dockerMojoParameters;
                return this;
            }

            public Builder buildRecreateMode(BuildRecreateMode buildRecreateMode) {
                config.buildRecreateMode = buildRecreateMode;
                return this;
            }

            public Builder openshiftBuildStrategy(OpenShiftBuildStrategy openshiftBuildStrategy) {
                config.openshiftBuildStrategy = openshiftBuildStrategy;
                return this;
            }

            public Builder forcePullEnabled(boolean forcePull) {
                config.forcePull = forcePull;
                return this;
            }

            public Builder s2iBuildNameSuffix(String s2iBuildNameSuffix) {
                config.s2iBuildNameSuffix = s2iBuildNameSuffix;
                return this;
            }

            public Builder openshiftPullSecret(String openshiftPullSecret) {
                config.openshiftPullSecret = openshiftPullSecret;
                return this;
            }

            public Builder s2iImageStreamLookupPolicyLocal(boolean s2iImageStreamLookupPolicyLocal) {
                config.s2iImageStreamLookupPolicyLocal = s2iImageStreamLookupPolicyLocal;
                return this;
            }

            public Builder enricherTask(Task<KubernetesListBuilder> enricherTask) {
                config.enricherTask = enricherTask;
                return this;
            }

            public Builder buildDirectory(String buildDirectory) {
                config.buildDirectory = buildDirectory;
                return this;
            }

            public Builder attacher(Attacher attacher) {
                config.attacher = attacher;
                return this;
            }

            public Builder imagePullManager(ImagePullManager imagePullManager) {
                config.imagePullManager = imagePullManager;
                return this;
            }

            public BuildServiceConfig build() {
                return config;
            }

        }

        // Delegate for attaching stuff
        public interface Attacher {
            void attach(String classifier, File destFile);
        }
    }

}
