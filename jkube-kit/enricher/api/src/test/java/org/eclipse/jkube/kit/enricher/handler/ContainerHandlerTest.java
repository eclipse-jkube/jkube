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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.VolumeConfig;
import mockit.Mocked;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ContainerHandlerTest {

    @Mocked
    private ProbeHandler probeHandler;

    private List<Container> containers;

    private JavaProject project;

    private JavaProject project1;

    private JavaProject project2;

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
    BuildConfiguration buildImageConfiguration1;
    ImageConfiguration imageConfiguration1;

    @BeforeEach
    void setUp() {
        project = JavaProject.builder().properties(new Properties()).build();
        project1 = JavaProject.builder().properties(new Properties()).build();
        project2 = JavaProject.builder().properties(new Properties()).build();
        config = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent")
                .controllerName("testing")
                .replicas(5)
                .build();
        config1 = ResourceConfig.builder()
                .imagePullPolicy("IfNotPresent").build();
        ports = new ArrayList<>();
        tags = new ArrayList<>();
        images = new ArrayList<>();
        volumes1 = new ArrayList<>();
        volumes2 = new ArrayList<>();
        buildImageConfiguration1 = BuildConfiguration.builder()
                .from("fabric8/maven:latest").build();
        imageConfiguration1 = ImageConfiguration.builder()
                .name("test").alias("test-app").build(buildImageConfiguration1).registry("docker.io").build();
    }

    @Test
    void getContainersWithAliasTest() {

        project.setArtifactId("test-artifact");
        project.setGroupId("test-group");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        ContainerHandler handler = new ContainerHandler(project.getProperties(), new GroupArtifactVersion("test-group", "test-artifact", "0"), probeHandler);

        //container name with alias
        final BuildConfiguration buildImageConfiguration = BuildConfiguration.builder()
                .ports(ports).from("fabric8/maven:latest").cleanup("try").tags(tags).compressionString("gzip").build();

        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                .name("docker.io/test/test-app:1.2").alias("test-app").build(buildImageConfiguration).registry("docker-alternate.io").build();

        images.clear();
        images.add(imageConfiguration);

        containers = handler.getContainers(config, images);
        assertThat(containers).isNotNull()
            .first()
            .hasFieldOrPropertyWithValue("name", "test-app")
            .hasFieldOrPropertyWithValue("image", "docker.io/test/test-app:1.2")
            .hasFieldOrPropertyWithValue("imagePullPolicy", "IfNotPresent");
    }

    @Test
    void registryHandling() {

        //container name with alias
        final BuildConfiguration buildImageConfiguration = BuildConfiguration.builder().build();

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
            JavaProject testProject = JavaProject.builder().properties(new Properties()).build();
            Properties testProps = new Properties();
            if (testData[i+2] != null) {
                testProps.put("jkube.docker.pull.registry", testData[i + 2]);
            }
            if (testData[i+3] != null) {
                testProps.put("jkube.docker.registry", testData[i + 3]);
            }

            Properties properties = testProject.getProperties();
            properties.putAll(testProps);
            testProject.setProperties(properties);
            ContainerHandler handler = createContainerHandler(testProject);

            //container name with alias
            ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                    .build(buildImageConfiguration)
                    .name(testData[i])
                    .registry(testData[i+1])
                    .build();

            images.clear();
            images.add(imageConfiguration);

            containers = handler.getContainers(config, images);
            String image = handler.getContainers(config, images).get(0).getImage();
            assertThat(image).isEqualTo(testData[i + 4]);
        }
    }

    private ContainerHandler createContainerHandler(JavaProject testProject) {
        return new ContainerHandler(
            testProject.getProperties(),
            new GroupArtifactVersion("g","a","v"),
            probeHandler);
    }


    @Test
    void getContainerWithGroupArtifactTest() {

        project.setArtifactId("test-artifact");
        project.setGroupId("test-group");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        ContainerHandler handler = new ContainerHandler(project.getProperties(), new GroupArtifactVersion("test-group", "test-artifact", "0"), probeHandler);
        //container name with group id and aritact id without alias and user
        final BuildConfiguration buildImageConfiguration = BuildConfiguration.builder()
                .ports(ports).from("fabric8/").cleanup("try").tags(tags)
                .compressionString("gzip").dockerFile("testFile").build();

        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                .name("test").build(buildImageConfiguration).registry("docker.io").build();

        images.clear();
        images.add(imageConfiguration);

        containers = handler.getContainers(config, images);
        assertThat(containers).isNotNull()
            .first()
            .hasFieldOrPropertyWithValue("name", "test-group-test-artifact")
            .hasFieldOrPropertyWithValue("image", "docker.io/test:latest")
            .hasFieldOrPropertyWithValue("imagePullPolicy", "IfNotPresent");
    }

    @Test
    void getContainerTestWithUser(){
        project.setArtifactId("test-artifact");
        project.setGroupId("test-group");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        //container name with user and image with tag
        ContainerHandler handler = new ContainerHandler(project.getProperties(), new GroupArtifactVersion("test-group", "test-artifact", "0"), probeHandler);

        final BuildConfiguration buildImageConfiguration = BuildConfiguration.builder()
                .ports(ports).from("fabric8/").cleanup("try").tags(tags)
                .compressionString("gzip").dockerFile("testFile").build();

        ImageConfiguration imageConfiguration = ImageConfiguration.builder()
                .name("user/test:latest").build(buildImageConfiguration).registry("docker.io").build();

        images.clear();
        images.add(imageConfiguration);

        containers = handler.getContainers(config, images);
        assertThat(containers).isNotNull()
            .first()
            .hasFieldOrPropertyWithValue("name", "user-test-artifact")
            .hasFieldOrPropertyWithValue("image", "docker.io/user/test:latest")
            .hasFieldOrPropertyWithValue("imagePullPolicy", "IfNotPresent");
    }

    @Test
    void imagePullPolicyWithPolicySetTest() {

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
        assertThat(containers.get(0).getImagePullPolicy()).isEqualTo("IfNotPresent");

        containers = handler2.getContainers(config1, images);
        assertThat(containers.get(0).getImagePullPolicy()).isEqualTo("IfNotPresent");
    }

    @Test
    void imagePullPolicyWithoutPolicySetTest(){

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
        ResourceConfig config2 = ResourceConfig.builder()
                .imagePullPolicy("").build();

        containers = handler1.getContainers(config2, images);
        assertThat(containers.get(0).getImagePullPolicy()).isEqualTo("PullAlways");

        containers = handler2.getContainers(config2, images);
        assertThat(containers.get(0).getImagePullPolicy()).isEmpty();

        containers = handler3.getContainers(config2, images);
        assertThat(containers.get(0).getImagePullPolicy()).isEmpty();
    }

    @Test
    void getImageNameTest(){

        ContainerHandler handler = createContainerHandler(project);

        //Image Configuration with name and without registry
        ImageConfiguration imageConfiguration2 = ImageConfiguration.builder()
                .name("test").alias("test-app").build(buildImageConfiguration1).build();

        //Image Configuration without name and with registry
        ImageConfiguration imageConfiguration3 = ImageConfiguration.builder().
                alias("test-app").build(buildImageConfiguration1).registry("docker.io").build();

        //Image Configuration without name and registry
        ImageConfiguration imageConfiguration4 = ImageConfiguration.builder().
                alias("test-app").build(buildImageConfiguration1).registry("docker.io").build();

        images.clear();
        images.add(imageConfiguration1);
        images.add(imageConfiguration2);
        images.add(imageConfiguration3);
        images.add(imageConfiguration4);

        containers = handler.getContainers(config1, images);

        assertThat(containers.get(0).getImage()).isEqualTo("docker.io/test:latest");
        assertThat(containers.get(1).getImage()).isEqualTo("test:latest");
        assertThat(containers.get(2).getImage()).isNull();;
        assertThat(containers.get(3).getImage()).isNull();
    }

    @Test
    void getRegistryTest() {
        ContainerHandler handler = createContainerHandler(project1);

        ImageConfiguration imageConfig = ImageConfiguration.builder()
                .name("test").alias("test-app").build(buildImageConfiguration1).build();

        images.clear();
        images.add(imageConfig);

        Properties properties = project1.getProperties();
        properties.setProperty("jkube.docker.pull.registry", "push.me");
        project1.setProperties(properties);
        containers = handler.getContainers(config1, images);

        project1.getProperties().remove("jkube.docker.pull.registry");
        assertThat(containers.get(0).getImage()).isEqualTo("push.me/test:latest");
    }

    @Test
    void getVolumeMountWithoutMountTest() {
        ContainerHandler handler = createContainerHandler(project);

        images.clear();
        images.add(imageConfiguration1);

        //volume config without mount
        VolumeConfig volumeConfig1 = VolumeConfig.builder().name("first").build();
        volumes1.add(volumeConfig1);
        ResourceConfig config1 = ResourceConfig.builder().volumes(volumes1).build();
        containers = handler.getContainers(config1, images);
        assertThat(containers.get(0).getVolumeMounts()).isEmpty();
    }

    @Test
    void getVolumeMountWithoutNameTest() {

        ContainerHandler handler = createContainerHandler(project);

        images.clear();
        images.add(imageConfiguration1);

        List<String> mounts = new ArrayList<>();
        mounts.add("/path/etc");

        //volume config without name but with mount
        VolumeConfig volumeConfig2 = VolumeConfig.builder().mounts(mounts).build();
        volumes1.clear();
        volumes1.add(volumeConfig2);

        ResourceConfig config2 = ResourceConfig.builder().volumes(volumes1).build();
        containers = handler.getContainers(config2, images);
        assertThat(containers).singleElement()
            .extracting(Container::getVolumeMounts).asList()
            .first()
            .hasFieldOrPropertyWithValue("name", null)
            .hasFieldOrPropertyWithValue("mountPath", "/path/etc");
    }

    @Test
    void getVolumeMountWithNameAndMountTest() {
        ContainerHandler handler = createContainerHandler(project);

        List<String> mounts = new ArrayList<>();
        mounts.add("/path/etc");

        images.clear();
        images.add(imageConfiguration1);

        //volume config with name and single mount
        VolumeConfig volumeConfig3 = VolumeConfig.builder().name("third").mounts(mounts).build();
        volumes1.clear();
        volumes1.add(volumeConfig3);
        ResourceConfig config3 = ResourceConfig.builder().volumes(volumes1).build();
        containers = handler.getContainers(config3, images);
        assertThat(containers).singleElement()
            .extracting(Container::getVolumeMounts).asList()
            .first()
            .hasFieldOrPropertyWithValue("name", "third")
            .hasFieldOrPropertyWithValue("mountPath", "/path/etc");
    }

    @Test
    void getVolumeMountWithMultipleMountTest() {
        ContainerHandler handler = createContainerHandler(project);

        images.clear();
        images.add(imageConfiguration1);

        List<String> mounts = new ArrayList<>();
        mounts.add("/path/etc");

        //volume config with name and multiple mount
        mounts.add("/path/system");
        mounts.add("/path/sys");
        VolumeConfig volumeConfig4 = VolumeConfig.builder().name("test").mounts(mounts).build();
        volumes1.clear();
        volumes1.add(volumeConfig4);
        ResourceConfig config4 = ResourceConfig.builder().volumes(volumes1).build();
        containers = handler.getContainers(config4, images);
        assertThat(containers.get(0).getVolumeMounts()).hasSize(3);
        for (int i = 0; i <= 2; i++) {
            assertThat(containers.get(0).getVolumeMounts().get(i).getName()).isEqualTo("test");
        }
    }

    @Test
    void getVolumeMountWithEmptyVolumeTest() {
        ContainerHandler handler = createContainerHandler(project);

        images.clear();
        images.add(imageConfiguration1);

        //empty volume
        ResourceConfig config5 = ResourceConfig.builder().volumes(volumes2).build();
        containers = handler.getContainers(config5, images);
        assertThat(containers.get(0).getVolumeMounts()).isEmpty();
    }

    @Test
    void containerEmptyPortsTest() {
        ContainerHandler handler = createContainerHandler(project);

        images.clear();
        images.add(imageConfiguration1);

        //Empty Ports
        containers = handler.getContainers(config, images);
        assertThat(containers.get(0).getPorts()).isNull();
    }

    @Test
    void containerPortsWithoutPortTest() {

        ContainerHandler handler = createContainerHandler(project);

        //without Ports
        final BuildConfiguration buildImageConfiguration2 = BuildConfiguration.builder()
                .from("fabric8/maven:latest").cleanup("try").compressionString("gzip").build();

        ImageConfiguration imageConfiguration2 = ImageConfiguration.builder()
                .name("test").alias("test-app").build(buildImageConfiguration2).registry("docker.io").build();

        images.clear();
        images.add(imageConfiguration2);

        containers = handler.getContainers(config, images);
        assertThat(containers.get(0).getPorts()).isNull();
    }

    @Test
    void containerPortsWithDifferentPortTest(){
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

        buildImageConfiguration1 = BuildConfiguration.builder()
                .ports(ports).from("fabric8/maven:latest").cleanup("try").compressionString("gzip").build();

        imageConfiguration1 = ImageConfiguration.builder()
                .name("test").alias("test-app").build(buildImageConfiguration1).registry("docker.io").build();

        images.clear();
        images.add(imageConfiguration1);

        ContainerHandler handler = createContainerHandler(project);

        containers = handler.getContainers(config, images);
        List<ContainerPort> outputPorts = containers.get(0).getPorts();
        assertThat(outputPorts).hasSize(9);
        int protocolCount=0,tcpCount=0,udpCount=0,containerPortCount=0,hostIPCount=0,hostPortCount=0;
        for(int i=0;i<9;i++){
            if(!StringUtils.isBlank(outputPorts.get(i).getProtocol())){
                protocolCount++;
                if(outputPorts.get(i).getProtocol().equalsIgnoreCase("tcp")){
                    tcpCount++;
                }
                else{
                    udpCount++;
                }
            }
            if(!StringUtils.isBlank(outputPorts.get(i).getHostIP())){
                hostIPCount++;
            }
            if(outputPorts.get(i).getContainerPort()!=null){
                containerPortCount++;
            }
            if(outputPorts.get(i).getHostPort()!=null){
                hostPortCount++;
            }
        }
        assertThat(protocolCount).isEqualTo(9);
        assertThat(tcpCount).isEqualTo(7);
        assertThat(udpCount).isEqualTo(2);
        assertThat(hostIPCount).isEqualTo(3);
        assertThat(containerPortCount).isEqualTo(9);
        assertThat(hostPortCount).isEqualTo(4);
    }

    @Test
    void testGetContainersWithUserAndImageAndTagWithPeriodInImageUser() {
        // Given
        ContainerHandler containerHandler = createContainerHandler(project);
        List<ImageConfiguration> imageConfigurations = new ArrayList<>();
        imageConfigurations.add(ImageConfiguration.builder()
                .name("roman.gordill/customer-service-cache:latest")
                .registry("quay.io")
                .build(BuildConfiguration.builder()
                        .from("quay.io/jkube/jkube-java:0.0.13")
                        .assembly(AssemblyConfiguration.builder()
                                .targetDir("/deployments")
                                .build())
                        .build())
                .build());

        // When
        List<Container> containers = containerHandler.getContainers(config, imageConfigurations);

        // Then
        assertThat(containers).isNotNull()
            .singleElement()
            .hasFieldOrPropertyWithValue("image", "quay.io/roman.gordill/customer-service-cache:latest");
    }
}
