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
package org.eclipse.jkube.kit.build.service.docker.access.hc;

import java.io.File;
import java.io.IOException;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.build.service.docker.access.hc.util.ClientBuilder;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "unused"})
class DockerAccessWithHcClientTest {

    private static final String BASE_URL = "tcp://1.2.3.4:2375";

    private AuthConfig authConfig;

    private DockerAccessWithHcClient client;

    private String imageName;

    private ApacheHttpClientDelegate mockDelegate;

    private int pushRetries;

    private String registry;

    private String archiveFile;
    private String filename;
    private ArchiveCompression compression;

    @BeforeEach
    void setup() throws IOException {
        mockDelegate = mock(ApacheHttpClientDelegate.class, RETURNS_DEEP_STUBS);
        client = new DockerAccessWithHcClient("tcp://1.2.3.4:2375", null, 1, new KitLogger.SilentLogger()) {
            @Override
            ApacheHttpClientDelegate createHttpClient(ClientBuilder builder) {
                return mockDelegate;
            }
        };
    }

    @Test
    void testPushImage_replacementOfExistingOfTheSameTag() throws Exception {
        String image = "test-image";
        String tag = "test-tag";
        String taggedImageName = String.format("%s:%s", image, tag);

        givenAnImageName(taggedImageName);
        givenRegistry("test-registry");
        givenThatGetWillSucceedWithOk();

        whenPushImage();

        thenImageWasTagged(image, tag);
        thenPushSucceeded(image, tag);
    }

    @Test
    void testPushImage_imageOfTheSameTagDoesNotExist() throws Exception {
        String image = "test-image";
        String tag = "test-tag";
        String taggedImageName = String.format("%s:%s", image, tag);

        givenAnImageName(taggedImageName);
        givenRegistry("test-registry");
        givenThatGetWillSucceedWithNotFound();
        givenThatDeleteWillSucceed();

        whenPushImage();

        thenImageWasTagged(image, tag);
        thenPushSucceeded(image, tag);
    }

    @Test
    void testPushFails_noRetry() throws Exception {
        givenAnImageName("test");
        givenThePushWillFail(0);
        assertThatExceptionOfType(DockerAccessException.class)
                .isThrownBy(this::whenPushImage)
                .withMessage("Unable to push 'test' : status code: 500, reason phrase: error");
    }

    @Test
    void testRetryPush() throws Exception {
        givenAnImageName("test");
        givenANumberOfRetries(1);
        givenThePushWillFailAndEventuallySucceed();
        whenPushImage();
        verify(mockDelegate, times(2)).post(anyString(), isNull(), anyMap(), any(), anyInt());
    }

    @Test
    void testRetriesExceeded() throws Exception {
        givenAnImageName("test");
        givenANumberOfRetries(1);
        givenThePushWillFail(1);
        assertThatExceptionOfType(DockerAccessException.class)
                .isThrownBy(this::whenPushImage)
                .withMessage("Unable to push 'test' : status code: 500, reason phrase: error");
    }

    @Test
    void testLoadImage() throws IOException {
        givenAnImageName("test");
        givenArchiveFile("test.tar");
        whenLoadImage();
        verify(mockDelegate).post(anyString(), any(), any(ApacheHttpClientDelegate.BodyAndStatusResponseHandler.class), eq(HTTP_OK));
    }

    @Test
    void testLoadImageFail() throws IOException {
        givenAnImageName("test");
        givenArchiveFile("test.tar");
        givenThePostWillFail();
        assertThatExceptionOfType(DockerAccessException.class)
                .isThrownBy(() -> client.loadImage(imageName, new File(archiveFile)))
                .withMessage("Unable to load test.tar : status code: 500, reason phrase: error");
    }

    @Test
    void testSaveImage() throws IOException {
        givenAnImageName("test");
        givenFilename("test.tar");
        givenCompression(ArchiveCompression.none);
        whenSaveImage();
        verify(mockDelegate).get(anyString(), any(ResponseHandler.class), eq(HTTP_OK));
    }

    @Test
    void testSaveImageFail() throws IOException {
        givenAnImageName("test");
        givenFilename("test.tar");
        givenCompression(ArchiveCompression.none);
        givenTheGetWillFail();
        assertThatExceptionOfType(DockerAccessException.class)
                .isThrownBy(this::whenSaveImage)
                .withMessage("Unable to save 'test' to 'test.tar' : status code: 500, reason phrase: error");
    }

    private void givenRegistry(String registry) {
        this.registry = registry;
    }

    private void givenAnImageName(String imageName) {
        this.imageName = imageName;
    }

    private void givenANumberOfRetries(int retries) {
        this.pushRetries = retries;
    }

    private void givenArchiveFile(String archiveFile) {
        this.archiveFile = archiveFile;
    }

    private void givenFilename(String filename) {
        this.filename = filename;
    }

    private void givenCompression(ArchiveCompression compression) {
        this.compression = compression;
    }

    private void givenThePushWillFailAndEventuallySucceed() throws IOException {
        when(mockDelegate.post(anyString(), isNull(), anyMap(), any(), anyInt()))
            .thenThrow(new HttpResponseException(HTTP_INTERNAL_ERROR, "error"))
            .thenReturn(null);
    }

    private void givenThePushWillFail(final int retries) throws IOException {
        OngoingStubbing<ApacheHttpClientDelegate> stubbing = when(mockDelegate.post(anyString(), isNull(), anyMap(), any(), eq(200)));

        for (int i = 0; i < retries + 1; i++) {
            stubbing.thenThrow(new HttpResponseException(HTTP_INTERNAL_ERROR, "error"));
        }
    }

    private void givenThePostWillFail() throws IOException {
        when(mockDelegate.post(anyString(), any(), any(ResponseHandler.class), eq(200)))
            .thenThrow(new HttpResponseException(HTTP_INTERNAL_ERROR, "error"));
    }

    private void givenThatGetWillSucceedWithOk() throws IOException {
        when(mockDelegate.get(anyString(), any(ResponseHandler.class), eq(HTTP_OK), eq(HTTP_NOT_FOUND)))
            .thenReturn(HTTP_OK);
    }

    private void givenThatGetWillSucceedWithNotFound() throws IOException {
        when(mockDelegate.get(anyString(), any(ResponseHandler.class), eq(HTTP_OK), eq(HTTP_NOT_FOUND)))
            .thenReturn(HTTP_NOT_FOUND);
    }

    private void givenTheGetWillFail() throws IOException {
        when(mockDelegate.get(anyString(), any(ResponseHandler.class), eq(200)))
            .thenThrow(new HttpResponseException(HTTP_INTERNAL_ERROR, "error"));
    }

    private void givenThatDeleteWillSucceed() throws IOException {
        when(mockDelegate.delete(anyString(), any(ResponseHandler.class), eq(HTTP_OK), eq(HTTP_NOT_FOUND)))
            .thenReturn(new ApacheHttpClientDelegate.HttpBodyAndStatus(HTTP_OK, "foo"));
    }

    private void thenPushSucceeded(String imageNameWithoutTag, String tag) throws IOException {
        String expectedUrl = String.format("%s/vnull/images/%s%%2F%s/push?force=1&tag=%s", BASE_URL, registry,
            imageNameWithoutTag, tag);
        verify(mockDelegate).post(eq(expectedUrl), isNull(), anyMap(), any(), eq(HTTP_OK));
    }

    private void thenImageWasTagged(String imageNameWithoutTag, String tag) throws IOException {
        String expectedUrl = String.format("%s/vnull/images/%s%%3A%s/tag?force=0&repo=%s%%2F%s&tag=%s", BASE_URL,
            imageNameWithoutTag, tag, registry, imageNameWithoutTag, tag);
        verify(mockDelegate).post(expectedUrl, HTTP_CREATED);
    }


    private void whenPushImage() throws DockerAccessException {
        client.pushImage(imageName, authConfig, registry, pushRetries);
    }

    private void whenLoadImage() throws DockerAccessException {
        client.loadImage(imageName, new File(archiveFile));
    }

    private void whenSaveImage() throws DockerAccessException {
        client.saveImage(imageName, filename, compression);
    }

    private String getImageNameWithRegistry() {
        return registry + "/" + imageName;
    }
}
