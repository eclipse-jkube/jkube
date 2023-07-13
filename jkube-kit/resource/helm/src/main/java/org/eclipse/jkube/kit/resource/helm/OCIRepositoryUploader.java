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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.commons.codec.digest.DigestUtils;
import org.eclipse.jkube.kit.resource.helm.oci.OCIRegistryClient;
import org.eclipse.jkube.kit.resource.helm.oci.OCIRegistryInterceptor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class OCIRepositoryUploader implements HelmUploader {
  private static final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());

  @Override
  public HelmRepository.HelmRepoType getType() {
    return HelmRepository.HelmRepoType.OCI;
  }

  @Override
  public void uploadSingle(File file, HelmRepository repository)
      throws IOException, BadUploadException {
    Chart chartConfig = createChartMetadataFromGeneratedChartYamlFile(file);
    HttpClient.Factory httpClientFactory = HttpClientUtils.getHttpClientFactory();
    try (HttpClient httpClient = httpClientFactory.newBuilder()
        .addOrReplaceInterceptor(OCIRegistryInterceptor.NAME, new OCIRegistryInterceptor(httpClientFactory, repository))
        .build()) {
      OCIRegistryClient oci = new OCIRegistryClient(repository, httpClient);

      uploadChartToOCIRegistry(oci, chartConfig, file);
    }
  }

  private void uploadChartToOCIRegistry(OCIRegistryClient oci, Chart chartConfig, File file) throws IOException, BadUploadException {
    String chartMetadataContentPayload = Serialization.asJson(chartConfig);
    String chartTarballBlobDigest = DigestUtils.sha256Hex(Files.newInputStream(file.toPath()));
    String chartMetadataBlobDigest = DigestUtils.sha256Hex(chartMetadataContentPayload);
    long chartMetadataPayloadSize = chartMetadataContentPayload.getBytes(Charset.defaultCharset()).length;
    long chartTarballSize = file.length();

    String chartTarballDockerContentDigest = uploadBlobIfNotExist(oci, chartConfig.getName(), chartTarballBlobDigest, chartTarballSize, null, file);
    String chartConfigDockerContentDigest = uploadBlobIfNotExist(oci, chartConfig.getName(), chartMetadataBlobDigest, chartMetadataPayloadSize, chartMetadataContentPayload, null);

    oci.uploadOCIManifest(chartConfig.getName(), chartConfig.getVersion(), chartConfigDockerContentDigest, chartTarballDockerContentDigest, chartMetadataPayloadSize, chartTarballSize);
  }

  private String uploadBlobIfNotExist(OCIRegistryClient oci, String chartName, String blob, long blobSize, String blobContentStr, File blobFile) throws IOException, BadUploadException {
    boolean alreadyUploaded = oci.isLayerUploadedAlready(chartName, blob);
    if (alreadyUploaded) {
      return String.format("sha256:%s", blob);
    } else {
      return uploadBlob(oci, chartName, blob, blobSize, blobContentStr, blobFile);
    }
  }

  private String uploadBlob(OCIRegistryClient oci, String chartName, String blob, long blobSize, String blobContentStr, File blobFile) throws IOException, BadUploadException {
    String uploadUrl = oci.initiateUploadProcess(chartName);
    return oci.uploadBlob(uploadUrl, blob, blobSize, blobContentStr, blobFile);
  }

  private Chart createChartMetadataFromGeneratedChartYamlFile(File chartFile) throws IOException {
    File chartMetadataFile = new File(chartFile.getParentFile(), "Chart.yaml");
    if (chartMetadataFile.exists()) {
      return yamlObjectMapper.readValue(chartMetadataFile, Chart.class);
    }
    throw new IllegalStateException("Could not found Chart.yaml file in " + chartMetadataFile.getPath());
  }
}