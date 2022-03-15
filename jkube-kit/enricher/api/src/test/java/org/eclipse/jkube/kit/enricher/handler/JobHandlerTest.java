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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThrows;

public class JobHandlerTest {

    @Mocked
    ProbeHandler probeHandler;

    @Mocked
    JavaProject project;

    List<String> mounts = new ArrayList<>();
    List<VolumeConfig> volumes1 = new ArrayList<>();

    List<ImageConfiguration> images = new ArrayList<>();

    List<String> ports = new ArrayList<>();

    List<String> tags = new ArrayList<>();

    private JobHandler jobHandler;

    @Before
    public void before(){

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

        jobHandler = new JobHandler(new PodTemplateHandler(new ContainerHandler(project.getProperties(),
            new GroupArtifactVersion("g","a","v"), probeHandler)));
    }

    @Test
    public void jobHandlerTest() {
        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .serviceAccount("test-account")
                .restartPolicy("OnFailure")
                .volumes(volumes1)
                .build();

        Job job = jobHandler.get(config,images);

        //Assertion
        assertNotNull(job.getSpec());
        assertNotNull(job.getMetadata());
        assertNotNull(job.getSpec().getTemplate());
        assertEquals("testing",job.getMetadata().getName());
        assertEquals("test-account",job.getSpec().getTemplate()
                .getSpec().getServiceAccountName());
        assertFalse(job.getSpec().getTemplate().getSpec().getVolumes().isEmpty());
        assertEquals("OnFailure", job.getSpec().getTemplate().getSpec().getRestartPolicy());
        assertEquals("test",job.getSpec().getTemplate().getSpec().
                getVolumes().get(0).getName());
        assertEquals("/test/path",job.getSpec().getTemplate()
                .getSpec().getVolumes().get(0).getHostPath().getPath());
        assertNotNull(job.getSpec().getTemplate().getSpec().getContainers());

    }

    @Test
    public void daemonTemplateHandlerWithInvalidNameTest() {
        // with invalid controller name
        assertThrows(IllegalArgumentException.class, () -> {
            ResourceConfig config = ResourceConfig.builder()
                    .imagePullPolicy("IfNotPresent")
                    .controllerName("TesTing")
                    .serviceAccount("test-account")
                    .volumes(volumes1)
                    .build();

            jobHandler.get(config, images);
        });
    }

    @Test
    public void daemonTemplateHandlerWithoutControllerTest() {
        // without controller name
        assertThrows(IllegalArgumentException.class, () -> {
            ResourceConfig config = ResourceConfig.builder()
                    .imagePullPolicy("IfNotPresent")
                    .serviceAccount("test-account")
                    .volumes(volumes1)
                    .build();

            jobHandler.get(config, images);
        });
    }

    @Test
    public void overrideReplicas() {
        // Given
        final KubernetesListBuilder klb = new KubernetesListBuilder().addToItems(new Job());
        // When
        jobHandler.overrideReplicas(klb, 1337);
        // Then
        assertThat(klb.buildItems())
            .hasSize(1)
            .first().isEqualTo(new Job());
    }
}
