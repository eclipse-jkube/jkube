package io.jshift.kit.common.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.jshift.kit.common.KitLogger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 *
 * Utilities for download and more
 * @author roland
 * @since 14/10/16
 */
public class IoUtil {

    /**
     * Download with showing the progress a given URL and store it in a file
     * @param log logger used to track progress
     * @param downloadUrl url to download
     * @param target target file where to store the downloaded data
     * @throws IOException IO Exception
     */
    public static void download(KitLogger log, URL downloadUrl, File target) throws IOException {
        log.progressStart();
        try {
            OkHttpClient client =
                    new OkHttpClient.Builder()
                            .readTimeout(30, TimeUnit.MINUTES).build();
            Request request = new Request.Builder()
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();

            try (OutputStream out = new FileOutputStream(target);
                 InputStream im = response.body().byteStream()) {

                long length = response.body().contentLength();
                InputStream in = response.body().byteStream();
                byte[] buffer = new byte[8192];

                long readBytes = 0;
                while (true) {
                    int len = in.read(buffer);
                    readBytes += len;
                    log.progressUpdate(target.getName(), "Downloading", getProgressBar(readBytes, length));
                    if (len <= 0) {
                        out.flush();
                        break;
                    }
                    out.write(buffer, 0, len);
                }
            }
        } catch (IOException e) {
            throw new IOException("Failed to download URL " + downloadUrl + " to  " + target + ": " + e, e);
        } finally {
            log.progressFinished();
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
        Random random = new Random();
        for (int i=0; i < attempts; i++) {
            int port = min + random.nextInt(max - min + 1);
            try (Socket socket = new Socket("localhost", port)) {
            } catch (ConnectException e) {
                return port;
            } catch (IOException e) {
                throw new IllegalStateException("Error while trying to check open ports", e);
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

    private static int PROGRESS_LENGTH = 50;

    private static String getProgressBar(long bytesRead, long length) {
        StringBuffer ret = new StringBuffer("[");
        if (length > - 1) {
            int bucketSize = (int) (length / PROGRESS_LENGTH + 0.5);
            int index = (int) (bytesRead / bucketSize + 0.5);
            for (int i = 0; i < PROGRESS_LENGTH; i++) {
                ret.append(i < index ? "=" : (i == index ? ">" : " "));
            }
            ret.append(String.format("] %.2f MB/%.2f MB",
                    ((float) bytesRead / (1024 * 1024)),
                    ((float) length / (1024 * 1024))));
        } else {
            int bucketSize = 200 * 1024; // 200k
            int index = (int) (bytesRead / bucketSize + 0.5) % PROGRESS_LENGTH;
            for (int i = 0; i < PROGRESS_LENGTH; i++) {
                ret.append(i == index ? "*" : " ");
            }
            ret.append("]");
        }

        return ret.toString();
    }
}
