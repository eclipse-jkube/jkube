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
package io.jkube.quarkus.generator;

import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.common.Configs;
import io.jkube.kit.common.util.FileUtil;
import io.jkube.kit.common.util.MavenUtil;
import io.jkube.kit.config.image.build.Arguments;
import io.jkube.kit.config.image.build.AssemblyConfiguration;
import io.jkube.kit.config.image.build.BuildConfiguration;
import io.jkube.generator.api.GeneratorContext;
import io.jkube.generator.api.support.BaseGenerator;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileSet;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static io.jkube.kit.config.image.build.util.BuildLabelUtil.addSchemaLabels;


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
               && MavenUtil.hasPlugin(getProject(), "io.quarkus", "quarkus-maven-plugin");
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> existingConfigs, boolean prePackagePhase) throws MojoExecutionException {
        ImageConfiguration.Builder imageBuilder = new ImageConfiguration.Builder()
            .name(getImageName())
            .registry(getRegistry())
            .alias(getAlias())
            .buildConfig(createBuildConfig(prePackagePhase));

        existingConfigs.add(imageBuilder.build());
        return existingConfigs;
    }

    private BuildConfiguration createBuildConfig(boolean prePackagePhase) throws MojoExecutionException {
        BuildConfiguration.Builder buildBuilder =
            new BuildConfiguration.Builder()
                // TODO: Check application.properties for a port
                .ports(Collections.singletonList(getConfig(Config.webPort)));
        addSchemaLabels(buildBuilder, getContext().getProject(), log);

        boolean isNative = Boolean.parseBoolean(getConfig(Config.nativeImage, "false"));

        if (isNative) {
            buildBuilder.from("registry.fedoraproject.org/fedora-minimal")
                        .entryPoint(new Arguments.Builder()
                                        .withParam("./" + findSingleFileThatEndsWith("-runner"))
                                        .withParam("-Dquarkus.http.host=0.0.0.0")
                                        .build())
                        .workdir("/");

            if (!prePackagePhase) {
                buildBuilder.assembly(
                    createAssemblyConfiguration(
                        "/", this::getNativeFileToInclude));
            }
        } else {
            buildBuilder.from("openjdk:11")
                        .entryPoint(new Arguments.Builder()
                                        .withParam("java")
                                        .withParam("-Dquarkus.http.host=0.0.0.0")
                                        .withParam("-jar")
                                        .withParam(findSingleFileThatEndsWith("-runner.jar"))
                                        .build())
                        .workdir("/opt");

            if (!prePackagePhase) {
                buildBuilder.assembly(
                    createAssemblyConfiguration("/opt", this::getJvmFilesToInclude));
            }
        }
        addLatestTagIfSnapshot(buildBuilder);
        return buildBuilder.build();
    }

    interface FileSetCreator {
        FileSet createFileSet() throws MojoExecutionException;
    }

    private AssemblyConfiguration createAssemblyConfiguration(String targetDir, FileSetCreator fsCreator) throws MojoExecutionException {
        AssemblyConfiguration.Builder builder =
            new AssemblyConfiguration.Builder().targetDir(targetDir);
        Assembly assembly = new Assembly();
        FileSet fileSet = fsCreator.createFileSet();
        fileSet.setOutputDirectory(".");
        assembly.addFileSet(fileSet);
        builder.assemblyDef(assembly);
        return builder.build();

    }

    private FileSet getJvmFilesToInclude() throws MojoExecutionException {
        FileSet fileSet = getFileSetWithFileFromBuildThatEndsWith("-runner.jar");
        fileSet.addInclude("lib/**");
        fileSet.setFileMode("0640");
        return fileSet;
    }

    private FileSet getNativeFileToInclude() throws MojoExecutionException {
        FileSet fileSet = getFileSetWithFileFromBuildThatEndsWith("-runner");
        fileSet.setFileMode("0755");
        return fileSet;
    }

    private FileSet getFileSetWithFileFromBuildThatEndsWith(String suffix) throws MojoExecutionException {
        FileSet fileSet = new FileSet();
        fileSet.setDirectory(
            FileUtil.getRelativePath(getProject().getBasedir(), getBuildDir()).getPath());
        fileSet.addInclude(findSingleFileThatEndsWith(suffix));
        return fileSet;
    }

    private String findSingleFileThatEndsWith(String suffix) throws MojoExecutionException {
        File buildDir = getBuildDir();
        String[] file = buildDir.list((dir, name) -> name.endsWith(suffix));
        if (file == null || file.length != 1) {
            throw new MojoExecutionException("Can't find single file with suffix '" + suffix + "' in " + buildDir + " (zero or more than one files found ending with '" + suffix + "')");
        }
        return file[0];
    }

    private File getBuildDir() {
        return new File(getProject().getBuild().getDirectory());
    }

}