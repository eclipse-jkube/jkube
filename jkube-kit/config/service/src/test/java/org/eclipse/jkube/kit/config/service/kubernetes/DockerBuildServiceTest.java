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
package org.eclipse.jkube.kit.config.service.kubernetes;

import mockit.Expectations;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImagePullManager;
import org.eclipse.jkube.kit.build.service.docker.helper.AutoPullMode;
import org.eclipse.jkube.kit.config.image.build.ImagePullPolicy;
import mockit.Mocked;
import mockit.VerificationsInOrder;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.junit.Test;

public class DockerBuildServiceTest {

    @Mocked
    private JKubeServiceHub jKubeServiceHub;

    @Test
    public void testSuccessfulBuild() throws Exception {

        final BuildServiceConfig config = BuildServiceConfig.builder()
                .imagePullManager(new ImagePullManager(new TestCacheStore(), ImagePullPolicy.Always.name(), AutoPullMode.ALWAYS.name()))
                .build();
        // @formatter:off
        new Expectations() {{
           jKubeServiceHub.getBuildServiceConfig();
           result = config;
        }};
        // @formatter:on

        final String imageName = "image-name";
        final ImageConfiguration image = ImageConfiguration.builder()
                .name(imageName)
                .build(BuildConfiguration.builder()
                        .from("from")
                        .build()
                ).build();

        new DockerBuildService().build(jKubeServiceHub, image);

        new VerificationsInOrder() {{
            jKubeServiceHub.getDockerServiceHub().getBuildService()
                .buildImage(image, config.getImagePullManager(), jKubeServiceHub.getConfiguration());
            jKubeServiceHub.getDockerServiceHub().getBuildService().tagImage(imageName, image);
        }};
    }

    private static class TestCacheStore implements ImagePullManager.CacheStore {

        String cache;

        @Override
        public String get(String key) {
            return cache;
        }

        @Override
        public void put(String key, String value) {
            cache = value;
        }
    }
}
