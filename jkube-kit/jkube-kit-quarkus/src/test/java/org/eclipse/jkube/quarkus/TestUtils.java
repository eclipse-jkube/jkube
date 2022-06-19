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
package org.eclipse.jkube.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author cdprete
 * @since  01/04/2022
 */
public final class TestUtils {
    private TestUtils() {
        throw new AssertionError("Utility classes must not be instantiated");
    }

    public static File getResourceTestFile(Class<?> testClass, String path) throws URISyntaxException {
        assertThat(testClass).isNotNull();

        final URL fileUrl = testClass.getResource(path);
        assertThat(fileUrl).isNotNull();

        return new File(fileUrl.toURI());
    }

    public static String getResourceTestFilePath(Class<?> testClass, String path) throws URISyntaxException {
        return getResourceTestFile(testClass, path).toString();
    }
}
