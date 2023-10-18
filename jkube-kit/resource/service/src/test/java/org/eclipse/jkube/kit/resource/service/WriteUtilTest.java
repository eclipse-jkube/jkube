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
package org.eclipse.jkube.kit.resource.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ResourceUtil;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

class WriteUtilTest {

  @TempDir
  File temporaryFolder;
  private KitLogger log;
  private MockedStatic<ResourceUtil> resourceUtil;

  private KubernetesListBuilder klb;
  private File resourceFileBase;

  @BeforeEach
  void initGlobalVariables()  {
    log = new KitLogger.SilentLogger();
    resourceUtil = mockStatic(ResourceUtil.class);
    klb = new KubernetesListBuilder();
    resourceFileBase = temporaryFolder;
  }
  @AfterEach
  public void close() {
    resourceUtil.close();
  }
  @Test
  void writeResource() throws IOException {
    // Given
    final File baton = Files.createTempFile(temporaryFolder.toPath(), "junit", "ext").toFile();
    mockResourceUtilSave(baton);
    // When
    final File result = WriteUtil.writeResource(null, null, null);
    // Then
    assertThat(result).isEqualTo(baton);
  }

  @Test
  void writeResourceThrowsException() throws IOException {
    // Given
    final File resource =
            Files.createDirectory(temporaryFolder.toPath().resolve("resource-base")).toFile();
    resourceUtil.when(() -> ResourceUtil.save(any(), isNull(), isNull())).thenThrow(new IOException("Message"));

    // When
    assertThatIOException()
            .isThrownBy(() -> WriteUtil.writeResource(resource, null, null))
            .withMessageStartingWith("Failed to write resource to ")
            .withMessageEndingWith("resource-base.");
  }

  @Test
  void writeResourcesIndividualAndCompositeWithNoResourcesShouldOnlyWriteComposite() throws IOException {
    // When
    WriteUtil.writeResourcesIndividualAndComposite(klb.build(), resourceFileBase, null, log);
    // Then
    verifyResourceUtilSave(resourceFileBase);
  }

  @Test
  void writeResourcesIndividualAndCompositeWithResourcesShouldWriteAll() throws IOException {
    // Given
    klb.addToItems(
      new ConfigMapBuilder().withNewMetadata().withName("cm-1").endMetadata().build(),
      new SecretBuilder().withNewMetadata().withName("secret-1").endMetadata().build(),
      new SecretBuilder().withNewMetadata().withName(" ").endMetadata().build()
    );
    // When
    WriteUtil.writeResourcesIndividualAndComposite(klb.build(), resourceFileBase, null, log);
    // Then
    verifyResourceUtilSave(resourceFileBase);
    verifyResourceUtilSave(new File(resourceFileBase, "cm-1-configmap"));
    verifyResourceUtilSave(new File(resourceFileBase, "secret-1-secret"));
  }

  @Test
  void writeResourcesIndividualAndComposite_withResourcesWithSameName_shouldWriteAll() throws IOException {
    // Given
    klb.addToItems(
        new ConfigMapBuilder().withNewMetadata().withNamespace("default").withName("cm-1").endMetadata().build(),
        new ConfigMapBuilder().withNewMetadata().withNamespace("different").withName("cm-1").endMetadata().build(),
        genericCustomResource("CustomResourceOfKind1"),
        genericCustomResource("CustomResourceOfKind2"),
        genericCustomResource("CustomResourceOfKind3")
    );
    // When
    WriteUtil.writeResourcesIndividualAndComposite(klb.build(), resourceFileBase, null, log);
    // Then
    verifyResourceUtilSave(resourceFileBase);
    verifyResourceUtilSave(new File(resourceFileBase, "cm-1-configmap"));
    verifyResourceUtilSave(new File(resourceFileBase, "cm-1-1-configmap"));
    verifyResourceUtilSave(new File(resourceFileBase, "repeated-cr"));
    verifyResourceUtilSave(new File(resourceFileBase, "repeated-1-cr"));
    verifyResourceUtilSave(new File(resourceFileBase, "repeated-2-cr"));
  }

  private void mockResourceUtilSave(Object returnValue) {
    resourceUtil.when(() -> ResourceUtil.save(any(), isNull(), isNull())).thenReturn(returnValue);
  }

  private void verifyResourceUtilSave(File file) {
    resourceUtil.verify(() -> ResourceUtil.save(eq(file), any(), isNull()),times(1));

  }

  private static GenericKubernetesResource genericCustomResource(String kind) {
    final GenericKubernetesResource gcr = new GenericKubernetesResource();
    gcr.setKind(kind);
    gcr.setMetadata(new ObjectMetaBuilder().withName("repeated").build());
    return gcr;
  }
}
