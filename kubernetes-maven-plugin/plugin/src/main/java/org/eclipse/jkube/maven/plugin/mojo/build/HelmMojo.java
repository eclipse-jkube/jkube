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
package org.eclipse.jkube.maven.plugin.mojo.build;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;
import org.eclipse.jkube.kit.resource.helm.HelmService;
import org.eclipse.jkube.kit.resource.helm.Maintainer;
import org.eclipse.jkube.maven.plugin.mojo.KitLoggerProvider;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Generates a Helm chart for the kubernetes resources
 */
@Mojo(name = "helm", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class HelmMojo extends AbstractJKubeMojo implements KitLoggerProvider {

  protected static final String PROPERTY_CHART = "jkube.helm.chart";
  protected static final String PROPERTY_CHART_EXTENSION = "jkube.helm.chartExtension";
  protected static final String PROPERTY_VERSION = "jkube.helm.version";
  protected static final String PROPERTY_DESCRIPTION = "jkube.helm.description";
  protected static final String PROPERTY_HOME = "jkube.helm.home";
  protected static final String PROPERTY_ICON = "jkube.helm.icon";
  protected static final String PROPERTY_TYPE = "jkube.helm.type";
  protected static final String PROPERTY_SOURCE_DIR = "jkube.helm.sourceDir";
  protected static final String PROPERTY_OUTPUT_DIR = "jkube.helm.outputDir";
  protected static final String PROPERTY_TARBALL_OUTPUT_DIR = "jkube.helm.tarballOutputDir";

  private static final String DEFAULT_CHART_EXTENSION = "tar.gz";
  static final String PROPERTY_KUBERNETES_MANIFEST = "jkube.kubernetesManifest";
  static final String PROPERTY_KUBERNETES_TEMPLATE = "jkube.kubernetesTemplate";

  @Component
  private MavenProjectHelper projectHelper;

  /**
   * The generated kubernetes YAML file
   */
  @Parameter(property = PROPERTY_KUBERNETES_MANIFEST, defaultValue = "${basedir}/target/classes/META-INF/jkube/kubernetes.yml")
  File kubernetesManifest;

  /**
   * The generated kubernetes YAML file
   */
  @Parameter(property = PROPERTY_KUBERNETES_TEMPLATE, defaultValue = "${basedir}/target/classes/META-INF/jkube/kubernetes")
  File kubernetesTemplate;


  @Parameter
  HelmConfig helm;


  @Override
  public void executeInternal() throws MojoExecutionException {
    try {
      initDefaults();
      HelmService.generateHelmCharts(log, helm);
    } catch (IOException exception) {
      throw new MojoExecutionException(exception.getMessage());
    }
  }

  protected void initDefaults() throws IOException, MojoExecutionException {
    if (helm == null) {
      helm = new HelmConfig();
    }
    initFromPropertyOrDefault(PROPERTY_CHART, helm::getChart, helm::setChart, project.getArtifactId());
    initFromPropertyOrDefault(PROPERTY_CHART_EXTENSION, helm::getChartExtension, helm::setChartExtension,
        DEFAULT_CHART_EXTENSION);
    initFromPropertyOrDefault(PROPERTY_VERSION, helm::getVersion, helm::setVersion, project.getVersion());
    initFromPropertyOrDefault(PROPERTY_DESCRIPTION, helm::getDescription, helm::setDescription, project.getDescription());
    initFromPropertyOrDefault(PROPERTY_HOME, helm::getHome, helm::setHome, project.getUrl());
    if (helm.getSources() == null) {
      helm.setSources(sourcesFromProject(project));
    }
    if (helm.getMaintainers() == null) {
      helm.setMaintainers(maintainersFromProject(project));
    }
    initFromPropertyOrDefault(PROPERTY_ICON, helm::getIcon, helm::setIcon, findIconURL());
    addAdditionalFiles();
    if (helm.getTemplates() == null) {
      helm.setTemplates(findTemplates());
    }
    initHelmTypes();
    initFromPropertyOrDefault(PROPERTY_SOURCE_DIR, helm::getSourceDir, helm::setSourceDir,
        String.format("%s/META-INF/jkube/", project.getBuild().getOutputDirectory()));
    initFromPropertyOrDefault(PROPERTY_OUTPUT_DIR, helm::getOutputDir, helm::setOutputDir,
        String.format("%s/jkube/helm/%s", project.getBuild().getDirectory(), helm.getChart()));
    initFromPropertyOrDefault(PROPERTY_TARBALL_OUTPUT_DIR, helm::getTarballOutputDir, helm::setTarballOutputDir,
        project.getBuild().getDirectory());
    helm.setGeneratedChartListeners(Collections.singletonList((helmConfig, type, chartFile) -> projectHelper
        .attachArtifact(project, helm.getChartExtension(), type.getClassifier(), chartFile)));

  }

  void initFromPropertyOrDefault(String property, Supplier<String> getter, Consumer<String> setter, String defaultValue){
    Optional.ofNullable(getProperty(property)).filter(StringUtils::isNotBlank).ifPresent(setter);
    if (StringUtils.isBlank(getter.get())) {
      setter.accept(defaultValue);
    }
  }

  protected File getKubernetesManifest() {
    return kubernetesManifest;
  }

  protected File getKubernetesTemplate() {
     return kubernetesTemplate;
  }

  protected HelmConfig.HelmType getDefaultHelmType() {
    return HelmConfig.HelmType.KUBERNETES;
  }

  HelmConfig getHelm() {
    return helm;
  }

  private static List<String> sourcesFromProject(MavenProject mavenProject) {
    return Optional.ofNullable(mavenProject)
        .map(MavenProject::getScm)
        .map(Scm::getUrl)
        .map(Collections::singletonList)
        .orElse(Collections.emptyList());
  }

  private static List<Maintainer> maintainersFromProject(MavenProject mavenProject) {
    return Optional.ofNullable(mavenProject)
        .map(MavenProject::getDevelopers)
        .orElse(Collections.emptyList())
        .stream()
        .filter(developer -> StringUtils.isNotBlank(developer.getName()) || StringUtils.isNotBlank(developer.getEmail()))
        .map(developer -> new Maintainer(developer.getName(), developer.getEmail()))
        .collect(Collectors.toList());
  }

  private String findIconURL() throws MojoExecutionException {
    String answer = null;
    if (getKubernetesManifest() != null && getKubernetesManifest().isFile()) {
      KubernetesResource dto;
      try {
        dto = ResourceUtil.load(getKubernetesManifest(), KubernetesResource.class);
      } catch (IOException e) {
        throw new MojoExecutionException("Failed to load kubernetes YAML " + getKubernetesManifest() + ". " + e, e);
      }
      if (dto instanceof HasMetadata) {
        answer = KubernetesHelper.getOrCreateAnnotations((HasMetadata) dto).get("jkube.io/iconUrl");
      }
      if (StringUtils.isBlank(answer) && dto instanceof KubernetesList) {
        KubernetesList list = (KubernetesList) dto;
        List<HasMetadata> items = list.getItems();
        if (items != null) {
          for (HasMetadata item : items) {
            answer = KubernetesHelper.getOrCreateAnnotations(item).get("jkube.io/iconUrl");
            if (StringUtils.isNotBlank(answer)) {
              break;
            }
          }
        }
      }
    } else {
      getLog().warn("No kubernetes manifest file has been generated yet by the kubernetes:resource goal at: " + getKubernetesManifest());
    }
    return answer;
  }

  private void addAdditionalFiles() {
    if (helm.getAdditionalFiles() == null) {
      helm.setAdditionalFiles(new ArrayList<>());
    }
    firstProjectFile("README").ifPresent(helm.getAdditionalFiles()::add);
    firstProjectFile("LICENSE").ifPresent(helm.getAdditionalFiles()::add);
  }

  private Optional<File> firstProjectFile(String fileName) {
    final FilenameFilter filter = (dir, name) -> {
      String lower = name.toLowerCase(Locale.ENGLISH);
      return lower.equalsIgnoreCase(fileName) || lower.startsWith(fileName.toLowerCase() + ".");
    };
    return Optional.ofNullable(project.getBasedir().listFiles(filter))
        .filter(files -> files.length > 0)
        .map(files -> files[0]);
  }

  private List<Template> findTemplates() throws IOException {
    final List<Template> ret = new ArrayList<>();
    final File[] sourceFiles;
    if (getKubernetesTemplate() != null && getKubernetesTemplate().isDirectory()) {
      sourceFiles = getKubernetesTemplate().listFiles((dir, filename) -> filename.endsWith("-template.yml"));
    } else if (getKubernetesTemplate() != null) {
      sourceFiles = new File[] { getKubernetesTemplate() };
    } else {
      sourceFiles = new File[0];
    }
    for (File sourceFile : Objects
        .requireNonNull(sourceFiles, "No template files found in the provided directory")) {
      final KubernetesResource dto = ResourceUtil.load(sourceFile, KubernetesResource.class, ResourceFileType.yaml);
      if (dto instanceof  Template) {
        ret.add((Template) dto);
      } else if (dto instanceof KubernetesList) {
        Optional.ofNullable(((KubernetesList)dto).getItems())
            .map(List::stream)
            .map(items -> items.filter(Template.class::isInstance)
                .map(Template.class::cast)
                .collect(Collectors.toList())
            )
            .ifPresent(ret::addAll);
      }
    }
    return ret;
  }

  private void initHelmTypes() {
    Optional.ofNullable(getProperty(PROPERTY_TYPE))
        .filter(StringUtils::isNotBlank)
        .map(types -> StringUtils.split(types, ","))
        .map(Stream::of)
        .map(s -> s.map(prop -> HelmConfig.HelmType.valueOf(prop.trim().toUpperCase())).collect(Collectors.toList()))
        .ifPresent(helm::setTypes);
    if (helm.getTypes() == null || helm.getTypes().isEmpty()) {
      helm.setTypes(Collections.singletonList(getDefaultHelmType()));
    }
  }

}
