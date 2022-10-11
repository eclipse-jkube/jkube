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

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenshiftHelperTest {
    private KubernetesClient kc;
    private OpenShiftClient oc;

    @BeforeEach
    public void setUp() {
        kc = mock(KubernetesClient.class);
        oc = mock(OpenShiftClient.class);
    }

    @Test
    void testAsOpenShiftClientWithNoOpenShift() {
        // Given
        when(kc.adapt(OpenShiftClient.class)).thenReturn(oc);
        //When
        OpenShiftClient result = OpenshiftHelper.asOpenShiftClient(kc);
        //Then
        assertThat(result).isNull();
    }

    @Test
    void testOpenShiftClientWithAdaptableToOpenShift() {
        // Given
        when(oc.isSupported()).thenReturn(true);
        doReturn(oc).when(kc).adapt(OpenShiftClient.class);
        //When
        OpenShiftClient result = OpenshiftHelper.asOpenShiftClient(kc);
        //Then
        assertThat(result).isNotNull();
    }

    @Test
    void testOpenShiftClientWithOpenShift() {
        //When
        OpenShiftClient result = OpenshiftHelper.asOpenShiftClient(oc);
        //Then
        assertThat(result).isNotNull();
    }

    @Test
    void testIsOpenShiftWhenSupported() {
        // Given
        doReturn(oc).when(kc).adapt(OpenShiftClient.class);
        when(oc.isSupported()).thenReturn(true);
        //When
        boolean result = OpenshiftHelper.isOpenShift(kc);
        //Then
        assertThat(result).isTrue();
    }

    @Test
    void testIsOpenShiftNotSupported() {
        // Given
        doReturn(oc).when(kc).adapt(OpenShiftClient.class);
        when(oc.isSupported()).thenReturn(false);
        //When
        boolean result = OpenshiftHelper.isOpenShift(kc);
        //Then
        assertThat(result).isFalse();
    }


    @Test
    void testProcessTemplatesLocallyNotNull() throws IOException {
        //Given
        Template template = new TemplateBuilder()
                .withNewMetadata().withName("redis-template").endMetadata()
                .withObjects(new PodBuilder()
                        .withNewMetadata().withName("redis-master").endMetadata()
                        .withNewSpec()
                        .addNewContainer()
                        .addNewEnv()
                        .withName("REDIS_PASSWORD")
                        .withValue("${REDIS_PASSWORD}")
                        .endEnv()
                        .withImage("dockerfile/redis")
                        .withName("master")
                        .addNewPort()
                        .withProtocol("TCP")
                        .withContainerPort(6379)
                        .endPort()
                        .endContainer()
                        .endSpec()
                        .build())
                .addNewParameter()
                .withDescription("Password used for Redis authentication")
                .withFrom("[A-Z0-9]{8}")
                .withGenerate("expression")
                .withName("REDIS_PASSWORD")
                .endParameter()
                .build();

        boolean failOnMissingParameterValue = false;
        //When
        KubernetesList result = OpenshiftHelper.processTemplatesLocally(template, failOnMissingParameterValue);
        //Then
        assertThat(result)
            .isNotNull()
            .extracting(KubernetesList::getItems)
            .asList()
            .singleElement()
            .isInstanceOf(Pod.class);

        Pod item = (Pod) result.getItems().get(0);
        assertThat(item.getSpec().getContainers().get(0).getEnv().get(0).getName()).isEqualTo("REDIS_PASSWORD");
        assertThat(item.getSpec().getContainers().get(0).getEnv().get(0).getValue()).isNotEqualTo("${REDIS_PASSWORD}");
    }

    @Test
    void testProcessTemplatesLocallyNull() throws IOException {
        //Given
        Template template = new TemplateBuilder()
                .withNewMetadata().withName("redis-template").endMetadata()
                .addNewParameter()
                .withDescription("Password used for Redis authentication")
                .withFrom("[A-Z0-9]{8}")
                .withGenerate("expression")
                .withName("REDIS_PASSWORD")
                .endParameter()
                .build();
        boolean failOnMissingParameterValue = true;
        //When
        KubernetesList result = OpenshiftHelper.processTemplatesLocally(template, failOnMissingParameterValue);
        //Then
        assertThat(result).isNull();
    }

    @Test
    void testCombineTemplates() {
        //Given
        Template template = new TemplateBuilder()
                .withNewMetadata().withName("redis-template").endMetadata()
                .withObjects(new PodBuilder()
                        .withNewMetadata().withName("redis-master").endMetadata()
                        .withNewSpec()
                        .addNewContainer()
                        .addNewEnv()
                        .withName("REDIS_PASSWORD")
                        .withValue("${REDIS_PASSWORD}")
                        .endEnv()
                        .withImage("dockerfile/redis")
                        .withName("master")
                        .addNewPort()
                        .withProtocol("TCP")
                        .withContainerPort(6379)
                        .endPort()
                        .endContainer()
                        .endSpec()
                        .build())
                .addNewParameter()
                .withDescription("Password used for Redis authentication")
                .withDisplayName("this is displayName")
                .withFrom("[A-Z0-9]{8}")
                .withGenerate("expression")
                .withName("REDIS_PASSWORD")
                .endParameter()
                .build();

        Map<String, String> map = new HashMap<>();
        map.put("app", "java");
        template.getMetadata().setAnnotations(map);

        Template first_template = new TemplateBuilder()
                .withNewMetadata().withName("redis-copy").endMetadata()
                .build();

        //When
        Template result = OpenshiftHelper.combineTemplates(first_template, template);
        //Then
        assertThat(result.getMetadata().getName()).isEqualTo("redis-copy");
        assertThat(result.getParameters().get(0).getDisplayName()).isEqualTo("this is displayName");
    }

}
