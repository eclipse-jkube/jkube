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
package org.eclipse.jkube.kit.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.Test;

public class GenericCustomResourceSerializationTest {

  @Test
  public void deserializeCustomResource() throws IOException {
    // When
    final GenericCustomResource result = Serialization.yamlMapper()
      .readValue(GenericCustomResourceSerializationTest.class.getResourceAsStream("/generic-resource/strimzi-cr.yaml"), GenericCustomResource.class);
    // Then
    assertThat(result)
      .hasFieldOrPropertyWithValue("metadata.name", "my-cluster")
      .extracting(GenericCustomResource::getAdditionalProperties)
      .hasFieldOrPropertyWithValue("spec.kafka.version", "2.6.0")
      .hasFieldOrPropertyWithValue("spec.kafka.storage.type", "jbod");
  }

  @Test
  public void serializeRandomResource() throws IOException {
    // Given
    final GenericCustomResource input = new GenericCustomResource();
    Map<String, String> additionalProperties = new HashMap<>();
    additionalProperties.put("field-1", "val-1");
    additionalProperties.put("field-2", "val-2");
    input.setMetadata(new ObjectMetaBuilder().withName("random-resource").build());
    input.setAdditionalProperties(new LinkedHashMap<>()); //Preserve insertion order for verification
    input.getAdditionalProperties().put("kind", "Kind");
    input.setAdditionalProperty("spec", additionalProperties);
    // When
    final String result = Serialization.yamlMapper().writeValueAsString(input);
    // Then
    assertThat(result).isEqualTo(
      "---\n" +
      "metadata:\n" +
      "  name: \"random-resource\"\n" +
      "kind: \"Kind\"\n" +
      "spec:\n" +
      "  field-1: \"val-1\"\n" +
      "  field-2: \"val-2\"\n"
    );
  }

  @Test
  public void serializeRandomResourceInListWithVisitor() throws IOException {
    // Given
    final GenericCustomResource gr = new GenericCustomResource();
    gr.setKind("Kind");
    gr.setApiVersion("test.marcnuri.com/v1beta1");
    gr.setMetadata(new ObjectMetaBuilder().withName("random-resource").build());
    final KubernetesList kl = new KubernetesListBuilder()
      .addToItems(gr)
      .accept(new TypedVisitor<ObjectMetaBuilder>() {
        @Override
        public void visit(ObjectMetaBuilder element) {
          element.addToAnnotations("test", "annotation");
        }
      })
      .build();
    // When
    final String result = Serialization.yamlMapper().writeValueAsString(kl);
    // Then
    assertThat(result).isEqualTo(
      "---\n" +
        "apiVersion: \"v1\"\n" +
        "kind: \"List\"\n" +
        "items:\n" +
        "- apiVersion: \"test.marcnuri.com/v1beta1\"\n" +
        "  kind: \"Kind\"\n" +
        "  metadata:\n" +
        "    annotations:\n" +
        "      test: \"annotation\"\n" +
        "    name: \"random-resource\"\n"
    );
  }

  @Test
  public void serializeRandomResourceInList() throws IOException {
    // Given
    final GenericCustomResource gr = new GenericCustomResource();
    gr.setKind("Kind");
    gr.setApiVersion("test.marcnuri.com/v1beta1");
    gr.setMetadata(new ObjectMetaBuilder().withName("random-resource").build());
    final KubernetesList kl = new KubernetesListBuilder()
      .addNewPodItem().withNewMetadata().withName("test-pod").endMetadata().endPodItem()
      .addToItems(gr)
      .build();
    // When
    final String result = Serialization.yamlMapper().writeValueAsString(kl);
    // Then
    assertThat(result).isEqualTo(
      "---\n" +
      "apiVersion: \"v1\"\n" +
      "kind: \"List\"\n" +
      "items:\n" +
      "- apiVersion: \"test.marcnuri.com/v1beta1\"\n" +
      "  kind: \"Kind\"\n" +
      "  metadata:\n" +
      "    name: \"random-resource\"\n" +
      "- apiVersion: \"v1\"\n" +
      "  kind: \"Pod\"\n" +
      "  metadata:\n" +
      "    name: \"test-pod\"\n"
    );
  }
}
