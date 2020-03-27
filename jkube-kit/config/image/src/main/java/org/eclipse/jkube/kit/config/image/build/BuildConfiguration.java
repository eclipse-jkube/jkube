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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.apache.commons.lang3.SerializationUtils;

/**
 * @author roland
 * @since 02.09.14
 */
public class BuildConfiguration<A extends AssemblyConfiguration> implements Serializable {
    public static final String DEFAULT_FILTER = "${*}";
    public static final String DEFAULT_CLEANUP = "try";

    /**
     * Directory used as the contexst directory, e.g. for a docker build.
     */
    private String contextDir;

    /**
     * Path to a dockerfile to use. Its parent directory is used as build context (i.e. as <code>dockerFileDir</code>).
     * Multiple different Dockerfiles can be specified that way. If set overwrites a possibly given
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
     * How interpolation of a dockerfile should be performed
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

    private List<String> ports;

    private Arguments shell;

    /**
     * Policy for pulling the base images
     */
    private String imagePullPolicy;

    /**
     * RUN Commands within Build/Image
     */
    private List<String> runCmds;

    private String cleanup;

    private Boolean nocache;

    private Boolean optimise;

    private List<String> volumes;

    private List<String> tags;

    private Map<String, String> env;

    private Map<String, String> labels;

    private Map<String, String> args;

    private Arguments entryPoint;

    private String workdir;

    private Arguments cmd;

    private String user;

    private HealthCheckConfiguration healthCheck;

    private A assembly;

    private Boolean skip;

    private ArchiveCompression compression = ArchiveCompression.none;

    private Map<String,String> buildOptions;

    /**
     * Directory holding an external Dockerfile which is used to build the
     * image. This Dockerfile will be enriched by the addition build configuration
     */
    @Deprecated
    private String dockerFileDir;

    // Path to Dockerfile to use, initialized lazily ....
    private File dockerFileFile, dockerArchiveFile;

    protected BuildConfiguration() {}

    public boolean isDockerFileMode() {
        return dockerFile != null || contextDir != null;
    }

    public File getDockerFile() {
        return dockerFileFile;
    }

    public File getDockerArchive() {
        return dockerArchiveFile;
    }

    public File getContextDir() {
        return contextDir != null ? new File(contextDir) : getDockerFile().getParentFile();
    }

    public String getFilter() {
        return filter;
    }

    public String getDockerFileRaw() {
        return dockerFile;
    }

    public String getContextDirRaw() {
        return contextDir;
    }

    public Arguments getShell() {
        return shell;
    }

    public String getDockerArchiveRaw() {
        return dockerArchive;
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

    public Map<String, String> getFromExt() {
        return fromExt;
    }

    public String getRegistry() {
        return registry;
    }

    public String getMaintainer() {
        return maintainer;
    }

    public String getWorkdir() {
        return workdir;
    }

    public A getAssemblyConfiguration() {
        return assembly;
    }

    public void setAssembly(A assembly) {
        this.assembly = assembly;
    }

    public List<String> getPorts() {
        return removeEmptyEntries(ports);
    }

    public String getImagePullPolicy() {
        return imagePullPolicy;
    }

    public List<String> getVolumes() {
        return removeEmptyEntries(volumes);
    }

    public List<String> getTags() {
        return removeEmptyEntries(tags);
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Arguments getCmd() {
        return cmd;
    }

    public String getCleanupMode() {
        return cleanup;
    }

    public Boolean getNoCache() {
        return nocache;
    }

    public Boolean getOptimise() {
        return optimise;
    }

    public Boolean getSkip() {
        return skip != null ? skip : false;
    }

    public ArchiveCompression getCompression() {
        return compression;
    }

    public Map<String, String> getBuildOptions() {
        return buildOptions;
    }

    public Arguments getEntryPoint() {
        return entryPoint;
    }

    public List<String> getRunCmds() {
        return removeEmptyEntries(runCmds);
    }

    public String getUser() {
      return user;
    }

    public HealthCheckConfiguration getHealthCheck() {
        return healthCheck;
    }

    public Map<String, String> getArgs() {
        return args;
    }

    public boolean optimise() {
        return optimise != null ? optimise : false;
    }

    public String getCleanup() {
        return cleanup;
    }

    public boolean nocache() {
        return nocache != null ? nocache : false;
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


    // ===========================================================================================
    public static class TypedBuilder<A extends AssemblyConfiguration, B extends BuildConfiguration<A>> {

        protected final BuildConfiguration<A> config;

        protected TypedBuilder(B config) {
            this.config = config;
        }

        public TypedBuilder<A, B> contextDir(String dir) {
            config.contextDir = dir;
            return this;
        }

        public TypedBuilder<A, B> dockerFile(String file) {
            config.dockerFile = file;
            return this;
        }

        public TypedBuilder<A, B> dockerArchive(String archive) {
            config.dockerArchive = archive;
            return this;
        }

        public TypedBuilder<A, B> dockerFileDir(String dir) {
            config.dockerFileDir = dir;
            return this;
        }

        public TypedBuilder<A, B> dockerFileFile(File dockerFile) {
            config.dockerFileFile = dockerFile;
            return this;
        }

        public TypedBuilder<A, B> filter(String filter) {
            config.filter = filter;
            return this;
        }

        public TypedBuilder<A, B> from(String from) {
            config.from = from;
            return this;
        }

        public TypedBuilder<A, B> fromExt(Map<String, String> fromExt) {
            config.fromExt = fromExt;
            return this;
        }

        public TypedBuilder<A, B> registry(String registry) {
            config.registry = registry;
            return this;
        }

        public TypedBuilder<A, B> maintainer(String maintainer) {
            config.maintainer = maintainer;
            return this;
        }

        public TypedBuilder<A, B> workdir(String workdir) {
            config.workdir = workdir;
            return this;
        }

        public TypedBuilder<A, B> assembly(A assembly) {
            config.assembly = assembly;
            return this;
        }

        public TypedBuilder<A, B> ports(List<String> ports) {
            config.ports = ports;
            return this;
        }

        public TypedBuilder<A, B> imagePullPolicy(String imagePullPolicy) {
            config.imagePullPolicy = imagePullPolicy;
            return this;
        }

        public TypedBuilder<A, B> runCmds(List<String> theCmds) {
            config.runCmds = theCmds;
            return this;
        }

        public TypedBuilder<A, B> volumes(List<String> volumes) {
            config.volumes = volumes;
            return this;
        }

        public TypedBuilder<A, B> tags(List<String> tags) {
            config.tags = tags;
            return this;
        }

        public TypedBuilder<A, B> env(Map<String, String> env) {
            config.env = env;
            return this;
        }

        public TypedBuilder<A, B> args(Map<String, String> args) {
            config.args = args;
            return this;
        }

        public TypedBuilder<A, B> labels(Map<String, String> labels) {
            config.labels = labels;
            return this;
        }

        public TypedBuilder<A, B> cmd(Arguments cmd) {
            if (cmd != null) {
                config.cmd = cmd;
            }
            return this;
        }

        public TypedBuilder<A, B> cleanup(String cleanup) {
            config.cleanup = cleanup;
            return this;
        }

        public TypedBuilder<A, B> compression(String compression) {
            if (compression == null) {
                config.compression = null;
            } else {
                config.compression = ArchiveCompression.valueOf(compression);
            }
            return this;
        }

        public TypedBuilder<A, B> nocache(Boolean nocache) {
            config.nocache = nocache;
            return this;
        }

        public TypedBuilder<A, B> optimise(Boolean optimise) {
            config.optimise = optimise;
            return this;
        }

        public TypedBuilder<A, B> entryPoint(Arguments entryPoint) {
            if (entryPoint != null) {
                config.entryPoint = entryPoint;
            }
            return this;
        }

        public TypedBuilder<A, B> user(String user) {
            config.user = user;
            return this;
        }

        public TypedBuilder<A, B> healthCheck(HealthCheckConfiguration healthCheck) {
            config.healthCheck = healthCheck;
            return this;
        }

        public TypedBuilder<A, B> skip(Boolean skip) {
            config.skip = skip;
            return this;
        }

        public TypedBuilder<A, B> buildOptions(Map<String,String> buildOptions) {
            config.buildOptions = buildOptions;
            return this;
        }

        public TypedBuilder<A, B> shell(Arguments shell) {
            if(shell != null) {
                config.shell = shell;
            }

            return this;
        }

        public B build() {
            return (B)config;
        }
    }

    public static class Builder extends TypedBuilder<AssemblyConfiguration, BuildConfiguration<AssemblyConfiguration>> {

        public Builder() {
            this(null);
        }

        public Builder(BuildConfiguration<AssemblyConfiguration> that) {
            super(that == null ? new BuildConfiguration<>() : SerializationUtils.clone(that));
        }
    }


    public String initAndValidate(KitLogger log) throws IllegalArgumentException {
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

    // ===============================================================================================================

    private List<String> removeEmptyEntries(List<String> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream()
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
    boolean isValidWindowsFileName(String filename) {
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
