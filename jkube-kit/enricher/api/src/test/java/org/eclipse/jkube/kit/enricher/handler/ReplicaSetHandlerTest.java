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
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ReplicaSetHandlerTest {

    ProbeHandler probeHandler;

    JavaProject project;

    List<String> mounts = new ArrayList<>();
    List<VolumeConfig> volumes1 = new ArrayList<>();

    List<ImageConfiguration> images = new ArrayList<>();

    List<String> ports = new ArrayList<>();

    List<String> tags = new ArrayList<>();

    private ReplicaSetHandler replicaSetHandler;

    @Before
    public void before(){
        probeHandler = mock(ProbeHandler.class);
        project = mock(JavaProject.class);
        //volume config with name and multiple mount
        mounts.add("/path/system");
        mounts.add("/path/sys");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        VolumeConfig volumeConfig1 = VolumeConfig.builder()
                .name("test").mounts(mounts).type("hostPath").path("/test/path").build();
        volumes1.add(volumeConfig1);

        //container name with alias
        final BuildConfiguration buildImageConfiguration = BuildConfiguration.builder()
                .ports(ports).from("fabric8/maven:latest").cleanup("try")
                .tags(tags).compressionString("gzip").build();

        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                .name("test").alias("test-app").build(buildImageConfiguration)
                .registry("docker.io").build();

        images.add(imageConfiguration);

        replicaSetHandler = new ReplicaSetHandler(new PodTemplateHandler(new ContainerHandler(project.getProperties(),
            new GroupArtifactVersion("g","a","v"), probeHandler)));
    }

    @Test
    public void replicaSetHandlerTest() {
        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .serviceAccount("test-account")
                .replicas(5)
                .volumes(volumes1)
                .build();

        ReplicaSet replicaSet = replicaSetHandler.get(config,images);

        //Assertion
        assertThat(replicaSet.getSpec()).isNotNull();
        assertThat(replicaSet.getMetadata()).isNotNull();
        assertThat(replicaSet.getSpec().getReplicas().intValue()).isEqualTo(5);
        assertThat(replicaSet.getSpec().getTemplate()).isNotNull();
        assertThat(replicaSet.getMetadata().getName()).isEqualTo("testing");
        assertThat(replicaSet.getSpec().getTemplate()
                .getSpec().getServiceAccountName()).isEqualTo("test-account");
        assertThat(replicaSet.getSpec().getTemplate().getSpec().getVolumes()).isNotEmpty();
        assertThat(replicaSet.getSpec().getTemplate().getSpec().
                getVolumes().get(0).getName()).isEqualTo("test");
        assertThat(replicaSet.getSpec().getTemplate()
                .getSpec().getVolumes().get(0).getHostPath().getPath()).isEqualTo("/test/path");
        assertThat(replicaSet.getSpec().getTemplate().getSpec().getContainers()).isNotNull();

    }

    @Test(expected = IllegalArgumentException.class)
    public void replicaSetHandlerWithInvalidNameTest() {
        // with invalid controller name
        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("TesTing")
                .serviceAccount("test-account")
                .replicas(5)
                .volumes(volumes1)
                .build();

        replicaSetHandler.get(config, images);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replicaSetHandlerWithoutControllerTest() {
        // without controller name
        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .serviceAccount("test-account")
                .replicas(5)
                .volumes(volumes1)
                .build();

        replicaSetHandler.get(config, images);
    }

    @Test
    public void overrideReplicas() {
        // Given
        final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(new ReplicaSetBuilder()
            .editOrNewSpec().withReplicas(1).endSpec()
            .build());
        // When
        replicaSetHandler.overrideReplicas(klb, 1337);
        // Then
        assertThat(klb.buildItems())
            .hasSize(1)
            .first().hasFieldOrPropertyWithValue("spec.replicas", 1337);
    }
}
