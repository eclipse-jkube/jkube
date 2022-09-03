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
package org.eclipse.jkube.kit.resource.service;

import java.io.IOException;
import java.nio.charset.Charset;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.api.model.ParameterBuilder;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.resource.service.TemplateUtil.getSingletonTemplate;
import static org.eclipse.jkube.kit.resource.service.TemplateUtil.interpolateTemplateVariables;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

class TemplateUtilTest {
  private FileUtils fileUtils;

  private KubernetesListBuilder klb;

  @BeforeEach
  void initGlobalVariables() {
    klb = new KubernetesListBuilder();
    fileUtils = mock(FileUtils.class);
  }

  @Test
  void getSingletonTemplateWithNullShouldReturnNull() {
    assertThat(getSingletonTemplate(null)).isNull();
  }

  @Test
  void getSingletonTemplateWithMultipleItemsShouldReturnNull() {
    // Given
    klb.addToItems(new Template(), new Template());
    // When - Then
    assertThat(getSingletonTemplate(klb.build())).isNull();
  }

  @Test
  void getSingletonTemplateWithSingleItemsShouldReturnTemplate() {
    // Given
    klb.addToItems(new TemplateBuilder().withNewMetadata().withName("template").endMetadata().build());
    // When - Then
    assertThat(getSingletonTemplate(klb.build()))
        .hasFieldOrPropertyWithValue("metadata.name", "template");
  }

  @Test
  void interpolateTemplateVariablesWithNoParametersShouldDoNothing() throws IOException {
    // When
    interpolateTemplateVariables(klb.build(), null);
    // Then
    verifyWriteStringToFile(0, null);
  }

  @Test
  void interpolateTemplateVariablesWithParametersAndNoPlaceholdersShouldDoNothing() throws IOException {
    // Given
    klb.addToItems(new TemplateBuilder()
        .addToParameters(new ParameterBuilder().withName("param1").withValue("value1").build())
        .build());
    mockReadFileToString("No parameters here");
    // When
    interpolateTemplateVariables(klb.build(), null);
    // Then
    verifyWriteStringToFile(0, null);
  }

  @Test
  void interpolateTemplateVariablesWithParametersAndPlaceholdersShouldReplace() throws IOException {
    // Given
    klb.addToItems(new TemplateBuilder()
        .addToParameters(new ParameterBuilder().withName("param1").withValue("value1").build())
        .build());
    mockReadFileToString("One parameter: ${param1}, and non-existent ${oops}");
    // When
    interpolateTemplateVariables(klb.build(), null);
    // Then
    verifyWriteStringToFile(1, "One parameter: value1, and non-existent ${oops}");
  }

  @Test
  void interpolateTemplateVariablesWithReadFileException() throws IOException {
    // Given
    klb.addToItems(new TemplateBuilder()
        .addToParameters(new ParameterBuilder().withName("param1").withValue("value1").build())
        .build());
    mockReadFileToString(new IOException());
    // When
    final IOException result = assertThrows(IOException.class, () -> {
      interpolateTemplateVariables(klb.build(), null);
      fail();
    });
    // Then
    assertThat(result)
        .isNotNull()
        .hasMessage("Failed to load null for template variable replacement");
  }

  @Test
  void interpolateTemplateVariablesWithWriteFileException() throws IOException {
    // Given
    klb.addToItems(new TemplateBuilder()
        .addToParameters(new ParameterBuilder().withName("param1").withValue("value1").build())
        .build());
    mockReadFileToString("One parameter: ${param1}, and non-existent ${oops}");
    doNothing().when(fileUtils).writeStringToFile(null, anyString(), Charset.defaultCharset());
    // When
    final IOException result = assertThrows(IOException.class, () -> {
      interpolateTemplateVariables(klb.build(), null);
      fail();
    });
    // Then
    assertThat(result)
        .isNotNull()
        .hasMessage("Failed to save null after replacing template expressions");
  }

  private void mockReadFileToString(Object readString) throws IOException {
    doNothing().when(fileUtils).readFileToString(null, Charset.defaultCharset());
  }

  private void verifyWriteStringToFile(int numTimes, String expectedString) throws IOException {
    ArgumentCaptor<String> s = ArgumentCaptor.forClass(String.class);
    doNothing().when(fileUtils).writeStringToFile(null, s.capture(), Charset.defaultCharset());
    if (numTimes > 0) {
      assertThat(s).isEqualTo(expectedString);
    }
  }
}
