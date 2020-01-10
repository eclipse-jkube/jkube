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

import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import org.eclipse.jkube.kit.build.maven.config.MavenBuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;
import mockit.Mocked;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ReplicaSetHandlerTest {

    @Mocked
    ProbeHandler probeHandler;

    MavenProject project = new MavenProject();

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
        final MavenBuildConfiguration buildImageConfiguration = new MavenBuildConfiguration.Builder()
                .ports(ports).from("fabric8/maven:latest").cleanup("try")
                .tags(tags).compression("gzip").build();

        ImageConfiguration imageConfiguration = new ImageConfiguration.Builder().
                name("test").alias("test-app").buildConfig(buildImageConfiguration)
                .registry("docker.io").build();

        images.add(imageConfiguration);
    }

    @Test
    public void replicaSetHandlerTest() {

        ContainerHandler containerHandler = getContainerHandler();

        PodTemplateHandler podTemplateHandler = new PodTemplateHandler(containerHandler);

        ReplicaSetHandler replicaSetHandler = new ReplicaSetHandler(podTemplateHandler);

        ResourceConfig config = new ResourceConfig.Builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .withServiceAccount("test-account")
                .withReplicas(5)
                .volumes(volumes1)
                .build();

        ReplicaSet replicaSet = replicaSetHandler.getReplicaSet(config,images);

        //Assertion
        assertNotNull(replicaSet.getSpec());
        assertNotNull(replicaSet.getMetadata());
        assertEquals(5,replicaSet.getSpec().getReplicas().intValue());
        assertNotNull(replicaSet.getSpec().getTemplate());
        assertEquals("testing",replicaSet.getMetadata().getName());
        assertEquals("test-account",replicaSet.getSpec().getTemplate()
                .getSpec().getServiceAccountName());
        assertFalse(replicaSet.getSpec().getTemplate().getSpec().getVolumes().isEmpty());
        assertEquals("test",replicaSet.getSpec().getTemplate().getSpec().
                getVolumes().get(0).getName());
        assertEquals("/test/path",replicaSet.getSpec().getTemplate()
                .getSpec().getVolumes().get(0).getHostPath().getPath());
        assertNotNull(replicaSet.getSpec().getTemplate().getSpec().getContainers());

    }

    private ContainerHandler getContainerHandler() {
        return new ContainerHandler(project.getProperties(), new GroupArtifactVersion("g","a","v"), probeHandler);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replicaSetHandlerWithInvalidNameTest() {
        //invalid controller name
        ContainerHandler containerHandler = getContainerHandler();

        PodTemplateHandler podTemplateHandler = new PodTemplateHandler(containerHandler);

        ReplicaSetHandler replicaSetHandler = new ReplicaSetHandler(podTemplateHandler);

        //with invalid controller name
        ResourceConfig config = new ResourceConfig.Builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("TesTing")
                .withServiceAccount("test-account")
                .withReplicas(5)
                .volumes(volumes1)
                .build();

        replicaSetHandler.getReplicaSet(config, images);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replicaSetHandlerWithoutControllerTest() {
        //without controller name
        ContainerHandler containerHandler = getContainerHandler();

        PodTemplateHandler podTemplateHandler = new PodTemplateHandler(containerHandler);

        ReplicaSetHandler replicaSetHandler = new ReplicaSetHandler(podTemplateHandler);

        //without controller name
        ResourceConfig config = new ResourceConfig.Builder()
                .imagePullPolicy("IfNotPresent")
                .withServiceAccount("test-account")
                .withReplicas(5)
                .volumes(volumes1)
                .build();

        replicaSetHandler.getReplicaSet(config, images);
    }
}
