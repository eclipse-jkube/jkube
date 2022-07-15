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
package org.eclipse.jkube.kit.resource.helm;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Maintainer;
import org.eclipse.jkube.kit.common.util.ResourceUtil;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.openshift.api.model.Template;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class HelmServiceUtilTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File manifest;
  private File templateDir;
  private JavaProject javaProject;

  @Before
  public void setUp() throws Exception {
    final File baseDir = temporaryFolder.newFolder("test-project");
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
  public void initHelmConfig_withNoConfig_shouldInitConfigWithDefaultValues() throws IOException {
    // When
    final HelmConfig result = HelmServiceUtil
        .initHelmConfig(HelmConfig.HelmType.KUBERNETES, javaProject, manifest, templateDir, null)
        .build();
    // Then
    assertThat(result)
      .isNotNull()
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
    assertThat(result.getTarballOutputDir()).endsWith("target");
  }

  @Test
  public void initHelmConfig_withOriginalConfig_shouldInitConfigWithoutOverriding() throws IOException {
    // Given
    final HelmConfig original = HelmConfig.builder()
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
  public void initHelmConfig_withTypeProperty_shouldInitConfigWithForSpecifiedTypes() throws IOException {
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
  public void initHelmPushConfig_withValidProperties_shouldInitHelmConfigWithConfiguredProperties() {
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
  public void initHelmPushConfig_withValidConfigurationAndProperties_thenPropertiesShouldTakePrecedence() {
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
  public void resolveFromPropertyOrDefaultPropertyHasPrecedenceOverConfiguration() {
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
  public void findIconUrl_fromProvidedFile_returnsValidUrl() throws IOException {
    ResourceUtil resourceUtil = mock(ResourceUtil.class,RETURNS_DEEP_STUBS);
    HasMetadata listEntry = mock(HasMetadata.class,RETURNS_DEEP_STUBS);
    // Given
    manifest.createNewFile();
    when(resourceUtil.load(manifest,KubernetesResource.class)).thenReturn(new KubernetesList("List", Collections.singletonList(listEntry), "Invented", null));
    when(listEntry.getMetadata().getAnnotations()).thenReturn(Collections.singletonMap("jkube.io/iconUrl", "https://my-icon"));
    // When
    String url = HelmServiceUtil.findIconURL(manifest);
    // Then
    assertThat(url).isEqualTo("https://my-icon");
  }

  @Test
  public void findTemplatesFromProvidedFile() throws Exception {
    ResourceUtil resourceUtil = mock(ResourceUtil.class,RETURNS_DEEP_STUBS);
    Template template = mock(Template.class);
    // Given
    when(resourceUtil.load(manifest, KubernetesResource.class)).thenReturn(template);
    // When
    List<Template> templateList = HelmServiceUtil.findTemplates(manifest);
    // Then
    assertThat(templateList)
      .hasSize(1)
      .contains(template);
  }

}
