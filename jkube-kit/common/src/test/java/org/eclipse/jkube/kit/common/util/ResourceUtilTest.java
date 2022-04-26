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

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1beta1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.api.model.Template;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jgit.util.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceUtilTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void deserializeKubernetesListOrTemplate_withNonExistentFile_returnsEmptyList() throws IOException {
        // Given
        final File kubernetesManifestFile = new File("i-dont-exist.yml");
        // When
        final List<HasMetadata> result = ResourceUtil.deserializeKubernetesListOrTemplate(kubernetesManifestFile);
        // Then
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void deserializeKubernetesListOrTemplate_withEmptyFile_returnsEmptyList() throws IOException {
        // Given
        final File empty = temporaryFolder.newFile("kubernetes-empty.yaml");
        FileUtils.touch(empty.toPath());
        // When
        final List<HasMetadata> result = ResourceUtil.deserializeKubernetesListOrTemplate(empty);
        // Then
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void deserializeKubernetesListOrTemplate_withMixedResources_returnsValidList() throws IOException {
        // Given
        final File kubernetesListFile = new File(ResourceUtilTest.class.getResource(
            "/util/resource-util/list-with-standard-template-and-cr-resources.yml").getFile());
        // When
        final List<HasMetadata> result = ResourceUtil.deserializeKubernetesListOrTemplate(
            kubernetesListFile);
        // Then
        assertThat(result)
            .hasSize(8)
            .allMatch(HasMetadata.class::isInstance)
            .hasAtLeastOneElementOfType(ServiceAccount.class)
            .hasAtLeastOneElementOfType(Template.class)
            .hasAtLeastOneElementOfType(Service.class)
            .hasAtLeastOneElementOfType(ConfigMap.class)
            .hasAtLeastOneElementOfType(CustomResourceDefinition.class)
            .hasAtLeastOneElementOfType(io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition.class)
            .hasAtLeastOneElementOfType(GenericKubernetesResource.class)
            .extracting("metadata.name")
            .containsExactly(
                "ribbon",
                "external-service",
                "external-config-map",
                "crd-v1",
                "dummies.demo.fabric8.io",
                "custom-resource",
                "my-new-cron-object-cr",
                "template-example"
                );
    }

    @Test
    public void deserializeKubernetesListOrTemplate_withTemplateFile_returnsValidList() throws IOException {
        // Given
        final File kubernetesListFile = new File(ResourceUtilTest.class.getResource(
            "/util/resource-util/template.yml").getFile());
        // When
        final List<HasMetadata> result = ResourceUtil.deserializeKubernetesListOrTemplate(
            kubernetesListFile);
        // Then
        assertThat(result)
            .singleElement()
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
    public void deserializeKubernetesListOrTemplate_withMultipleYamlAndSeparator_returnsValidList() throws IOException {
        // Given
        final File kubernetesListFile = new File(ResourceUtilTest.class.getResource(
          "/util/resource-util/multiple-k8s-documents.yml").getFile());
        // When
        final List<HasMetadata> result = ResourceUtil.deserializeKubernetesListOrTemplate(
          kubernetesListFile);
        // Then
        assertThat(result)
          .hasSize(2).first()
          .isInstanceOf(Service.class)
          .asInstanceOf(InstanceOfAssertFactories.type(Service.class))
          .hasFieldOrPropertyWithValue("metadata.name", "test-project")
          .extracting("spec.ports").asList()
          .containsExactly(new ServicePortBuilder()
            .withName("http")
            .withPort(8080)
            .withTargetPort(new IntOrString(8080))
            .build()
          );
        assertThat(result).element(1)
          .isInstanceOf(Deployment.class)
          .asInstanceOf(InstanceOfAssertFactories.type(Deployment.class))
          .hasFieldOrPropertyWithValue("metadata.name", "test-project")
          .extracting("spec.template.spec.containers").asList().first()
          .extracting("env").asList()
          .containsExactly(new EnvVarBuilder()
            .withName("KUBERNETES_NAMESPACE")
            .withNewValueFrom()
            .withNewFieldRef().withFieldPath("metadata.namespace").endFieldRef()
            .endValueFrom()
            .build());
    }

    @Test
    public void deserializeKubernetesListOrTemplate_withStandardResourcesAndPlaceholders_returnsValidList() throws IOException {
        // Given
        final File kubernetesListFile = new File(ResourceUtilTest.class.getResource(
            "/util/resource-util/list-with-standard-resources-and-placeholders.yml").getFile());
        // When
        final List<HasMetadata> result = ResourceUtil.deserializeKubernetesListOrTemplate(
            kubernetesListFile);
        // Then
        assertThat(result)
            .hasSize(2)
            .satisfies(l -> assertThat(l).first()
                .isInstanceOf(Service.class)
                .hasFieldOrPropertyWithValue("metadata.name", "the-service")
                .hasFieldOrPropertyWithValue("metadata.additionalProperties.annotations", "${annotations_placeholder}")
                .extracting("spec.ports").asList().singleElement()
                .hasFieldOrPropertyWithValue("protocol", "TCP")
                .hasFieldOrPropertyWithValue("additionalProperties.port", "{{ .Values.service.port }}")
            )
            .satisfies(l -> assertThat(l).element(1)
                .isInstanceOf(Deployment.class)
                .hasFieldOrPropertyWithValue("metadata.name", "the-deployment")
                .hasFieldOrPropertyWithValue("metadata.additionalProperties.annotations", "${annotations_placeholder}")
                .hasFieldOrPropertyWithValue("spec.additionalProperties.replicas", "{{ .Values.deployment.replicas }}")
            );
    }

    @Test
    public void deserializeKubernetesListOrTemplate_withSingleResource_returnsValidList() throws IOException {
        // Given
        final File kubernetesListFile = new File(ResourceUtilTest.class.getResource(
            "/util/resource-util/custom-resource-cr.yml").getFile());
        // When
        final List<HasMetadata> result = ResourceUtil.deserializeKubernetesListOrTemplate(
            kubernetesListFile);
        // Then
        assertThat(result)
            .singleElement()
            .isInstanceOf(GenericKubernetesResource.class)
            .hasFieldOrPropertyWithValue("kind", "SomeCustomResource")
            .hasFieldOrPropertyWithValue("metadata.name", "my-custom-resource");;
    }

    @Test
    public void load_withCustomResourceFile_shouldLoadGenericKubernetesResource() throws Exception {
        // When
        final HasMetadata result = ResourceUtil.load(
            new File(ResourceUtilTest.class.getResource( "/util/resource-util/custom-resource-cr.yml").getFile()),
            HasMetadata.class
        );
        // Then
        assertThat(result)
            .isInstanceOf(GenericKubernetesResource.class)
            .hasFieldOrPropertyWithValue("kind", "SomeCustomResource")
            .hasFieldOrPropertyWithValue("metadata.name", "my-custom-resource");
    }

    @Test
    public void load_withTemplate_shouldLoadTemplate() throws Exception {
        // When
        final HasMetadata result = ResourceUtil.load(
            new File(ResourceUtilTest.class.getResource( "/util/resource-util/template.yml").getFile()),
            HasMetadata.class
        );
        // Then
        assertThat(result)
            .isInstanceOf(Template.class)
            .hasFieldOrPropertyWithValue("metadata.name", "template-example")
            .extracting("objects").asList().singleElement()
            .isInstanceOf(Pod.class)
            .hasFieldOrPropertyWithValue("metadata.name", "pod-from-template")
            .extracting("spec.containers").asList().singleElement()
            .hasFieldOrPropertyWithValue("image", "busybox")
            .hasFieldOrPropertyWithValue("securityContext.additionalProperties.privileged", "${POD_SECURITY_CONTEXT}")
            .extracting("env").asList().singleElement()
            .hasFieldOrPropertyWithValue("value", "${ENV_VAR_KEY}");
    }

    @Test
    public void save_withValidYamlFileAndItem_shouldSave() throws IOException {
        // Given
        final File file = temporaryFolder.newFile("temp-resource.yaml");
        final ConfigMap resource = new ConfigMapBuilder().withNewMetadata().withName("cm").endMetadata()
            .addToData("field", "value").build();
        // When
        ResourceUtil.save(file, resource);
        // Then
        assertThat(file).hasSameTextualContentAs(
            new File(ResourceUtilTest.class.getResource( "/util/resource-util/expected/config-map-simple.yml").getFile()));
    }

    @Test
    public void save_withValidJsonFileAndItem_shouldSave() throws IOException {
        // Given
        final File file = temporaryFolder.newFile("temp-resource.json");
        final ConfigMap resource = new ConfigMapBuilder().withNewMetadata().withName("cm").endMetadata()
            .addToData("field", "value").build();
        // When
        ResourceUtil.save(file, resource);
        // Then
        assertThat(file).hasSameTextualContentAs(
            new File(ResourceUtilTest.class.getResource( "/util/resource-util/expected/config-map-simple.json").getFile()));
    }

    @Test
    public void save_withValidYamlFileAndItemWithAdditionalProperties_shouldSave() throws IOException {
        // Given
        final File file = temporaryFolder.newFile("temp-resource.yaml");
        final ConfigMap resource = new ConfigMapBuilder().withNewMetadata().withName("cm").endMetadata()
            .addToData("field", "value")
            .withImmutable(true)
            .build();
        resource.setAdditionalProperty("immutable", "${immutable}");
        resource.setAdditionalProperty("not-cm-field-1", "test");
        resource.setAdditionalProperty("not-cm-field-2", "{{ .Values.value }}");
        resource.setAdditionalProperty("not-cm-field-bool", true);
        resource.setAdditionalProperty("not-cm-field-double", 1.337D);
        // When
        ResourceUtil.save(file, resource);
        // Then
        assertThat(file).hasSameTextualContentAs(
            new File(ResourceUtilTest.class.getResource( "/util/resource-util/expected/config-map-placeholders.yml").getFile()));
    }

    @Test
    public void getFinalResourceDirs_withMultipleEnvs_shouldReturnEnvResourceDirList() throws IOException {
        // Given
        File resourceDir = temporaryFolder.newFolder("src", "main", "jkube");

        // When
        List<File> finalResourceDirs = ResourceUtil.getFinalResourceDirs(resourceDir, "common,dev");

        // Then
        assertThat(finalResourceDirs)
            .hasSize(2)
            .containsExactlyInAnyOrder(new File(resourceDir, "common"), new File(resourceDir, "dev"));
    }

    @Test
    public void getFinalResourceDirs_withNullEnv_shouldReturnResourceDir() throws IOException {
        // Given
        File resourceDir = temporaryFolder.newFolder("src", "main", "jkube");

        // When
        List<File> finalResourceDirs = ResourceUtil.getFinalResourceDirs(resourceDir, null);

        // Then
        assertThat(finalResourceDirs)
            .hasSize(1)
            .containsExactly(resourceDir);
    }

    @Test
    public void getFinalResourceDirs_withEmptyEnv_shouldReturnResourceDir() throws IOException {
        // Given
        File resourceDir = temporaryFolder.newFolder("src", "main", "jkube");

        // When
        List<File> finalResourceDirs = ResourceUtil.getFinalResourceDirs(resourceDir, "");

        // Then
        assertThat(finalResourceDirs)
            .hasSize(1)
            .containsExactly(resourceDir);
    }
}
