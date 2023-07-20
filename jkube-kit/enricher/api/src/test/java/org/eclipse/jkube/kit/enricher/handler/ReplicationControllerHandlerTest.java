/*
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
package org.eclipse.jkube.kit.enricher.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class ReplicationControllerHandlerTest {

    private List<VolumeConfig> volumes;
    private List<ImageConfiguration> images;
    private ReplicationControllerHandler replicationControllerHandler;

    @BeforeEach
    void setUp(){
        volumes = new ArrayList<>();
        images = new ArrayList<>();
        List<String> mounts = new ArrayList<>();
        List<String> ports = new ArrayList<>();
        List<String> tags = new ArrayList<>();

        //volume config with name and multiple mount
        mounts.add("/path/system");
        mounts.add("/path/sys");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        VolumeConfig volumeConfig = VolumeConfig.builder()
                .name("test").mounts(mounts).type("hostPath").path("/test/path").build();
        volumes.add(volumeConfig);

        //container name with alias
        final BuildConfiguration buildImageConfiguration = BuildConfiguration.builder()
                .ports(ports).from("fabric8/maven:latest").cleanup("try")
                .tags(tags).compressionString("gzip").build();

        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                .name("test").alias("test-app").build(buildImageConfiguration)
                .registry("docker.io").build();
        images.add(imageConfiguration);

        replicationControllerHandler = new ReplicationControllerHandler(new PodTemplateHandler(new ContainerHandler(
            new Properties(), new GroupArtifactVersion("g","a","v"), mock(ProbeHandler.class))));
    }

    @Test
    void get_withValidControllerName_shouldReturnConfigWithContainers() {
        ControllerResourceConfig config = ControllerResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .replicas(5)
                .volumes(volumes)
                .build();

        ReplicationController replicationController = replicationControllerHandler.get(config,images);
        assertThat(replicationController.getSpec().getTemplate().getSpec().getContainers()).isNotNull();
        assertThat(replicationController)
            .satisfies(rc -> assertThat(rc.getMetadata())
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "testing")
            )
            .satisfies(rc -> assertThat(rc.getSpec())
                .isNotNull()
                .hasFieldOrPropertyWithValue("replicas", 5)
                .extracting(ReplicationControllerSpec::getTemplate).isNotNull()
                .extracting(PodTemplateSpec::getSpec)
                .extracting(PodSpec::getVolumes).asList()
                .isNotEmpty()
                .first()
                .hasFieldOrPropertyWithValue("name", "test")
                .hasFieldOrPropertyWithValue("hostPath.path", "/test/path")
            );
    }

    @Test
    void get_withInvalidControllerName_shouldThrowException() {
        ControllerResourceConfig config = ControllerResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("TesTing")
                .replicas(5)
                .volumes(volumes)
                .build();
        assertThatIllegalArgumentException()
            .isThrownBy(() -> replicationControllerHandler.get(config, images))
            .withMessageStartingWith("Invalid upper case letter 'T'")
            .withMessageEndingWith("controller name value: TesTing");
    }

    @Test
    void get_withoutControllerName_shouldThrowException() {
        ControllerResourceConfig config = ControllerResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .replicas(5)
                .volumes(volumes)
                .build();
        assertThatIllegalArgumentException()
            .isThrownBy(() -> replicationControllerHandler.get(config, images))
            .withMessage("No replication controller name is specified!");
    }

    @Test
    void overrideReplicas() {
        // Given
        final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(new ReplicationControllerBuilder()
            .editOrNewSpec().withReplicas(1).endSpec()
            .build());
        // When
        replicationControllerHandler.overrideReplicas(klb, 1337);
        // Then
        assertThat(klb.buildItems())
            .singleElement()
            .hasFieldOrPropertyWithValue("spec.replicas", 1337);
    }
}
