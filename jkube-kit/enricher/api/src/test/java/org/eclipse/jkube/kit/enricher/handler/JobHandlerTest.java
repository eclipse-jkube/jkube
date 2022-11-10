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

import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

public class JobHandlerTest {


    ProbeHandler probeHandler;


    JavaProject project;

    List<String> mounts = new ArrayList<>();
    List<VolumeConfig> volumes1 = new ArrayList<>();

    List<ImageConfiguration> images = new ArrayList<>();

    List<String> ports = new ArrayList<>();

    List<String> tags = new ArrayList<>();

    private JobHandler jobHandler;

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
        assertThat(job)
        .hasFieldOrPropertyWithValue("metadata.name", "testing")
        .satisfies(d -> assertThat(d.getSpec().getTemplate().getSpec().getContainers()).isNotNull())
        .hasFieldOrPropertyWithValue("spec.template.spec.serviceAccountName", "test-account")
        .extracting(Job::getSpec)
        .extracting(JobSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getVolumes)
        .asList()
        .first(InstanceOfAssertFactories.type(Volume.class))
        .hasFieldOrPropertyWithValue("name", "test")
        .hasFieldOrPropertyWithValue("hostPath.path", "/test/path");

    }

    @Test
    public void daemonTemplateHandlerWithInvalidNameTest() {
        // with invalid controller name
      ResourceConfig config = ResourceConfig.builder()
              .imagePullPolicy("IfNotPresent")
              .controllerName("TesTing")
              .serviceAccount("test-account")
              .volumes(volumes1)
              .build();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> jobHandler.get(config, images));
    }

    @Test
    public void daemonTemplateHandlerWithoutControllerTest() {
        // without controller name
      ResourceConfig config = ResourceConfig.builder()
              .imagePullPolicy("IfNotPresent")
              .serviceAccount("test-account")
              .volumes(volumes1)
              .build();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> jobHandler.get(config, images));
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
