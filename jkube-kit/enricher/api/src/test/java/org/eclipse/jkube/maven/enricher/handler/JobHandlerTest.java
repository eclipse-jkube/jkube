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
package org.eclipse.jkube.maven.enricher.handler;

import io.fabric8.kubernetes.api.model.batch.Job;
import org.eclipse.jkube.kit.build.core.config.JKubeBuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class JobHandlerTest {

    @Mocked
    ProbeHandler probeHandler;

    JKubeProject project = new JKubeProject();

    List<String> mounts = new ArrayList<>();
    List<VolumeConfig> volumes1 = new ArrayList<>();

    List<ImageConfiguration> images = new ArrayList<>();

    List<String> ports = new ArrayList<>();

    List<String> tags = new ArrayList<>();

    @Before
    public void before(){

        //volume config with name and multiple mount
        mounts.add("/path/system");
        mounts.add("/path/sys");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        VolumeConfig volumeConfig1 = new VolumeConfig.Builder()
                .name("test").mounts(mounts).type("hostPath").path("/test/path").build();
        volumes1.add(volumeConfig1);

        //container name with alias
        final JKubeBuildConfiguration buildImageConfiguration = new JKubeBuildConfiguration.Builder()
                .ports(ports).from("fabric8/maven:latest").cleanup("try")
                .tags(tags).compression("gzip").build();

        ImageConfiguration imageConfiguration = new ImageConfiguration.Builder().
                name("test").alias("test-app").buildConfig(buildImageConfiguration)
                .registry("docker.io").build();

        images.add(imageConfiguration);
    }

    @Test
    public void jobHandlerTest() {

        ContainerHandler containerHandler = getContainerHandler();

        PodTemplateHandler podTemplateHandler = new PodTemplateHandler(containerHandler);

        JobHandler jobHandler = new JobHandler(podTemplateHandler);

        ResourceConfig config = new ResourceConfig.Builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .withServiceAccount("test-account")
                .volumes(volumes1)
                .build();

        Job job = jobHandler.getJob(config,images);

        //Assertion
        assertNotNull(job.getSpec());
        assertNotNull(job.getMetadata());
        assertNotNull(job.getSpec().getTemplate());
        assertEquals("testing",job.getMetadata().getName());
        assertEquals("test-account",job.getSpec().getTemplate()
                .getSpec().getServiceAccountName());
        assertFalse(job.getSpec().getTemplate().getSpec().getVolumes().isEmpty());
        assertEquals("test",job.getSpec().getTemplate().getSpec().
                getVolumes().get(0).getName());
        assertEquals("/test/path",job.getSpec().getTemplate()
                .getSpec().getVolumes().get(0).getHostPath().getPath());
        assertNotNull(job.getSpec().getTemplate().getSpec().getContainers());

    }

    private ContainerHandler getContainerHandler() {
        return new ContainerHandler(project.getProperties(), new GroupArtifactVersion("g","a","v"), probeHandler);
    }

    @Test(expected = IllegalArgumentException.class)
    public void daemonTemplateHandlerWithInvalidNameTest() {
        //invalid controller name
        ContainerHandler containerHandler = getContainerHandler();

        PodTemplateHandler podTemplateHandler = new PodTemplateHandler(containerHandler);

        JobHandler jobHandler = new JobHandler(podTemplateHandler);

        //with invalid controller name
        ResourceConfig config = new ResourceConfig.Builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("TesTing")
                .withServiceAccount("test-account")
                .volumes(volumes1)
                .build();

        jobHandler.getJob(config, images);
    }

    @Test(expected = IllegalArgumentException.class)
    public void daemonTemplateHandlerWithoutControllerTest() {
        //without controller name
        ContainerHandler containerHandler = getContainerHandler();

        PodTemplateHandler podTemplateHandler = new PodTemplateHandler(containerHandler);

        JobHandler jobHandler = new JobHandler(podTemplateHandler);

        //without controller name
        ResourceConfig config = new ResourceConfig.Builder()
                .imagePullPolicy("IfNotPresent")
                .withServiceAccount("test-account")
                .volumes(volumes1)
                .build();

        jobHandler.getJob(config, images);
    }
}
