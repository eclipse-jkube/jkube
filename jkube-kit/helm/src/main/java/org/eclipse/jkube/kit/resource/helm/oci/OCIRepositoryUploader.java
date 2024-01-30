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
package org.eclipse.jkube.kit.resource.helm.oci;

import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.resource.helm.BadUploadException;
import org.eclipse.jkube.kit.resource.helm.Chart;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;
import org.eclipse.jkube.kit.resource.helm.HelmUploader;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.eclipse.jkube.kit.resource.helm.HelmService.CHART_FILENAME;

public class OCIRepositoryUploader implements HelmUploader {

  @Override
  public HelmRepository.HelmRepoType getType() {
    return HelmRepository.HelmRepoType.OCI;
  }

  @Override
  public void uploadSingle(File file, HelmRepository repository)
      throws IOException, BadUploadException {
    HttpClient.Factory httpClientFactory = HttpClientUtils.getHttpClientFactory();
    try (HttpClient httpClient = httpClientFactory.newBuilder()
      .addOrReplaceInterceptor(OCIRegistryInterceptor.NAME, new OCIRegistryInterceptor(httpClientFactory, repository))
      .build()
    ) {
      final Chart chart = readGeneratedChartYamlFile(file);
      validateChartName(chart);
      final byte[] chartYamlBytes = Serialization.asJson(chart).getBytes(StandardCharsets.UTF_8);
      final OCIRegistryClient oci = new OCIRegistryClient(repository, httpClient);
      final OCIManifestLayer chartConfig = oci.uploadBlobIfNotUploadedYet(chart, new ByteArrayInputStream(chartYamlBytes));
      final OCIManifestLayer chartTarball = oci.uploadBlobIfNotUploadedYet(chart, new BufferedInputStream(Files.newInputStream(file.toPath())));
      oci.uploadOCIManifest(chart, chartConfig, chartTarball);
    }
  }

  private static Chart readGeneratedChartYamlFile(File chartArchive) throws IOException {
    File chartMetadataFile = new File(chartArchive.getParentFile(), CHART_FILENAME);
    if (chartMetadataFile.exists()) {
      return Serialization.unmarshal(chartMetadataFile, Chart.class);
    }
    throw new IllegalStateException("Could not find Chart.yaml file in " + chartArchive.getParentFile());
  }

  private static void validateChartName(Chart chart) {
    try {
      new ImageName(chart.getName());
    } catch (IllegalArgumentException illegalArgumentException) {
      throw new IllegalArgumentException("Chart name " + chart.getName() + " is invalid for uploading to OCI registry", illegalArgumentException);
    }
  }
}
