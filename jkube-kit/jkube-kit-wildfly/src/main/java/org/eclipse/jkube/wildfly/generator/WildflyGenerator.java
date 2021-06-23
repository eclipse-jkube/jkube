/**
 * Copyright (c) 2021 Red Hat, Inc.
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
package org.eclipse.jkube.wildfly.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.wildfly.jar.enricher.WildflyJARHealthCheckEnricher;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.support.BaseGenerator;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.wildfly.jar.generator.WildflyGeneratorCommon;

public class WildflyGenerator extends BaseGenerator {

    static final String TARGET_DIR = "/opt";
    static final String SERVER_DIR = "server";
    static final String TARGET_SERVER_PATH = TARGET_DIR + "/" + SERVER_DIR;
    

    private final WildflyGeneratorCommon common;

    @AllArgsConstructor
    public enum Config implements Configs.Config {
        // Server Directory name in the project target dir
        SERVER_DIRECTORY_NAME("serverDirectoryName", null),
        // Webport to expose. Set to 0 if no port should be exposed
        WEB_PORT("webPort", "8080");

        @Getter
        protected String key;
        @Getter(AccessLevel.PUBLIC)
        protected String defaultValue;
    }

    public WildflyGenerator(GeneratorContext context) {
        super(context, "wildfly", new FromSelector.Default(context, "wildfly.provision"));
        common = new WildflyGeneratorCommon(TARGET_DIR, context);
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddGeneratedImageConfiguration(configs)
                && JKubeProjectUtil.hasPlugin(getProject(),
                        WildflyJARHealthCheckEnricher.BOOTABLE_JAR_GROUP_ID, WildflyJARHealthCheckEnricher.BOOTABLE_JAR_ARTIFACT_ID) && 
                common.isServerEnabled();
    }

    public List<AssemblyFileSet> getAdditionalFiles() {
        return common.getAdditionalFiles();
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> configs, boolean prePackagePhase) {
        final ImageConfiguration.ImageConfigurationBuilder imageBuilder = ImageConfiguration.builder();
        final BuildConfiguration.BuildConfigurationBuilder buildBuilder = BuildConfiguration.builder();
        buildBuilder.ports(extractPorts());
        addSchemaLabels(buildBuilder, log);
        addFrom(buildBuilder);
        if (!prePackagePhase) {
            try {
                // Only add assembly if not in a pre-package phase where the referenced files
                // won't be available.
                buildBuilder.assembly(createServerAssembly());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        addLatestTagIfSnapshot(buildBuilder);
        imageBuilder
                .name(getImageName())
                .registry(getRegistry())
                .alias(getAlias())
                .build(buildBuilder.build());
        configs.add(imageBuilder.build());
        return configs;
    }

    private List<String> extractPorts() {
        List<String> answer = new ArrayList<>();
        addPortIfValid(answer, getConfig(Config.WEB_PORT));
        return answer;
    }

    private void addPortIfValid(List<String> list, String port) {
        if (StringUtils.isNotBlank(port) && Integer.parseInt(port) > 0) {
            list.add(port);
        }
    }

    private Map<String, String> getServerEnv() {
        List<String> options = getOptions();
        Map<String, String> ret = new HashMap<>();
        if (!options.isEmpty()) {
            // Use JAVA_OPTS in order to convey the local repo location to all JBoss scripts (CLI, ...) present in the image
            ret.put("JAVA_OPTS", StringUtils.join(options.iterator(), " "));
        }
        return ret;
    }
    
    public  List<String> getOptions() {
       return common.getOptions();
    }

    AssemblyConfiguration createServerAssembly() throws IOException {
        final AssemblyConfiguration.AssemblyConfigurationBuilder builder = AssemblyConfiguration.builder();
        builder.targetDir(TARGET_DIR);
        builder.excludeFinalOutputArtifact(true);
        addServerAssembly(builder);
        builder.name(SERVER_DIR);
        return builder.build();
    }

    void addServerAssembly(AssemblyConfiguration.AssemblyConfigurationBuilder builder) throws IOException {
        File buildDirectory = getProject().getBuildDirectory();
        File server;
        String configuredDirectory = getConfig(Config.SERVER_DIRECTORY_NAME);
        if (configuredDirectory != null) {
            server = new File(buildDirectory, configuredDirectory);
            if (!server.exists()) {
                throw new IllegalArgumentException("Server directory " + server + 
                        " doesn't exist, the " + Config.SERVER_DIRECTORY_NAME.getKey() + " value must reference an existing directory.");
            }
        } else {
            server = scanBuildDirectory(buildDirectory);
        }
        if (server != null) {
            List<AssemblyFileSet> fileSets = new ArrayList<>();
            fileSets.addAll(getAdditionalFiles());
            fileSets.add(AssemblyFileSet.builder()
                    .directory(server)
                    .outputDirectory(new File(SERVER_DIR))
                    .include("**")
                    .exclude("**.sh")
                    .fileMode("0644")
                    .build());
            fileSets.add(AssemblyFileSet.builder()
                    .directory(server)
                    .outputDirectory(new File(SERVER_DIR))
                    .include("**.sh")
                    .fileMode("0755")
                    .build());

            builder.inline(Assembly.builder().fileSets(fileSets).build());
            builder.user("jboss:root:jboss");
        } else {
            throw new IllegalArgumentException("No server detected, make sure your image assembly configuration contains all the required"
                    + " dependencies for your application to run.");
        }
    }

    File scanBuildDirectory(File build) throws IOException {
        if (!build.exists()) {
            return null;
        }
        List<Path> servers;
        try (Stream<Path> stream = Files.find(
                build.toPath(),
                1,
                (filePath, fileAttr) -> fileAttr.isDirectory()
                && !filePath.equals(build.toPath())
                && Files.exists(filePath.resolve("jboss-modules.jar"))
                && Files.exists(filePath.resolve("bin"))
                && Files.exists(filePath.resolve("standalone"))
                && Files.exists(filePath.resolve("modules"))
        )) {
            servers = stream.collect(Collectors.toList());
        }
        if (servers.isEmpty()) {
            return null;
        }
        if (servers.size() > 1) {
            throw new RuntimeException("Multiple server exist, use the option "+ Config.SERVER_DIRECTORY_NAME.getKey() + 
                    " to identify the right server inside " + build + " directory");
        }
        return servers.get(0).toFile();
    }
}
