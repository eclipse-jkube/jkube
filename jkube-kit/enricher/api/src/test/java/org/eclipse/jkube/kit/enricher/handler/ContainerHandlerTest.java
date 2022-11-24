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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ContainerHandlerTest {

    @Mocked
    private ProbeHandler probeHandler;
    private JavaProject project;
    private ResourceConfig config;
    private List<String> ports;
    private List<String> tags;
    private List<ImageConfiguration> images;
    private List<VolumeConfig> volumes;
    private List<VolumeConfig> emptyVolumes;

    //a sample image configuration
    private BuildConfiguration buildImageConfiguration;
    private ImageConfiguration imageConfiguration;

    @BeforeEach
    void setUp() {
      project = JavaProject.builder().properties(new Properties()).build();
      config = ResourceConfig.builder()
          .imagePullPolicy("IfNotPresent")
          .controllerName("testing")
          .replicas(5)
          .build();
      ports = new ArrayList<>();
      tags = new ArrayList<>();
      images = new ArrayList<>();
      volumes = new ArrayList<>();
      emptyVolumes = new ArrayList<>();
      buildImageConfiguration = BuildConfiguration.builder()
          .from("fabric8/maven:latest").build();
      imageConfiguration = ImageConfiguration.builder()
          .name("test").alias("test-app").build(buildImageConfiguration).registry("docker.io").build();
    }

    @Nested
    @DisplayName("get containers")
    class GetContainer {
      @Test
      void withAlias() {
        project.setArtifactId("test-artifact");
        project.setGroupId("test-group");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        ContainerHandler handler = new ContainerHandler(project.getProperties(),
            new GroupArtifactVersion("test-group", "test-artifact", "0"), probeHandler);

        //container name with alias
        buildImageConfiguration = BuildConfiguration.builder()
            .ports(ports).from("fabric8/maven:latest").cleanup("try").tags(tags).compressionString("gzip").build();

        imageConfiguration = ImageConfiguration.builder()
            .name("docker.io/test/test-app:1.2").alias("test-app").build(buildImageConfiguration)
            .registry("docker-alternate.io").build();
        images.add(imageConfiguration);

        List<Container> containers = handler.getContainers(config, images);
        assertThat(containers).isNotNull()
            .first()
            .hasFieldOrPropertyWithValue("name", "test-app")
            .hasFieldOrPropertyWithValue("image", "docker.io/test/test-app:1.2")
            .hasFieldOrPropertyWithValue("imagePullPolicy", "IfNotPresent");
      }

      @Test
      @DisplayName("with groupId, artifactId and version, should return configured container")
      void withGroupArtifactVersion_shouldReturnConfiguredContainer() {
        project.setArtifactId("test-artifact");
        project.setGroupId("test-group");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        ContainerHandler handler = new ContainerHandler(project.getProperties(),
            new GroupArtifactVersion("test-group", "test-artifact", "0"), probeHandler);
        //container name with group id and aritact id without alias and user
        buildImageConfiguration = BuildConfiguration.builder()
            .ports(ports).from("fabric8/").cleanup("try").tags(tags)
            .compressionString("gzip").dockerFile("testFile").build();

        imageConfiguration = ImageConfiguration.builder()
            .name("test").build(buildImageConfiguration).registry("docker.io").build();
        images.add(imageConfiguration);

        List<Container> containers = handler.getContainers(config, images);
        assertThat(containers).isNotNull()
            .first()
            .hasFieldOrPropertyWithValue("name", "test-group-test-artifact")
            .hasFieldOrPropertyWithValue("image", "docker.io/test:latest")
            .hasFieldOrPropertyWithValue("imagePullPolicy", "IfNotPresent");
      }

      @Test
      @DisplayName("with user in image, should return container with configured image")
      void withUser() {
        project.setArtifactId("test-artifact");
        project.setGroupId("test-group");

        ports.add("8080");
        ports.add("9090");

        tags.add("latest");
        tags.add("test");

        //container name with user and image with tag
        ContainerHandler handler = new ContainerHandler(project.getProperties(),
            new GroupArtifactVersion("test-group", "test-artifact", "0"), probeHandler);

        buildImageConfiguration = BuildConfiguration.builder()
            .ports(ports).from("fabric8/").cleanup("try").tags(tags)
            .compressionString("gzip").dockerFile("testFile").build();

        imageConfiguration = ImageConfiguration.builder()
            .name("user/test:latest").build(buildImageConfiguration).registry("docker.io").build();
        images.add(imageConfiguration);

        List<Container> containers = handler.getContainers(config, images);
        assertThat(containers).isNotNull()
            .first()
            .hasFieldOrPropertyWithValue("name", "user-test-artifact")
            .hasFieldOrPropertyWithValue("image", "docker.io/user/test:latest")
            .hasFieldOrPropertyWithValue("imagePullPolicy", "IfNotPresent");
      }

      @Test
      @DisplayName("with image names, should return containers with configured images")
      void withImageNames_shouldReturnContainersWithConfiguredImages() {
        ContainerHandler handler = createContainerHandler(project);

        ImageConfiguration imageConfigWithNameAndWithoutRegistry = ImageConfiguration.builder()
            .name("test").alias("test-app").build(buildImageConfiguration).build();

        ImageConfiguration imageConfigWithoutNameAndWithRegistry = ImageConfiguration.builder().alias("test-app")
            .build(buildImageConfiguration).registry("docker.io").build();

        ImageConfiguration imageConfigWithoutNameAndRegistry = ImageConfiguration.builder().alias("test-app")
            .build(buildImageConfiguration).registry("docker.io").build();

        ResourceConfig config1 = ResourceConfig.builder().imagePullPolicy("IfNotPresent").build();

        images.add(imageConfiguration);
        images.add(imageConfigWithNameAndWithoutRegistry);
        images.add(imageConfigWithoutNameAndWithRegistry);
        images.add(imageConfigWithoutNameAndRegistry);

        List<Container> containers = handler.getContainers(config1, images);
        assertThat(containers)
            .extracting("image")
            .containsExactly("docker.io/test:latest", "test:latest", null, null);
      }

      @Test
      @DisplayName("with user, image and tag with period in image user, should return configured image")
      void withUserAndImageAndTagWithPeriodInImageUser_shouldReturnConfiguredImage() {
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
        assertThat(containers).singleElement()
            .hasFieldOrPropertyWithValue("image", "quay.io/roman.gordill/customer-service-cache:latest");
      }
    }

    @Test
    void getImage_withConfiguredRegistries_shouldReturnImageWithConfiguredRegistries() {
        //container name with alias
        buildImageConfiguration = BuildConfiguration.builder().build();

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
            imageConfiguration = ImageConfiguration.builder()
                    .build(buildImageConfiguration)
                    .name(testData[i])
                    .registry(testData[i+1])
                    .build();

            images.clear();
            images.add(imageConfiguration);

            String image = handler.getContainers(config, images).get(0).getImage();
            assertThat(image).isEqualTo(testData[i + 4]);
        }
    }

    @Test
    void getImagePullPolicy_withPolicySet_shouldReturnSetPullPolicy() {
        //check if policy is set then both in case of version is not null or null

        //project with version and ending in SNAPSHOT
        JavaProject projectWithSnapshotVersion = JavaProject.builder()
                .version("3.5-SNAPSHOT")
                .properties(new Properties())
                .build();

        //project with version but not ending in SNAPSHOT
        JavaProject projectWithoutSnapshotVersion = JavaProject.builder()
                .version("3.5-NEW")
                .properties(new Properties())
                .build();

        //creating container Handler for all
        ContainerHandler handler1 = new ContainerHandler(projectWithSnapshotVersion.getProperties(), new GroupArtifactVersion("g","a","3.5-SNAPSHOT"), probeHandler);
        ContainerHandler handler2 = new ContainerHandler(projectWithoutSnapshotVersion.getProperties(), new GroupArtifactVersion("g","a", "3.5-NEW"), probeHandler);

        images.add(imageConfiguration);
        ResourceConfig config1 = ResourceConfig.builder().imagePullPolicy("IfNotPresent").build();

        String handler1ImagePullPolicy = handler1.getContainers(config1, images).get(0).getImagePullPolicy();
        assertThat(handler1ImagePullPolicy).isEqualTo("IfNotPresent");

        String handler2ImagePullPolicy = handler2.getContainers(config1, images).get(0).getImagePullPolicy();
        assertThat(handler2ImagePullPolicy).isEqualTo("IfNotPresent");
    }

    @Test
    void getImagePullPolicy_withoutPolicySet_shouldBeEmpty(){
        //project with version and ending in SNAPSHOT
        JavaProject projectWithSnapshotVersion = JavaProject.builder()
                .version("3.5-SNAPSHOT")
                .properties(new Properties())
                .build();

        //project with version but not ending in SNAPSHOT
        JavaProject projectWithoutSnapshotVersion = JavaProject.builder()
                .version("3.5-NEW")
                .properties(new Properties())
                .build();

        //creating container Handler for two
        ContainerHandler handler1 = new ContainerHandler(projectWithSnapshotVersion.getProperties(), new GroupArtifactVersion("g", "a", "3.5-SNAPSHOT"), probeHandler);
        ContainerHandler handler2 = new ContainerHandler(projectWithoutSnapshotVersion.getProperties(), new GroupArtifactVersion("g" , "a", "3.5-NEW"), probeHandler);

        //project without version
        ContainerHandler handler3 = createContainerHandler(project);

        images.add(imageConfiguration);

        //check if policy is not set then both in case of version is set or not
        ResourceConfig config1 = ResourceConfig.builder().imagePullPolicy("").build();

        String handler1ImagePullPolicy = handler1.getContainers(config1, images).get(0).getImagePullPolicy();
        assertThat(handler1ImagePullPolicy).isEqualTo("PullAlways");

        String handler2ImagePullPolicy = handler2.getContainers(config1, images).get(0).getImagePullPolicy();
        assertThat(handler2ImagePullPolicy).isEmpty();

        String handler3ImagePullPolicy = handler3.getContainers(config1, images).get(0).getImagePullPolicy();
        assertThat(handler3ImagePullPolicy).isEmpty();
    }

    @Test
    void getImage_withPullRegistry_shouldReturnImageWithConfiguredPullRegistry() {
      ContainerHandler handler = createContainerHandler(project);
      ResourceConfig config1 = ResourceConfig.builder().imagePullPolicy("IfNotPresent").build();

      imageConfiguration = ImageConfiguration.builder()
          .name("test").alias("test-app").build(buildImageConfiguration).build();
      images.add(imageConfiguration);

      Properties properties = project.getProperties();
      properties.setProperty("jkube.docker.pull.registry", "push.me");
      project.setProperties(properties);
      String image = handler.getContainers(config1, images).get(0).getImage();

      project.getProperties().remove("jkube.docker.pull.registry");
      assertThat(image).isEqualTo("push.me/test:latest");
    }

    @Nested
    @DisplayName("get containers volume mount")
    class VolumeMount {

      @Test
      @DisplayName("without mount path and with name, should be empty")
      void withoutMountPathAndWithName_shouldBeEmpty() {
        ContainerHandler handler = createContainerHandler(project);

        images.add(imageConfiguration);

        VolumeConfig volumeConfigWithoutMount = VolumeConfig.builder().name("first").build();
        volumes.add(volumeConfigWithoutMount);
        ResourceConfig config1 = ResourceConfig.builder().volumes(volumes).build();
        List<io.fabric8.kubernetes.api.model.VolumeMount> volumeMounts = handler.getContainers(config1, images).get(0)
            .getVolumeMounts();
        assertThat(volumeMounts).isEmpty();
      }

      @Test
      @DisplayName("with mount path and without name, should return mount path")
      void withMountPathAndWithoutName_shouldReturnMountPath() {
        ContainerHandler handler = createContainerHandler(project);

        images.add(imageConfiguration);

        List<String> mounts = new ArrayList<>();
        mounts.add("/path/etc");

        VolumeConfig volumeConfigWithoutNameAndWithMount = VolumeConfig.builder().mounts(mounts).build();
        volumes.add(volumeConfigWithoutNameAndWithMount);

        ResourceConfig config1 = ResourceConfig.builder().volumes(volumes).build();
        List<io.fabric8.kubernetes.api.model.VolumeMount> volumeMounts = handler.getContainers(config1, images).get(0)
            .getVolumeMounts();
        assertThat(volumeMounts).singleElement()
            .hasFieldOrPropertyWithValue("name", null)
            .hasFieldOrPropertyWithValue("mountPath", "/path/etc");
      }

      @Test
      @DisplayName("with mount path and name, should return both mount path and name")
      void withMountAndName_shouldReturnBoth() {
        ContainerHandler handler = createContainerHandler(project);

        List<String> mounts = new ArrayList<>();
        mounts.add("/path/etc");

        images.add(imageConfiguration);

        VolumeConfig volumeConfigWithNameAndSingleMount = VolumeConfig.builder().name("third").mounts(mounts).build();
        volumes.add(volumeConfigWithNameAndSingleMount);
        ResourceConfig config1 = ResourceConfig.builder().volumes(volumes).build();
        List<io.fabric8.kubernetes.api.model.VolumeMount> volumeMounts = handler.getContainers(config1, images).get(0)
            .getVolumeMounts();
        assertThat(volumeMounts).singleElement()
            .hasFieldOrPropertyWithValue("name", "third")
            .hasFieldOrPropertyWithValue("mountPath", "/path/etc");
      }

      @Test
      @DisplayName("with multiple mount paths for same volume config, should return multiple mounts")
      void withMultipleMountPaths_shouldReturnName() {
        ContainerHandler handler = createContainerHandler(project);

        images.add(imageConfiguration);

        VolumeConfig volumeConfigWithNameAndMultipleMounts = VolumeConfig.builder()
          .name("test")
          .mount("/path/etc")
          .mount("/path/system")
          .mount("/path/sys")
          .build();
        volumes.add(volumeConfigWithNameAndMultipleMounts);
        ResourceConfig config1 = ResourceConfig.builder().volumes(volumes).build();
        List<io.fabric8.kubernetes.api.model.VolumeMount> volumeMounts = handler.getContainers(config1, images).get(0)
            .getVolumeMounts();
        assertThat(volumeMounts).hasSize(3)
          .extracting(
            io.fabric8.kubernetes.api.model.VolumeMount::getName,
            io.fabric8.kubernetes.api.model.VolumeMount::getMountPath
          )
          .containsExactly(
            tuple("test", "/path/etc"),
            tuple("test", "/path/system"),
            tuple("test", "/path/sys")
          );
      }

      @Test
      @DisplayName("with empty volume, should be empty")
      void withEmptyVolume_shouldBeEmpty() {
        ContainerHandler handler = createContainerHandler(project);

        images.add(imageConfiguration);

        //empty volume
        ResourceConfig config1 = ResourceConfig.builder().volumes(emptyVolumes).build();
        List<io.fabric8.kubernetes.api.model.VolumeMount> volumeMounts = handler.getContainers(config1, images).get(0)
            .getVolumeMounts();
        assertThat(volumeMounts).isEmpty();
      }
    }

    @Test
    void getPorts_withEmptyPorts_shouldBeNull() {
        ContainerHandler handler = createContainerHandler(project);

        images.add(imageConfiguration);

        //Empty Ports
        List<ContainerPort> containerPorts = handler.getContainers(config, images).get(0).getPorts();
        assertThat(containerPorts).isNull();
    }

    @Test
    void getPorts_withoutPort_shouldBeNull() {
        ContainerHandler handler = createContainerHandler(project);

        //without Ports
        buildImageConfiguration = BuildConfiguration.builder()
                .from("fabric8/maven:latest").cleanup("try").compressionString("gzip").build();

        imageConfiguration = ImageConfiguration.builder()
                .name("test").alias("test-app").build(buildImageConfiguration).registry("docker.io").build();
        images.add(imageConfiguration);

        List<ContainerPort> containerPorts = handler.getContainers(config, images).get(0).getPorts();
        assertThat(containerPorts).isNull();
    }

    @Test
    void getPorts_withDifferentPorts_shouldReturnConfiguredPorts(){
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

        buildImageConfiguration = BuildConfiguration.builder()
                .ports(ports).from("fabric8/maven:latest").cleanup("try").compressionString("gzip").build();

        imageConfiguration = ImageConfiguration.builder()
                .name("test").alias("test-app").build(buildImageConfiguration).registry("docker.io").build();
        images.add(imageConfiguration);

        ContainerHandler handler = createContainerHandler(project);

        List<Container> containers = handler.getContainers(config, images);
        List<ContainerPort> outputPorts = containers.get(0).getPorts();
        assertThat(outputPorts).hasSize(9);
        int protocolCount = 0, tcpCount = 0, udpCount = 0, containerPortCount = 0, hostIPCount = 0, hostPortCount = 0;
        for (int i = 0; i < 9; i++){
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
            if (outputPorts.get(i).getContainerPort() != null){
                containerPortCount++;
            }
            if (outputPorts.get(i).getHostPort() != null){
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


    private ContainerHandler createContainerHandler(JavaProject testProject) {
      return new ContainerHandler(
          testProject.getProperties(),
          new GroupArtifactVersion("g", "a", "v"),
          probeHandler);
    }
}
