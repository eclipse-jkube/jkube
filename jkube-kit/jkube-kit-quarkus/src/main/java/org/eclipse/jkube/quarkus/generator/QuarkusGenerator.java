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

import org.eclipse.jkube.kit.build.core.config.JkubeAssemblyConfiguration;
import org.eclipse.jkube.kit.build.core.config.JkubeBuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.JkubeAssemblyFileSet;
import org.eclipse.jkube.kit.common.JkubeProjectAssembly;
import org.eclipse.jkube.kit.common.util.FileUtil;
import org.eclipse.jkube.kit.common.util.JkubeProjectUtil;
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
        return shouldAddImageConfiguration(configs)
               && JkubeProjectUtil.hasPlugin(getProject(), "io.quarkus", "quarkus-maven-plugin");
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> existingConfigs, boolean prePackagePhase) {
        ImageConfiguration.Builder imageBuilder = new ImageConfiguration.Builder()
            .name(getImageName())
            .registry(getRegistry())
            .alias(getAlias())
            .buildConfig(createBuildConfig(prePackagePhase));

        existingConfigs.add(imageBuilder.build());
        return existingConfigs;
    }

    private JkubeBuildConfiguration createBuildConfig(boolean prePackagePhase) {
        final JkubeBuildConfiguration.Builder buildBuilder = new JkubeBuildConfiguration.Builder();
        // TODO: Check application.properties for a port
        buildBuilder.ports(Collections.singletonList(getConfig(Config.webPort)));
        addSchemaLabels(buildBuilder, log);

        boolean isNative = Boolean.parseBoolean(getConfig(Config.nativeImage, "false"));

        Optional<String> fromConfigured = Optional.ofNullable(getFromAsConfigured());
        if (isNative) {
            buildBuilder.from(fromConfigured.orElse("registry.fedoraproject.org/fedora-minimal"))
                        .entryPoint(new Arguments.Builder()
                                        .withParam("./" + findSingleFileThatEndsWith("-runner"))
                                        .withParam("-Dquarkus.http.host=0.0.0.0")
                                        .build())
                        .workdir("/");

            if (!prePackagePhase) {
                buildBuilder.assembly(createAssemblyConfiguration("/", getNativeFileToInclude()));
            }
        } else {
            buildBuilder.from(fromConfigured.orElse("openjdk:11"))
                        .entryPoint(new Arguments.Builder()
                                        .withParam("java")
                                        .withParam("-Dquarkus.http.host=0.0.0.0")
                                        .withParam("-jar")
                                        .withParam(findSingleFileThatEndsWith("-runner.jar"))
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

    interface FileSetCreator {
        JkubeProjectAssembly createFileSet();
    }

    private JkubeAssemblyConfiguration createAssemblyConfiguration(String targetDir, JkubeAssemblyFileSet jkubeProjectAssemblyFileSet) {
        final JkubeAssemblyConfiguration.Builder builder = new JkubeAssemblyConfiguration.Builder();
        builder.targetDir(targetDir);
        jkubeProjectAssemblyFileSet.setOutputDirectory(".");
        JkubeProjectAssembly jkubeProjectAssembly = new JkubeProjectAssembly.Builder()
                .fileSet(jkubeProjectAssemblyFileSet)
                .build();
        builder.assemblyDef(jkubeProjectAssembly);
        return builder.build();
    }

    private JkubeAssemblyFileSet getJvmFilesToInclude() {
        JkubeAssemblyFileSet fileSet = getFileSetWithFileFromBuildThatEndsWith("-runner.jar");
        fileSet.addInclude("lib/**");
        // We also need to exclude default jar file
        File defaultJarFile = JkubeProjectUtil.getFinalOutputArtifact(getContext().getProject());
        if (defaultJarFile != null) {
            fileSet.addExclude(defaultJarFile.getName());
        }
        fileSet.setFileMode("0640");
        return fileSet;
    }

    private JkubeAssemblyFileSet getNativeFileToInclude() {
        JkubeAssemblyFileSet fileSet = getFileSetWithFileFromBuildThatEndsWith("-runner");
        fileSet.setFileMode("0755");

        return fileSet;
    }

    private JkubeAssemblyFileSet getFileSetWithFileFromBuildThatEndsWith(String suffix) {
        List<String> relativePaths = new ArrayList<>();

        String fileToInclude = findSingleFileThatEndsWith(suffix);
        if (fileToInclude != null && !fileToInclude.isEmpty()) {
            relativePaths.add(fileToInclude);
        }
        return new JkubeAssemblyFileSet.Builder()
                .directory(FileUtil.getRelativePath(getProject().getBaseDirectory(), getBuildDir()).getPath())
                .includes(relativePaths)
                .fileMode("0777")
                .build();
    }

    private String findSingleFileThatEndsWith(String suffix) {
        File buildDir = getBuildDir();
        String[] file = buildDir.list((dir, name) -> name.endsWith(suffix));
        if (file == null || file.length != 1) {
            throw new IllegalStateException("Can't find single file with suffix '" + suffix + "' in " + buildDir + " (zero or more than one files found ending with '" + suffix + "')");
        }
        return file[0];
    }

    private File getBuildDir() {
        return new File(getProject().getBuildDirectory());
    }

}