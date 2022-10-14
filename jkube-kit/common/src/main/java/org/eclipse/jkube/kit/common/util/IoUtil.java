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

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Random;

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
            try (Socket ignored = new Socket("localhost", port)) { // NOSONAR
                // Port is open for communication, meaning it's used up, try again
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

}
