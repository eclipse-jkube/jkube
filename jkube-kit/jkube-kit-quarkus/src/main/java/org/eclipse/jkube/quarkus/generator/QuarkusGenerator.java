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
package org.eclipse.jkube.quarkus.generator;

import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.support.BaseGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


public class QuarkusGenerator extends BaseGenerator {

    public QuarkusGenerator(GeneratorContext context) {
        super(context, "quarkus");
    }

    public enum Config implements Configs.Key {
        // Webport to expose. Set to 0 if no port should be exposed
        webPort {{
            d = "8080";
        }},

        // Whether to add native image or plain java image
        nativeImage {{
            d = "false";
        }};

        public String def() {
            return d;
        }

        protected String d;
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddGeneratedImageConfiguration(configs)
               && JKubeProjectUtil.hasPlugin(getProject(), "io.quarkus", "quarkus-maven-plugin");
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> existingConfigs, boolean prePackagePhase) {
        ImageConfiguration.ImageConfigurationBuilder imageBuilder = ImageConfiguration.builder()
            .name(getImageName())
            .registry(getRegistry())
            .alias(getAlias())
            .build(createBuildConfig(prePackagePhase));

        existingConfigs.add(imageBuilder.build());
        return existingConfigs;
    }

    private BuildConfiguration createBuildConfig(boolean prePackagePhase) {
        final BuildConfiguration.BuildConfigurationBuilder buildBuilder = BuildConfiguration.builder();
        // TODO: Check application.properties for a port
        buildBuilder.port(getConfig(Config.webPort));
        addSchemaLabels(buildBuilder, log);

        boolean isNative = Boolean.parseBoolean(getConfig(Config.nativeImage, "false"));

        Optional<String> fromConfigured = Optional.ofNullable(getFromAsConfigured());
        if (isNative) {
            buildBuilder.from(fromConfigured.orElse("registry.access.redhat.com/ubi8/ubi-minimal:8.1"))
                        .entryPoint(Arguments.builder()
                                        .execArgument("./" + findSingleFileThatEndsWith("-runner"))
                                        .execArgument("-Dquarkus.http.host=0.0.0.0")
                                        .build())
                        .workdir("/");

            if (!prePackagePhase) {
                buildBuilder.assembly(createAssemblyConfiguration("/", getNativeFileToInclude()));
            }
        } else {
            buildBuilder.from(fromConfigured.orElse("openjdk:11"))
                        .entryPoint(Arguments.builder()
                                        .execArgument("java")
                                        .execArgument("-Dquarkus.http.host=0.0.0.0")
                                        .execArgument("-jar")
                                        .execArgument(findSingleFileThatEndsWith("-runner.jar"))
                                        .build())
                        .workdir("/opt");

            if (!prePackagePhase) {
                buildBuilder.assembly(
                    createAssemblyConfiguration("/opt", getJvmFilesToInclude()));
            }
        }
        addLatestTagIfSnapshot(buildBuilder);
        return buildBuilder.build();
    }

    private AssemblyConfiguration createAssemblyConfiguration(String targetDir, AssemblyFileSet jKubeAssemblyFileSet) {
        jKubeAssemblyFileSet.setOutputDirectory(".");
        return AssemblyConfiguration.builder()
            .targetDir(targetDir)
            .inline(Assembly.builder().fileSet(jKubeAssemblyFileSet).build())
            .build();
    }

    private AssemblyFileSet getJvmFilesToInclude() {
        AssemblyFileSet.AssemblyFileSetBuilder fileSetBuilder =
            getFileSetWithFileFromBuildThatEndsWith("-runner.jar");
        fileSetBuilder.include("lib");
        // We also need to exclude default jar file
        File defaultJarFile = JKubeProjectUtil.getFinalOutputArtifact(getContext().getProject());
        if (defaultJarFile != null) {
            fileSetBuilder.exclude(defaultJarFile.getName());
        }
        fileSetBuilder.fileMode("0640");
        return fileSetBuilder.build();
    }

    private AssemblyFileSet getNativeFileToInclude() {
        return getFileSetWithFileFromBuildThatEndsWith("-runner")
            .fileMode("0755")
            .build();
    }

    private AssemblyFileSet.AssemblyFileSetBuilder getFileSetWithFileFromBuildThatEndsWith(String suffix) {
        List<String> relativePaths = new ArrayList<>();

        String fileToInclude = findSingleFileThatEndsWith(suffix);
        if (fileToInclude != null && !fileToInclude.isEmpty()) {
            relativePaths.add(fileToInclude);
        }
        return AssemblyFileSet.builder()
                .directory(FileUtil.getRelativePath(getProject().getBaseDirectory(), getProject().getBuildDirectory()))
                .includes(relativePaths)
                .fileMode("0777");
    }

    private String findSingleFileThatEndsWith(String suffix) {
        File buildDir = getProject().getBuildDirectory();
        String[] file = buildDir.list((dir, name) -> name.endsWith(suffix));
        if (file == null || file.length != 1) {
            throw new IllegalStateException("Can't find single file with suffix '" + suffix + "' in " + buildDir + " (zero or more than one files found ending with '" + suffix + "')");
        }
        return file[0];
    }

}