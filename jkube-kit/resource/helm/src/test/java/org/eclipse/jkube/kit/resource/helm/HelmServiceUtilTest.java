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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Maintainer;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.ResourceUtil;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.openshift.api.model.Template;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class HelmServiceUtilTest {

  @Mocked
  private JavaProject javaProject;

  @Mocked
  KitLogger kitLogger;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void initHelmConfig_withNoConfig_shouldInitConfigWithDefaultValues() throws IOException {
    // Given
    File baseDir = temporaryFolder.newFolder("test-project");
    File manifest = new File(baseDir, "target/classes/META-INF/jkube/kubernetes.yml");
    File templateDir = new File(baseDir, "target/jkube");
    List<Maintainer> maintainers = Arrays.asList(
      Maintainer.builder().email("john@example.com").name("John").build(),
      Maintainer.builder().email(null).name(null).build()
    );
    boolean templateDirCreated = templateDir.mkdirs();
    // @formatter:off
    new Expectations() {{
      javaProject.getProperties(); result = new Properties();
      javaProject.getOutputDirectory(); result = new File(baseDir, "target/classes");
      javaProject.getBuildDirectory(); result = "target";
      javaProject.getArtifactId(); result = "artifact-id";
      javaProject.getVersion(); result = "1337";
      javaProject.getDescription(); result = "A description from Maven";
      javaProject.getUrl(); result = "https://project.url";
      javaProject.getScmUrl(); result = "https://scm.url";
      javaProject.getMaintainers(); result = maintainers;
    }};
    // @formatter:on

    // When
    HelmConfig helm = HelmServiceUtil.initHelmConfig(HelmConfig.HelmType.KUBERNETES, javaProject, manifest, templateDir, null)
        .build();

    // Then
    assertThat(templateDirCreated).isTrue();
    assertThat(helm)
      .isNotNull()
      .hasFieldOrPropertyWithValue("chart", "artifact-id")
      .hasFieldOrPropertyWithValue("chartExtension", "tar.gz")
      .hasFieldOrPropertyWithValue("version", "1337")
      .hasFieldOrPropertyWithValue("description", "A description from Maven")
      .hasFieldOrPropertyWithValue("home", "https://project.url")
      .hasFieldOrPropertyWithValue("sources", Collections.singletonList("https://scm.url"))
      .hasFieldOrProperty("icon");
    assertThat(helm.getSourceDir()).endsWith("target/classes/META-INF/jkube/");
    assertThat(helm.getOutputDir()).endsWith("target/jkube/helm/artifact-id");
    assertThat(helm.getTarballOutputDir()).endsWith("target");
    assertThat(helm.getMaintainers()).contains(new Maintainer("John", "john@example.com"));
    assertThat(helm.getAdditionalFiles()).isEmpty();
    assertThat(helm.getTemplates()).isEmpty();
    assertThat(helm.getTypes()).contains(HelmConfig.HelmType.KUBERNETES);
  }

  @Test
  public void initHelmPushConfig_withValidProperties_shouldInitHelmConfigWithConfiguredProperties() {
    // Given
    HelmConfig helm = new HelmConfig();
    Properties properties = new Properties();
    properties.put("jkube.helm.snapshotRepository.name", "props repo");
    properties.put("jkube.helm.snapshotRepository.type", "nExus");
    properties.put("jkube.helm.snapshotRepository.url", "http://example.com/url");
    properties.put("jkube.helm.snapshotRepository.username", "propsUser");
    properties.put("jkube.helm.snapshotRepository.password", "propS3cret");
    // @formatter:off
    new Expectations() {{
      javaProject.getProperties(); result = properties;
    }};
    // @formatter:on
    // When
    HelmServiceUtil.initHelmPushConfig(helm, javaProject);
    // Then
    assertThat(helm)
      .hasFieldOrPropertyWithValue("snapshotRepository.name", "props repo")
      .hasFieldOrPropertyWithValue("snapshotRepository.url", "http://example.com/url")
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
    Properties properties = new Properties();
    properties.put("jkube.helm.snapshotRepository.password", "propS3cret");
    // @formatter:off
    new Expectations() {{
      javaProject.getProperties(); result = properties;
    }};
    // @formatter:on
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
    final Properties properties = new Properties();
    properties.put("jkube.helm.property", "Overrides Current Value");
    properties.put("jkube.helm.otherProperty", "Ignored");
    final HelmConfig config = new HelmConfig();
    config.setChart("This value will be overridden");
    new Expectations() {{
      javaProject.getProperties();
      result = properties;
    }};
    // When
    String value = HelmServiceUtil.resolveFromPropertyOrDefault(
      "jkube.helm.property", javaProject, config::getChart, "default is ignored");
    // Then
    assertThat(value).isEqualTo("Overrides Current Value");
  }

  @Test
  public void findIconUrl_fromProvidedFile_returnsValidUrl(@Mocked File kubernetesManifest, @Mocked ResourceUtil resourceUtil, @Mocked HasMetadata listEntry) throws IOException {
    // Given
    new Expectations() {{
      kubernetesManifest.isFile();
      result = true;
      ResourceUtil.load(kubernetesManifest, KubernetesResource.class);
      result = new KubernetesList("List", Collections.singletonList(listEntry), "Invented", null);
      listEntry.getMetadata().getAnnotations();
      result = Collections.singletonMap("jkube.io/iconUrl", "https://my-icon");
    }};
    // When
    String url = HelmServiceUtil.findIconURL(kubernetesManifest);
    // Then
    assertThat(url).isEqualTo("https://my-icon");
  }

  @Test
  public void findTemplatesFromProvidedFile(
    @Mocked File kubernetesTemplate, @Mocked ResourceUtil resourceUtil, @Mocked Template template) throws Exception {

    // Given
    new Expectations() {{
      ResourceUtil.load(kubernetesTemplate, KubernetesResource.class, ResourceFileType.yaml);
      result = template;
    }};
    // When
    List<Template> templateList = HelmServiceUtil.findTemplates(kubernetesTemplate);
    // Then
    assertThat(templateList)
      .hasSize(1)
      .contains(template);
  }

}
