/*
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
import org.eclipse.jkube.kit.common.BuildRecreateMode;
import org.eclipse.jkube.kit.common.Arguments;
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

  ///////////////////////////////////////////////////////////////////////////
  // Generic fields applicable to all build strategies
  ///////////////////////////////////////////////////////////////////////////
  /**
   * The base image which should be used for this image.
   *
   * <p> If not given this default to <code>busybox:latest</code> and is suitable for a pure data image.
   * <p>
   * This field is applicable for all build strategies.
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
   * <p>
   * This field is applicable for all build strategies.
   */
  private Map<String, String> fromExt;
  /**
   * The exposed ports which is a list of &lt;port&gt; elements, one for each port to expose.
   * Whitespace is trimmed from each element and empty elements are ignored.
   *
   * <p> The format can be either pure numerical (<code>8080</code>) or with the protocol attached (<code>8080/tcp</code>).
   * <p>
   * This field is applicable for all build strategies.
   */
  @Singular
  private List<String> ports;

  /**
   * Specific pull policy for the base image. This overrides any global image pull policy.
   * <p>
   * This field is applicable for all build strategies.
   */
  private String imagePullPolicy;

  /**
   * List of &lt;volume%gt; elements to create a container volume.
   * Whitespace is trimmed from each element and empty elements are ignored.
   * <p>
   * This field is applicable for all build strategies.
   */
  @Singular
  private List<String> volumes;

  /**
   * List of additional tag elements with which an image is to be tagged after the build.
   * Whitespace is trimmed from each element and empty elements are ignored.
   * <p>
   * This field is applicable for all build strategies.
   */
  @Singular
  private List<String> tags;

  @Singular
  private List<String> platforms;

  /**
   * Environment variables.
   * <p>
   * This field is applicable for all build strategies.
   */
  @Singular("putEnv")
  private Map<String, String> env;

  /**
   * Labels.
   * <p>
   * This field is applicable for all build strategies.
   */
  @Singular
  private Map<String, String> labels;

  /**
   * Directory to change to when starting the container.
   * <p>
   * This field is applicable for all build strategies.
   */
  private String workdir;

  /**
   * A command to execute by default.
   * <p>
   * This field is applicable for all build strategies.
   */
  private Arguments cmd;

  /**
   * User to which the image should switch to the end (corresponds to the <code>USER</code> Dockerfile directive).
   * <p>
   * This field is applicable for all build strategies.
   */
  private String user;

  /**
   * An entrypoint allows you to configure a container that will run as an executable.
   * <p>
   * This field is applicable for all build strategies.
   */
  private Arguments entryPoint;

  /**
   * Specifies the assembly configuration.
   * <p>
   * This field is applicable for all build strategies.
   */
  private AssemblyConfiguration assembly;

  /**
   * If set to true disables building of the image.
   * <p>
   * This field is applicable for all build strategies.
   */
  private Boolean skip;

  ///////////////////////////////////////////////////////////////////////////
  // Fields applicable to docker build strategy
  ///////////////////////////////////////////////////////////////////////////
  /**
   * Path to a directory used for the build's context. You can specify the Dockerfile to use with dockerFile, which by
   * default is the Dockerfile found in the contextDir.
   * The Dockerfile can be also located outside the contextDir, if provided with an absolute file path.
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private String contextDir;
  /**
   * Path to a Dockerfile which also triggers Dockerfile mode.
   * The Docker build context directory is set to <code>contextDir</code> if given.
   * If not the directory by default is the directory in which the Dockerfile is stored.
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private String dockerFile;
  /**
   * Path to a docker archive to load an image instead of building from scratch.
   * If a dockerArchive is provided, no {@link BuildConfiguration#dockerFile} must be given.
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private String dockerArchive;
  /**
   * Enable and set the delimiters for property replacements.
   *
   * <p>By default properties in the format <code>${..}</code> are replaced with Maven properties.
   * When using a single char like <code>@</code> then this is used as a delimiter (e.g @…​@).
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private String filter;
  /**
   * The author (MAINTAINER) field for the generated image
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private String maintainer;
  /**
   * Shell to be used for the {@link BuildConfiguration#runCmds}. It contains arg elements which are defining the
   * executable and its params.
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private Arguments shell;

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
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
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
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private String cleanup;
  /**
   * Don't use Docker’s build cache.
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private Boolean nocache;
  /**
   * If set to true then it will compress all the {@link BuildConfiguration#runCmds} into a single RUN directive so that
   * only one image layer is created.
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private Boolean optimise;
  /**
   * Map specifying the value of Docker build args which should be used when building the image with an external
   * Dockerfile which uses build arguments.
   *
   * <p> The key-value syntax is the same as when defining Maven properties (or labels or env). This argument is
   * ignored when no external Dockerfile is used.
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  @Singular
  private Map<String, String> args;
  /**
   * Health check configuration.
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private HealthCheckConfiguration healthCheck;
  /**
   * The compression mode how the build archive is transmitted to the docker daemon and how docker build archives are
   * attached to this build as sources.
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private ArchiveCompression compression;
  /**
   * Map specifying the build options to provide to the docker daemon when building the image.
   *
   * <p> These options map to the ones listed as query parameters in the Docker Remote API and are restricted to
   * simple options (e.g.: memory, shmsize).
   *
   * @see <a href="https://docs.docker.com/engine/api/v1.41/#operation/ImageBuild">Docker Engine API v1.40</a>
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private Map<String, String> buildOptions;
  /**
   * Map specifying the create image options to provide to the docker daemon when pulling or importing an image.
   *
   * <p> These options map to the ones listed as query parameters in the Docker Remote API and are restricted to
   * simple options (e.g.: fromImage, fromSrc, platform).
   *
   * @see <a href="https://docs.docker.com/engine/api/v1.41/#operation/ImageCreate">Docker Engine API v1.41</a>
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  private Map<String, String> createImageOptions;
  /**
   * Path to Dockerfile to use, initialized lazily.
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  @Setter(AccessLevel.PRIVATE)
  private File dockerFileFile;
  /**
   * Path to archive file passed to docker daemon.
   *
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  @Setter(AccessLevel.PRIVATE)
  private File dockerArchiveFile;

  /**
   * Array of images used for build cache resolution.
   * <p>
   * This field is applicable only for <code>docker</code> build strategy
   */
  @Singular("addCacheFrom")
  private List<String> cacheFrom;

  ///////////////////////////////////////////////////////////////////////////
  // Fields applicable to buildpacks build strategy
  ///////////////////////////////////////////////////////////////////////////
  /**
   * Configure BuildPack <a href="https://buildpacks.io/docs/for-platform-operators/concepts/builder/">builder</a> OCI image for BuildPack Build.
   * <p>
   * This field is applicable for only <code>buildpacks</code> build strategy.
   */
  private String buildpacksBuilderImage;

  ///////////////////////////////////////////////////////////////////////////
  // Fields applicable to openshift build strategy
  ///////////////////////////////////////////////////////////////////////////
  /**
   * While creating a BuildConfig, By default, if the builder image specified in the
   * build configuration is available locally on the node, that image will be used.
   * <p>
   * ForcePull to override the local image and refresh it from the registry to which the image stream points.
   * <p>
   * This field is applicable in case of <code>s2i</code> build strategy
   */
  private boolean openshiftForcePull;

  /**
   * The S2I binary builder BuildConfig name suffix appended to the image name to avoid
   * clashing with the underlying BuildConfig for the Jenkins pipeline
   * <p>
   * This field is applicable in case of OpenShift <code>s2i</code> build strategy
   */
  private String openshiftS2iBuildNameSuffix;

  /**
   * Allow the ImageStream used in the S2I binary build to be used in standard
   * Kubernetes resources such as Deployment or StatefulSet.
   * <p>
   * This field is only applicable in case of OpenShift <code>s2i</code> build strategy
   */
  private boolean openshiftS2iImageStreamLookupPolicyLocal;

  /**
   * The name of pullSecret to be used to pull the base image in case pulling from a protected
   * registry which requires authentication.
   * <p>
   * This field is applicable in case of OpenShift <code>s2i</code> build strategy
   */
  private String openshiftPullSecret;

  /**
   * The name of pushSecret to be used to push the final image in case pushing from a protected
   * registry which requires authentication.
   * <p>
   * This field is applicable in case of OpenShift <code>s2i</code> build strategy
   */
  private String openshiftPushSecret;

  /**
   * Allow specifying in which registry to push the container image at the end of the build.
   * If the output kind is ImageStreamTag, then the image will be pushed to the internal OpenShift registry.
   * If the output is of type DockerImage, then the name of the output reference will be used as a Docker push specification.
   * <p>
   * This field is applicable in case of OpenShift <code>s2i</code> build strategy
   */
  private String openshiftBuildOutputKind;

  /**
   * How the OpenShift resource objects associated with the build should be treated when they already exist

   * <code>buildConfig</code> or <code>bc</code> : Only the BuildConfig is recreated
   * <code>all</code> or <code>is</code> : Only the ImageStream is recreated
   * <code>all</code> : Both, BuildConfig and ImageStream are recreated
   * <code>none</code> : Neither BuildConfig nor ImageStream is recreated
   * The default is <code>none</code>. If you provide the property without value then <code>all</code> is assumed, so everything gets recreated.
   *
   * <p>
   * This field is applicable in case of OpenShift <code>s2i</code> build strategy
   */
  private BuildRecreateMode openshiftBuildRecreateMode;

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
