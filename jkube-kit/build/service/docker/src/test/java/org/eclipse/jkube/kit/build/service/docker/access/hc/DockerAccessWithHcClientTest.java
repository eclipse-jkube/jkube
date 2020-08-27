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
package org.eclipse.jkube.kit.build.service.docker.access.hc;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.service.docker.access.hc.util.ClientBuilder;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.junit.Before;
import org.junit.Test;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DockerAccessWithHcClientTest {


    private AuthConfig authConfig;

    private DockerAccessWithHcClient client;

    private String imageName;

    @Mocked
    private ApacheHttpClientDelegate mockDelegate;

    @Mocked
    private KitLogger mockLogger;

    private int pushRetries;

    private String registry;

    private Exception thrownException;
    private String archiveFile;
    private String filename;
    private ArchiveCompression compression;


    @Before
    public void setup() throws IOException {
        client = new DockerAccessWithHcClient("tcp://1.2.3.4:2375", null, 1, mockLogger) {
            @Override
            ApacheHttpClientDelegate createHttpClient(ClientBuilder builder) throws IOException {
                return mockDelegate;
            }
        };
    }

    @Test
    public void testPushFailes_noRetry() throws Exception {
        givenAnImageName("test");
        givenThePushWillFail(0);
        whenPushImage();
        thenImageWasNotPushed();
    }

    @Test
    public void testRetryPush() throws Exception {
        givenAnImageName("test");
        givenANumberOfRetries(1);
        givenThePushWillFailAndEventuallySucceed(1);
        whenPushImage();
        thenImageWasPushed();
    }

    @Test
    public void testRetriesExceeded() throws Exception {
        givenAnImageName("test");
        givenANumberOfRetries(1);
        givenThePushWillFail(1);
        whenPushImage();
        thenImageWasNotPushed();
    }

    @Test
    public void testLoadImage() {
        givenAnImageName("test");
        givenArchiveFile("test.tar");
        whenLoadImage();
        thenNoException();
    }
    @Test
    public void testLoadImageFail() throws IOException {
        givenAnImageName("test");
        givenArchiveFile("test.tar");
        givenThePostWillFail();
        whenLoadImage();
        thenImageWasNotLoaded();
    }

    @Test
    public void testSaveImage() throws IOException {
        givenAnImageName("test");
        givenFilename("test.tar");
        givenCompression(ArchiveCompression.none);
        whenSaveImage();
        thenNoException();
    }

    @Test
    public void testSaveImageFail() throws IOException {
        givenAnImageName("test");
        givenFilename("test.tar");
        givenCompression(ArchiveCompression.none);
        givenTheGetWillFail();
        whenSaveImage();
        thenImageWasNotSaved();
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void givenThePushWillFailAndEventuallySucceed(final int retries) throws IOException {
        new Expectations() {{
            int fail = retries;
            mockDelegate.post(anyString, null, (Map<String, String>) any, (ResponseHandler) any,  200);
            minTimes = fail; maxTimes = fail;
            result = new HttpResponseException(HTTP_INTERNAL_ERROR, "error");
            mockDelegate.post(anyString, null, (Map<String, String>) any, (ResponseHandler) any, 200);
            minTimes = 1; maxTimes = 1;
        }};
    }

    private void givenThePushWillFail(final int retries) throws IOException {
        new Expectations() {{
            int fail = retries + 1;
            mockDelegate.post(anyString, null, (Map<String, String>) any, (ResponseHandler) any,  200);
            minTimes = fail; maxTimes = fail;
            result = new HttpResponseException(HTTP_INTERNAL_ERROR, "error");
        }};
    }

    private void givenThePostWillFail() throws IOException {
        new Expectations() {{
            mockDelegate.post(anyString, any, (ResponseHandler) any, 200);
            result = new HttpResponseException(HTTP_INTERNAL_ERROR, "error");
        }};
    }

    private void givenTheGetWillFail() throws IOException {
        new Expectations() {{
            mockDelegate.get(anyString, (ResponseHandler) any, 200);
            result = new HttpResponseException(HTTP_INTERNAL_ERROR, "error");
        }};
    }

    private void thenImageWasNotPushed() {
        assertNotNull(thrownException);
    }

    private void thenImageWasPushed() {
        assertNull(thrownException);
    }

    private void whenPushImage() {
        try {
            client.pushImage(imageName, authConfig, registry, pushRetries);
        } catch (Exception e) {
            thrownException = e;
        }
    }
    private void whenLoadImage() {
        try {
            client.loadImage(imageName, new File(archiveFile));
        } catch (Exception e) {
            thrownException = e;
        }
    }

    private void whenSaveImage() {
        try {
            client.saveImage(imageName, filename, compression);
        } catch (Exception e) {
            thrownException = e;
        }
    }

    private void thenNoException() {
        assertNull(thrownException);
    }

    private void thenImageWasNotLoaded() {
        assertNotNull(thrownException);
    }

    private void thenImageWasNotSaved() {
        assertNotNull(thrownException);
    }


}
