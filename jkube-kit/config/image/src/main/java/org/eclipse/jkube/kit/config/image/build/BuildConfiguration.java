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
package org.eclipse.jkube.kit.config.image.build;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.common.util.EnvUtil;

/**
 * @author roland
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode(doNotUseGetters = true)
public class BuildConfiguration implements Serializable {

    private static final long serialVersionUID = 3904939784596208966L;

    public static final String DEFAULT_FILTER = "${*}";
    public static final String DEFAULT_CLEANUP = "try";

    /**
     * Directory used as the context directory, e.g. for a docker build.
     */
    private String contextDir;
    /**
     * Path to a dockerfile to use. Its parent directory is used as build context (i.e. as <code>dockerFileDir</code>).
     * Multiple different Dockerfiles can be specified that way. If set overwrites a possibly given.
     * <code>contextDir</code>
     */
    private String dockerFile;
    /**
     * Path to a docker archive to load an image instead of building from scratch.
     * Note only either dockerFile/dockerFileDir or
     * dockerArchive can be used.
     */
    private String dockerArchive;
    /**
     * How interpolation of a dockerfile should be performed.
     */
    private String filter;
    /**
     * Base Image
     */
    private String from;
    /**
     * Extended version for ;&lt;from;&gt;
     */
    private Map<String, String> fromExt;
    private String registry;
    private String maintainer;
    @Singular
    private List<String> ports;
    private Arguments shell;
    /**
     * Policy for pulling the base images
     */
    private String imagePullPolicy;
    /**
     * RUN Commands within Build/Image
     */
    @Singular
    private List<String> runCmds;
    private String cleanup;
    private Boolean nocache;
    private Boolean optimise;
    @Singular
    private List<String> volumes;
    @Singular
    private List<String> tags;
    @Singular("putEnv")
    private Map<String, String> env;
    @Singular
    private Map<String, String> labels;
    private Map<String, String> args;
    private Arguments entryPoint;
    private String workdir;
    private Arguments cmd;
    private String user;
    private HealthCheckConfiguration healthCheck;
    private AssemblyConfiguration assembly;
    private Boolean skip;
    private ArchiveCompression compression;
    private Map<String,String> buildOptions;
    /**
     * Directory holding an external Dockerfile which is used to build the
     * image. This Dockerfile will be enriched by the addition build configuration
     */
    @Deprecated
    private String dockerFileDir;
    /**
     * Path to Dockerfile to use, initialized lazily.
     */
    private File dockerFileFile;
    private File dockerArchiveFile;

    public boolean isDockerFileMode() {
        return dockerFile != null || contextDir != null;
    }

    public File getDockerFile() {
        return dockerFileFile;
    }

    public String getDockerFileRaw() {
        return dockerFile;
    }

    public File getDockerArchive() {
        return dockerArchiveFile;
    }

    public String getDockerArchiveRaw() {
        return dockerArchive;
    }

    public File getContextDir() {
        return contextDir != null ? new File(contextDir) : getDockerFile().getParentFile();
    }


    public String getContextDirRaw() {
        return contextDir;
    }


    public String getDockerFileDirRaw() {
        return dockerFileDir;
    }

    public String getFilterRaw() {
        return filter;
    }

    public String getFrom() {
        if (from == null && getFromExt() != null) {
            return getFromExt().get("name");
        }
        return from;
    }


    public AssemblyConfiguration getAssemblyConfiguration() {
        return assembly;
    }

    public List<String> getPorts() {
        return removeEmptyEntries(ports);
    }

    public List<String> getVolumes() {
        return removeEmptyEntries(volumes);
    }

    public List<String> getTags() {
        return removeEmptyEntries(tags);
    }

    public String getCleanupMode() {
        return cleanup;
    }

    public Boolean getNoCache() {
        return nocache;
    }


    public Boolean getSkip() {
        return Optional.ofNullable(skip).orElse(false);
    }

    public ArchiveCompression getCompression() {
        return Optional.ofNullable(compression).orElse(ArchiveCompression.none);
    }

    public List<String> getRunCmds() {
        return removeEmptyEntries(runCmds);
    }


    public boolean optimise() {
        return Optional.ofNullable(optimise).orElse(false);
    }

    public boolean nocache() {
        return Optional.ofNullable(nocache).orElse(false);
    }

    public CleanupMode cleanupMode() {
        return CleanupMode.parse(cleanup != null ? cleanup : DEFAULT_CLEANUP);
    }


    public File getAbsoluteContextDirPath(String sourceDirectory, String projectBaseDir) {
        return EnvUtil.prepareAbsoluteSourceDirPath(sourceDirectory, projectBaseDir, getContextDir().getPath());
    }

    public File getAbsoluteDockerFilePath(String sourceDirectory, String projectBaseDir) {
        return EnvUtil.prepareAbsoluteSourceDirPath(sourceDirectory, projectBaseDir, getDockerFile().getPath());
    }

    public File getAbsoluteDockerTarPath(String sourceDirectory, String projectBaseDir) {
        return EnvUtil.prepareAbsoluteSourceDirPath(sourceDirectory, projectBaseDir, getDockerArchive().getPath());
    }

    public String initAndValidate(KitLogger log) {
        if (entryPoint != null) {
            entryPoint.validate();
        }
        if (cmd != null) {
            cmd.validate();
        }
        if (healthCheck != null) {
            healthCheck.validate();
        }

        initDockerFileFile(log);

        if (healthCheck != null) {
            // HEALTHCHECK support added later
            return "1.24";
        } else if (args != null) {
            // ARG support came in later
            return "1.21";
        } else {
            return null;
        }
    }

    // Initialize the dockerfile location and the build mode
    private void initDockerFileFile(KitLogger log) {
        // can't have dockerFile/dockerFileDir and dockerArchive
        if ((dockerFile != null || dockerFileDir != null) && dockerArchive != null) {
            throw new IllegalArgumentException("Both <dockerFile> (<dockerFileDir>) and <dockerArchive> are set. " +
                    "Only one of them can be specified.");
        }
        dockerFileFile = findDockerFileFile(log);

        if (dockerArchive != null) {
            dockerArchiveFile = new File(dockerArchive);
        }
    }

    private File findDockerFileFile(KitLogger log) {
        if(dockerFileDir != null && contextDir != null) {
            log.warn("Both contextDir (%s) and deprecated dockerFileDir (%s) are configured. Using contextDir.", contextDir, dockerFileDir);
        }

        if (dockerFile != null) {
            File dFile = new File(dockerFile);
            if (dockerFileDir == null && contextDir == null) {
                return dFile;
            } else {
                if(contextDir != null) {
                    if (dFile.isAbsolute()) {
                        return dFile;
                    }
                    return new File(contextDir, dockerFile);
                }
                if (dFile.isAbsolute()) {
                    throw new IllegalArgumentException("<dockerFile> can not be absolute path if <dockerFileDir> also set.");
                }
                log.warn("dockerFileDir parameter is deprecated, please migrate to contextDir");
                return new File(dockerFileDir, dockerFile);
            }
        }


        if (contextDir != null) {
            return new File(contextDir, "Dockerfile");
        }

        if (dockerFileDir != null) {
            return new File(dockerFileDir, "Dockerfile");
        }

        // TODO: Remove the following deprecated handling section
        if (dockerArchive == null) {
            String deprecatedDockerFileDir =
                    getAssemblyConfiguration() != null ?
                            getAssemblyConfiguration().getDockerFileDir() :
                            null;
            if (deprecatedDockerFileDir != null) {
                log.warn("<dockerFileDir> in the <assembly> section of a <build> configuration is deprecated");
                log.warn("Please use <dockerFileDir> or <dockerFile> directly within the <build> configuration instead");
                return new File(deprecatedDockerFileDir,"Dockerfile");
            }
        }

        // No dockerfile mode
        return null;
    }

    public String validate() throws IllegalArgumentException {
        if (entryPoint != null) {
            entryPoint.validate();
        }
        if (cmd != null) {
            cmd.validate();
        }
        if (healthCheck != null) {
            healthCheck.validate();
        }

        // can't have dockerFile/dockerFileDir and dockerArchive
        if ((dockerFile != null || contextDir != null) && dockerArchive != null) {
            throw new IllegalArgumentException("Both <dockerFile> (<dockerFileDir>) and <dockerArchive> are set. " +
                                               "Only one of them can be specified.");
        }

        if (healthCheck != null) {
            // HEALTHCHECK support added later
            return "1.24";
        } else if (args != null) {
            // ARG support came in later
            return "1.21";
        } else {
            return null;
        }
    }

    public File calculateDockerFilePath() {
        if (dockerFile != null) {
            File dFile = new File(dockerFile);
            if (contextDir == null) {
                return dFile;
            }
            if (dFile.isAbsolute()) {
                return dFile;
            }
            if (System.getProperty("os.name").toLowerCase().contains("windows") &&
                !isValidWindowsFileName(dockerFile)) {
                throw new IllegalArgumentException(String.format("Invalid Windows file name %s for <dockerFile>", dockerFile));
            }
            return new File(contextDir, dFile.getPath());
        }

        if (contextDir != null) {
            return new File(contextDir, "Dockerfile");
        }

        // No dockerfile mode
        throw new IllegalArgumentException("Can't calculate a docker file path if neither dockerFile nor contextDir is specified");
    }

    public static class BuildConfigurationBuilder {
        public BuildConfigurationBuilder compressionString(String compressionString) {
            compression = Optional.ofNullable(compressionString).map(ArchiveCompression::valueOf).orElse(null);
            return this;
        }
    }

    private static List<String> removeEmptyEntries(List<String> list) {
        return Optional.ofNullable(list).orElse(Collections.emptyList()).stream()
                   .filter(Objects::nonNull)
                   .map(String::trim)
                   .filter(s -> !s.isEmpty())
                   .collect(Collectors.toList());
    }


   /**
     * Validate that the provided filename is a valid Windows filename.
     *
     * The validation of the Windows filename is copied from stackoverflow: https://stackoverflow.com/a/6804755
     *
     * @param filename the filename
     * @return filename is a valid Windows filename
     */
    static boolean isValidWindowsFileName(String filename) {
        Pattern pattern = Pattern.compile(
            "# Match a valid Windows filename (unspecified file system).          \n" +
            "^                                # Anchor to start of string.        \n" +
            "(?!                              # Assert filename is not: CON, PRN, \n" +
            "  (?:                            # AUX, NUL, COM1, COM2, COM3, COM4, \n" +
            "    CON|PRN|AUX|NUL|             # COM5, COM6, COM7, COM8, COM9,     \n" +
            "    COM[1-9]|LPT[1-9]            # LPT1, LPT2, LPT3, LPT4, LPT5,     \n" +
            "  )                              # LPT6, LPT7, LPT8, and LPT9...     \n" +
            "  (?:\\.[^.]*)?                  # followed by optional extension    \n" +
            "  $                              # and end of string                 \n" +
            ")                                # End negative lookahead assertion. \n" +
            "[^<>:\"/\\\\|?*\\x00-\\x1F]*     # Zero or more valid filename chars.\n" +
            "[^<>:\"/\\\\|?*\\x00-\\x1F .]    # Last char is not a space or dot.  \n" +
            "$                                # Anchor to end of string.            ",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.COMMENTS);
        Matcher matcher = pattern.matcher(filename);
        return matcher.matches();
    }

}
