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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Maintainer;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.eclipse.jkube.kit.common.util.JKubeProjectUtil.getProperty;
import static org.eclipse.jkube.kit.common.util.YamlUtil.listYamls;

public class HelmServiceUtil {

  private static final String PROPERTY_API_VERSION = "jkube.helm.apiVersion";
  private static final String DEFAULT_API_VERSION = "v1";
  private static final String PROPERTY_ICON = "jkube.helm.icon";
  private static final String PROPERTY_APP_VERSION = "jkube.helm.appVersion";
  private static final String PROPERTY_TYPE = "jkube.helm.type";
  private static final String PROPERTY_CHART = "jkube.helm.chart";
  private static final String PROPERTY_CHART_EXTENSION = "jkube.helm.chartExtension";
  private static final String DEFAULT_CHART_EXTENSION = "tar.gz";
  private static final String PROPERTY_VERSION = "jkube.helm.version";
  private static final String PROPERTY_DESCRIPTION = "jkube.helm.description";
  private static final String PROPERTY_SOURCE_DIR = "jkube.helm.sourceDir";
  private static final String PROPERTY_OUTPUT_DIR = "jkube.helm.outputDir";
  private static final String PROPERTY_TARBALL_OUTPUT_DIR = "jkube.helm.tarballOutputDir";
  private static final String PROPERTY_TARBALL_CLASSIFIER = "jkube.helm.tarFileClassifier";
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

  protected static final String PROPERTY_HELM_LINT_STRICT = "jkube.helm.lint.strict";
  protected static final String PROPERTY_HELM_LINT_QUIET = "jkube.helm.lint.quiet";

  private HelmServiceUtil() { }

  public static HelmConfig.HelmConfigBuilder initHelmConfig(
      HelmConfig.HelmType defaultHelmType, JavaProject project, File template, HelmConfig original) throws IOException {

    final HelmConfig helmConfig = original == null ? new HelmConfig() : original;
    helmConfig.setApiVersion(resolveFromPropertyOrDefault(PROPERTY_API_VERSION, project, helmConfig::getApiVersion, () -> DEFAULT_API_VERSION));
    helmConfig.setChart(resolveFromPropertyOrDefault(PROPERTY_CHART, project, helmConfig::getChart, project::getArtifactId));
    helmConfig.setChartExtension(resolveFromPropertyOrDefault(PROPERTY_CHART_EXTENSION, project, helmConfig::getChartExtension, () -> DEFAULT_CHART_EXTENSION));
    helmConfig.setVersion(resolveFromPropertyOrDefault(PROPERTY_VERSION, project, helmConfig::getVersion, project::getVersion));
    helmConfig.setDescription(resolveFromPropertyOrDefault(PROPERTY_DESCRIPTION, project, helmConfig::getDescription, project::getDescription));
    helmConfig.setHome(resolveFromPropertyOrDefault(PROPERTY_HOME, project, helmConfig::getHome, project::getUrl));
    if (helmConfig.getSources() == null) {
      helmConfig.setSources(project.getScmUrl() != null ? Collections.singletonList(project.getScmUrl()) : Collections.emptyList());
    }
    if (helmConfig.getMaintainers() == null && project.getMaintainers() != null) {
      helmConfig.setMaintainers(project.getMaintainers().stream()
          .filter(m -> StringUtils.isNotBlank(m.getName()) || StringUtils.isNotBlank(m.getEmail()))
          .map(m -> new Maintainer(m.getName(), m.getEmail()))
          .collect(Collectors.toList()));
    }
    helmConfig.setAdditionalFiles(getAdditionalFiles(helmConfig, project));
    if (helmConfig.getParameterTemplates() == null) {
      helmConfig.setParameterTemplates(findTemplates(template));
    }
    helmConfig.setTypes(resolveHelmTypes(defaultHelmType, project));

    helmConfig.setSourceDir(resolveFromPropertyOrDefault(PROPERTY_SOURCE_DIR, project, helmConfig::getSourceDir,
        () -> String.format("%s/META-INF/jkube/", project.getOutputDirectory())));
    helmConfig.setOutputDir(resolveFromPropertyOrDefault(PROPERTY_OUTPUT_DIR, project, helmConfig::getOutputDir,
      () -> String.format("%s/jkube/helm/%s", project.getBuildDirectory(), helmConfig.getChart())));
    helmConfig.setIcon(resolveFromPropertyOrDefault(PROPERTY_ICON, project, helmConfig::getIcon,
        () -> findIconURL(new File(helmConfig.getSourceDir()), helmConfig.getTypes())));
    helmConfig.setAppVersion(resolveFromPropertyOrDefault(PROPERTY_APP_VERSION, project, helmConfig::getAppVersion, project::getVersion));
    helmConfig.setTarFileClassifier(resolveFromPropertyOrDefault(PROPERTY_TARBALL_CLASSIFIER, project, helmConfig::getTarFileClassifier, () -> EMPTY));
    helmConfig.setTarballOutputDir(resolveFromPropertyOrDefault(PROPERTY_TARBALL_OUTPUT_DIR, project, helmConfig::getTarballOutputDir,
        helmConfig::getOutputDir));
    helmConfig.setLintStrict(resolveBooleanFromPropertyOrDefault(PROPERTY_HELM_LINT_STRICT, project, helmConfig::isLintStrict));
    helmConfig.setLintQuiet(resolveBooleanFromPropertyOrDefault(PROPERTY_HELM_LINT_QUIET, project, helmConfig::isLintQuiet));
    return helmConfig.toBuilder();
  }

  public static HelmConfig initHelmPushConfig(HelmConfig helmConfig, JavaProject project) {
    if (helmConfig == null) {
      helmConfig = new HelmConfig();
    }

    helmConfig.setStableRepository(initHelmRepository(helmConfig.getStableRepository(), project, STABLE_REPOSITORY));
    helmConfig.setSnapshotRepository(initHelmRepository(helmConfig.getSnapshotRepository(), project, SNAPSHOT_REPOSITORY));

    helmConfig.setSecurity(resolveFromPropertyOrDefault(PROPERTY_SECURITY, project, helmConfig::getSecurity, () -> DEFAULT_SECURITY));
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

  static String resolveFromPropertyOrDefault(String property, JavaProject project, Supplier<String> getter, Supplier<String> defaultValue) {
    return Optional.ofNullable(getProperty(property, project))
      .filter(StringUtils::isNotBlank)
      .orElse(Optional.ofNullable(getter.get())
        .filter(StringUtils::isNotBlank)
        .orElseGet(defaultValue == null ? () -> null : defaultValue));
  }

  static boolean resolveBooleanFromPropertyOrDefault(String property, JavaProject project, BooleanSupplier getter) {
    return Optional.ofNullable(getProperty(property, project))
      .filter(StringUtils::isNotBlank)
      .map(Boolean::parseBoolean)
      .orElse(getter.getAsBoolean());
  }

  static List<File> getAdditionalFiles(HelmConfig helm, JavaProject project) {
    List<File> additionalFiles = new ArrayList<>();
    if (helm.getAdditionalFiles() != null) {
      additionalFiles.addAll(helm.getAdditionalFiles());
    }
    firstProjectFile("README", project).ifPresent(additionalFiles::add);
    firstProjectFile("LICENSE", project).ifPresent(additionalFiles::add);
    firstProjectFile("values.schema.json", project).ifPresent(additionalFiles::add);
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

  static String findIconURL(File directory, Collection<HelmConfig.HelmType> types) {
    String answer = null;
    for (HelmConfig.HelmType type : types) {
      for (File yaml : listYamls(new File(directory, type.getSourceDir()))) {
        KubernetesResource dto;
        try {
          dto = Serialization.unmarshal(yaml);
        } catch (IOException e) {
          throw new IllegalStateException("Failed to load kubernetes YAML " + yaml + ". " + e, e);
        }
        if (dto instanceof HasMetadata) {
          answer = getJKubeIconUrlFromAnnotations(KubernetesHelper.getOrCreateAnnotations((HasMetadata) dto));
        } else if (StringUtils.isBlank(answer) && dto instanceof KubernetesList) {
          answer = extractIconUrlAnnotationFromKubernetesList((KubernetesList) dto);
        }
      }
    }
    return answer;
  }

  private static String extractIconUrlAnnotationFromKubernetesList(KubernetesList list) {
    if (list.getItems() != null) {
      for (HasMetadata item : list.getItems()) {
        final String url = getJKubeIconUrlFromAnnotations(KubernetesHelper.getOrCreateAnnotations(item));
        if (StringUtils.isNotBlank(url)) {
          return url;
        }
      }
    }
    return null;
  }

  static List<Template> findTemplates(File templateDir) throws IOException {
    final List<Template> ret = new ArrayList<>();
    final File[] sourceFiles;
    if (templateDir != null && templateDir.isDirectory()) {
      sourceFiles = templateDir.listFiles((dir, filename) ->
              filename.toLowerCase(Locale.ROOT).endsWith("-template.yml") ||
              filename.toLowerCase(Locale.ROOT).endsWith("-template.yaml"));
    } else if (templateDir != null) {
      sourceFiles = new File[] { templateDir };
    } else {
      sourceFiles = new File[0];
    }
    for (File sourceFile : Objects
      .requireNonNull(sourceFiles, "No template files found in the provided directory")) {
      final KubernetesResource dto = Serialization.unmarshal(sourceFile);
      if (dto instanceof Template) {
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


  private static String getJKubeIconUrlFromAnnotations(Map<String, String> annotations) {
    if (annotations.containsKey(JKubeAnnotations.ICON_URL.value(true))) {
      return annotations.get(JKubeAnnotations.ICON_URL.value(true));
    }
    if (annotations.containsKey(JKubeAnnotations.ICON_URL.value())) {
      return annotations.get(JKubeAnnotations.ICON_URL.value());
    }
    return null;
  }
}
