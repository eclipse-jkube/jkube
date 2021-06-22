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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.jkube.kit.build.service.docker.ImagePullManager;
import org.eclipse.jkube.kit.build.service.docker.helper.Task;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;

import java.io.File;

/**
 * Class to hold configuration parameters for the building service.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class BuildServiceConfig {

    private BuildRecreateMode buildRecreateMode;
    private JKubeBuildStrategy jKubeBuildStrategy;
    private boolean forcePull;
    private String s2iBuildNameSuffix;
    private String openshiftPullSecret;
    private String openshiftPushSecret;
    private Task<KubernetesListBuilder> enricherTask;
    private String buildDirectory;
    private Attacher attacher;
    private ImagePullManager imagePullManager;
    private boolean s2iImageStreamLookupPolicyLocal;
    private ResourceConfig resourceConfig;
    private File resourceDir;
    private String buildOutputKind;

    public void attachArtifact(String classifier, File destFile) {
        if (attacher != null) {
            attacher.attach(classifier, destFile);
        }
    }

    public interface Attacher {
        void attach(String classifier, File destFile);
    }

    public static BuildServiceConfig.BuildServiceConfigBuilder getBuildServiceConfigBuilder(BuildRecreateMode buildRecreateMode, JKubeBuildStrategy jKubeBuildStrategy,
        boolean forcePull, ImagePullManager imagePullManager, String buildDirectory, BuildServiceConfig.Attacher buildServiceConfigAttacher,
        Task<KubernetesListBuilder> enricherTask, ResourceConfig resourceConfig, File resourceDir) {
        return BuildServiceConfig.builder()
                .buildRecreateMode(buildRecreateMode)
                .jKubeBuildStrategy(jKubeBuildStrategy)
                .forcePull(forcePull)
                .imagePullManager(imagePullManager)
                .buildDirectory(buildDirectory)
                .attacher(buildServiceConfigAttacher)
                .resourceConfig(resourceConfig)
                .resourceDir(resourceDir)
                .enricherTask(enricherTask);
    }
}
