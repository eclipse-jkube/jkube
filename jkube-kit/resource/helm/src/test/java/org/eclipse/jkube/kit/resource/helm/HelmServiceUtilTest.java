/*
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
package org.eclipse.jkube.kit.resource.helm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.openshift.api.model.TemplateBuilder;
import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Maintainer;

import io.fabric8.openshift.api.model.Template;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class HelmServiceUtilTest {

  private File manifest;
  private File templateDir;
  private JavaProject javaProject;

  @BeforeEach
  void setUp(@TempDir Path temporaryFolder) throws Exception {
    final File baseDir = Files.createDirectory(temporaryFolder.resolve("test-project")).toFile();
    final File buildDir = new File(baseDir, "target");
    manifest = buildDir.toPath().resolve("classes").resolve("META-INF").resolve("jkube")
        .resolve("kubernetes.yml").toFile();
    FileUtils.forceMkdir(manifest.getParentFile());
    templateDir = new File(buildDir, "jkube");
    FileUtils.forceMkdir(templateDir);
    javaProject = JavaProject.builder()
        .properties(new Properties())
        .baseDirectory(baseDir)
        .outputDirectory(new File(buildDir, "classes"))
        .buildDirectory(buildDir)
        .artifactId("artifact-id")
        .version("1337")
        .description("A configured description")
        .url("https://project.url")
        .scmUrl("https://scm.url")
        .maintainer(Maintainer.builder().email("john@example.com").name("John").build())
        .maintainer(Maintainer.builder().email(null).name(null).build())
        .build();
  }

  @Test
  void initHelmConfig_withNoConfig_shouldInitConfigWithDefaultValues() throws IOException {
    // When
    final HelmConfig result = HelmServiceUtil
        .initHelmConfig(HelmConfig.HelmType.KUBERNETES, javaProject, manifest, templateDir, null)
        .build();
    // Then
    assertThat(result)
      .isNotNull()
      .hasFieldOrPropertyWithValue("apiVersion", "v1")
      .hasFieldOrPropertyWithValue("chart", "artifact-id")
      .hasFieldOrPropertyWithValue("chartExtension", "tar.gz")
      .hasFieldOrPropertyWithValue("version", "1337")
      .hasFieldOrPropertyWithValue("description", "A configured description")
      .hasFieldOrPropertyWithValue("home", "https://project.url")
      .hasFieldOrPropertyWithValue("sources", Collections.singletonList("https://scm.url"))
      .hasFieldOrPropertyWithValue("maintainers",
          Collections.singletonList(new Maintainer("John", "john@example.com")))
      .hasFieldOrPropertyWithValue("types", Collections.singletonList(HelmConfig.HelmType.KUBERNETES))
      .hasFieldOrPropertyWithValue("additionalFiles", Collections.emptyList())
      .hasFieldOrPropertyWithValue("parameterTemplates", Collections.emptyList())
      .hasFieldOrProperty("icon");
    assertThat(result.getSourceDir()).endsWith("target/classes/META-INF/jkube/");
    assertThat(result.getOutputDir()).endsWith("target/jkube/helm/artifact-id");
    assertThat(result.getTarballOutputDir()).endsWith("target/jkube/helm/artifact-id");
  }

  @Test
  void initHelmConfig_withOriginalConfig_shouldInitConfigWithoutOverriding() throws IOException {
    // Given
    final HelmConfig original = HelmConfig.builder()
        .apiVersion("v1337")
        .chart("Original Name")
        .version("313373")
        .sources(Collections.emptyList())
        .maintainers(Collections.emptyList())
        .sourceDir("sources")
        .outputDir("output")
        .build();
    // When
    final HelmConfig result = HelmServiceUtil
        .initHelmConfig(HelmConfig.HelmType.KUBERNETES, javaProject, manifest, templateDir, original)
        .build();
    // Then
    assertThat(result)
      .isNotNull()
      .hasFieldOrPropertyWithValue("apiVersion", "v1337")
      .hasFieldOrPropertyWithValue("chart", "Original Name")
      .hasFieldOrPropertyWithValue("chartExtension", "tar.gz")
      .hasFieldOrPropertyWithValue("version", "313373")
      .hasFieldOrPropertyWithValue("description", "A configured description")
      .hasFieldOrPropertyWithValue("home", "https://project.url")
      .hasFieldOrPropertyWithValue("sources", Collections.emptyList())
      .hasFieldOrPropertyWithValue("maintainers", Collections.emptyList())
      .hasFieldOrPropertyWithValue("sourceDir", "sources")
      .hasFieldOrPropertyWithValue("outputDir", "output");
  }

  @Test
  void initHelmConfig_withTypeProperty_shouldInitConfigWithForSpecifiedTypes() throws IOException {
    // Given
    javaProject.getProperties().put("jkube.helm.type", "Openshift,KuBernetEs");
    // When
    final HelmConfig result = HelmServiceUtil
        .initHelmConfig(HelmConfig.HelmType.KUBERNETES, javaProject, manifest, templateDir, null)
        .build();
    // Then
    assertThat(result)
      .hasFieldOrPropertyWithValue("chart", "artifact-id")
      .hasFieldOrPropertyWithValue("version", "1337")
      .extracting(HelmConfig::getTypes).asList()
      .containsExactlyInAnyOrder(
          HelmConfig.HelmType.KUBERNETES,
          HelmConfig.HelmType.OPENSHIFT
      );
  }

  @Test
  void initHelmPushConfig_withValidProperties_shouldInitHelmConfigWithConfiguredProperties() {
    // Given
    HelmConfig helm = new HelmConfig();
    javaProject.getProperties().put("jkube.helm.snapshotRepository.name", "props repo");
    javaProject.getProperties().put("jkube.helm.snapshotRepository.type", "nExus");
    javaProject.getProperties().put("jkube.helm.snapshotRepository.url", "https://example.com/url");
    javaProject.getProperties().put("jkube.helm.snapshotRepository.username", "propsUser");
    javaProject.getProperties().put("jkube.helm.snapshotRepository.password", "propS3cret");
    // When
    HelmServiceUtil.initHelmPushConfig(helm, javaProject);
    // Then
    assertThat(helm)
      .hasFieldOrPropertyWithValue("snapshotRepository.name", "props repo")
      .hasFieldOrPropertyWithValue("snapshotRepository.url", "https://example.com/url")
      .hasFieldOrPropertyWithValue("snapshotRepository.username", "propsUser")
      .hasFieldOrPropertyWithValue("snapshotRepository.password", "propS3cret")
      .hasFieldOrPropertyWithValue("snapshotRepository.type", HelmRepository.HelmRepoType.NEXUS)
      .hasFieldOrPropertyWithValue("security", "~/.m2/settings-security.xml");
  }

  @Test
  void initHelmPushConfig_withValidConfigurationAndProperties_thenPropertiesShouldTakePrecedence() {
    // Given
    HelmConfig helm = new HelmConfig();
    helm.setSnapshotRepository(HelmRepository.builder()
      .name("SNAP-REPO")
      .type(HelmRepository.HelmRepoType.ARTIFACTORY)
      .url("https://example.com/artifactory")
      .username("jkubeUser")
      .password("S3cret")
      .build());
    javaProject.getProperties().put("jkube.helm.snapshotRepository.password", "propS3cret");
    // When
    HelmServiceUtil.initHelmPushConfig(helm, javaProject);
    // Then
    assertThat(helm)
      .hasFieldOrPropertyWithValue("snapshotRepository.username", "jkubeUser")
      .hasFieldOrPropertyWithValue("snapshotRepository.password", "propS3cret")
      .hasFieldOrPropertyWithValue("snapshotRepository.url", "https://example.com/artifactory")
      .hasFieldOrPropertyWithValue("snapshotRepository.type", HelmRepository.HelmRepoType.ARTIFACTORY)
      .hasFieldOrPropertyWithValue("security", "~/.m2/settings-security.xml");
  }

  @Test
  void resolveFromPropertyOrDefaultPropertyHasPrecedenceOverConfiguration() {
    // Given
    javaProject.getProperties().put("jkube.helm.property", "Overrides Current Value");
    javaProject.getProperties().put("jkube.helm.otherProperty", "Ignored");
    final HelmConfig config = new HelmConfig();
    config.setChart("This value will be overridden");
    // When
    String value = HelmServiceUtil.resolveFromPropertyOrDefault(
      "jkube.helm.property", javaProject, config::getChart, "default is ignored");
    // Then
    assertThat(value).isEqualTo("Overrides Current Value");
  }

  @Test
  void findIconUrl_fromProvidedFile_returnsValidUrl() throws IOException {
    // Given
    final GenericKubernetesResource inventedType = new GenericKubernetesResourceBuilder()
      .withKind("Invented").withApiVersion("jkube.eclipse.org/v1alpha1")
      .withNewMetadata().addToAnnotations("jkube.io/iconUrl", "https://my-icon").endMetadata()
      .build();
    Serialization.saveYaml(manifest, new KubernetesListBuilder().addToItems(inventedType).build());
    // When
    String url = HelmServiceUtil.findIconURL(manifest);
    // Then
    assertThat(url).isEqualTo("https://my-icon");
  }

  @Test
  void findOpenShiftParameterTemplatesFromProvidedFile() throws Exception {
    // Given
    Serialization.saveYaml(manifest, new TemplateBuilder()
      .withNewMetadata().withName("template-from-manifest").endMetadata()
      .build());
    // When
    List<Template> templateList = HelmServiceUtil.findTemplates(manifest);
    // Then
    assertThat(templateList)
      .singleElement()
      .hasFieldOrPropertyWithValue("metadata.name", "template-from-manifest");
  }

  @Test
  void findOpenShiftParameterTemplatesFromNull() throws Exception {
    // When
    List<Template> templateList = HelmServiceUtil.findTemplates(null);
    // Then
    assertThat(templateList).isEmpty();
  }

  @Test
  void findOpenShiftParameterTemplatesFromProvidedFileAsList() throws Exception {
    // Given
    Serialization.saveYaml(manifest, new KubernetesListBuilder()
      .addToItems(new TemplateBuilder()
        .withNewMetadata().withName("template-from-manifest-list").endMetadata()
        .build()).build());
    // When
    List<Template> templateList = HelmServiceUtil.findTemplates(manifest);
    // Then
    assertThat(templateList)
      .singleElement()
      .hasFieldOrPropertyWithValue("metadata.name", "template-from-manifest-list");
  }

  @Test
  void findOpenShiftParameterTemplatesFromProvidedDirectoryWhenEmpty() throws Exception {
    // When
    List<Template> templateList = HelmServiceUtil.findTemplates(templateDir);
    // Then
    assertThat(templateList).isEmpty();
  }

  @Test
  void findOpenShiftParameterTemplatesFromProvidedDirectoryWithTemplateFiles() throws Exception {
    // Given
    Serialization.saveYaml(new File(templateDir, "template-1-template.yml"), new TemplateBuilder()
      .withNewMetadata().withName("template-1").endMetadata()
      .build());
    Serialization.saveYaml(new File(templateDir, "template-2-template.yaml"), new TemplateBuilder()
      .withNewMetadata().withName("template-2").endMetadata()
      .build());
    Serialization.saveYaml(new File(templateDir, "pod-pod.yaml"), new PodBuilder()
      .withNewMetadata().withName("pod").endMetadata()
      .build());
    // When
    List<Template> templateList = HelmServiceUtil.findTemplates(templateDir);
    // Then
    assertThat(templateList)
      .hasSize(2)
      .extracting("metadata.name")
      .containsExactlyInAnyOrder("template-1", "template-2");
  }
}
