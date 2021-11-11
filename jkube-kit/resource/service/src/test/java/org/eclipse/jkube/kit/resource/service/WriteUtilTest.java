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

import java.io.File;
import java.io.IOException;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.ResourceUtil;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@SuppressWarnings({"ConstantConditions", "AccessStaticViaInstance"})
public class WriteUtilTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @SuppressWarnings("unused")
  @Mocked
  private KitLogger log;
  @SuppressWarnings("unused")
  @Mocked
  private ResourceUtil resourceUtil;

  private KubernetesListBuilder klb;
  private File resourceFileBase;

  @Before
  public void initGlobalVariables() throws IOException {
    klb = new KubernetesListBuilder();
    resourceFileBase = temporaryFolder.newFolder();
  }

  @Test
  public void writeResource() throws IOException {
    // Given
    final File baton = temporaryFolder.newFile();
    mockResourceUtilSave(baton);
    // When
    final File result = WriteUtil.writeResource(null, null, null);
    // Then
    assertThat(result).isEqualTo(baton);
  }

  @Test
  public void writeResourceThrowsException() throws IOException {
    // Given
    final File resource = temporaryFolder.newFolder("resource-base");
    mockResourceUtilSave(new IOException("Message"));
    // When
    final IOException result = assertThrows(IOException.class,
        () -> WriteUtil.writeResource(resource, null, null)
    );
    // Then
    assertThat(result)
        .isNotNull()
        .hasMessageStartingWith("Failed to write resource to ")
        .hasMessageEndingWith("resource-base.");
  }

  @Test
  public void writeResourcesIndividualAndCompositeWithNoResourcesShouldOnlyWriteComposite() throws IOException {
    // When
    WriteUtil.writeResourcesIndividualAndComposite(klb.build(), resourceFileBase, null, log);
    // Then
    verifyResourceUtilSave(resourceFileBase, 1);
  }

  @Test
  public void writeResourcesIndividualAndCompositeWithResourcesShouldWriteAll() throws IOException {
    // Given
    klb.addToItems(
      new ConfigMapBuilder().withNewMetadata().withName("cm-1").endMetadata().build(),
      new SecretBuilder().withNewMetadata().withName("secret-1").endMetadata().build(),
      new SecretBuilder().withNewMetadata().withName(" ").withClusterName("skipped (blank name)").endMetadata().build()
    );
    // When
    WriteUtil.writeResourcesIndividualAndComposite(klb.build(), resourceFileBase, null, log);
    // Then
    verifyResourceUtilSave(null, 3);
    verifyResourceUtilSave(resourceFileBase, 1);
    verifyResourceUtilSave(new File(resourceFileBase, "cm-1-configmap"), 1);
    verifyResourceUtilSave(new File(resourceFileBase, "secret-1-secret"), 1);
  }

  @Test
  public void writeResourcesIndividualAndComposite_withResourcesWithSameName_shouldWriteAll() throws IOException {
    // Given
    klb.addToItems(
        new ConfigMapBuilder().withNewMetadata().withNamespace("default").withName("cm-1").endMetadata().build(),
        new ConfigMapBuilder().withNewMetadata().withNamespace("different").withName("cm-1").endMetadata().build(),
        genericCustomResource("CustomResourceOfKind1","repeated"),
        genericCustomResource("CustomResourceOfKind2","repeated"),
        genericCustomResource("CustomResourceOfKind3","repeated")
    );
    // When
    WriteUtil.writeResourcesIndividualAndComposite(klb.build(), resourceFileBase, null, log);
    // Then
    verifyResourceUtilSave(null, 6);
    verifyResourceUtilSave(resourceFileBase, 1);
    verifyResourceUtilSave(new File(resourceFileBase, "cm-1-configmap"), 1);
    verifyResourceUtilSave(new File(resourceFileBase, "cm-1-1-configmap"), 1);
    verifyResourceUtilSave(new File(resourceFileBase, "repeated-cr"), 1);
    verifyResourceUtilSave(new File(resourceFileBase, "repeated-1-cr"), 1);
    verifyResourceUtilSave(new File(resourceFileBase, "repeated-2-cr"), 1);
  }

  private void mockResourceUtilSave(Object returnValue) throws IOException {
    // @formatter:off
    new Expectations() {{
      resourceUtil.save((File)any, null, null); result = returnValue;
    }};
    // @formatter:on
  }

  private void verifyResourceUtilSave(File file, int numTimes) throws IOException {
    // @formatter:off
    new Verifications() {{
      String s;
      resourceUtil.save(file, any, null); times = numTimes;
    }};
    // @formatter:on
  }

  private static GenericKubernetesResource genericCustomResource(String kind, String name) {
    final GenericKubernetesResource gcr = new GenericKubernetesResource();
    gcr.setKind(kind);
    gcr.setMetadata(new ObjectMetaBuilder().withName(name).build());
    return gcr;
  }
}
