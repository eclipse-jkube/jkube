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
import mockit.Mocked;
import org.eclipse.jkube.kit.build.service.docker.ImagePullManager;
import org.eclipse.jkube.kit.build.service.docker.helper.Task;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.resource.BuildRecreateMode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BuildServiceConfigTest {
    @Mocked
    private ImagePullManager imagePullManager;

    @Test
    public void testGetBuildServiceConfigBuilder() {
        // Given
        BuildRecreateMode buildRecreateMode = BuildRecreateMode.all;
        JKubeBuildStrategy jKubeBuildStrategy = JKubeBuildStrategy.docker;
        BuildServiceConfig.Attacher buildSvcConfigAttacher = (classifier, destFile) -> {};
        Task<KubernetesListBuilder> enricherTask = object -> {};
        String buildDir = "target";
        boolean forcePull = false;

        // When
        BuildServiceConfig buildServiceConfig = BuildServiceConfig.getBuildServiceConfigBuilder(buildRecreateMode, jKubeBuildStrategy, forcePull, imagePullManager, buildDir, buildSvcConfigAttacher, enricherTask, null, null).build();

        // Then
        assertNotNull(buildServiceConfig);
        assertEquals(JKubeBuildStrategy.docker, buildServiceConfig.getJKubeBuildStrategy());
        assertEquals("target", buildServiceConfig.getBuildDirectory());
        assertEquals(BuildRecreateMode.all, buildServiceConfig.getBuildRecreateMode());
    }
}
