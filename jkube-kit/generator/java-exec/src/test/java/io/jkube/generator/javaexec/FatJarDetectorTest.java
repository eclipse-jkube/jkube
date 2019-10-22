/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jkube.generator.javaexec;

import java.io.File;
import java.net.URL;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static io.jkube.kit.common.util.FileUtil.getAbsolutePath;
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
