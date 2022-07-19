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
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpenshiftHelperTest {

    @Mock
    KubernetesClient kc;

    @Mock
    OpenShiftClient oc;

    @Test
    public void testAsOpenShiftClientWithNoOpenShift() {
        // Given
        doThrow(new KubernetesClientException("")).when(kc).adapt(OpenShiftClient.class);
        //When
        OpenShiftClient result = OpenshiftHelper.asOpenShiftClient(kc);
        //Then
        assertNull(result);
    }

    @Test
    public void testOpenShiftClientWithAdaptableToOpenShift() {
        // Given
        doReturn(oc).when(kc).adapt(OpenShiftClient.class);
        //When
        OpenShiftClient result = OpenshiftHelper.asOpenShiftClient(kc);
        //Then
        assertNotNull(result);
    }

    @Test
    public void testOpenShiftClientWithOpenShift() {
        //When
        OpenShiftClient result = OpenshiftHelper.asOpenShiftClient(oc);
        //Then
        assertNotNull(result);
    }

    @Test
    public void testIsOpenShiftWhenSupported() {
        // Given
        doReturn(oc).when(kc).adapt(OpenShiftClient.class);
        when(oc.isSupported()).thenReturn(true);
        //When
        boolean result = OpenshiftHelper.isOpenShift(kc);
        //Then
        assertTrue(result);
    }

    @Test
    public void testIsOpenShiftNotSupported() {
        // Given
        doReturn(oc).when(kc).adapt(OpenShiftClient.class);
        when(oc.isSupported()).thenReturn(false);
        //When
        boolean result = OpenshiftHelper.isOpenShift(kc);
        //Then
        assertFalse(result);
    }


    @Test
    public void testProcessTemplatesLocallyNotNull() throws IOException {
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
        assertEquals(1, result.getItems().size());
        assertTrue(result.getItems().get(0) instanceof Pod);
        Pod item = (Pod) result.getItems().get(0);
        assertEquals("REDIS_PASSWORD", item.getSpec().getContainers().get(0).getEnv().get(0).getName());
        assertNotEquals("${REDIS_PASSWORD}", item.getSpec().getContainers().get(0).getEnv().get(0).getValue());
    }

    @Test
    public void testProcessTemplatesLocallyNull() throws IOException {
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
        assertNull(result);
    }

    @Test
    public void testCombineTemplates() {
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

        Map<String, String> map = new HashMap<String, String>();
        map.put("app", "java");
        template.getMetadata().setAnnotations(map);

        Template first_template = new TemplateBuilder()
                .withNewMetadata().withName("redis-copy").endMetadata()
                .build();

        //When
        Template result = OpenshiftHelper.combineTemplates(first_template, template);
        //Then
        assertEquals("redis-copy", result.getMetadata().getName());
        assertEquals("this is displayName", result.getParameters().get(0).getDisplayName());
    }

}
