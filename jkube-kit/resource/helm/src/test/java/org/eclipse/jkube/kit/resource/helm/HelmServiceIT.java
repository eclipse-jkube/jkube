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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ResourceUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.openshift.api.model.Template;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class HelmServiceIT {

  @Test
  public void generateHelmChartsTest() throws Exception {
    // Given
    final HelmConfig helmConfig = new HelmConfig();
    helmConfig.setChart("ITChart");
    helmConfig.setVersion("1337");
    helmConfig.setType("KUBERNEtES,oPenShift");
    helmConfig.setSourceDir(new File(HelmServiceIT.class.getResource("/it/sources").toURI()).getAbsolutePath());
    helmConfig.setOutputDir("target/helm-it");
    helmConfig.setTarballOutputDir("target/helm-it");
    helmConfig.setChartExtension("tar");
    helmConfig.setAdditionalFiles(Collections.singletonList(
        new File(HelmServiceIT.class.getResource("/it/sources/additional-file.txt").toURI())
    ));
    helmConfig.setTemplates(Collections.singletonList(
        ResourceUtil.load(new File(HelmServiceIT.class.getResource("/it/sources/global-template.yml").toURI()), Template.class)
    ));
    final AtomicInteger generatedChartCount = new AtomicInteger(0);
    helmConfig.setGeneratedChartListeners(Collections.singletonList(
        (helmConfig1, type, chartFile) -> generatedChartCount.incrementAndGet()));
    final KitLogger logger = new KitLogger.StdoutLogger();
    // When
    HelmService.generateHelmCharts(logger, helmConfig);
    // Then
    assertThat(new File("target/helm-it/kubernetes/Chart.yaml").exists(), is(true));
    assertThat(new File("target/helm-it/kubernetes/values.yaml").exists(), is(true));
    assertThat(new File("target/helm-it/kubernetes/additional-file.txt").exists(), is(true));
    assertThat(new File("target/helm-it/kubernetes/templates/kubernetes.yaml").exists(), is(true));
    assertThat(new File("target/helm-it/openshift/Chart.yaml").exists(), is(true));
    assertThat(new File("target/helm-it/openshift/values.yaml").exists(), is(true));
    assertThat(new File("target/helm-it/openshift/additional-file.txt").exists(), is(true));
    assertThat(new File("target/helm-it/openshift/templates/test-pod.yaml").exists(), is(true));
    assertThat(new File("target/helm-it/openshift/templates/openshift.yaml").exists(), is(true));
    assertThat(new File("target/helm-it/ITChart-1337-helm.tar").exists(), is(true));
    assertThat(new File("target/helm-it/ITChart-1337-helmshift.tar").exists(), is(true));
    assertYamls();
    assertThat(generatedChartCount.get(), is(2));
  }

  private static void assertYamls() throws Exception {
    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    final Path expectations = new File(HelmServiceIT.class.getResource("/it/expected").toURI()).toPath();
    final Path generatedYamls = new File("target/helm-it").toPath();
    for (Path expected : Files.walk(expectations).filter(Files::isRegularFile).collect(Collectors.toList())) {
      final Map<String, ?> expectedContent = mapper.readValue(replacePlaceholders(expected), Map.class);
      final Map<String, ?> actualContent =
          mapper.readValue(replacePlaceholders(generatedYamls.resolve(expectations.relativize(expected))), Map.class);
      assertThat(expectedContent, equalTo(actualContent));
    }
  }

  private static String replacePlaceholders(final Path yamlWithJsonPlaceholders) throws IOException {
    return FileUtils.readFileToString(yamlWithJsonPlaceholders.toFile(), StandardCharsets.UTF_8)
        .replace("\"{{\"{{\"}}", "").replace("{{\"}}\"}}", "")
        .replace("{", "(").replace("}", ")");
  }
}
