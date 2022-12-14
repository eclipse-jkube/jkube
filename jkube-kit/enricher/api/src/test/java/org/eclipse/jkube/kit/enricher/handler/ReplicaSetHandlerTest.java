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
package org.eclipse.jkube.kit.enricher.handler;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class ReplicaSetHandlerTest {

    private List<VolumeConfig> volumes;
    private List<ImageConfiguration> images;
    private ReplicaSetHandler replicaSetHandler;

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

        replicaSetHandler = new ReplicaSetHandler(new PodTemplateHandler(new ContainerHandler(new Properties(),
            new GroupArtifactVersion("g","a","v"), mock(ProbeHandler.class))));
    }

    @Test
    void get_withValidControllerName_shouldReturnConfigsWithContainers() {
        ControllerResourceConfig config = ControllerResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .replicas(5)
                .volumes(volumes)
                .build();

        ReplicaSet replicaSet = replicaSetHandler.get(config,images);
        assertThat(replicaSet.getSpec().getTemplate().getSpec().getContainers()).isNotNull();
        assertThat(replicaSet)
            .satisfies(rs -> assertThat(rs.getMetadata())
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "testing")
            )
            .satisfies(rs -> assertThat(rs.getSpec())
                .isNotNull()
                .hasFieldOrPropertyWithValue("replicas", 5)
                .extracting(ReplicaSetSpec::getTemplate).isNotNull()
                .extracting(PodTemplateSpec::getSpec)
                .extracting(PodSpec::getVolumes).asList()
                .isNotEmpty()
                .first()
                .hasFieldOrPropertyWithValue("hostPath.path", "/test/path")
            );
    }

    @Test
    void get_withInvalidName_shouldThrowException() {
        ControllerResourceConfig config = ControllerResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("TesTing")
                .replicas(5)
                .volumes(volumes)
                .build();
        assertThatIllegalArgumentException()
            .isThrownBy(() -> replicaSetHandler.get(config, images))
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
            .isThrownBy(() -> replicaSetHandler.get(config, images))
            .withMessage("No controller name is specified!");
    }

    @Test
    void overrideReplicas() {
        // Given
        final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(new ReplicaSetBuilder()
            .editOrNewSpec().withReplicas(1).endSpec()
            .build());
        // When
        replicaSetHandler.overrideReplicas(klb, 1337);
        // Then
        assertThat(klb.buildItems())
            .singleElement()
            .hasFieldOrPropertyWithValue("spec.replicas", 1337);
    }
}
