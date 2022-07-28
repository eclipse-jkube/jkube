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
package org.eclipse.jkube.kit.common.util;

import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.summary.ImageSummary;
import org.eclipse.jkube.kit.common.summary.KubernetesResourceSummary;
import org.eclipse.jkube.kit.common.summary.Summary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static org.eclipse.jkube.kit.common.util.FileUtil.createDirectory;

public class SummaryUtil {
  private static final String SUMMARY_FILE_NAME = "summary.json";
  private static final String DASHED_LINE = "-------------------------------";
  private static final String LIST_ELEMENT = " - %s";
  private static File summaryFile = null;
  private static File summaryOutputDir = null;
  private static KitLogger logger = null;

  private SummaryUtil() { }

  public static void initSummary(File summaryOutputDirectory, KitLogger kitLogger) {
    summaryOutputDir = summaryOutputDirectory;
    logger = kitLogger;
  }

  public static void addGeneratedResourceFile(File resourceFilePath) {
    addToSummary(s -> {
      s.setGeneratedResourceFiles(createOrAddToExistingList(s.getGeneratedResourceFiles(), resourceFilePath));
      return s;
    });
  }

  public static void addAppliedKubernetesResource(KubernetesResourceSummary kubernetesResource) {
    addToSummary(s -> {
      s.setAppliedKubernetesResources(createOrAddToExistingList(s.getAppliedKubernetesResources(), kubernetesResource));
      return s;
    });
  }

  public static void addDeletedKubernetesResource(KubernetesResourceSummary kubernetesResource) {
    addToSummary(s -> {
      s.setDeletedKubernetesResources(createOrAddToExistingList(s.getDeletedKubernetesResources(), kubernetesResource));
      return s;
    });
  }

  public static void addToGenerators(String generator) {
    addToSummary(s -> {
      s.setGeneratorsApplied(createOrAddToExistingList(s.getGeneratorsApplied(), generator));
      return s;
    });
  }

  public static void addToEnrichers(String enricher) {
    addToSummary(s -> {
      s.setEnrichersApplied(createOrAddToExistingList(s.getEnrichersApplied(), enricher));
      return s;
    });
  }

  public static void setSuccessful(boolean isSuccessful) {
    addToSummary(s -> {
      s.setSuccessful(isSuccessful);
      return s;
    });
  }

  public static void setFailureCause(String failureCause) {
    addToSummary(s -> {
      s.setFailureCause(failureCause);
      return s;
    });
  }

  public static void setDockerFileImageSummary(String imageName, String dockerFileLocation) {
    addToSummary(s -> {
      s.setImageSummariesMap(createOrAddToExistingMap(s.getImageSummariesMap(), imageName, () -> ImageSummary.builder().build(), is -> {
        is.setDockerfilePath(dockerFileLocation);
        return is;
      }));
      return s;
    });
  }

  public static void setImageShaImageSummary(String imageName, String imageSha) {
    addToSummary(s -> {
      s.setImageSummariesMap(createOrAddToExistingMap(s.getImageSummariesMap(), imageName, () -> ImageSummary.builder().build(), is -> {
        is.setImageSha(imageSha);
        return is;
      }));
      return s;
    });
  }

  public static void setImageStreamUsedImageSummary(String imageName, String imageStreamUsed) {
    addToSummary(s -> {
      s.setImageSummariesMap(createOrAddToExistingMap(s.getImageSummariesMap(), imageName, () -> ImageSummary.builder().build(), is -> {
        is.setImageStreamUsed(imageStreamUsed);
        return is;
      }));
      return s;
    });
  }

  public static void setBaseImageNameImageSummary(String imageName, String baseImage) {
    addToSummary(s -> {
      s.setImageSummariesMap(createOrAddToExistingMap(s.getImageSummariesMap(), imageName, () -> ImageSummary.builder().build(), is -> {
        is.setBaseImageName(baseImage);
        return is;
      }));
      return s;
    });
  }

  public static void setPushRegistry(String registry) {
    addToSummary(s -> {
      s.setPushRegistry(registry);
      return s;
    });
  }

  public static void setBuildStrategy(String buildStrategy) {
    addToSummary(s -> {
      s.setBuildStrategy(buildStrategy);
      return s;
    });
  }

  public static void setAppliedClusterUrl(String targetClusterUrl) {
    addToSummary(s -> {
      s.setAppliedClusterUrl(targetClusterUrl);
      return s;
    });
  }

  public static void setUndeployedClusterUrl(String targetClusterUrl) {
    addToSummary(s -> {
      s.setUndeployedClusterUrl(targetClusterUrl);
      return s;
    });
  }

  public static void setOpenShiftBuildConfigName(String buildConfigName) {
    addToSummary(s -> {
      s.setOpenShiftBuildConfigName(buildConfigName);
      return s;
    });
  }

  public static void setHelmChartName(String chart) {
    addToSummary(s -> {
      s.setHelmChartName(chart);
      return s;
    });
  }

  public static void setHelmChartLocation(File chart) {
    addToSummary(s -> {
      s.setHelmChart(chart);
      return s;
    });
  }

  public static void setHelmRepository(String helmRepository) {
    addToSummary(s -> {
      s.setHelmRepository(helmRepository);
      return s;
    });
  }

  public static void setHelmChartCompressedLocation(File chartCompressed) {
    addToSummary(s -> {
      s.setHelmChartCompressed(chartCompressed);
      return s;
    });
  }

  public static void setAggregateResourceFile(File aggregateResourceFile) {
    addToSummary(s -> {
      s.setAggregateResourceFile(aggregateResourceFile);
      return s;
    });
  }

  public static void clear() {
    if (summaryFile != null && summaryFile.exists()) {
      try {
        Files.delete(summaryFile.toPath());
        summaryFile = null;
      } catch (IOException e) {
        throw new IllegalStateException("Unable to delete summary file", e);
      }
    }
  }

  public static <T extends Exception> void setFailureIfSummaryEnabledOrThrow(boolean summaryEnabled, String failureMessage, Supplier<T> exceptionSupplier) throws T {
    if (summaryEnabled) {
      setSuccessful(false);
      setFailureCause(failureMessage);
    } else {
      throw exceptionSupplier.get();
    }
  }

  public static void printSummary(File baseDirectory, boolean summaryEnabled) {
    Summary summaryInstance = loadSummaryFromFile();
    if (summaryInstance != null && summaryEnabled) {
      printBanner(logger);
      printCommonSummary(summaryInstance, baseDirectory, logger);
      printBuildSummary(summaryInstance, logger);
      printPushSummary(summaryInstance, logger);
      printResourceSummary(summaryInstance, baseDirectory, logger);
      printApplySummary(summaryInstance, logger);
      printUndeploySummary(summaryInstance, logger);
      printHelmSummary(summaryInstance, baseDirectory, logger);
      printHelmPushSummary(summaryInstance, logger);
      logger.info(DASHED_LINE);
      if (summaryInstance.isSuccessful()) {
        logger.info("SUCCESS");
      } else {
        logger.error("FAILURE [%s]", summaryInstance.getFailureCause());
      }
      logger.info(DASHED_LINE);
    }
  }

  private static void printCommonSummary(Summary summaryInstance, File baseDirectory, KitLogger logger) {
    if (summaryInstance.getImageSummariesMap() != null && !summaryInstance.getImageSummariesMap().isEmpty()) {
      logger.info("Container images:");
      for (Map.Entry<String, ImageSummary> imageSummaryEntry : summaryInstance.getImageSummariesMap().entrySet()) {
        printImageSummary(logger, baseDirectory, imageSummaryEntry.getKey(), imageSummaryEntry.getValue());
      }
      logger.info("");
    }
  }

  private static void printImageSummary(KitLogger logger, File baseDirectory, String imageName, ImageSummary imageSummary) {
    logger.info(LIST_ELEMENT, imageName);
    if (imageSummary.getBaseImageName() != null) {
      logger.info("    * Base image: %s", imageSummary.getBaseImageName());
    }
    if (imageSummary.getDockerfilePath() != null) {
      logger.info("    * Dockerfile image: %s", FileUtil.getRelativeFilePath(baseDirectory.getAbsolutePath(), imageSummary.getDockerfilePath()));
    }
    if (imageSummary.getImageStreamUsed() != null) {
      logger.info("    * ImageStream: %s", imageSummary.getImageStreamUsed());
    }
    if (imageSummary.getImageSha() != null) {
      logger.info("    * SHA: %s", imageSummary.getImageSha());
    }
  }

  private static void printBanner(KitLogger logger) {
    logger.info(" __ / / //_/ / / / _ )/ __/");
    logger.info("/ // / ,< / /_/ / _  / _/  ");
    logger.info("\\___/_/|_|\\____/____/___/  \n");
    logger.info(DASHED_LINE);
    logger.info("      SUMMARY");
    logger.info(DASHED_LINE);
  }

  private static void printBuildSummary(Summary summary, KitLogger logger) {
    if (summary.getBuildStrategy() != null) {
      logger.info("Build Strategy : %s", summary.getBuildStrategy());
    }
    if (summary.getGeneratorsApplied() != null && !summary.getGeneratorsApplied().isEmpty()) {
      logger.info("Generators applied: [%s]",  String.join(",", summary.getGeneratorsApplied()));
    }
    if (summary.getOpenShiftBuildConfigName() != null) {
      logger.info("Build config: %s", summary.getOpenShiftBuildConfigName());
    }
    logger.info("");
  }

  private static void printResourceSummary(Summary summary, File baseDir, KitLogger logger) {
    if (summary.getGeneratedResourceFiles() != null && !summary.getGeneratedResourceFiles().isEmpty()) {
      if (summary.getEnrichersApplied() != null && summary.getEnrichersApplied().size() < 20) {
        logger.info("Enrichers applied: [%s]", String.join(",", summary.getEnrichersApplied()));
      }
      logger.info("Generated resources:");
      List<String> generatedFilesResourcePaths = summary.getGeneratedResourceFiles().stream()
          .map(File::getAbsolutePath)
          .map(p -> FileUtil.getRelativeFilePath(baseDir.getAbsolutePath(), p))
          .collect(Collectors.toList());
      logList(logger, generatedFilesResourcePaths);
    }
    if (summary.getAggregateResourceFile() != null) {
      logger.info(LIST_ELEMENT, FileUtil.getRelativeFilePath(baseDir.getAbsolutePath(), summary.getAggregateResourceFile().getAbsolutePath()));
    }
    logger.info("");
  }

  private static void printApplySummary(Summary summary, KitLogger logger) {
    if (StringUtils.isNotBlank(summary.getAppliedClusterUrl())) {
      logger.info("Applied resources from %s", summary.getAppliedClusterUrl());
      printKubernetesResourceSummary(summary.getAppliedKubernetesResources(), logger);
      logger.info("");
    }
  }

  private static void printUndeploySummary(Summary summary, KitLogger logger) {
    if (StringUtils.isNotBlank(summary.getUndeployedClusterUrl())) {
      logger.info("Undeployed resources from %s", summary.getUndeployedClusterUrl());
      printKubernetesResourceSummary(summary.getDeletedKubernetesResources(), logger);
      logger.info("");
    }
  }

  private static void printKubernetesResourceSummary(List<KubernetesResourceSummary> kubernetesResourceSummaries, KitLogger logger) {
    if (kubernetesResourceSummaries != null && !kubernetesResourceSummaries.isEmpty()) {
      for (KubernetesResourceSummary kubernetesResourceSummary : kubernetesResourceSummaries) {
        logger.info(LIST_ELEMENT, kubernetesResourceSummary.getResourceName());
        if (kubernetesResourceSummary.getGroup().equals(kubernetesResourceSummary.getVersion())) {
          logger.info("   * %s %s", kubernetesResourceSummary.getGroup(), kubernetesResourceSummary.getKind());
        } else {
          logger.info("   * %s/%s %s", kubernetesResourceSummary.getGroup(), kubernetesResourceSummary.getVersion(), kubernetesResourceSummary.getKind());
        }
        logger.info("   * Namespace: %s", kubernetesResourceSummary.getNamespace());
      }
    }
  }

  private static void printPushSummary(Summary summary, KitLogger logger) {
    if (StringUtils.isNotBlank(summary.getPushRegistry())) {
      logger.info("Registry: %s", summary.getPushRegistry());
    }
  }

  private static void printHelmSummary(Summary summary, File baseDir, KitLogger logger) {
    if (StringUtils.isNotBlank(summary.getHelmChartName())) {
      logger.info("Chart : %s", summary.getHelmChartName());
    }
    if (summary.getHelmChart() != null) {
      logger.info("Location : %s", FileUtil.getRelativeFilePath(baseDir.getAbsolutePath(), summary.getHelmChart().getAbsolutePath()));
    }
    if (summary.getHelmChartCompressed() != null) {
      logger.info("Compressed : %s", FileUtil.getRelativeFilePath(baseDir.getAbsolutePath(), summary.getHelmChartCompressed().getAbsolutePath()));
    }
  }

  private static void printHelmPushSummary(Summary summary, KitLogger logger) {
    if (StringUtils.isNotBlank(summary.getHelmRepository())) {
      logger.info("Repository : %s", summary.getHelmRepository());
    }
  }

  private static void logList(KitLogger logger, List<String> list) {
    for (String item : list) {
      logger.info(LIST_ELEMENT, item);
    }
  }

  private static void addToSummary(UnaryOperator<Summary> summaryConsumer) {
    if (summaryOutputDir != null && summaryOutputDir.exists()) {
      Summary summary = loadSummaryFromFile();
      if (summary == null) {
        summary = new Summary();
      }
      summary = summaryConsumer.apply(summary);
      writeSummaryToFile(summary);
    }
  }

  private static <K, V> Map<K, V> createOrAddToExistingMap(Map<K, V> orignalMap, K key,  Supplier<V> emptySupplier, UnaryOperator<V> valueConsumer) {
    if (orignalMap == null) {
      orignalMap = new HashMap<>();
    }
    orignalMap.computeIfAbsent(key, s -> emptySupplier.get());
    orignalMap.put(key, valueConsumer.apply(orignalMap.get(key)));
    return orignalMap;
  }

  private static <T> List<T> createOrAddToExistingList(List<T> currentList, T item) {
    if (currentList == null) {
      currentList = new ArrayList<>();
    }
    if (!currentList.contains(item)) {
      currentList.add(item);
    }
    return currentList;
  }

  private static synchronized Summary loadSummaryFromFile() {
    try {
      if (!isValidSummaryFile(summaryFile)) {
        summaryFile = createSummaryFile();
        summaryFile.deleteOnExit();
        return null;
      }
      return Serialization.jsonMapper().readValue(summaryFile, Summary.class);
    } catch (IOException ioException) {
      throw new IllegalStateException("Failure in loading Summary file: ", ioException);
    }
  }

  private static File createSummaryFile() throws IOException {
    if (!summaryOutputDir.exists()) {
      createDirectory(summaryOutputDir);
    }
    File summaryFile = new File(summaryOutputDir, SUMMARY_FILE_NAME);
    if (summaryFile.createNewFile()) {
      logger.verbose("Created summary file");
    }
    return summaryFile;
  }

  private static void writeSummaryToFile(Summary summary) {
    if (isValidSummaryFile(summaryFile)) {
      try (FileWriter fileWriter = new FileWriter(summaryFile)) {
        fileWriter.write(Serialization.jsonMapper().writeValueAsString(summary));
      } catch (IOException ioException) {
        throw new IllegalStateException("Failure in writing to Summary file: ", ioException);
      }
    }
  }

  private static boolean isValidSummaryFile(File summaryFile) {
    return summaryFile != null &&
        StringUtils.isNotBlank(summaryFile.getAbsolutePath()) &&
        summaryFile.getAbsolutePath().contains(SUMMARY_FILE_NAME) &&
        summaryFile.exists();
  }
}
