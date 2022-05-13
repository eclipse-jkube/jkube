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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.archive.ArchiveCompression;
import org.eclipse.jkube.kit.common.util.EnvUtil;

import javax.annotation.Nonnull;

import static org.eclipse.jkube.kit.common.util.EnvUtil.isWindows;

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
   * Path to a directory used for the build's context. You can specify the Dockerfile to use with dockerFile, which by
   * default is the Dockerfile found in the contextDir.
   * The Dockerfile can be also located outside of the contextDir, if provided with an absolute file path.
   */
  private String contextDir;
  /**
   * Path to a Dockerfile which also triggers Dockerfile mode.
   * The Docker build context directory is set to <code>contextDir</code> if given.
   * If not the directory by default is the directory in which the Dockerfile is stored.
   */
  private String dockerFile;
  /**
   * Path to a docker archive to load an image instead of building from scratch.
   * If a dockerArchive is provided, no {@link BuildConfiguration#dockerFile} must be given.
   */
  private String dockerArchive;
  /**
   * Enable and set the delimiters for property replacements.
   *
   * <p>By default properties in the format <code>${..}</code> are replaced with Maven properties.
   * When using a single char like <code>@</code> then this is used as a delimiter (e.g @…​@).
   */
  private String filter;
  /**
   * The base image which should be used for this image.
   *
   * <p> If not given this default to <code>busybox:latest</code> and is suitable for a pure data image.
   */
  private String from;
  /**
   * Extended definition for a base image. This field holds a map of defined in key:value format.
   * <p> The known keys are:
   * <ul>
   *   <li><b>name</b>: Name of the base image</li>
   * </ul>
   * <p> A provided {@link BuildConfiguration#from} takes precedence over the name given here.
   * This tag is useful for extensions of this plugin.
   */
  private Map<String, String> fromExt;
  /**
   * The author (MAINTAINER) field for the generated image
   */
  private String maintainer;
  /**
   * The exposed ports which is a list of &lt;port&gt; elements, one for each port to expose.
   * Whitespace is trimmed from each element and empty elements are ignored.
   *
   * <p> The format can be either pure numerical (<code>8080</code>) or with the protocol attached (<code>8080/tcp</code>).
   */
  @Singular
  private List<String> ports;
  /**
   * Shell to be used for the {@link BuildConfiguration#runCmds}. It contains arg elements which are defining the
   * executable and its params.
   */
  private Arguments shell;
  /**
   * Specific pull policy for the base image. This overrides any global image pull policy.
   */
  private String imagePullPolicy;
  /**
   * Commands to be run during the build process.
   *
   * <p> It contains &lt;run&gt; elements which are passed to the shell.
   * Whitespace is trimmed from each element and empty elements are ignored.
   *
   * <p> The run commands are inserted right after the assembly and after {@link BuildConfiguration#workdir} into the
   * Dockerfile.
   *
   * <p> This setting is not to be confused with the &lt;run&gt; section for this image which specifies the runtime
   * behaviour when starting containers.
   */
  @Singular
  private List<String> runCmds;
  /**
   * Cleanup dangling (untagged) images after each build (including any containers created from them)
   * <ul>
   *   <li><b>try</b>: tries to remove the old image but doesn't fail the build if this is not possible</li>
   *   <li><b>remove</b>: removes old image or fails if it doesn't</li>
   *   <li><b>none</b>: No cleanup is requested</li>
   * </ul>
   */
  private String cleanup;
  /**
   * Don't use Docker’s build cache.
   */
  private Boolean nocache;
  /**
   * If set to true then it will compress all the {@link BuildConfiguration#runCmds} into a single RUN directive so that
   * only one image layer is created.
   */
  private Boolean optimise;
  /**
   * List of &lt;volume%gt; elements to create a container volume.
   * Whitespace is trimmed from each element and empty elements are ignored.
   */
  @Singular
  private List<String> volumes;
  /**
   * List of additional tag elements with which an image is to be tagged after the build.
   * Whitespace is trimmed from each element and empty elements are ignored.
   */
  @Singular
  private List<String> tags;
  /**
   * Environment variables.
   */
  @Singular("putEnv")
  private Map<String, String> env;
  /**
   * Labels.
   */
  @Singular
  private Map<String, String> labels;
  /**
   * Map specifying the value of Docker build args which should be used when building the image with an external
   * Dockerfile which uses build arguments.
   *
   * <p> The key-value syntax is the same as when defining Maven properties (or labels or env). This argument is
   * ignored when no external Dockerfile is used.
   */
  @Singular
  private Map<String, String> args;
  /**
   * An entrypoint allows you to configure a container that will run as an executable.
   */
  private Arguments entryPoint;
  /**
   * Directory to change to when starting the container.
   */
  private String workdir;
  /**
   * A command to execute by default.
   */
  private Arguments cmd;
  /**
   * User to which the Dockerfile should switch to the end (corresponds to the <code>USER</code> Dockerfile directive).
   */
  private String user;
  /**
   * Health check configuration.
   */
  private HealthCheckConfiguration healthCheck;
  /**
   * Specifies the assembly configuration.
   */
  private AssemblyConfiguration assembly;
  /**
   * If set to true disables building of the image.
   */
  private Boolean skip;
  /**
   * The compression mode how the build archive is transmitted to the docker daemon and how docker build archives are
   * attached to this build as sources.
   */
  private ArchiveCompression compression;
  /**
   * Map specifying the build options to provide to the docker daemon when building the image.
   *
   * <p> These options map to the ones listed as query parameters in the Docker Remote API and are restricted to
   * simple options (e.g.: memory, shmsize).
   *
   * @see <a href="https://docs.docker.com/engine/api/v1.41/#operation/ImageBuild">Docker Engine API v1.40</a>
   */
  private Map<String, String> buildOptions;
  /**
   * Map specifying the create image options to provide to the docker daemon when pulling or importing an image.
   *
   * <p> These options map to the ones listed as query parameters in the Docker Remote API and are restricted to
   * simple options (e.g.: fromImage, fromSrc, platform).
   *
   * @see <a href="https://docs.docker.com/engine/api/v1.41/#operation/ImageCreate">Docker Engine API v1.41</a>
   */
  private Map<String, String> createImageOptions;
  /**
   * Path to Dockerfile to use, initialized lazily.
   */
  @Setter(AccessLevel.PRIVATE)
  private File dockerFileFile;
  @Setter(AccessLevel.PRIVATE)
  private File dockerArchiveFile;

  @Singular("addCacheFrom")
  private List<String> cacheFrom;

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

  @Nonnull
  public File getContextDir() {
    if (contextDir != null) {
      return new File(contextDir);
    }
    return Optional.ofNullable(getDockerFile()).map(File::getParentFile).orElse(new File("."));
  }

  public String getContextDirRaw() {
    return contextDir;
  }

  public String getFrom() {
    if (from == null && getFromExt() != null) {
      return getFromExt().get("name");
    }
    return from;
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

  public String initAndValidate() {
    if (entryPoint != null) {
      entryPoint.validate();
    }
    if (cmd != null) {
      cmd.validate();
    }
    if (healthCheck != null) {
      healthCheck.validate();
    }

    initDockerFileFile();

    if (healthCheck != null) {
      // HEALTHCHECK support added later
      return "1.24";
    } else if (args != null && !args.isEmpty()) {
      // ARG support came in later
      return "1.21";
    } else {
      return null;
    }
  }

  // Initialize the dockerfile location and the build mode
  private void initDockerFileFile() {
    if (dockerFile != null && dockerArchive != null) {
      throw new IllegalArgumentException("Both <dockerFile> and <dockerArchive> are set. " +
          "Only one of them can be specified.");
    }
    dockerFileFile = findDockerFileFile();

    if (dockerArchive != null) {
      dockerArchiveFile = new File(dockerArchive);
    }
  }

  private File findDockerFileFile() {
    if (dockerFile != null) {
      File dFile = new File(dockerFile);
      if (contextDir == null) {
        return dFile;
      } else {
        if (dFile.isAbsolute()) {
          return dFile;
        }
        return new File(contextDir, dockerFile);
      }
    }

    if (contextDir != null) {
      return new File(contextDir, "Dockerfile");
    }

    // No dockerfile mode
    return null;
  }

  public String validate() {
    if (entryPoint != null) {
      entryPoint.validate();
    }
    if (cmd != null) {
      cmd.validate();
    }
    if (healthCheck != null) {
      healthCheck.validate();
    }

    if ((dockerFile != null || contextDir != null) && dockerArchive != null) {
      throw new IllegalArgumentException("Both <dockerFile> (<contextDir>) and <dockerArchive> are set. " +
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

  @Nonnull
  public File calculateDockerFilePath() {
    if (dockerFile != null) {
      File dFile = new File(dockerFile);
      if (contextDir == null) {
        return dFile;
      }
      if (dFile.isAbsolute()) {
        return dFile;
      }
      if (isWindows() && !isValidWindowsFileName(dockerFile)) {
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
