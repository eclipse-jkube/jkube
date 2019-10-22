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
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Class for finding out the fat jar of a directory and provide
 * some insights into the fat jar
 * @author roland
 * @since 10/11/16
 */
public class FatJarDetector {

    private File directory;
    private Result result;

    FatJarDetector(String dir) {
        this.directory = new File(dir);
    }

    Result scan() throws MojoExecutionException {
        // Scanning is lazy ...
        if (result == null) {
            if (!directory.exists()) {
                // No directory to check found so we return null here ...
                return null;
            }
            String[] jarOrWars = directory.list((dir, name) -> name.endsWith(".war") || name.endsWith(".jar"));
            if (jarOrWars == null || jarOrWars.length == 0) {
                return null;
            }
            long maxSize = 0;
            for (String jarOrWar : jarOrWars) {
                File archiveFile = new File(directory, jarOrWar);
                try (JarFile archive = new JarFile(archiveFile)){
                    Manifest mf = archive.getManifest();
                    Attributes mainAttributes = mf.getMainAttributes();
                    if (mainAttributes != null) {
                        String mainClass = mainAttributes.getValue("Main-Class");
                        if (mainClass != null) {
                            long size = archiveFile.length();
                            // Take the largest jar / war file found
                            if (size > maxSize) {
                                maxSize = size;
                                result = new Result(archiveFile, mainClass, mainAttributes);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new MojoExecutionException("Cannot examine file " + archiveFile + " for the manifest");
                }
            }
        }
        return result;
    }

    public class Result {

        private final File archiveFile;
        private final String mainClass;
        private final Attributes attributes;

        public Result(File archiveFile, String mainClass, Attributes attributes) {
            this.archiveFile = archiveFile;
            this.mainClass = mainClass;
            this.attributes = attributes;
        }

        public File getArchiveFile() {
            return archiveFile;
        }

        public String getMainClass() {
            return mainClass;
        }

        public String getManifestEntry(String key) {
            return attributes.getValue(key);
        }
    }
}