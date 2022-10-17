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
package org.eclipse.jkube.kit.common.service;

import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.summary.ImageSummary;
import org.eclipse.jkube.kit.common.summary.KubernetesResourceSummary;
import org.eclipse.jkube.kit.common.summary.Summary;
import org.eclipse.jkube.kit.common.util.FileUtil;

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

public class SummaryService {
  private static final String SUMMARY_FILE_NAME = "summary.json";
  private static final String DASHED_LINE = "-------------------------------";
  private static final String LIST_ELEMENT = " - %s";
  private static File summaryFile = null;
  private final File summaryOutputDir;
  private final KitLogger logger;
  private final boolean summaryEnabled;

  public SummaryService(File summaryOutputDirectory, KitLogger kitLogger, boolean summaryEnabled) {
    this.summaryOutputDir = summaryOutputDirectory;
    this.logger = kitLogger;
    this.summaryEnabled = summaryEnabled;
  }


  public void addGeneratedResourceFile(File resourceFilePath) {
    addToSummary(s -> {
      s.setGeneratedResourceFiles(createOrAddToExistingList(s.getGeneratedResourceFiles(), resourceFilePath));
      return s;
    });
  }

  public void addAppliedKubernetesResource(KubernetesResourceSummary kubernetesResource) {
    addToSummary(s -> {
      s.setAppliedKubernetesResources(createOrAddToExistingList(s.getAppliedKubernetesResources(), kubernetesResource));
      return s;
    });
  }

  public void addDeletedKubernetesResource(KubernetesResourceSummary kubernetesResource) {
    addToSummary(s -> {
      s.setDeletedKubernetesResources(createOrAddToExistingList(s.getDeletedKubernetesResources(), kubernetesResource));
      return s;
    });
  }

  public void addToGenerators(String generator) {
    addToSummary(s -> {
      s.setGeneratorsApplied(createOrAddToExistingList(s.getGeneratorsApplied(), generator));
      return s;
    });
  }

  public void addToEnrichers(String enricher) {
    addToSummary(s -> {
      s.setEnrichersApplied(createOrAddToExistingList(s.getEnrichersApplied(), enricher));
      return s;
    });
  }

  public void addToActions(String action) {
    addToSummary(s -> {
      s.setActionsRun(createOrAddToExistingList(s.getActionsRun(), action));
      return s;
    });
  }

  public void setActionType(String actionType) {
    addToSummary(s -> {
      s.setActionType(actionType);
      return s;
    });
  }

  public void setSuccessful(boolean isSuccessful) {
    addToSummary(s -> {
      s.setSuccessful(isSuccessful);
      return s;
    });
  }

  public void setFailureCause(String failureCause) {
    addToSummary(s -> {
      s.setFailureCause(failureCause);
      return s;
    });
  }

  public void setDockerFileImageSummary(String imageName, String dockerFileLocation) {
    addToSummary(s -> {
      s.setImageSummariesMap(createOrAddToExistingMap(s.getImageSummariesMap(), imageName, () -> ImageSummary.builder().build(), is -> {
        is.setDockerfilePath(dockerFileLocation);
        return is;
      }));
      return s;
    });
  }

  public void setImageShaImageSummary(String imageName, String imageSha) {
    addToSummary(s -> {
      s.setImageSummariesMap(createOrAddToExistingMap(s.getImageSummariesMap(), imageName, () -> ImageSummary.builder().build(), is -> {
        is.setImageSha(imageSha);
        return is;
      }));
      return s;
    });
  }

  public void setImageStreamUsedImageSummary(String imageName, String imageStreamUsed) {
    addToSummary(s -> {
      s.setImageSummariesMap(createOrAddToExistingMap(s.getImageSummariesMap(), imageName, () -> ImageSummary.builder().build(), is -> {
        is.setImageStreamUsed(imageStreamUsed);
        return is;
      }));
      return s;
    });
  }

  public void setBaseImageNameImageSummary(String imageName, String baseImage) {
    addToSummary(s -> {
      s.setImageSummariesMap(createOrAddToExistingMap(s.getImageSummariesMap(), imageName, () -> ImageSummary.builder().build(), is -> {
        is.setBaseImageName(baseImage);
        return is;
      }));
      return s;
    });
  }

  public void setPushRegistry(String registry) {
    addToSummary(s -> {
      s.setPushRegistry(registry);
      return s;
    });
  }

  public void setBuildStrategy(String buildStrategy) {
    addToSummary(s -> {
      s.setBuildStrategy(buildStrategy);
      return s;
    });
  }

  public void setAppliedClusterUrl(String targetClusterUrl) {
    addToSummary(s -> {
      s.setAppliedClusterUrl(targetClusterUrl);
      return s;
    });
  }

  public void setUndeployedClusterUrl(String targetClusterUrl) {
    addToSummary(s -> {
      s.setUndeployedClusterUrl(targetClusterUrl);
      return s;
    });
  }

  public void setOpenShiftBuildConfigName(String buildConfigName) {
    addToSummary(s -> {
      s.setOpenShiftBuildConfigName(buildConfigName);
      return s;
    });
  }

  public void setHelmChartName(String chart) {
    addToSummary(s -> {
      s.setHelmChartName(chart);
      return s;
    });
  }

  public void setHelmChartLocation(File chart) {
    addToSummary(s -> {
      s.setHelmChart(chart);
      return s;
    });
  }

  public void setHelmRepository(String helmRepository) {
    addToSummary(s -> {
      s.setHelmRepository(helmRepository);
      return s;
    });
  }

  public void setHelmChartCompressedLocation(File chartCompressed) {
    addToSummary(s -> {
      s.setHelmChartCompressed(chartCompressed);
      return s;
    });
  }

  public void setAggregateResourceFile(File aggregateResourceFile) {
    addToSummary(s -> {
      s.setAggregateResourceFile(aggregateResourceFile);
      return s;
    });
  }

  public void clear() {
    if (summaryFile != null && summaryFile.exists()) {
      try {
        Files.delete(summaryFile.toPath());
      } catch (IOException e) {
        logger.verbose("Unable to delete summary file", e);
      }
    }
  }

  public void setFailureAndCause(String failureMessage) {
    setSuccessful(false);
    setFailureCause(failureMessage);
  }

  public void printSummary(File baseDirectory) {
    Summary summaryInstance = loadSummaryFromFile();
    if (summaryInstance != null && summaryEnabled) {
      printBanner();
      printCommonSummary(summaryInstance, baseDirectory);
      printBuildSummary(summaryInstance);
      printPushSummary(summaryInstance);
      printResourceSummary(summaryInstance, baseDirectory);
      printApplySummary(summaryInstance);
      printUndeploySummary(summaryInstance);
      printHelmSummary(summaryInstance, baseDirectory);
      printHelmPushSummary(summaryInstance);
      printActions(summaryInstance);
      logger.info(DASHED_LINE);
      if (summaryInstance.isSuccessful()) {
        logger.info("SUCCESS");
      } else {
        logger.error("FAILURE [%s]", summaryInstance.getFailureCause());
      }
      logger.info(DASHED_LINE);
    }
  }

  private void printActions(Summary summaryInstance) {
    if (StringUtils.isNotBlank(summaryInstance.getActionType()) &&
        summaryInstance.getActionsRun() != null &&
        !summaryInstance.getActionsRun().isEmpty()) {
      logger.info("%s executed : [ %s ]", summaryInstance.getActionType(), String.join(", ", summaryInstance.getActionsRun()));
    }
  }

  private void printCommonSummary(Summary summaryInstance, File baseDirectory) {
    if (summaryInstance.getImageSummariesMap() != null && !summaryInstance.getImageSummariesMap().isEmpty()) {
      logger.info("Container images:");
      for (Map.Entry<String, ImageSummary> imageSummaryEntry : summaryInstance.getImageSummariesMap().entrySet()) {
        printImageSummary(baseDirectory, imageSummaryEntry.getKey(), imageSummaryEntry.getValue());
      }
      logger.info("");
    }
  }

  private void printImageSummary(File baseDirectory, String imageName, ImageSummary imageSummary) {
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

  private void printBanner() {
    logger.info(" __ / / //_/ / / / _ )/ __/");
    logger.info("/ // / ,< / /_/ / _  / _/  ");
    logger.info("\\___/_/|_|\\____/____/___/  \n");
    logger.info(DASHED_LINE);
    logger.info("      SUMMARY");
    logger.info(DASHED_LINE);
  }

  private void printBuildSummary(Summary summary) {
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

  private void printResourceSummary(Summary summary, File baseDir) {
    if (summary.getGeneratedResourceFiles() != null && !summary.getGeneratedResourceFiles().isEmpty()) {
      if (summary.getEnrichersApplied() != null && summary.getEnrichersApplied().size() < 20) {
        logger.info("Enrichers applied: [%s]", String.join(", ", summary.getEnrichersApplied()));
      }
      logger.info("Generated resources:");
      List<String> generatedFilesResourcePaths = summary.getGeneratedResourceFiles().stream()
          .map(File::getAbsolutePath)
          .map(p -> FileUtil.getRelativeFilePath(baseDir.getAbsolutePath(), p))
          .collect(Collectors.toList());
      logList(generatedFilesResourcePaths);
    }
    if (summary.getAggregateResourceFile() != null) {
      logger.info(LIST_ELEMENT, FileUtil.getRelativeFilePath(baseDir.getAbsolutePath(), summary.getAggregateResourceFile().getAbsolutePath()));
    }
    logger.info("");
  }

  private void printApplySummary(Summary summary) {
    if (StringUtils.isNotBlank(summary.getAppliedClusterUrl())) {
      String appliedResources = createKubernetesResourceSummary(summary.getAppliedKubernetesResources());
      logger.info("Applied resources to %s: %s", summary.getAppliedClusterUrl(), appliedResources);
      logger.info("");
    }
  }

  private void printUndeploySummary(Summary summary) {
    if (StringUtils.isNotBlank(summary.getUndeployedClusterUrl())) {
      String deletedResources = createKubernetesResourceSummary(summary.getDeletedKubernetesResources());
      logger.info("Undeployed resources to %s: %s", summary.getUndeployedClusterUrl(), deletedResources);
      logger.info("");
    }
  }

  private String createKubernetesResourceSummary(List<KubernetesResourceSummary> kubernetesResourceSummaries) {
    StringBuilder sb = new StringBuilder();
    if (kubernetesResourceSummaries != null && !kubernetesResourceSummaries.isEmpty()) {
      sb.append("[");
      for (int i = 0; i < kubernetesResourceSummaries.size(); i++) {
        KubernetesResourceSummary kubernetesResourceSummary = kubernetesResourceSummaries.get(i);
        sb.append(kubernetesResourceSummary.getKind())
            .append("/")
            .append(kubernetesResourceSummary.getResourceName());

        if (i < kubernetesResourceSummaries.size() - 1) {
          sb.append(", ");
        }
      }

      sb.append("]");
    }
    return sb.toString();
  }

  private void printPushSummary(Summary summary) {
    if (StringUtils.isNotBlank(summary.getPushRegistry())) {
      logger.info("Registry: %s", summary.getPushRegistry());
    }
  }

  private void printHelmSummary(Summary summary, File baseDir) {
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

  private void printHelmPushSummary(Summary summary) {
    if (StringUtils.isNotBlank(summary.getHelmRepository())) {
      logger.info("Repository : %s", summary.getHelmRepository());
    }
  }

  private void logList(List<String> list) {
    for (String item : list) {
      logger.info(LIST_ELEMENT, item);
    }
  }

  private void addToSummary(UnaryOperator<Summary> summaryConsumer) {
    if (summaryOutputDir != null) {
      if (!summaryOutputDir.exists() && !summaryOutputDir.mkdir()) {
        logger.debug("Failure in creating Summary output directory %s", summaryOutputDir.getAbsolutePath());
        return;
      }
      Summary summary = loadSummaryFromFile();
      if (summary == null) {
        summary = new Summary();
      }
      summary = summaryConsumer.apply(summary);
      writeSummaryToFile(summary);
    }
  }

  private <K, V> Map<K, V> createOrAddToExistingMap(Map<K, V> orignalMap, K key,  Supplier<V> emptySupplier, UnaryOperator<V> valueConsumer) {
    if (orignalMap == null) {
      orignalMap = new HashMap<>();
    }
    orignalMap.computeIfAbsent(key, s -> emptySupplier.get());
    orignalMap.put(key, valueConsumer.apply(orignalMap.get(key)));
    return orignalMap;
  }

  private <T> List<T> createOrAddToExistingList(List<T> currentList, T item) {
    if (currentList == null) {
      currentList = new ArrayList<>();
    }
    if (!currentList.contains(item)) {
      currentList.add(item);
    }
    return currentList;
  }

  private synchronized Summary loadSummaryFromFile() {
    try {
      if (!isValidSummaryFile()) {
        summaryFile = createSummaryFile();
        summaryFile.deleteOnExit();
      } else {
        return Serialization.jsonMapper().readValue(summaryFile, Summary.class);
      }
    } catch (IOException ioException) {
      logger.verbose("Failure in loading Summary file: ", ioException);
    }
    return null;
  }

  private File createSummaryFile() throws IOException {
    if (!summaryOutputDir.exists()) {
      createDirectory(summaryOutputDir);
    }
    File newSummaryFile = new File(summaryOutputDir, SUMMARY_FILE_NAME);
    if (newSummaryFile.createNewFile()) {
      logger.verbose("Created summary file");
    }
    return newSummaryFile;
  }

  private void writeSummaryToFile(Summary summary) {
    if (isValidSummaryFile()) {
      try (FileWriter fileWriter = new FileWriter(summaryFile)) {
        fileWriter.write(Serialization.jsonMapper().writeValueAsString(summary));
      } catch (IOException ioException) {
        logger.verbose("Failure in writing to Summary file: ", ioException);
      }
    }
  }

  private static boolean isValidSummaryFile() {
    return summaryFile != null &&
        StringUtils.isNotBlank(summaryFile.getAbsolutePath()) &&
        summaryFile.getAbsolutePath().contains(SUMMARY_FILE_NAME) &&
        summaryFile.exists();
  }
}
