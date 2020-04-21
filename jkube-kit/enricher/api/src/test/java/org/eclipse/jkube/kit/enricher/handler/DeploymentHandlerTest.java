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

import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
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

public class DeploymentHandlerTest {

    @Mocked
    ProbeHandler probeHandler;

    @Mocked
    JavaProject project;

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
    }

    @Test
    public void deploymentTemplateHandlerTest() {

        ContainerHandler containerHandler = getContainerHandler();

        PodTemplateHandler podTemplateHandler = new PodTemplateHandler(containerHandler);

        DeploymentHandler deploymentHandler = new DeploymentHandler(podTemplateHandler);

        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .serviceAccount("test-account")
                .replicas(5)
                .volumes(volumes1)
                .build();

        Deployment deployment = deploymentHandler.getDeployment(config,images);

        //Assertion
        assertNotNull(deployment.getSpec());
        assertNotNull(deployment.getMetadata());
        assertEquals(5,deployment.getSpec().getReplicas().intValue());
        assertNotNull(deployment.getSpec().getTemplate());
        assertEquals("testing",deployment.getMetadata().getName());
        assertEquals("test-account",deployment.getSpec().getTemplate()
                .getSpec().getServiceAccountName());
        assertFalse(deployment.getSpec().getTemplate().getSpec().getVolumes().isEmpty());
        assertEquals("test",deployment.getSpec().getTemplate().getSpec().
                getVolumes().get(0).getName());
        assertEquals("/test/path",deployment.getSpec().getTemplate()
                .getSpec().getVolumes().get(0).getHostPath().getPath());
        assertNotNull(deployment.getSpec().getTemplate().getSpec().getContainers());

    }

    private ContainerHandler getContainerHandler() {
        return new ContainerHandler(project.getProperties(), new GroupArtifactVersion("g","a","v"), probeHandler);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deploymentTemplateHandlerWithInvalidNameTest() {
        //invalid controller name
        ContainerHandler containerHandler = getContainerHandler();

        PodTemplateHandler podTemplateHandler = new PodTemplateHandler(containerHandler);

        DeploymentHandler deploymentHandler = new DeploymentHandler(podTemplateHandler);

        //with invalid controller name
        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("TesTing")
                .serviceAccount("test-account")
                .replicas(5)
                .volumes(volumes1)
                .build();

        deploymentHandler.getDeployment(config, images);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deploymentTemplateHandlerWithoutControllerTest() {
        //without controller name
        ContainerHandler containerHandler = getContainerHandler();

        PodTemplateHandler podTemplateHandler = new PodTemplateHandler(containerHandler);

        DeploymentHandler deploymentHandler = new DeploymentHandler(podTemplateHandler);

        //without controller name
        ResourceConfig config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .serviceAccount("test-account")
                .replicas(5)
                .volumes(volumes1)
                .build();

        deploymentHandler.getDeployment(config, images);
    }
}
