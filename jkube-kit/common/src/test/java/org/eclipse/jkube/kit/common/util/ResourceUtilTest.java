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
package org.eclipse.jkube.kit.common.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.openshift.api.model.Template;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.GenericCustomResource;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author roland
 * @since 07/02/17
 */
public class ResourceUtilTest {

    @Test
    public void simple() {
        JsonParser parser = new JsonParser();
        JsonObject first = parser.parse("{first: bla, second: blub}").getAsJsonObject();
        JsonObject same = parser.parse("{second: blub, first: bla   }").getAsJsonObject();
        JsonObject different = parser.parse("{second: blub, first: bla2   }").getAsJsonObject();
        assertTrue(ResourceUtil.jsonEquals(first, same));
        assertFalse(ResourceUtil.jsonEquals(first, different));
    }

    @Test
    public void testDeserializeKubernetesListOrTemplateWithNonExistentFile() throws IOException {
        // Given
        File kubernetesManifestFile = new File("i-dont-exist.yml");

        // When
        List<HasMetadata> kubernetesResourceList = ResourceUtil.deserializeKubernetesListOrTemplate(kubernetesManifestFile);

        // Then
        assertNotNull(kubernetesResourceList);
        assertEquals(0, kubernetesResourceList.size());
    }

    @Test
    public void testDeserializeKubernetesListOrTemplateWithEmptyFile() throws IOException {
        // Given
        File kubernetesManifestFile = Files.createTempFile("kubernetes-", ".yaml").toFile();

        // When
        List<HasMetadata> kubernetesResourceList = ResourceUtil.deserializeKubernetesListOrTemplate(kubernetesManifestFile);

        // Then
        assertNotNull(kubernetesResourceList);
        assertEquals(0, kubernetesResourceList.size());
    }

    @Test
    public void testDeserializeKubernetesListOrTemplateWithMixedResourcesFile() throws IOException {
        // Given
        final File kubernetesListFile = new File(ResourceUtilTest.class.getResource(
            "/util/resource-util/list-with-standard-template-and-cr-resources.yml").getFile());
        // When
        final List<HasMetadata> result = ResourceUtil.deserializeKubernetesListOrTemplate(
            kubernetesListFile);
        // Then
        assertThat(result)
            .hasSize(7)
            .allMatch(HasMetadata.class::isInstance)
            .hasAtLeastOneElementOfType(ServiceAccount.class)
            .hasAtLeastOneElementOfType(Template.class)
            .hasAtLeastOneElementOfType(Service.class)
            .hasAtLeastOneElementOfType(ConfigMap.class)
            .hasAtLeastOneElementOfType(CustomResourceDefinition.class)
            .extracting("metadata.name")
            .containsExactly(
                "my-new-cron-object-cr",
                "ribbon",
                "external-service",
                "external-config-map",
                "template-example",
                "dummies.demo.fabric8.io",
                "custom-resource"
            );
    }

    @Test
    public void testDeserializeKubernetesListOrTemplateWithTemplateFile() throws IOException {
        // Given
        final File kubernetesListFile = new File(ResourceUtilTest.class.getResource(
            "/util/resource-util/template.yml").getFile());
        // When
        final List<HasMetadata> result = ResourceUtil.deserializeKubernetesListOrTemplate(
            kubernetesListFile);
        // Then
        assertThat(result)
            .hasSize(1).first()
            .isInstanceOf(Pod.class)
            .asInstanceOf(InstanceOfAssertFactories.type(Pod.class))
            .hasFieldOrPropertyWithValue("metadata.name", "pod-from-template")
            .extracting("spec.containers").asList().first()
            .extracting("env").asList()
            .containsExactly(new EnvVarBuilder()
                .withName("ENV_VAR_FROM_PARAMETER")
                .withValue("replaced_value")
                .build()
            );
    }

    @Test
    public void load_withCustomResourceFile_shouldLoadGenericCustomResource() throws Exception {
        // When
        final HasMetadata result = ResourceUtil.load(
            new File(ResourceUtilTest.class.getResource( "/util/resource-util/custom-resource-cr.yml").getFile()),
            HasMetadata.class
        );
        // Then
        assertThat(result)
            .isInstanceOf(GenericCustomResource.class)
            .hasFieldOrPropertyWithValue("kind", "SomeCustomResource")
            .hasFieldOrPropertyWithValue("metadata.name", "my-custom-resource");
    }

    @Test
    public void load_withCustomResourceStream_shouldLoadGenericCustomResource() throws Exception {
        // When
        final HasMetadata result = ResourceUtil.load(
            ResourceUtilTest.class.getResourceAsStream( "/util/resource-util/custom-resource-cr.yml"),
            HasMetadata.class,
            ResourceFileType.yaml
        );
        // Then
        assertThat(result)
            .isInstanceOf(GenericCustomResource.class)
            .hasFieldOrPropertyWithValue("kind", "SomeCustomResource")
            .hasFieldOrPropertyWithValue("metadata.name", "my-custom-resource");
    }
}
