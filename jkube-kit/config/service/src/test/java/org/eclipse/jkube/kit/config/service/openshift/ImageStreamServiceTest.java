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
package org.eclipse.jkube.kit.config.service.openshift;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageName;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.ImageStreamList;
import io.fabric8.openshift.api.model.TagEvent;
import io.fabric8.openshift.client.OpenShiftClient;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author roland
 * @since 16/01/17
 */
public class ImageStreamServiceTest {

    @Mocked
    OpenShiftClient client;

    @Mocked
    MixedOperation<ImageStream, ImageStreamList, Resource<ImageStream>> imageStreamsOp;

    @Mocked
    Resource resource;

    @Mocked
    KitLogger log;

    @Test
    public void simple() throws Exception {
        ImageStreamService service = new ImageStreamService(client, "default", log);

        final ImageStream lookedUpIs = lookupImageStream("ab12cd");
        setupClientMock(lookedUpIs,"default", "test");
        ImageName name = new ImageName("test:1.0");
        File target = File.createTempFile("ImageStreamServiceTest",".yml");
        service.appendImageStreamResource(name, target);

        assertTrue(target.exists());

        Map result = readImageStreamDescriptor(target);
        System.out.println(result.toString());
        assertNotNull(result);
        List<Map> items = getItemsList(result);
        assertEquals(1, items.size());
        Map isRead = (Map<String, Object>) items.get(0);
        assertNotNull(isRead);
        assertEquals("ImageStream", isRead.get("kind"));
        Map spec = (Map<String, Object>) isRead.get("spec");
        assertNotNull(spec);
        List tags = (List) spec.get("tags");
        assertNotNull(tags);
        assertEquals(1,tags.size());
        Map tag = (Map) tags.get(0);
        Map from = (Map) tag.get("from");
        assertEquals("ImageStreamImage", from.get("kind"));
        assertEquals("test@ab12cd", from.get("name"));
        assertEquals("default", from.get("namespace"));

        // Add a second image stream
        ImageStream secondIs = lookupImageStream("secondIS");
        setupClientMock(secondIs, "default", "second-test");
        ImageName name2 = new ImageName("second-test:1.0");
        service.appendImageStreamResource(name2, target);

        result = readImageStreamDescriptor(target);
        System.out.println(result.toString());
        items = getItemsList(result);
        assertEquals(2,items.size());
        Set<String> names = new HashSet<>(Arrays.asList("second-test", "test"));
        for (Map item : items) {
            assertTrue(names.remove( ((Map) item.get("metadata")).get("name")));
        }
        assertTrue(names.isEmpty());
    }

    private List<Map> getItemsList(Map result) {
        List items = (List) result.get("items");
        assertNotNull(items);
        return items;
    }

    private Map readImageStreamDescriptor(File target) throws IOException {
        try (InputStream fis = new FileInputStream(target)) {
            return Serialization.unmarshal(fis, Map.class);
        }
    }

    private void setupClientMock(final ImageStream lookedUpIs, final String namespace, final String name) {
        new Expectations() {{
            client.imageStreams(); result = imageStreamsOp;
            imageStreamsOp.inNamespace(namespace).withName(name); result = resource;
            resource.get(); result = lookedUpIs;
        }};
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
    public void should_return_newer_tag() {
        // GIVEN
        ImageStreamService service = new ImageStreamService(client, "default", log);
        TagEvent oldTag = new TagEvent("2018-03-09T03:27:05Z\n", null, null, null);
        TagEvent latestTag = new TagEvent("2018-03-09T03:28:05Z\n", null, null, null);

        // WHEN
        TagEvent resultedTag = service.newerTag(oldTag, latestTag);

        // THEN
        Assert.assertEquals(latestTag, resultedTag);
    }

    @Test
    public void should_return_first_tag() {
        // GIVEN
        ImageStreamService service = new ImageStreamService(client, "default", log);
        TagEvent first = new TagEvent("2018-03-09T03:27:05Z\n", null, null, null);
        TagEvent latestTag = null;

        // WHEN
        TagEvent resultedTag = service.newerTag(first, latestTag);

        // THEN
        Assert.assertEquals(first, resultedTag);
    }
}
