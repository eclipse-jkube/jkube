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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.api.model.KubernetesList;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.image.ImageName;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.TagEvent;
import io.fabric8.openshift.client.OpenShiftClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author roland
 * @since 16/01/17
 */

@SuppressWarnings({"unchecked", "rawtypes", "unused"})
@EnableKubernetesMockClient
class ImageStreamServiceTest {

    private OpenShiftClient client;
    private KubernetesMockServer server;
    private MixedOperation mixedOperation;
    private NonNamespaceOperation nonNamespaceOperation;
    private Resource resourceOp;
    private KitLogger log;

    @BeforeEach
    public void setUp() {
        log = new KitLogger.SilentLogger();
    }

    @Test
    void simple(@TempDir Path temporaryFolder) throws Exception {
        ImageStreamService service = new ImageStreamService(client, "default", log);

        final ImageStream lookedUpIs = lookupImageStream("ab12cd").build();
        server.expect().get().withPath("/apis/image.openshift.io/v1/namespaces/default/imagestreams/test")
            .andReturn(HTTP_OK, lookedUpIs)
            .once();
        ImageName name = new ImageName("test:1.0");
        File target = Files.createTempFile(temporaryFolder, "ImageStreamServiceTest", ".yml").toFile();
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
        ImageStream secondIs = lookupImageStream("secondIS").build();
        server.expect().get().withPath("/apis/image.openshift.io/v1/namespaces/default/imagestreams/second-test")
            .andReturn(HTTP_OK, secondIs)
            .once();
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


    private ImageStreamBuilder lookupImageStream(String sha) {
        return new ImageStreamBuilder()
            .withNewStatus()
            .addNewTag()
            .addNewItem()
            .withImage(sha)
            .endItem()
            .endTag()
            .endStatus();
    }

    @Test
    @DisplayName("when ImageStream not found on OpenShift, then throw exception")
    void appendImageStreamResource_whenImageStreamNotFound_thenThrowException(@TempDir File temporaryFolder) {
        // Given
        File imageStream = new File(temporaryFolder, "imagestream.yml");
        ImageStreamService imageStreamService = new ImageStreamService(client, "default", log, 2, 0);

        // When + Then
        assertThatIllegalStateException()
            .isThrownBy(() -> imageStreamService.appendImageStreamResource(new ImageName("foo:latest"), imageStream))
            .withMessage("Could not find a current ImageStream with name foo in namespace default");
    }

    @Test
    @DisplayName("when ImageStreamStatus from Api server has no tags, then throw exception")
    void appendImageStreamResource_whenNoTagInImageStream_thenThrowException(@TempDir File temporaryFolder) {
        // Given
        File imageStream = new File(temporaryFolder, "imagestream.yml");
        ImageStreamService service = new ImageStreamService(client, "default", log, 2, 0);
        server.expect().get().withPath("/apis/image.openshift.io/v1/namespaces/default/imagestreams/foo")
            .andReturn(HTTP_OK, lookupImageStream(null)
                .editStatus()
                .withTags(Collections.emptyList())
                .endStatus().build())
            .times(2);

        // When + Then
        assertThatIllegalStateException()
            .isThrownBy(() -> service.appendImageStreamResource(new ImageName("foo:latest"), imageStream))
            .withMessage("Could not find a tag in the ImageStream foo");
    }

    @Test
    @DisplayName("when ImageStream from Api server has no status, then throw exception")
    void appendImageStreamResource_whenNoImageStreamStatus_thenThrowException(@TempDir File temporaryFolder) {
        // Given
        File imageStream = new File(temporaryFolder, "imagestream.yml");
        ImageStreamService service = new ImageStreamService(client, "default", log, 2, 0);
        server.expect().get().withPath("/apis/image.openshift.io/v1/namespaces/default/imagestreams/foo")
            .andReturn(HTTP_OK, lookupImageStream(null)
                .withStatus(null)
                .build())
            .times(2);

        // When + Then
        assertThatIllegalStateException()
            .isThrownBy(() -> service.appendImageStreamResource(new ImageName("foo:latest"), imageStream))
            .withMessage("Could not find a tag in the ImageStream foo");
    }

    @Test
    @DisplayName("multiple tags in image stream status, then add latest tag to ImageStream")
    void appendImageStreamResource_whenMultipleTagsInImageStreamStatus_thenAddLatestOneToImageStream(@TempDir File temporaryFolder) throws IOException {
        // Given
        File imageStream = new File(temporaryFolder, "imagestream.yml");
        ImageStreamService service = new ImageStreamService(client, "default", log, 1, 0);
        server.expect().get().withPath("/apis/image.openshift.io/v1/namespaces/default/imagestreams/foo")
            .andReturn(HTTP_OK, lookupImageStream("t1").editStatus()
                .addNewTag()
                .withTag("t0")
                .addNewItem()
                .withImage("sha256:109de62d1f609a717ec433cc25ca5cf00941545c83a01fb31527771e1fab3fc5")
                .withCreated("2017-09-03T10:15:09Z")
                .endItem()
                .endTag()
                .addNewTag()
                .withTag("t1")
                .addNewItem()
                .withImage("sha256:909de62d1f609a717ec433cc25ca5cf00941545c83a01fb31527771e1fab3fc5")
                .withCreated("2017-09-02T10:15:09Z")
                .endItem()
                .endTag()
                .addNewTag()
                .withTag("t2")
                .addNewItem()
                .withImage("sha256:47463d94eb5c049b2d23b03a9530bf944f8f967a0fe79147dd6b9135bf7dd13d")
                .endItem()
                .endTag()
                .addNewTag()
                .withTag("t3")
                .endTag()
                .addNewTag()
                .withTag("t4")
                .addNewItem()
                .withCreated("2017-invalid-date")
                .endItem()
                .endTag()
                .endStatus()
                .build())
            .always();

        // When
        service.appendImageStreamResource(new ImageName("foo:latest"), imageStream);

        // Then
        KubernetesList kubernetesList = Serialization.unmarshal(imageStream, KubernetesList.class);
        assertThat(kubernetesList)
            .isNotNull()
            .extracting(DefaultKubernetesResourceList::getItems)
            .asList()
            .element(0)
            .asInstanceOf(InstanceOfAssertFactories.type(ImageStream.class))
            .hasFieldOrPropertyWithValue("metadata.name", "foo")
            .extracting("spec.tags")
            .asList()
            .element(0)
            .hasFieldOrPropertyWithValue("name", "latest")
            .hasFieldOrPropertyWithValue("from.kind", "ImageStreamImage")
            .hasFieldOrPropertyWithValue("from.name", "foo@sha256:109de62d1f609a717ec433cc25ca5cf00941545c83a01fb31527771e1fab3fc5")
            .hasFieldOrPropertyWithValue("from.namespace", "default");
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
