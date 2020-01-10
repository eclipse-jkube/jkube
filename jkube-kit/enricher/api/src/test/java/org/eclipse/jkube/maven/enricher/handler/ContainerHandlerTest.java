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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import org.eclipse.jkube.kit.build.maven.config.MavenBuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;
import mockit.Mocked;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ContainerHandlerTest {

    @Mocked
    ProbeHandler probeHandler;

    private List<Container> containers;

    private MavenProject project;

    private MavenProject project1;

    private MavenProject project2;

    private ResourceConfig config;

    //policy is set in config
    private ResourceConfig config1;

    private List<String> ports;

    private List<String> tags;

    private List<ImageConfiguration> images;

    //volumes with volumeconfigs
    private List<VolumeConfig> volumes1;

    //empty volume, no volumeconfigs
    private List<VolumeConfig> volumes2;

    //a sample image configuration
    MavenBuildConfiguration buildImageConfiguration1;
    ImageConfiguration imageConfiguration1;

    @Before
    public void setUp() {
        project = new MavenProject();
        project1 = new MavenProject();
        project2 = new MavenProject();
        config = new ResourceConfig.Builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .withReplicas(5)
                .build();
        config1 = new ResourceConfig.Builder()
                .imagePullPolicy("IfNotPresent").build();
        ports = new ArrayList<>();
        tags = new ArrayList<>();
        images = new ArrayList<>();
        volumes1 = new ArrayList<>();
        volumes2 = new ArrayList<>();
        buildImageConfiguration1 = new MavenBuildConfiguration.Builder()
                .from("fabric8/maven:latest").build();
        imageConfiguration1 = new ImageConfiguration.Builder().
                name("test").alias("test-app").buildConfig(buildImageConfiguration1).registry("docker.io").build();
    }

    @Test
    public void getContainersWithAliasTest() {

        project.setArtifactId("test-artifact");
        project.setGroupId("test-group");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        ContainerHandler handler = new ContainerHandler(project.getProperties(), new GroupArtifactVersion("test-group", "test-artifact", "0"), probeHandler);

        //container name with alias
        final MavenBuildConfiguration buildImageConfiguration = new MavenBuildConfiguration.Builder()
                .ports(ports).from("fabric8/maven:latest").cleanup("try").tags(tags).compression("gzip").build();

        ImageConfiguration imageConfiguration = new ImageConfiguration.Builder().
                name("docker.io/test/test-app:1.2").alias("test-app").buildConfig(buildImageConfiguration).registry("docker-alternate.io").build();

        images.clear();
        images.add(imageConfiguration);

        containers = handler.getContainers(config, images);
        assertNotNull(containers);
        assertEquals("test-app", containers.get(0).getName());
        assertEquals("docker.io/test/test-app:1.2", containers.get(0).getImage());
        assertEquals("IfNotPresent", containers.get(0).getImagePullPolicy());
    }

    @Test
    public void registryHandling() {

        //container name with alias
        final MavenBuildConfiguration buildImageConfiguration = new MavenBuildConfiguration.Builder().build();



        String[] testData = {
            "docker.io/test/test-app:1.2",
            "docker-alternate.io",
            null,
            null,
            "docker.io/test/test-app:1.2",

            "test/test-app:1.2",
            "docker-image-config.io",
            "docker-pull.io",
            "docker-default.io",
            "docker-image-config.io/test/test-app:1.2",

            "test/test-app",
            null,
            "docker-pull.io",
            "docker-default.io",
            "docker-pull.io/test/test-app:latest",

            "test/test-app",
            null,
            null,
            "docker-default.io",
            "docker-default.io/test/test-app:latest"
        };

        for (int i = 0; i < testData.length; i += 5) {
            MavenProject testProject = new MavenProject();
            Properties testProps = new Properties();
            if (testData[i+2] != null) {
                testProps.put("docker.pull.registry", testData[i + 2]);
            }
            if (testData[i+3] != null) {
                testProps.put("docker.registry", testData[i + 3]);
            }

            testProject.getModel().setProperties(testProps);
            ContainerHandler handler = createContainerHandler(testProject);

            //container name with alias
            ImageConfiguration imageConfiguration =
                new ImageConfiguration.Builder()
                    .buildConfig(buildImageConfiguration)
                    .name(testData[i])
                    .registry(testData[i+1])
                    .build();

            images.clear();
            images.add(imageConfiguration);

            containers = handler.getContainers(config, images);
            assertNotNull(containers);
            assertEquals(testData[i+4], containers.get(0).getImage());
        }
    }

    private ContainerHandler createContainerHandler(MavenProject testProject) {
        return new ContainerHandler(
            testProject.getProperties(),
            new GroupArtifactVersion("g","a","v"),
            probeHandler);
    }


    @Test
    public void getContainerWithGroupArtifactTest() {

        project.setArtifactId("test-artifact");
        project.setGroupId("test-group");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        ContainerHandler handler = new ContainerHandler(project.getProperties(), new GroupArtifactVersion("test-group", "test-artifact", "0"), probeHandler);
        //container name with group id and aritact id without alias and user
        final MavenBuildConfiguration buildImageConfiguration = new MavenBuildConfiguration.Builder()
                .ports(ports).from("fabric8/").cleanup("try").tags(tags)
                .compression("gzip").dockerFile("testFile").dockerFileDir("/demo").build();

        ImageConfiguration imageConfiguration = new ImageConfiguration.Builder().
                name("test").buildConfig(buildImageConfiguration).registry("docker.io").build();

        images.clear();
        images.add(imageConfiguration);

        containers = handler.getContainers(config, images);
        assertNotNull(containers);
        assertEquals("test-group-test-artifact", containers.get(0).getName());
        assertEquals("docker.io/test:latest", containers.get(0).getImage());
        assertEquals("IfNotPresent", containers.get(0).getImagePullPolicy());
    }
    @Test
    public void getContainerTestWithUser(){
        project.setArtifactId("test-artifact");
        project.setGroupId("test-group");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        //container name with user and image with tag
        ContainerHandler handler = new ContainerHandler(project.getProperties(), new GroupArtifactVersion("test-group", "test-artifact", "0"), probeHandler);

        final MavenBuildConfiguration buildImageConfiguration = new MavenBuildConfiguration.Builder()
                .ports(ports).from("fabric8/").cleanup("try").tags(tags)
                .compression("gzip").dockerFile("testFile").dockerFileDir("/demo").build();

        ImageConfiguration imageConfiguration = new ImageConfiguration.Builder().
                name("user/test:latest").buildConfig(buildImageConfiguration).registry("docker.io").build();

        images.clear();
        images.add(imageConfiguration);

        containers = handler.getContainers(config, images);
        assertNotNull(containers);
        assertEquals("user-test-artifact",containers.get(0).getName());
        assertEquals("docker.io/user/test:latest",containers.get(0).getImage());
        assertEquals("IfNotPresent",containers.get(0).getImagePullPolicy());
    }

    @Test
    public void imagePullPolicyWithPolicySetTest() {

        //check if policy is set then both in case of version is not null or null

        //project with version and ending in SNAPSHOT
        project1.setVersion("3.5-SNAPSHOT");

        //project with version but not ending in SNAPSHOT
        project2.setVersion("3.5-NEW");

        //creating container Handler for all
        ContainerHandler handler1 = new ContainerHandler(project1.getProperties(), new GroupArtifactVersion("g","a","3.5-SNAPSHOT"), probeHandler);
        ContainerHandler handler2 = new ContainerHandler(project2.getProperties(), new GroupArtifactVersion("g","a", "3.5-NEW"), probeHandler);

        images.clear();
        images.add(imageConfiguration1);

        containers = handler1.getContainers(config1, images);
        assertEquals("IfNotPresent", containers.get(0).getImagePullPolicy());

        containers = handler2.getContainers(config1, images);
        assertEquals("IfNotPresent", containers.get(0).getImagePullPolicy());
    }
    @Test
    public void imagePullPolicyWithoutPolicySetTest(){

        //project with version and ending in SNAPSHOT
        project1.setVersion("3.5-SNAPSHOT");

        //project with version but not ending in SNAPSHOT
        project2.setVersion("3.5-NEW");

        //creating container Handler for two
        ContainerHandler handler1 = new ContainerHandler(project1.getProperties(), new GroupArtifactVersion("g", "a", "3.5-SNAPSHOT"), probeHandler);
        ContainerHandler handler2 = new ContainerHandler(project2.getProperties(), new GroupArtifactVersion("g" , "a", "3.5-NEW"), probeHandler);

        //project without version
        ContainerHandler handler3 = createContainerHandler(project);

        images.clear();
        images.add(imageConfiguration1);

        //check if policy is not set then both in case of version is set or not
        ResourceConfig config2 = new ResourceConfig.Builder()
                .imagePullPolicy("").build();

        containers = handler1.getContainers(config2, images);
        assertEquals("PullAlways",containers.get(0).getImagePullPolicy());

        containers = handler2.getContainers(config2, images);
        assertEquals("",containers.get(0).getImagePullPolicy());

        containers = handler3.getContainers(config2, images);
        assertEquals("",containers.get(0).getImagePullPolicy());

    }

    @Test
    public void getImageNameTest(){

        ContainerHandler handler = createContainerHandler(project);

        //Image Configuration with name and without registry
        ImageConfiguration imageConfiguration2 = new ImageConfiguration.Builder().
                name("test").alias("test-app").buildConfig(buildImageConfiguration1).build();

        //Image Configuration without name and with registry
        ImageConfiguration imageConfiguration3 = new ImageConfiguration.Builder().
                alias("test-app").buildConfig(buildImageConfiguration1).registry("docker.io").build();

        //Image Configuration without name and registry
        ImageConfiguration imageConfiguration4 = new ImageConfiguration.Builder().
                alias("test-app").buildConfig(buildImageConfiguration1).registry("docker.io").build();

        images.clear();
        images.add(imageConfiguration1);
        images.add(imageConfiguration2);
        images.add(imageConfiguration3);
        images.add(imageConfiguration4);

        containers = handler.getContainers(config1, images);

        assertEquals("docker.io/test:latest",containers.get(0).getImage());
        assertEquals("test:latest",containers.get(1).getImage());
        assertNull(containers.get(2).getImage());
        assertNull(containers.get(3).getImage());
    }

    @Test
    public void getRegistryTest() {
        ContainerHandler handler = createContainerHandler(project1);

        ImageConfiguration imageConfig = new ImageConfiguration.Builder().
                name("test").alias("test-app").buildConfig(buildImageConfiguration1).build();

        images.clear();
        images.add(imageConfig);

        project1.getProperties().setProperty("docker.pull.registry", "push.me");
        containers = handler.getContainers(config1, images);

        project1.getProperties().remove("docker.pull.registry");
        assertEquals("push.me/test:latest", containers.get(0).getImage());
    }

    @Test
    public void getVolumeMountWithoutMountTest() {
        ContainerHandler handler = createContainerHandler(project);

        images.clear();
        images.add(imageConfiguration1);

        //volume config without mount
        VolumeConfig volumeConfig1 = new VolumeConfig.Builder().name("first").build();
        volumes1.add(volumeConfig1);
        ResourceConfig config1 = new ResourceConfig.Builder().volumes(volumes1).build();
        containers = handler.getContainers(config1, images);
        assertTrue(containers.get(0).getVolumeMounts().isEmpty());
    }

    @Test
    public void getVolumeMountWithoutNameTest() {

        ContainerHandler handler = createContainerHandler(project);

        images.clear();
        images.add(imageConfiguration1);

        List<String> mounts = new ArrayList<>();
        mounts.add("/path/etc");

        //volume config without name but with mount
        VolumeConfig volumeConfig2 = new VolumeConfig.Builder().mounts(mounts).build();
        volumes1.clear();
        volumes1.add(volumeConfig2);

        ResourceConfig config2 = new ResourceConfig.Builder().volumes(volumes1).build();
        containers = handler.getContainers(config2, images);
        assertEquals(1, containers.get(0).getVolumeMounts().size());
        assertNull(containers.get(0).getVolumeMounts().get(0).getName());
        assertEquals("/path/etc", containers.get(0).getVolumeMounts().get(0).getMountPath());
    }

    @Test
    public void getVolumeMountWithNameAndMountTest() {
        ContainerHandler handler = createContainerHandler(project);

        List<String> mounts = new ArrayList<>();
        mounts.add("/path/etc");

        images.clear();
        images.add(imageConfiguration1);

        //volume config with name and single mount
        VolumeConfig volumeConfig3 = new VolumeConfig.Builder().name("third").mounts(mounts).build();
        volumes1.clear();
        volumes1.add(volumeConfig3);
        ResourceConfig config3 = new ResourceConfig.Builder().volumes(volumes1).build();
        containers = handler.getContainers(config3, images);
        assertEquals(1, containers.get(0).getVolumeMounts().size());
        assertEquals("third", containers.get(0).getVolumeMounts().get(0).getName());
        assertEquals("/path/etc", containers.get(0).getVolumeMounts().get(0).getMountPath());
    }

    @Test
    public void getVolumeMountWithMultipleMountTest() {
        ContainerHandler handler = createContainerHandler(project);

        images.clear();
        images.add(imageConfiguration1);

        List<String> mounts = new ArrayList<>();
        mounts.add("/path/etc");

        //volume config with name and multiple mount
        mounts.add("/path/system");
        mounts.add("/path/sys");
        VolumeConfig volumeConfig4 = new VolumeConfig.Builder().name("test").mounts(mounts).build();
        volumes1.clear();
        volumes1.add(volumeConfig4);
        ResourceConfig config4 = new ResourceConfig.Builder().volumes(volumes1).build();
        containers = handler.getContainers(config4, images);
        assertEquals(3, containers.get(0).getVolumeMounts().size());
        for (int i = 0; i <= 2; i++)
            assertEquals("test", containers.get(0).getVolumeMounts().get(i).getName());
    }

    @Test
    public void getVolumeMountWithEmptyVolumeTest() {
        ContainerHandler handler = createContainerHandler(project);

        images.clear();
        images.add(imageConfiguration1);

        //empty volume
        ResourceConfig config5 = new ResourceConfig.Builder().volumes(volumes2).build();
        containers = handler.getContainers(config5, images);
        assertTrue(containers.get(0).getVolumeMounts().isEmpty());
    }

    @Test
    public void containerEmptyPortsTest() {
        ContainerHandler handler = createContainerHandler(project);

        images.clear();
        images.add(imageConfiguration1);

        //Empty Ports
        containers = handler.getContainers(config, images);
        assertTrue(containers.get(0).getPorts().isEmpty());
    }

    @Test
    public void containerPortsWithoutPortTest() {

        ContainerHandler handler = createContainerHandler(project);

        //without Ports
        final MavenBuildConfiguration buildImageConfiguration2 = new MavenBuildConfiguration.Builder()
                .from("fabric8/maven:latest").cleanup("try").compression("gzip").build();

        ImageConfiguration imageConfiguration2 = new ImageConfiguration.Builder().
                name("test").alias("test-app").buildConfig(buildImageConfiguration2).registry("docker.io").build();

        images.clear();
        images.add(imageConfiguration2);

        containers = handler.getContainers(config, images);
        assertTrue(containers.get(0).getPorts().isEmpty());
    }

    @Test
    public void containerPortsWithDifferentPortTest(){
        //Different kind of Ports Specification
        ports.add("172.22.27.82:82:8082");
        ports.add("172.22.27.81:81:8081/tcp");
        ports.add("172.22.27.83:83:8083/udp");
        ports.add("90:9093/tcp");
        ports.add("172.22.27.84:8084/tcp");
        ports.add("172.22.27.84:84/tcp");
        ports.add("9090/tcp");
        ports.add("9091");
        ports.add("9092/udp");

        buildImageConfiguration1 = new MavenBuildConfiguration.Builder()
                .ports(ports).from("fabric8/maven:latest").cleanup("try").compression("gzip").build();

        imageConfiguration1 = new ImageConfiguration.Builder().
                name("test").alias("test-app").buildConfig(buildImageConfiguration1).registry("docker.io").build();

        images.clear();
        images.add(imageConfiguration1);

        ContainerHandler handler = createContainerHandler(project);

        containers = handler.getContainers(config, images);
        List<ContainerPort> outputports = containers.get(0).getPorts();
        assertEquals(9,outputports.size());
        int protocolCount=0,tcpCount=0,udpCount=0,containerPortCount=0,hostIPCount=0,hostPortCount=0;
        for(int i=0;i<9;i++){
            if(!StringUtils.isBlank(outputports.get(i).getProtocol())){
                protocolCount++;
                if(outputports.get(i).getProtocol().equalsIgnoreCase("tcp")){
                    tcpCount++;
                }
                else{
                    udpCount++;
                }
            }
            if(!StringUtils.isBlank(outputports.get(i).getHostIP())){
                hostIPCount++;
            }
            if(outputports.get(i).getContainerPort()!=null){
                containerPortCount++;
            }
            if(outputports.get(i).getHostPort()!=null){
                hostPortCount++;
            }
        }
        assertEquals(9,protocolCount);
        assertEquals(7,tcpCount);
        assertEquals(2,udpCount);
        assertEquals(3,hostIPCount);
        assertEquals(9,containerPortCount);
        assertEquals(4,hostPortCount);
    }
}