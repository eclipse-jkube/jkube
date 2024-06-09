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
package org.eclipse.jkube.kit.service.buildpacks.controller;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.service.buildpacks.BuildPackBuildOptions;
import org.eclipse.jkube.kit.service.buildpacks.BuildPackCommand;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class BuildPackCliController implements BuildPackController {
  private final File pack;
  private final KitLogger kitLogger;

  public BuildPackCliController(File binaryFile, KitLogger kitLogger) {
    this.pack = binaryFile;
    this.kitLogger = kitLogger;
  }

  @Override
  public void build(BuildPackBuildOptions buildOptions) {
    BuildPackCommand buildPackCommand = new BuildPackCommand(kitLogger, pack,
        createBuildCommandArguments(buildOptions),
        l -> kitLogger.info("[[s]]%s", l));
    try {
      buildPackCommand.execute();
    } catch (IOException e) {
      throw new IllegalStateException("Process Existed With : " + buildPackCommand.getExitCode() + " [" + e.getMessage() + "]", e);
    }
  }

  @Override
  public String version() {
    AtomicReference<String> versionRef = new AtomicReference<>();
    BuildPackCommand versionCommand = new BuildPackCommand(kitLogger, pack, Collections.singletonList("--version"), versionRef::set);
    try {
      versionCommand.execute();
    } catch (IOException e) {
      kitLogger.warn(e.getMessage());
    }
    if (StringUtils.isNotBlank(versionRef.get())) {
      return versionRef.get();
    }
    return null;
  }

  private List<String> createBuildCommandArguments(BuildPackBuildOptions buildOptions) {
    List<String> buildArgs = new ArrayList<>();
    buildArgs.add("build");
    buildArgs.add(buildOptions.getImageName());
    buildArgs.addAll(Arrays.asList("--builder", buildOptions.getBuilderImage()));
    buildArgs.addAll(Arrays.asList("--creation-time", buildOptions.getCreationTime()));
    buildArgs.addAll(extractStringArg("--pull-policy", buildOptions.getImagePullPolicy()));
    buildArgs.addAll(extractRepeatedArgsForListElements("--volume", buildOptions.getVolumes()));
    if (buildOptions.getTags() != null && !buildOptions.getTags().isEmpty()) {
      ImageName specifiedImageName = new ImageName(buildOptions.getImageName());
      List<String> imageNameWithAdditionalTags = buildOptions.getTags().stream()
          .map(t -> specifiedImageName.getNameWithoutTag() + ":" + t)
          .collect(Collectors.toList());
      buildArgs.addAll(extractRepeatedArgsForListElements("--tag", imageNameWithAdditionalTags));
    }
    if (buildOptions.getEnv() != null) {
      List<String> keyValueEntryList = buildOptions.getEnv().entrySet().stream()
          .map(this::getSingleEnvArg)
          .collect(Collectors.toList());
      buildArgs.addAll(extractRepeatedArgsForListElements("--env", keyValueEntryList));
    }
    if (buildOptions.isClearCache()) {
      buildArgs.add("--clear-cache");
    }
    buildArgs.addAll(Arrays.asList("--path", buildOptions.getPath()));
    return buildArgs;
  }

  private List<String> extractRepeatedArgsForListElements(String flag, List<String> flagValues) {
    List<String> args = new ArrayList<>();
    if (flagValues != null && !flagValues.isEmpty()) {
      flagValues.forEach(v -> args.addAll(extractStringArg(flag, v)));
    }
    return args;
  }

  private List<String> extractStringArg(String flag, String flagValue) {
    List<String> args = new ArrayList<>();
    if (StringUtils.isNotBlank(flagValue)) {
      args.add(flag);
      args.add(flagValue);
    }
    return args;
  }

  private String getSingleEnvArg(Map.Entry<String, String> e) {
    String entryKeyValue = e.getKey();
    if (StringUtils.isNotBlank(e.getValue())) {
      entryKeyValue = entryKeyValue + "=" + e.getValue();
    }
    return entryKeyValue;
  }
}
