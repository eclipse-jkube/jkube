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
package org.eclipse.jkube.kit.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.http.HttpClient;
import io.fabric8.kubernetes.client.http.HttpResponse;
import io.fabric8.kubernetes.client.http.StandardHttpHeaders;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import org.eclipse.jkube.kit.common.KitLogger;

import static org.apache.commons.io.IOUtils.EOF;
import static org.eclipse.jkube.kit.common.archive.ArchiveDecompressor.extractArchive;

/**
 *
 * Utilities for download and more
 * @author roland
 * @since 14/10/16
 */
public class IoUtil {

    private static final Random RANDOM = new Random();

    private IoUtil() { }

    /**
     * Download with showing the progress a given URL and store it in a file
     * @param log logger used to track progress
     * @param downloadUrl url to download
     * @param target target file where to store the downloaded data
     * @throws IOException IO Exception
     */
    public static void download(KitLogger log, URL downloadUrl, File target) throws IOException {
        download(downloadUrl, response -> {
            try (OutputStream out = Files.newOutputStream(target.toPath()); InputStream is = response.body()) {
                log.progressStart();
                final int length = Integer.parseInt(response.headers(StandardHttpHeaders.CONTENT_LENGTH)
                    .stream().findAny().orElse("-1"));
                final byte[] buffer = new byte[8192];
                long readBytes = 0;
                int len;
                while (EOF != (len = is.read(buffer))) {
                    readBytes += len;
                    log.progressUpdate(target.getName(), "Downloading", getProgressBar(readBytes, length));
                    out.write(buffer, 0, len);
                }
            } finally {
                log.progressFinished();
            }
        });
    }

    /**
     * Download and extract a binary file packaged in an archive.
     *
     * @param downloadUrl download URL to archive containing binary file
     * @param target target directory where this archive would be extracted to
     * @throws IOException in case of error while downloading or extracting archive
     */
    public static void downloadArchive(URL downloadUrl, File target) throws IOException {
        download(downloadUrl, response -> {
            try (InputStream is = response.body()) {
                extractArchive(is, target);
            }
        });
    }

    private static void download(URL downloadUrl, HttpResponseConsumer<InputStream> responseConsumer) throws IOException {
        try (HttpClient httpClient = HttpClientUtils.getHttpClientFactory().newBuilder(Config.empty()).build()) {
            final HttpResponse<InputStream> response = httpClient.sendAsync(
                    httpClient.newHttpRequestBuilder().timeout(30, TimeUnit.MINUTES).url(downloadUrl).build(), InputStream.class)
                .get();
            if (!response.isSuccessful()) {
                throw new IOException("Got (" + response.code() + ") while downloading from URL " + downloadUrl);
            }
            responseConsumer.consume(response);
        }  catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", ex);
        } catch (ExecutionException e) {
            throw new IOException("Failed to download from URL " + downloadUrl, e);
        }
    }

    /**
     * Find a free (on localhost) random port in the range [49152, 65535] after 100 attempts.
     *
     * @return a random port where a server socket can be bound to
     */
    public static int getFreeRandomPort() {
        // 100 attempts should be enough
        return getFreeRandomPort(49152, 65535, 100);
    }

    /**
     *
     * Find a free (on localhost) random port in the specified range after the given number of attempts.
     *
     * @param min minimum value for port
     * @param max maximum value for port
     * @param attempts number of attempts
     * @return random port as integer
     */
    public static int getFreeRandomPort(int min, int max, int attempts) {
        for (int i=0; i < attempts; i++) {
            int port = min + RANDOM.nextInt(max - min + 1);
            try (ServerSocket ignored = new ServerSocket(port)) {
                return port;
            } catch (Exception e) {
                // NOOP
            }
        }
        throw new IllegalStateException("Cannot find a free random port in the range [" + min + ", " + max + "] after " + attempts + " attempts");
    }

    /**
     * Returns an identifier from the given string that can be used as file name.
     *
     * @param name file name
     * @return sanitized file name
     */
    public static String sanitizeFileName(String name) {
        if (name != null) {
            return name.replaceAll("[^A-Za-z0-9]+", "-");
        }

        return null;
    }

    // ========================================================================================

    private static final int PROGRESS_LENGTH = 50;

    private static String getProgressBar(long bytesRead, long length) {
        StringBuilder ret = new StringBuilder("[");
        if (length > - 1) {
            int bucketSize = (int) ((double)length / PROGRESS_LENGTH + 0.5D);
            int index = (int) ((double)bytesRead / bucketSize + 0.5D);
            for (int i = 0; i < PROGRESS_LENGTH; i++) {
                ret.append(i < index ? "=" : (i == index ? ">" : " "));
            }
            ret.append(String.format("] %.2f MB/%.2f MB",
                    ((float) bytesRead / (1024 * 1024)),
                    ((float) length / (1024 * 1024))));
        } else {
            int bucketSize = 200 * 1024; // 200k
            int index = (int) ((double)bytesRead / bucketSize + 0.5D) % PROGRESS_LENGTH;
            for (int i = 0; i < PROGRESS_LENGTH; i++) {
                ret.append(i == index ? "*" : " ");
            }
            ret.append("]");
        }

        return ret.toString();
    }

    @FunctionalInterface
    private interface HttpResponseConsumer<T> {
        void consume(HttpResponse<T> httpResponse) throws IOException;
    }
}
