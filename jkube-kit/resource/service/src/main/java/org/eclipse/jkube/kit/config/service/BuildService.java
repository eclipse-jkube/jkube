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
package org.eclipse.jkube.kit.config.service;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.build.core.JkubeBuildContext;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImagePullManager;
import org.eclipse.jkube.kit.build.service.docker.helper.Task;
import org.eclipse.jkube.kit.config.image.build.OpenShiftBuildStrategy;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;

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

        private org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext dockerBuildContext;

        private JkubeBuildContext dockerMojoParameters;

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

        public org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext getDockerBuildContext() {
            return dockerBuildContext;
        }

        public JkubeBuildContext getDockerMavenContext() {
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

            public Builder dockerBuildContext(org.eclipse.jkube.kit.build.service.docker.BuildService.BuildContext dockerBuildContext) {
                config.dockerBuildContext = dockerBuildContext;
                return this;
            }

            public Builder dockerMavenBuildContext(JkubeBuildContext dockerMojoParameters) {
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
