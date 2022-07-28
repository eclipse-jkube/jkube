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
package org.eclipse.jkube.kit.common.summary;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Summary {
  private Map<String, ImageSummary> imageSummariesMap;
  private String buildStrategy;
  private List<String> generatorsApplied;
  private String openShiftBuildConfigName;
  private boolean successful;
  private String failureCause;
  private File aggregateResourceFile;
  private String appliedClusterUrl;
  private String undeployedClusterUrl;
  private String pushRegistry;
  private String helmChartName;
  private File helmChartCompressed;
  private File helmChart;
  private String helmRepository;
  private List<File> generatedResourceFiles;
  private List<KubernetesResourceSummary> appliedKubernetesResources;
  private List<KubernetesResourceSummary> deletedKubernetesResources;
  private List<String> enrichersApplied;
}
