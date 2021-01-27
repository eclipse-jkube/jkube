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
import io.fabric8.kubernetes.api.model.KubernetesResource;
import org.eclipse.jkube.kit.common.GenericCustomResource;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

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
    public void testLoadKubernetesResourceListWithFileContainingRegularAndCustomResources() throws IOException {
        // Given
        File kubernetesManifestFile = new File(getClass().getResource("/test-kubernetes.yml").getFile());

        // When
        List<KubernetesResource> kubernetesResourceList = ResourceUtil.loadKubernetesResourceList(kubernetesManifestFile);

        // Then
        assertNotNull(kubernetesResourceList);
        assertEquals(10, kubernetesResourceList.size());
        assertEquals(5, kubernetesResourceList.stream().filter(h -> h instanceof GenericCustomResource).count());
    }

    @Test
    public void testLoadKubernetesResourceListWithNonExistentFile() throws IOException {
        // Given
        File kubernetesManifestFile = new File("i-dont-exist.yml");

        // When
        List<KubernetesResource> kubernetesResourceList = ResourceUtil.loadKubernetesResourceList(kubernetesManifestFile);

        // Then
        assertNotNull(kubernetesResourceList);
        assertEquals(0, kubernetesResourceList.size());
    }

    @Test
    public void testLoadKubernetesResourceListWithEmptyFile() throws IOException {
        // Given
        File kubernetesManifestFile = Files.createTempFile("kubernetes-", ".yaml").toFile();

        // When
        List<KubernetesResource> kubernetesResourceList = ResourceUtil.loadKubernetesResourceList(kubernetesManifestFile);

        // Then
        assertNotNull(kubernetesResourceList);
        assertEquals(0, kubernetesResourceList.size());
    }
}
