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
package org.eclipse.jkube.kit.config.service.openshift;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.image.ImageName;

import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.TagEvent;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author roland
 * @since 16/01/17
 */

@SuppressWarnings({"unchecked", "rawtypes", "unused"})
class ImageStreamServiceTest {

    private OpenShiftClient client;
    private MixedOperation mixedOperation;
    private NonNamespaceOperation nonNamespaceOperation;
    private Resource resourceOp;
    private KitLogger log;

    @BeforeEach
    public void setUp() {
        client = mock(OpenShiftClient.class);
        mixedOperation = mock(MixedOperation.class);
        nonNamespaceOperation = mock(NonNamespaceOperation.class);
        resourceOp = mock(Resource.class);
        log = new KitLogger.SilentLogger();
    }

    @Test
    void simple(@TempDir File temporaryFolder) throws Exception {
        ImageStreamService service = new ImageStreamService(client, "default", log);

        final ImageStream lookedUpIs = lookupImageStream("ab12cd");
        setupClientMock(lookedUpIs,"default", "test");
        ImageName name = new ImageName("test:1.0");
        File target = File.createTempFile("ImageStreamServiceTest",".yml", temporaryFolder);
        service.appendImageStreamResource(name, target);
        assertThat(target).exists();

        Map result = readImageStreamDescriptor(target);
        assertThat(result).isNotNull();

        List<Map> items = getItemsList(result);
        assertThat(items).hasSize(1);

        Map isRead = (Map<String, Object>) items.get(0);
        assertThat(isRead).isNotNull()
            .containsEntry("kind", "ImageStream");

        Map spec = (Map<String, Object>) isRead.get("spec");
        assertThat(spec).isNotNull();

        List tags = (List) spec.get("tags");
        assertThat(tags).isNotNull().hasSize(1);

        Map tag = (Map) tags.get(0);
        Map from = (Map) tag.get("from");
        assertThat(from)
                .containsEntry("kind", "ImageStreamImage")
                .containsEntry("name", "test@ab12cd")
                .containsEntry("namespace", "default");

        // Add a second image stream
        ImageStream secondIs = lookupImageStream("secondIS");
        setupClientMock(secondIs, "default", "second-test");
        ImageName name2 = new ImageName("second-test:1.0");
        service.appendImageStreamResource(name2, target);

        result = readImageStreamDescriptor(target);
        items = getItemsList(result);
        assertThat(items).hasSize(2);
        Set<String> names = new HashSet<>(Arrays.asList("second-test", "test"));
        for (Map item : items) {
            assertThat(names.remove(((Map) item.get("metadata")).get("name"))).isTrue();
        }
        assertThat(names).isEmpty();
    }

    private List<Map> getItemsList(Map result) {
        List items = (List) result.get("items");
        assertThat(items).isNotNull();
        return items;
    }

    private Map readImageStreamDescriptor(File target) throws IOException {
        return Serialization.unmarshal(target, Map.class);
    }

    private void setupClientMock(final ImageStream lookedUpIs, final String namespace, final String name) {
        when(client.imageStreams()).thenReturn(mixedOperation);
        when(mixedOperation.inNamespace(namespace)).thenReturn(nonNamespaceOperation);
        when(nonNamespaceOperation.withName(name)).thenReturn(resourceOp);
        when(resourceOp.get()).thenReturn(lookedUpIs);
    }

    private ImageStream lookupImageStream(String sha) {
        return new ImageStreamBuilder()
            .withNewStatus()
            .addNewTag()
            .addNewItem()
            .withImage(sha)
            .endItem()
            .endTag()
            .endStatus()
            .build();
    }

    @Test
    void should_return_newer_tag() {
        // GIVEN
        ImageStreamService service = new ImageStreamService(client, "default", log);
        TagEvent oldTag = new TagEvent("2018-03-09T03:27:05Z\n", null, null, null);
        TagEvent latestTag = new TagEvent("2018-03-09T03:28:05Z\n", null, null, null);

        // WHEN
        TagEvent resultedTag = service.newerTag(oldTag, latestTag);

        // THEN
        assertThat(resultedTag).isEqualTo(latestTag);
    }

    @Test
    void should_return_first_tag() {
        // GIVEN
        ImageStreamService service = new ImageStreamService(client, "default", log);
        TagEvent first = new TagEvent("2018-03-09T03:27:05Z\n", null, null, null);
        TagEvent latestTag = null;

        // WHEN
        TagEvent resultedTag = service.newerTag(first, latestTag);

        // THEN
        assertThat(resultedTag).isEqualTo(first);
    }

    @Test
    void resolveImageStreamTagName_withTagInImageName_shouldReturnImageStreamTagNameSameAsImageName() {
        // Given
        ImageName imageName = new ImageName("quay.io/foo/bar:1.0");

        // When
        String imageStreamTagName = ImageStreamService.resolveImageStreamTagName(imageName);

        // Then
        assertThat(imageStreamTagName).isEqualTo("bar:1.0");
    }

    @Test
    void resolveImageStreamTagName_withNoTag_shouldReturnImageStreamTagNameSameLatest() {
        // Given
        ImageName imageName = new ImageName("quay.io/foo/bar");

        // When
        String imageStreamTagName = ImageStreamService.resolveImageStreamTagName(imageName);

        // Then
        assertThat(imageStreamTagName).isEqualTo("bar:latest");
    }
}
