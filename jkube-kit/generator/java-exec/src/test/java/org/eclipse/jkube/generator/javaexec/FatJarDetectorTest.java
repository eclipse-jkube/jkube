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
package org.eclipse.jkube.generator.javaexec;

import java.io.File;
import java.net.URL;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static org.eclipse.jkube.kit.common.util.FileUtil.getAbsolutePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author roland
 * @since 14/11/16
 */
public class FatJarDetectorTest {

    @Test
    public void simple() throws MojoExecutionException {
        URL testDirUrl = getClass().getResource("/fatjar-simple");
        FatJarDetector detector = new FatJarDetector(getAbsolutePath(testDirUrl));
        FatJarDetector.Result result = detector.scan();
        assertNotNull(result);
        assertEquals(new File(getAbsolutePath(testDirUrl) + "/test.jar"), result.getArchiveFile());
        assertEquals("org.springframework.boot.loader.JarLauncher", result.getMainClass());
        assertEquals("Plexus Archiver", result.getManifestEntry("Archiver-Version"));
    }
}
