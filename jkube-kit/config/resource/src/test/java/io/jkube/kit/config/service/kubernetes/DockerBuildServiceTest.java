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
package io.jkube.kit.config.service.kubernetes;

import io.jkube.kit.build.service.docker.BuildService;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.build.service.docker.ImagePullManager;
import io.jkube.kit.build.service.docker.ServiceHub;
import io.jkube.kit.build.service.docker.helper.AutoPullMode;
import io.jkube.kit.config.image.build.BuildConfiguration;
import io.jkube.kit.config.image.build.ImagePullPolicy;
import mockit.Expectations;
import mockit.Mocked;
import mockit.VerificationsInOrder;
import org.junit.Test;

public class DockerBuildServiceTest {

    @Mocked
    private ServiceHub hub;

    @Mocked
    private BuildService buildService;

    @Test
    public void testSuccessfulBuild() throws Exception {

        final BuildService.BuildContext context = new BuildService.BuildContext.Builder()
                .build();

        final io.jkube.kit.config.service.BuildService.BuildServiceConfig config = new io.jkube.kit.config.service.BuildService.BuildServiceConfig.Builder()
                .dockerBuildContext(context)
                .imagePullManager(new ImagePullManager(new TestCacheStore(), ImagePullPolicy.Always.name(), AutoPullMode.ALWAYS.name()))
                .build();

        final String imageName = "image-name";
        final ImageConfiguration image = new ImageConfiguration.Builder()
                .name(imageName)
                .buildConfig(new BuildConfiguration.Builder()
                        .from("from")
                        .build()
                ).build();

        DockerBuildService service = new DockerBuildService(hub, config);
        service.build(image);

        new VerificationsInOrder() {{
            buildService.buildImage(image, config.getImagePullManager(), context);
            buildService.tagImage(imageName, image);
        }};
    }

    private class TestCacheStore implements ImagePullManager.CacheStore {

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
