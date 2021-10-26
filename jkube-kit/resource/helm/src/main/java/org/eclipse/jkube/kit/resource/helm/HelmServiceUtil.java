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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Maintainer;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.ResourceUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.jkube.kit.common.util.JKubeProjectUtil.getProperty;

public class HelmServiceUtil {
  private static final String PROPERTY_ICON = "jkube.helm.icon";
  private static final String PROPERTY_TYPE = "jkube.helm.type";
  private static final String PROPERTY_CHART = "jkube.helm.chart";
  private static final String PROPERTY_CHART_EXTENSION = "jkube.helm.chartExtension";
  private static final String DEFAULT_CHART_EXTENSION = "tar.gz";
  private static final String PROPERTY_VERSION = "jkube.helm.version";
  private static final String PROPERTY_DESCRIPTION = "jkube.helm.description";
  private static final String PROPERTY_SOURCE_DIR = "jkube.helm.sourceDir";
  private static final String PROPERTY_OUTPUT_DIR = "jkube.helm.outputDir";
  private static final String PROPERTY_TARBALL_OUTPUT_DIR = "jkube.helm.tarballOutputDir";
  private static final String PROPERTY_HOME = "jkube.helm.home";
  protected static final String PROPERTY_UPLOAD_REPO_NAME = "jkube.helm.%s.name";
  protected static final String PROPERTY_UPLOAD_REPO_URL = "jkube.helm.%s.url";
  protected static final String PROPERTY_UPLOAD_REPO_USERNAME = "jkube.helm.%s.username";
  protected static final String PROPERTY_UPLOAD_REPO_PASSWORD = "jkube.helm.%s.password";
  protected static final String PROPERTY_UPLOAD_REPO_TYPE = "jkube.helm.%s.type";
  protected static final String STABLE_REPOSITORY = "stableRepository";
  protected static final String SNAPSHOT_REPOSITORY = "snapshotRepository";
  protected static final String PROPERTY_SECURITY =  "jkube.helm.security";
  protected static final String DEFAULT_SECURITY = "~/.m2/settings-security.xml";

  private HelmServiceUtil() { }

  public static HelmConfig.HelmConfigBuilder initHelmConfig(
      HelmConfig.HelmType defaultHelmType, JavaProject project, File manifest, File templateDir, HelmConfig original)
      throws IOException {

    if (original == null) {
      original = new HelmConfig();
    }
    original.setChart(resolveFromPropertyOrDefault(PROPERTY_CHART, project, original::getChart, project.getArtifactId()));
    original.setChartExtension(resolveFromPropertyOrDefault(PROPERTY_CHART_EXTENSION, project, original::getChartExtension, DEFAULT_CHART_EXTENSION));
    original.setVersion(resolveFromPropertyOrDefault(PROPERTY_VERSION, project, original::getVersion, project.getVersion()));
    original.setDescription(resolveFromPropertyOrDefault(PROPERTY_DESCRIPTION, project, original::getDescription, project.getDescription()));
    original.setHome(resolveFromPropertyOrDefault(PROPERTY_HOME, project, original::getHome, project.getUrl()));
    if (original.getSources() == null) {
      original.setSources(project.getScmUrl() != null ? Collections.singletonList(project.getScmUrl()) : Collections.emptyList());
    }
    if (original.getMaintainers() == null) {
      original.setMaintainers(project.getMaintainers().stream()
          .filter(m -> StringUtils.isNotBlank(m.getName()) || StringUtils.isNotBlank(m.getEmail()))
          .map(m -> new Maintainer(m.getName(), m.getEmail()))
          .collect(Collectors.toList()));
    }
    original.setIcon(resolveFromPropertyOrDefault(PROPERTY_ICON, project, original::getIcon, findIconURL(manifest)));
    original.setAdditionalFiles(getAdditionalFiles(original, project));
    if (original.getTemplates() == null) {
      original.setTemplates(findTemplates(templateDir));
    }
    original.setTypes(resolveHelmTypes(defaultHelmType, project));

    original.setSourceDir(resolveFromPropertyOrDefault(PROPERTY_SOURCE_DIR, project, original::getSourceDir,
        String.format("%s/META-INF/jkube/", project.getOutputDirectory())));
    original.setOutputDir(resolveFromPropertyOrDefault(PROPERTY_OUTPUT_DIR, project, original::getOutputDir,
        String.format("%s/jkube/helm/%s", project.getBuildDirectory(), original.getChart())));
    original.setTarballOutputDir(resolveFromPropertyOrDefault(PROPERTY_TARBALL_OUTPUT_DIR, project, original::getTarballOutputDir,
        project.getBuildDirectory().getAbsolutePath()));
    return original.toBuilder();
  }

  public static HelmConfig initHelmPushConfig(HelmConfig helmConfig, JavaProject project) {
    if (helmConfig == null) {
      helmConfig = new HelmConfig();
    }

    helmConfig.setStableRepository(initHelmRepository(helmConfig.getStableRepository(), project, STABLE_REPOSITORY));
    helmConfig.setSnapshotRepository(initHelmRepository(helmConfig.getSnapshotRepository(), project, SNAPSHOT_REPOSITORY));

    helmConfig.setSecurity(resolveFromPropertyOrDefault(PROPERTY_SECURITY, project, helmConfig::getSecurity, DEFAULT_SECURITY));
    return helmConfig;
  }

  static HelmRepository initHelmRepository(HelmRepository helmRepository, JavaProject project, String repositoryType) {
    HelmRepository resolvedHelmRepository = helmRepository;
    if (helmRepository == null) {
      resolvedHelmRepository = new HelmRepository();
    }

    resolvedHelmRepository.setType(resolveFromPropertyOrDefault(String.format(PROPERTY_UPLOAD_REPO_TYPE, repositoryType), project,
      resolvedHelmRepository::getTypeAsString, null));
    resolvedHelmRepository.setName(resolveFromPropertyOrDefault(String.format(PROPERTY_UPLOAD_REPO_NAME, repositoryType), project,
      resolvedHelmRepository::getName, null));
    resolvedHelmRepository.setUrl(resolveFromPropertyOrDefault(String.format(PROPERTY_UPLOAD_REPO_URL, repositoryType), project,
      resolvedHelmRepository::getUrl, null));
    resolvedHelmRepository.setUsername(resolveFromPropertyOrDefault(String.format(PROPERTY_UPLOAD_REPO_USERNAME, repositoryType), project,
      resolvedHelmRepository::getUsername,  null));
    resolvedHelmRepository.setPassword(resolveFromPropertyOrDefault(String.format(PROPERTY_UPLOAD_REPO_PASSWORD, repositoryType), project,
      resolvedHelmRepository::getPassword, null));

    return resolvedHelmRepository;
  }

  static List<HelmConfig.HelmType> resolveHelmTypes(HelmConfig.HelmType defaultHelmType, JavaProject project) {
    final List<HelmConfig.HelmType> helmTypes = Optional.ofNullable(getProperty(PROPERTY_TYPE, project))
      .filter(StringUtils::isNotBlank)
      .map(types -> StringUtils.split(types, ","))
      .map(Stream::of)
      .map(s -> s.map(prop -> HelmConfig.HelmType.valueOf(prop.trim().toUpperCase())).collect(Collectors.toList())).orElse(null);
    if (helmTypes == null || helmTypes.isEmpty()) {
      return Collections.singletonList(defaultHelmType);
    }
    return helmTypes;
  }

  static String resolveFromPropertyOrDefault(String property, JavaProject project, Supplier<String> getter, String defaultValue) {
    return Optional.ofNullable(getProperty(property, project))
      .filter(StringUtils::isNotBlank)
      .orElse(Optional.ofNullable(getter.get())
        .filter(StringUtils::isNotBlank)
        .orElse(defaultValue));
  }

  static List<File> getAdditionalFiles(HelmConfig helm, JavaProject project) {
    List<File> additionalFiles = new ArrayList<>();
    if (helm.getAdditionalFiles() != null) {
      additionalFiles.addAll(helm.getAdditionalFiles());
    }
    firstProjectFile("README", project).ifPresent(additionalFiles::add);
    firstProjectFile("LICENSE", project).ifPresent(additionalFiles::add);
    return additionalFiles;
  }

  static Optional<File> firstProjectFile(String fileName, JavaProject project) {
    final FilenameFilter filter = (dir, name) -> {
      String lower = name.toLowerCase(Locale.ENGLISH);
      return lower.equalsIgnoreCase(fileName) || lower.startsWith(fileName.toLowerCase() + ".");
    };
    return Optional.ofNullable(project.getBaseDirectory().listFiles(filter))
      .filter(files -> files.length > 0)
      .map(files -> files[0]);
  }

  static String findIconURL(File manifest) {
    String answer = null;
    if (manifest != null && manifest.isFile()) {
      KubernetesResource dto;
      try {
        dto = ResourceUtil.load(manifest, KubernetesResource.class);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to load kubernetes YAML " + manifest + ". " + e, e);
      }
      if (dto instanceof HasMetadata) {
        answer = KubernetesHelper.getOrCreateAnnotations((HasMetadata) dto).get("jkube.io/iconUrl");
      }
      answer = extractIconUrlAnnotationFromKubernetesList(answer, dto);
    }
    return answer;
  }

  static List<Template> findTemplates(File templateDir) throws IOException {
    final List<Template> ret = new ArrayList<>();
    final File[] sourceFiles;
    if (templateDir != null && templateDir.isDirectory()) {
      sourceFiles = templateDir.listFiles((dir, filename) -> filename.endsWith("-template.yml"));
    } else if (templateDir != null) {
      sourceFiles = new File[] { templateDir };
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

  static HelmRepository selectHelmRepository(HelmConfig helmConfig) {
    if (helmConfig.getVersion() != null && helmConfig.getVersion().endsWith("-SNAPSHOT")) {
      return helmConfig.getSnapshotRepository();
    }
    return helmConfig.getStableRepository();
  }

  static boolean isRepositoryValid(HelmRepository repository) {
    return repository != null
      && repository.getType() != null
      && StringUtils.isNotBlank(repository.getName())
      && StringUtils.isNotBlank(repository.getUrl());
  }

  static void setAuthentication(HelmRepository repository, KitLogger logger, List<RegistryServerConfiguration> registryServerConfigurations, UnaryOperator<String> passwordDecrypter)  {
    final String id = repository.getName();
    final String REPO = "Repo ";
    if (repository.getUsername() != null) {
      if (repository.getPassword() == null) {
        throw new IllegalArgumentException(REPO + id + " has a username but no password defined.");
      }
      logger.debug(REPO + id + " has credentials defined, skip searching in server list.");
    } else {

      RegistryServerConfiguration server = registryServerConfigurations.stream()
        .filter(r -> r.getId().equals(id))
        .findAny()
        .orElse(null);
      if (server == null) {
        throw new IllegalArgumentException(
          "No credentials found for " + id + " in configuration or settings.xml server list.");
      } else {

        logger.debug("Use credentials from server list for " + id + ".");
        if (server.getUsername() == null || server.getPassword() == null) {
          throw new IllegalArgumentException("Repo "
            + id
            + " was found in server list but has no username/password.");
        }
        repository.setUsername(server.getUsername());
        repository.setPassword(passwordDecrypter.apply(server.getPassword()));
      }
    }
  }

  private static String extractIconUrlAnnotationFromKubernetesList(String answer, KubernetesResource dto) {
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
    return answer;
  }

}
