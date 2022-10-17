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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.RegistryConfig;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.common.archive.JKubeTarArchiver;
import org.eclipse.jkube.kit.common.service.SummaryService;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;

import com.google.common.collect.Streams;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.openshift.api.model.Parameter;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import static org.eclipse.jkube.kit.common.util.MapUtil.getNestedMap;
import static org.eclipse.jkube.kit.common.util.TemplateUtil.escapeYamlTemplate;
import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.isRepositoryValid;
import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.selectHelmRepository;
import static org.eclipse.jkube.kit.resource.helm.HelmServiceUtil.setAuthentication;

public class HelmService {

  private static final String YAML_EXTENSION = ".yaml";
  private static final String CHART_API_VERSION = "v1";
  private static final String CHART_FILENAME = "Chart" + YAML_EXTENSION;
  private static final String VALUES_FILENAME = "values" + YAML_EXTENSION;
  private static final String GOLANG_EXPRESSION_REGEX = "\\{\\{.+}}";

  private final JKubeConfiguration jKubeConfiguration;
  private final KitLogger logger;
  private final SummaryService summaryService;

  public HelmService(JKubeConfiguration jKubeConfiguration, KitLogger logger, SummaryService summaryService) {
    this.jKubeConfiguration = jKubeConfiguration;
    this.logger = logger;
    this.summaryService = summaryService;
  }

  /**
   * Generates Helm Charts for the provided {@link HelmConfig}.
   *
   * @param helmConfig Configuration for which to generate the Charts.
   * @throws IOException in case of any I/O exception when writing the Chart files.
   */
  public void generateHelmCharts(HelmConfig helmConfig) throws IOException {
    for (HelmConfig.HelmType helmType : helmConfig.getTypes()) {
      logger.info("Creating Helm Chart \"%s\" for %s", helmConfig.getChart(), helmType.getDescription());
      logger.debug("Source directory: %s", helmConfig.getSourceDir());
      logger.debug("OutputDir: %s", helmConfig.getOutputDir());

      final File sourceDir = prepareSourceDir(helmConfig, helmType);
      final File outputDir = prepareOutputDir(helmConfig, helmType);
      final File tarballOutputDir = new File(Objects.requireNonNull(helmConfig.getTarballOutputDir(),
          "Tarball output directory is required"));
      final File templatesDir = new File(outputDir, "templates");
      FileUtils.forceMkdir(templatesDir);

      logger.debug("Processing source files");
      processSourceFiles(sourceDir, templatesDir);
      logger.debug("Creating %s", CHART_FILENAME);
      createChartYaml(helmConfig, outputDir);
      logger.debug("Copying additional files");
      copyAdditionalFiles(helmConfig, outputDir);
      logger.debug("Gathering parameters for placeholders");
      final List<HelmParameter> parameters = collectParameters(helmConfig);
      logger.debug("Generating values.yaml");
      createValuesYaml(parameters, outputDir);
      logger.debug("Interpolating YAML Chart templates");
      interpolateChartTemplates(parameters, templatesDir);
      final File tarballFile = new File(tarballOutputDir, String.format("%s-%s-%s.%s",
          helmConfig.getChart(), helmConfig.getVersion(), helmType.getClassifier(), helmConfig.getChartExtension()));
      logger.debug("Creating Helm configuration Tarball: '%s'", tarballFile.getAbsolutePath());
      final Consumer<TarArchiveEntry> prependNameAsDirectory = tae ->
          tae.setName(String.format("%s/%s", helmConfig.getChart(), tae.getName()));
      summaryService.setHelmChartName(helmConfig.getChart());
      summaryService.setHelmChartLocation(outputDir);
      File helmTarball = JKubeTarArchiver.createTarBall(
          tarballFile, outputDir, FileUtil.listFilesAndDirsRecursivelyInDirectory(outputDir), Collections.emptyMap(),
          ArchiveCompression.fromFileName(tarballFile.getName()), null, prependNameAsDirectory);
      summaryService.setHelmChartCompressedLocation(helmTarball);
      Optional.ofNullable(helmConfig.getGeneratedChartListeners()).orElse(Collections.emptyList())
          .forEach(listener -> listener.chartFileGenerated(helmConfig, helmType, tarballFile));
    }
  }

  /**
   * Uploads the charts defined in the provided {@link HelmConfig} to the applicable configured repository.
   *
   * <p> For Charts with versions ending in "-SNAPSHOT" the {@link HelmConfig#getSnapshotRepository()} is used.
   * {@link HelmConfig#getStableRepository()} is used for other versions.
   *
   *
   * @param helm Configuration for which to generate the Charts.
   * @throws BadUploadException in case the chart cannot be uploaded.
   * @throws IOException in case of any I/O exception when .
   */
  public void uploadHelmChart(HelmConfig helm) throws BadUploadException, IOException {
    final HelmRepository helmRepository = selectHelmRepository(helm);
    if (isRepositoryValid(helmRepository)) {
      final List<RegistryServerConfiguration> registryServerConfigurations = Optional
          .ofNullable(jKubeConfiguration).map(JKubeConfiguration::getRegistryConfig).map(RegistryConfig::getSettings)
          .orElse(Collections.emptyList());
      final UnaryOperator<String> passwordDecryptor = Optional.ofNullable(jKubeConfiguration)
          .map(JKubeConfiguration::getRegistryConfig).map(RegistryConfig::getPasswordDecryptionMethod)
          .orElse(s -> s);
      setAuthentication(helmRepository, logger, registryServerConfigurations, passwordDecryptor);
      uploadHelmChart(helm, helmRepository);
      summaryService.setHelmRepository(helmRepository.getName());
    } else {
      String error = "No repository or invalid repository configured for upload";
      logger.error(error);
      throw new IllegalStateException(error);
    }
  }

  private void uploadHelmChart(HelmConfig helmConfig, HelmRepository helmRepository)
      throws IOException, BadUploadException {

    for (HelmConfig.HelmType helmType : helmConfig.getTypes()) {
      final HelmUploader helmUploader = new HelmUploader(logger);
      logger.info("Uploading Helm Chart \"%s\" to %s", helmConfig.getChart(), helmRepository.getName());
      logger.debug("OutputDir: %s", helmConfig.getOutputDir());

      final File tarballOutputDir =
          new File(Objects.requireNonNull(helmConfig.getTarballOutputDir(), "Tarball output directory is required"));
      final File tarballFile = new File(tarballOutputDir, String.format("%s-%s-%s.%s",
          helmConfig.getChart(), helmConfig.getVersion(), helmType.getClassifier(), helmConfig.getChartExtension()));

      helmUploader.uploadSingle(tarballFile, helmRepository);
    }
  }


  static File prepareSourceDir(HelmConfig helmConfig, HelmConfig.HelmType type) throws IOException {
    final File sourceDir = new File(helmConfig.getSourceDir(), type.getSourceDir());
    if (!sourceDir.isDirectory()) {
      throw new IOException(String.format(
        "Chart source directory %s does not exist so cannot make chart \"%s\". " +
          "Probably you need run 'mvn kubernetes:resource' before.",
        sourceDir.getAbsolutePath(), helmConfig.getChart()));
    }
    if (!containsYamlFiles(sourceDir)) {
      throw new IOException(String.format(
        "Chart source directory %s does not contain any YAML manifest to make chart \"%s\". " +
          "Probably you need run 'mvn kubernetes:resource' before.",
        sourceDir.getAbsolutePath(), helmConfig.getChart()));
    }
    return sourceDir;
  }

  private static File prepareOutputDir(HelmConfig helmConfig, HelmConfig.HelmType type) throws IOException {
    final File outputDir = new File(helmConfig.getOutputDir(), type.getOutputDir());
    if (outputDir.exists() && !outputDir.isDirectory()) {
      FileUtils.forceDelete(outputDir);
    }
    FileUtils.forceMkdir(outputDir);
    return outputDir;
  }

  public static boolean isYaml(File file) {
    return file.getName().toLowerCase().matches(".*?\\.ya?ml$");
  }

  public static List<File> listYamls(File directory) {
    return Stream.of(Optional.ofNullable(directory.listFiles()).orElse(new File[0]))
        .filter(File::isFile)
        .filter(HelmService::isYaml)
        .collect(Collectors.toList());
  }

  public static boolean containsYamlFiles(File directory) {
    return !listYamls(directory).isEmpty();
  }

  private static void processSourceFiles(File sourceDir, File templatesDir) throws IOException {
    for (File file : listYamls(sourceDir)) {
      final KubernetesResource dto = ResourceUtil.load(file, KubernetesResource.class);
      if (dto instanceof Template) {
        splitAndSaveTemplate((Template) dto, templatesDir);
      } else {
        final String fileName = FileUtil.stripPostfix(FileUtil.stripPostfix(file.getName(), ".yml"), YAML_EXTENSION) + YAML_EXTENSION;
        File targetFile = new File(templatesDir, fileName);
        // lets escape any {{ or }} characters to avoid creating invalid templates
        String text = FileUtils.readFileToString(file, Charset.defaultCharset());
        text = escapeYamlTemplate(text);
        FileUtils.write(targetFile, text, Charset.defaultCharset());
      }
    }
  }

  private static void splitAndSaveTemplate(Template template, File templatesDir) throws IOException {
    for (HasMetadata object : Optional.ofNullable(template.getObjects()).orElse(Collections.emptyList())) {
      String name = KubernetesResourceUtil.getNameWithSuffix(KubernetesHelper.getName(object),
          KubernetesHelper.getKind(object)) + YAML_EXTENSION;
      File outFile = new File(templatesDir, name);
      ResourceUtil.save(outFile, object);
    }
  }

  static void createChartYaml(HelmConfig helmConfig, File outputDir) throws IOException {
    final Chart chart = new Chart();
    chart.setApiVersion(CHART_API_VERSION);
    chart.setName(helmConfig.getChart());
    chart.setVersion(helmConfig.getVersion());
    chart.setDescription(helmConfig.getDescription());
    chart.setHome(helmConfig.getHome());
    chart.setSources(helmConfig.getSources());
    chart.setMaintainers(helmConfig.getMaintainers());
    chart.setIcon(helmConfig.getIcon());
    chart.setKeywords(helmConfig.getKeywords());
    chart.setEngine(helmConfig.getEngine());
    chart.setDependencies(helmConfig.getDependencies());

    File outputChartFile = new File(outputDir, CHART_FILENAME);
    ResourceUtil.save(outputChartFile, chart, ResourceFileType.yaml);
  }

  private static void copyAdditionalFiles(HelmConfig helmConfig, File outputDir) throws IOException {
    for (File additionalFile : Optional.ofNullable(helmConfig.getAdditionalFiles()).orElse(Collections.emptyList())) {
      FileUtils.copyFile(additionalFile, new File(outputDir, additionalFile.getName()));
    }
  }

  private static String interpolateTemplateWithHelmParameter(String template, HelmParameter parameter) {
    String name = parameter.getParameter().getName();
    final String from = "$" + name;
    final String braceEnclosedFrom = "${" + name + "}";
    final String quotedBraceEnclosedFrom = "\"" + braceEnclosedFrom + "\"";
    String answer = template;
    final String to = expression(parameter);
    answer = answer.replace(quotedBraceEnclosedFrom, to);
    answer = answer.replace(braceEnclosedFrom, to);
    answer = answer.replace(from, to);
    return answer;
  }

  private static String expression(HelmParameter parameter) {
    final String value = Optional.ofNullable(parameter.getParameter().getValue()).map(StringUtils::trimToEmpty).orElse("");
    if (value.matches(GOLANG_EXPRESSION_REGEX)) {
      return value;
    }
    String defaultExpression = "";
    String required = "";
    if (StringUtils.isNotBlank(value)) {
      defaultExpression = " | default \"" + value + "\"";
    }
    if (Boolean.TRUE.equals(parameter.getParameter().getRequired())) {
      required = "required \"A valid .Values." + parameter.getHelmName() + " entry required!\" ";
    }
    return "{{ " + required + ".Values." + parameter.getHelmName() + defaultExpression + " }}";
  }

  private static void interpolateTemplateParameterExpressionsWithHelmExpressions(File file, List<HelmParameter> helmParameters) throws IOException {
    final String originalTemplate = FileUtils.readFileToString(file, Charset.defaultCharset());

    String interpolatedTemplate = originalTemplate;
    for (HelmParameter helmParameter : helmParameters) {
      interpolatedTemplate = interpolateTemplateWithHelmParameter(interpolatedTemplate, helmParameter);
    }
    if (!originalTemplate.equals(interpolatedTemplate)) {
      FileUtils.writeStringToFile(file, interpolatedTemplate, Charset.defaultCharset());
    }
  }

  private static void interpolateChartTemplates(List<HelmParameter> helmParameters, File templatesDir) throws IOException {
    // now lets replace all the parameter expressions in each template
    for (File file : listYamls(templatesDir)) {
      interpolateTemplateParameterExpressionsWithHelmExpressions(file, helmParameters);
    }
  }

  private static void createValuesYaml(List<HelmParameter> helmParameters, File outputDir) throws IOException {
    final Map<String, String> values = helmParameters.stream()
        .filter(hp -> hp.getParameter().getValue() != null)
        // Placeholders replaced by Go expressions don't need to be persisted in the values.yaml file
        .filter(hp -> !hp.getParameter().getValue().trim().matches(GOLANG_EXPRESSION_REGEX))
        .collect(Collectors.toMap(HelmParameter::getHelmName, hp -> hp.getParameter().getValue()));

    File outputChartFile = new File(outputDir, VALUES_FILENAME);
    ResourceUtil.save(outputChartFile, getNestedMap(values), ResourceFileType.yaml);
  }

  private static List<HelmParameter> collectParameters(HelmConfig helmConfig) {
    final List<HelmParameter> parameters = new ArrayList<>();
    final Stream<Parameter> fromYaml = Optional.ofNullable(helmConfig.getParameterTemplates())
        .orElse(Collections.emptyList()).stream()
        .map(Template::getParameters).flatMap(List::stream);
    final Stream<Parameter> fromConfig = Optional.ofNullable(helmConfig.getParameters())
        .orElse(Collections.emptyList()).stream();
    Streams.concat(fromYaml, fromConfig).map(HelmParameter::new).forEach(parameters::add);
    parameters.stream().filter(p -> p.getParameter().getName() == null).findAny().ifPresent(p -> {
      throw new IllegalArgumentException("Helm parameters must be declared with a valid name: " + p.getParameter().toString());
    });
    return parameters;
  }
}
