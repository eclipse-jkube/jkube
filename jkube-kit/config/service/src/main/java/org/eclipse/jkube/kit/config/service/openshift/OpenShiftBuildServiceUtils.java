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
package org.eclipse.jkube.kit.config.service.openshift;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigSpec;
import io.fabric8.openshift.api.model.BuildOutput;
import io.fabric8.openshift.api.model.BuildOutputBuilder;
import io.fabric8.openshift.api.model.BuildStrategy;
import io.fabric8.openshift.api.model.BuildStrategyBuilder;
import io.fabric8.openshift.api.model.ImageStreamTag;
import io.fabric8.openshift.api.model.ImageStreamTagBuilder;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.build.api.assembly.ArchiverCustomizer;
import org.eclipse.jkube.kit.common.util.IoUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.eclipse.jkube.kit.build.api.helper.BuildUtil.extractBaseFromDockerfile;
import static org.eclipse.jkube.kit.config.service.openshift.ImageStreamService.resolveImageStreamName;
import static org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService.DEFAULT_BUILD_OUTPUT_KIND;
import static org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService.DEFAULT_S2I_BUILD_SUFFIX;
import static org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService.DOCKER_IMAGE;
import static org.eclipse.jkube.kit.config.service.openshift.OpenshiftBuildService.IMAGE_STREAM_TAG;

public class OpenShiftBuildServiceUtils {

  private OpenShiftBuildServiceUtils() {}

  protected static File createBuildArchive(JKubeServiceHub jKubeServiceHub, ImageConfiguration imageConfig) throws JKubeServiceException {
    // Adding S2I artifacts such as environment variables in S2I mode
    final ArchiverCustomizer customizer = createS2IArchiveCustomizer(jKubeServiceHub.getBuildServiceConfig(), imageConfig);
    try {
      return jKubeServiceHub.getDockerServiceHub().getArchiveService()
          .createDockerBuildArchive(imageConfig, jKubeServiceHub.getConfiguration(), customizer);
    } catch (IOException e) {
      throw new JKubeServiceException("Unable to create the build archive", e);
    }
  }

  /**
   * Returns the applicable name for the S2I Build resource considering the provided {@link ImageName} and
   * {@link BuildServiceConfig}.
   */
  static String computeS2IBuildName(BuildServiceConfig config, ImageName imageName) {
    final StringBuilder s2IBuildName = new StringBuilder(resolveImageStreamName(imageName));
    if (!StringUtils.isEmpty(config.getS2iBuildNameSuffix())) {
      s2IBuildName.append(config.getS2iBuildNameSuffix());
    } else if (config.getJKubeBuildStrategy() == JKubeBuildStrategy.s2i) {
      s2IBuildName.append(DEFAULT_S2I_BUILD_SUFFIX);
    }
    return s2IBuildName.toString();
  }

  private static ArchiverCustomizer createS2IArchiveCustomizer(
      BuildServiceConfig buildServiceConfig, ImageConfiguration imageConfiguration) throws JKubeServiceException {
    try {
      if (imageConfiguration.getBuildConfiguration() != null && imageConfiguration.getBuildConfiguration().getEnv() != null) {
        String fileName = IoUtil.sanitizeFileName("s2i-env-" + imageConfiguration.getName());
        final File environmentFile = new File(buildServiceConfig.getBuildDirectory(), fileName);

        try (PrintWriter out = new PrintWriter(new FileWriter(environmentFile))) {
          for (Map.Entry<String, String> e : imageConfiguration.getBuildConfiguration().getEnv().entrySet()) {
            out.println(e.getKey() + "=" + e.getValue());
          }
        }

        return tarArchiver -> {
          tarArchiver.includeFile(environmentFile, ".s2i/environment");
          return tarArchiver;
        };
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new JKubeServiceException("Unable to add environment variables to the S2I build archive", e);
    }
  }

  protected static BuildStrategy createBuildStrategy(
      JKubeServiceHub jKubeServiceHub, ImageConfiguration imageConfig, String openshiftPullSecret) {
    final BuildServiceConfig config = jKubeServiceHub.getBuildServiceConfig();
    final JKubeBuildStrategy osBuildStrategy = config.getJKubeBuildStrategy();
    final BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();
    final Map<String, String> fromExt = buildConfig.getFromExt();
    final String fromName;
    if (buildConfig.isDockerFileMode()) {
      fromName = extractBaseFromDockerfile(jKubeServiceHub.getConfiguration(), buildConfig);
    } else {
      fromName = getMapValueWithDefault(fromExt, JKubeBuildStrategy.SourceStrategy.name, buildConfig.getFrom());
    }
    final String fromKind = getMapValueWithDefault(fromExt, JKubeBuildStrategy.SourceStrategy.kind, DOCKER_IMAGE);
    final String fromNamespace = getMapValueWithDefault(fromExt, JKubeBuildStrategy.SourceStrategy.namespace,
        IMAGE_STREAM_TAG.equals(fromKind) ? "openshift" : null);
    if (osBuildStrategy == JKubeBuildStrategy.docker) {
      BuildStrategy buildStrategy = new BuildStrategyBuilder()
          .withType("Docker")
          .withNewDockerStrategy()
            .withNewFrom()
              .withKind(fromKind)
              .withName(fromName)
              .withNamespace(StringUtils.isEmpty(fromNamespace) ? null : fromNamespace)
            .endFrom()
            .withEnv(checkForEnv(imageConfig))
            .withNoCache(checkForNocache(imageConfig))
          .endDockerStrategy().build();
      if (openshiftPullSecret != null) {
        buildStrategy.getDockerStrategy().setPullSecret(new LocalObjectReferenceBuilder()
            .withName(openshiftPullSecret)
            .build());
      }
      return buildStrategy;
    } else if (osBuildStrategy == JKubeBuildStrategy.s2i) {
      BuildStrategy buildStrategy = new BuildStrategyBuilder()
          .withType("Source")
          .withNewSourceStrategy()
            .withNewFrom()
              .withKind(fromKind)
              .withName(fromName)
              .withNamespace(StringUtils.isEmpty(fromNamespace) ? null : fromNamespace)
            .endFrom()
            .withForcePull(config.isForcePull())
          .endSourceStrategy()
          .build();
      if (openshiftPullSecret != null) {
        buildStrategy.getSourceStrategy().setPullSecret(new LocalObjectReferenceBuilder()
            .withName(openshiftPullSecret)
            .build());
      }
      return buildStrategy;
    } else {
      throw new IllegalArgumentException("Unsupported BuildStrategy " + osBuildStrategy);
    }
  }

  protected static BuildOutput createBuildOutput(BuildServiceConfig config, ImageName imageName) {
    final String buildOutputKind = Optional.ofNullable(config.getBuildOutputKind()).orElse(DEFAULT_BUILD_OUTPUT_KIND);
    final String outputImageStreamTag = resolveImageStreamName(imageName) + ":" + (imageName.getTag() != null ? imageName.getTag() : "latest");
    final BuildOutputBuilder buildOutputBuilder = new BuildOutputBuilder();
    buildOutputBuilder.withNewTo().withKind(buildOutputKind).withName(outputImageStreamTag).endTo();
    if (DOCKER_IMAGE.equals(buildOutputKind)) {
      buildOutputBuilder.editTo().withName(imageName.getFullName()).endTo();
    }
    if(StringUtils.isNotBlank(config.getOpenshiftPushSecret())) {
      buildOutputBuilder.withNewPushSecret().withName(config.getOpenshiftPushSecret()).endPushSecret();
    }
    return buildOutputBuilder.build();
  }

  protected static BuildConfigSpec getBuildConfigSpec(BuildConfig buildConfig) {
    BuildConfigSpec spec = buildConfig.getSpec();
    if (spec == null) {
      spec = new BuildConfigSpec();
      buildConfig.setSpec(spec);
    }
    return spec;
  }

  protected static List<ImageStreamTag> createAdditionalTagsIfPresent(ImageConfiguration imageConfiguration, String namespace, ImageStreamTag imageStreamTag) {
    List<ImageStreamTag> imageStreamTags = new ArrayList<>();
    ImageName imageName = new ImageName(imageConfiguration.getName());
    for (String tag : getAdditionalTagsToCreate(imageConfiguration)) {
      imageStreamTags.add(createNewImageStreamTag(resolveImageStreamName(imageName) + ":" + tag, namespace, imageStreamTag));
    }

    return imageStreamTags;
  }

  protected static List<String> getAdditionalTagsToCreate(ImageConfiguration imageConfiguration) {
    if (imageConfiguration != null &&
        imageConfiguration.getBuildConfiguration() != null &&
        imageConfiguration.getBuildConfiguration().getTags() != null) {
      ImageName imageName = new ImageName(imageConfiguration.getName());
      return imageConfiguration.getBuildConfiguration().getTags().stream()
          .filter(t -> !t.equals(imageName.getTag()))
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  protected static ImageStreamTag createNewImageStreamTag(String name, String namespace, ImageStreamTag orignalImageStreamTag) {
    return new ImageStreamTagBuilder()
        .withNewMetadata()
        .withLabels(orignalImageStreamTag.getMetadata().getLabels())
        .withAnnotations(orignalImageStreamTag.getMetadata().getAnnotations())
        .withName(name)
        .withNamespace(namespace)
        .endMetadata()
        .withNewTag()
        .withNewFrom()
        .withKind(DOCKER_IMAGE)
        .withName(orignalImageStreamTag.getImage().getDockerImageReference())
        .endFrom()
        .endTag()
        .withGeneration(0L)
        .build();
  }

  private static String getMapValueWithDefault(Map<String, String> map, JKubeBuildStrategy.SourceStrategy strategy, String defaultValue) {
    return getMapValueWithDefault(map, strategy.key(), defaultValue);
  }

  private static String getMapValueWithDefault(Map<String, String> map, String field, String defaultValue) {
    if (map == null) {
      return defaultValue;
    }
    String value = map.get(field);
    return value != null ? value : defaultValue;
  }

  private static boolean checkForNocache(ImageConfiguration imageConfig) {
    String nocache = System.getProperty("docker.nocache");
    if (nocache != null) {
      return nocache.length() == 0 || Boolean.parseBoolean(nocache);
    } else {
      BuildConfiguration buildConfig = imageConfig.getBuildConfiguration();
      return buildConfig.nocache();
    }
  }

  private static List<EnvVar> checkForEnv(ImageConfiguration imageConfiguration) {
    BuildConfiguration buildImageConfiguration = imageConfiguration.getBuildConfiguration();
    if (buildImageConfiguration.getArgs() != null) {
      return KubernetesHelper.convertToEnvVarList(buildImageConfiguration.getArgs());
    }
    return Collections.emptyList();
  }
}
