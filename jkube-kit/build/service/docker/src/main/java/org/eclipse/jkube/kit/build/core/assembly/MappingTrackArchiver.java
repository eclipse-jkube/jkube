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
package org.eclipse.jkube.kit.build.core.assembly;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.jkube.kit.common.JkubeProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.diags.TrackingArchiver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResource;

/**
 * An archiver which remembers all resolved files and directories and returns them
 * on request.
 *
 * @author roland
 * @since 15/06/15
 */
@Component(role = Archiver.class, hint = "track", instantiationStrategy = "singleton")
public class MappingTrackArchiver extends TrackingArchiver {

    // Logger to use
    protected KitLogger log;

    // Target directory to use for storing the assembly files (== name)
    private String assemblyName;

    /**
     * Get all files depicted by this assembly.
     *
     * @return assembled files
     */
    public AssemblyFiles getAssemblyFiles(JkubeProject jkubeProject) throws IOException {
        AssemblyFiles ret = new AssemblyFiles(new File(getDestFile().getParentFile(), assemblyName));

        // Where the 'real' files are copied to
        for (Addition addition : added) {
            Object resource = addition.resource;
            File target = new File(ret.getAssemblyDirectory(), addition.destination);
            if (resource instanceof File && addition.destination != null) {
                addFileEntry(ret, jkubeProject, (File) resource, target);
            } else if (resource instanceof PlexusIoFileResource) {
                addFileEntry(ret, jkubeProject, ((PlexusIoFileResource) resource).getFile(), target);
            } else if (resource instanceof FileSet) {
                File base = addition.directory;

                List<File> filesInFolder = Files.walk(Paths.get(base.getAbsolutePath()))
                        .filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .collect(Collectors.toList());
                for (File f : filesInFolder) {
                    File source = new File(base, f.getName());
                    File subTarget = new File(target, f.getName());
                    addFileEntry(ret, jkubeProject, source, subTarget);
                }

            } else {
                throw new IllegalStateException("Unknown resource type " + resource.getClass() + ": " + resource);
            }
        }
        return ret;
    }

    private void addFileEntry(AssemblyFiles ret, JkubeProject project, File source, File target) {
        ret.addEntry(source, target);
        addLocalMavenRepoEntry(ret, project, source, target);
    }

    private void addLocalMavenRepoEntry(AssemblyFiles ret, JkubeProject project, File source, File target) {
        File localMavenRepoFile = getLocalMavenRepoFile(project, source);
        try {
            if (localMavenRepoFile != null &&
                ! source.getCanonicalFile().equals(localMavenRepoFile.getCanonicalFile())) {
                ret.addEntry(localMavenRepoFile, target);
            }
        } catch (IOException e) {
            log.warn("Cannot add %s for watching: %s. Ignoring for watch ...", localMavenRepoFile, e.getMessage());
        }
    }

    private File getLocalMavenRepoFile(JkubeProject project, File source) {
        String localRepo = project.getLocalRepositoryBaseDirectory();
        if (localRepo == null) {
            log.warn("No local repo found so not adding any extra watches in the local repository");
            return null;
        }

        File artifact = getArtifactFromJar(source);
        if (artifact != null) {
                return new File(localRepo, artifact.getAbsolutePath());
        }
        return null;
    }

    // look into a jar file and check for pom.properties. The first pom.properties found are returned.
    private File getArtifactFromJar(File jar) {
        // Lets figure the real mvn source of file.
        String type = extractFileType(jar);
        if (type != null) {
            try {
                ArrayList<Properties> options = new ArrayList<Properties>();
                try (ZipInputStream in = new ZipInputStream(new FileInputStream(jar))) {
                    ZipEntry entry;
                    while ((entry = in.getNextEntry()) != null) {
                        if (entry.getName().startsWith("META-INF/maven/") && entry.getName().endsWith("pom.properties")) {
                            byte[] buf = new byte[1024];
                            int len;
                            ByteArrayOutputStream out = new ByteArrayOutputStream(); //change output stream as required
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                            Properties properties = new Properties();
                            properties.load(new ByteArrayInputStream(out.toByteArray()));
                            options.add(properties);
                        }
                    }
                }
                if (options.size() == 1) {
                    return getArtifactFromPomProperties(type,options.get(0));
                } else {
                    log.warn("Found %d pom.properties in %s", options.size(), jar);
                }
            } catch (IOException e) {
                log.warn("IO Exception while examining %s for maven coordinates: %s. Ignoring for watching ...",
                         jar, e.getMessage());
            }
        }
        return null;
    }

    // type when it is a Java archive, null otherwise
    private final static Pattern JAVA_ARCHIVE_DETECTOR = Pattern.compile("^.*\\.(jar|war|ear)$");
    private String extractFileType(File source) {
        Matcher matcher = JAVA_ARCHIVE_DETECTOR.matcher(source.getName());
        return matcher.matches() ? matcher.group(1) : null;
    }

    private File getArtifactFromPomProperties(String type, Properties pomProps) throws IOException {
        File pomProperties = new File("pom.properties");
        try (FileWriter fileWriter = new FileWriter(pomProperties)) {
            fileWriter.write("version=" + pomProps.getProperty("version"));
            fileWriter.write("groupId=" + pomProps.getProperty("groupId"));
            fileWriter.write("artifactId=" + pomProps.getProperty("artifactId"));
        }
        return pomProperties;
    }

    public void init(KitLogger log, String assemblyName) {
        this.log = log;
        this.assemblyName = assemblyName;
        added.clear();
    }
}
