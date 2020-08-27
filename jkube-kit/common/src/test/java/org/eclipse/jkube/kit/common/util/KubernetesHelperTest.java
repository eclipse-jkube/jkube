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

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import mockit.Mocked;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KubernetesHelperTest {
    @Mocked
    private KitLogger logger;

    @Test
    public void testListResourceFragments() {
        // Given
        File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());

        // When & Then
        assertLocalFragments(KubernetesHelper.listResourceFragments(localResourceDir, null, logger), 2);
    }

    @Test
    public void testResourceFragmentsWithRemotes() {
        // Given
        List<String> remoteStrList = getRemoteFragments();
        File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());

        // When
        File[] fragments = KubernetesHelper.listResourceFragments(localResourceDir, remoteStrList, logger);

        // Then
        assertLocalFragments(fragments, 4);
        assertTrue(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("deployment.yaml")));
        assertTrue(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("sa.yml")));
    }

    @Test
    public void testGetResourceFragmentFromSourceWithSomeResourceDirAndNullRemotes() {
        // Given
        File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());

        // When
        File fragmentFile = KubernetesHelper.getResourceFragmentFromSource(localResourceDir, Collections.emptyList(), "service.yml", logger);

        // Then
        assertNotNull(fragmentFile);
        assertTrue(fragmentFile.exists());
        assertEquals("service.yml", fragmentFile.getName());
    }

    @Test
    public void testGetResourceFragmentWithNullResourceDirAndNullRemotes() {
        assertNull(KubernetesHelper.getResourceFragmentFromSource(null, null, "service.yml", logger));
    }

    @Test
    public void testGetResourceFragmentFromSourceWithNullResourceDirAndSomeRemotes() {
        // Given
        List<String> remotes = getRemoteFragments();

        // When
        File fragmentFile = KubernetesHelper.getResourceFragmentFromSource(null, remotes, "deployment.yaml", logger);

        // Then
        assertNotNull(fragmentFile);
        assertTrue(fragmentFile.exists());
        assertEquals("deployment.yaml", fragmentFile.getName());
    }

    @Test
    public void testGetResourceFragmentFromSourceWithSomeResourceDirAndSomeRemotes() {
        // Given
        File localResourceDir = new File(getClass().getResource("/util/fragments").getPath());
        List<String> remotes = getRemoteFragments();

        // When
        File fragmentFile = KubernetesHelper.getResourceFragmentFromSource(localResourceDir, remotes, "sa.yml", logger);

        // Then
        assertTrue(fragmentFile.exists());
        assertEquals("sa.yml", fragmentFile.getName());
    }

    @Test
    public void testGetQuantityFromString() {
        // Given
        Map<String, String> limitsAsStr = new HashMap<>();
        limitsAsStr.put("cpu", "200m");
        limitsAsStr.put("memory", "1Gi");

        // When
        Map<String, Quantity> limitAsQuantity = KubernetesHelper.getQuantityFromString(limitsAsStr);

        // Then
        assertNotNull(limitAsQuantity);
        assertEquals(2, limitAsQuantity.size());
        assertEquals(new Quantity("200m"), limitAsQuantity.get("cpu"));
        assertEquals(new Quantity("1Gi"), limitAsQuantity.get("memory"));
    }

    @Test
    public void testGetEnvVar() {
        // Given
        List<EnvVar> envVarList = prepareEnvVarList();

        // When
        String value1 = KubernetesHelper.getEnvVar(envVarList, "env1", "defaultValue");
        String value2 = KubernetesHelper.getEnvVar(envVarList, "JAVA_OPTIONS", "defaultValue");
        String value3 = KubernetesHelper.getEnvVar(envVarList, "FOO", "defaultValue");
        String value4 = KubernetesHelper.getEnvVar(envVarList, "UNKNOWN", "defaultValue");

        // Then
        assertEquals("value1", value1);
        assertEquals("-Dfoo=bar -Dxyz=abc", value2);
        assertEquals("BAR", value3);
        assertEquals("defaultValue", value4);
    }

    @Test
    public void testSetEnvVar() {
        // Given
        List<EnvVar> envVarList = prepareEnvVarList();

        // When
        boolean statusCode1 = KubernetesHelper.setEnvVar(envVarList, "FOO", "NEW_BAR");
        boolean statusCode2 = KubernetesHelper.setEnvVar(envVarList, "UNKNOWN_KEY", "UNKNOWN_VALUE");

        // Then
        assertTrue(statusCode1);
        assertEquals("NEW_BAR", KubernetesHelper.getEnvVar(envVarList, "FOO", "defaultValue"));
        assertTrue(statusCode2);
        assertEquals("UNKNOWN_VALUE", KubernetesHelper.getEnvVar(envVarList, "UNKNOWN_KEY", "defaultValue"));
    }

    @Test
    public void testRemoveEnvVar() {
        // Given
        List<EnvVar> envVarList = prepareEnvVarList();

        // When
        boolean statusCode1 = KubernetesHelper.removeEnvVar(envVarList, "FOO");

        // Then
        assertTrue(statusCode1);
        assertEquals("defaultValue", KubernetesHelper.getEnvVar(envVarList, "FOO", "defaultValue"));
    }

    @Test
    public void testConvertToEnvVarList() {
        // Given
        Map<String, String> envVarAsStringMap = new HashMap<>();
        envVarAsStringMap.put("env1", "value1");
        envVarAsStringMap.put("JAVA_OPTIONS", "-Dfoo=bar -Dxyz=abc");
        envVarAsStringMap.put("FOO", "BAR");

        // When
        List<EnvVar> envVarList = KubernetesHelper.convertToEnvVarList(envVarAsStringMap);

        // Then
        assertNotNull(envVarList);
        assertEquals(3, envVarList.size());
        assertEquals("value1", KubernetesHelper.getEnvVar(envVarList, "env1", "defaultValue"));
        assertEquals("-Dfoo=bar -Dxyz=abc", KubernetesHelper.getEnvVar(envVarList, "JAVA_OPTIONS", "defaultValue"));
        assertEquals("BAR", KubernetesHelper.getEnvVar(envVarList, "FOO", "defaultValue"));

    }

    @Test
    public void getCustomResourcesFileToNameMapWithNoFragmentsShouldReturnEmptyMap() throws Exception {
        // When
        final Map<File, String> result = KubernetesHelper.getCustomResourcesFileToNameMap(null, null, logger);
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private void assertLocalFragments(File[] fragments, int expectedSize) {
        assertEquals(expectedSize, fragments.length);
        assertTrue(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("deployment.yml")));
        assertTrue(Arrays.stream(fragments).anyMatch( f -> f.getName().equals("service.yml")));
    }

    private List<EnvVar> prepareEnvVarList() {
        List<EnvVar> envVarList = new ArrayList<>();
        envVarList.add(new EnvVarBuilder().withName("env1").withValue("value1").build());
        envVarList.add(new EnvVarBuilder().withName("JAVA_OPTIONS").withValue("-Dfoo=bar -Dxyz=abc").build());
        envVarList.add(new EnvVarBuilder().withName("FOO").withValue("BAR").build());

        return envVarList;
    }

    private List<String> getRemoteFragments() {
        List<String> remoteStrList = new ArrayList<>();

        remoteStrList.add("https://gist.githubusercontent.com/lordofthejars/ac2823cec7831697d09444bbaa76cd50/raw/e4b43f1b6494766dfc635b5959af7730c1a58a93/deployment.yaml");
        remoteStrList.add("https://gist.githubusercontent.com/rohanKanojia/c4ac4ae5533f0bf0dd77d13c905face7/raw/8a7de1e27c1f437c1ccbd186ed247efd967953ee/sa.yml");
        return remoteStrList;
    }
}
