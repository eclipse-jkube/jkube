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

import io.fabric8.openshift.api.model.Template;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuration for a helm chart
 * @author roland
 * @since 11/08/16
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class HelmConfig {

  private String chart;
  private String chartExtension;
  private String version;
  private String description;
  private String home;
  private List<String> sources;
  private List<Maintainer> maintainers;
  private String icon;
  private List<String> keywords;
  private String engine;
  private List<File> additionalFiles;
  private List<Template> templates;

  private List<HelmType> types;
  private String sourceDir;
  private String outputDir;
  private String tarballOutputDir;
  private List<GeneratedChartListener> generatedChartListeners;
  private HelmRepository stableRepository;
  private HelmRepository snapshotRepository;
  private String security;

  // Plexus deserialization specific setters
  /**
   * Used by Plexus/Eclipse Sisu deserialization (pom.xml Unmarshalling)
   *
   * @param types String with a comma separated list of {@link HelmType}
   */
  public void setType(String types) {
    setTypes(HelmType.parseString(types));
  }

  public enum HelmType {
    KUBERNETES("helm", "kubernetes", "kubernetes", "Kubernetes"),
    OPENSHIFT("helmshift", "openshift","openshift", "OpenShift");

    private final String classifier;
    private final String sourceDir;
    private final String outputDir;
    private final String description;

    HelmType(String classifier, String sourceDir, String outputDir, String description) {
      this.classifier = classifier;
      this.sourceDir = sourceDir;
      this.outputDir = outputDir;
      this.description = description;
    }

    public String getClassifier() {
      return classifier;
    }

    public String getSourceDir() {
      return sourceDir;
    }

    public String getOutputDir() {
      return outputDir;
    }

    public String getDescription() {
      return description;
    }

    public static List<HelmType> parseString(String types) {
      return Optional.ofNullable(types)
          .map(t -> t.split(","))
          .map(Stream::of)
          .map(s -> s.filter(StringUtils::isNotBlank).map(String::toUpperCase).map(HelmType::valueOf)
              .collect(Collectors.toList()))
          .orElse(Collections.emptyList());
    }
  }
}
