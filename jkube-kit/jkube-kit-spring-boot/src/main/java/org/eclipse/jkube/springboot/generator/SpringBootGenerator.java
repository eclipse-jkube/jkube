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
package org.eclipse.jkube.springboot.generator;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.GeneratorMode;
import org.eclipse.jkube.generator.javaexec.JavaExecGenerator;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.SpringBootConfiguration;
import org.eclipse.jkube.kit.common.util.SpringBootUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import static org.eclipse.jkube.kit.common.util.SpringBootUtil.DEV_TOOLS_REMOTE_SECRET;
import static org.eclipse.jkube.kit.common.util.SpringBootUtil.isSpringBootRepackage;
import static org.eclipse.jkube.springboot.SpringBootDevtoolsUtils.addDevToolsFilesToFatJar;
import static org.eclipse.jkube.springboot.SpringBootDevtoolsUtils.ensureSpringDevToolSecretToken;
import static org.eclipse.jkube.springboot.generator.SpringBootGenerator.Config.COLOR;

/**
 * @author roland
 */
public class SpringBootGenerator extends JavaExecGenerator {

    @AllArgsConstructor
    public enum Config implements Configs.Config {
        COLOR("color", "");

        @Getter
        protected String key;
        @Getter
        protected String defaultValue;
    }

    private final SpringBootNestedGenerator nestedGenerator;

    public SpringBootGenerator(GeneratorContext context) {
        super(context, "spring-boot");
        nestedGenerator = SpringBootNestedGenerator.from(context, getGeneratorConfig(), detectFatJar());
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddGeneratedImageConfiguration(configs) &&
          (JKubeProjectUtil.hasPluginOfAnyArtifactId(getProject(), SpringBootUtil.SPRING_BOOT_MAVEN_PLUGIN_ARTIFACT_ID) ||
            JKubeProjectUtil.hasPluginOfAnyArtifactId(getProject(), SpringBootUtil.SPRING_BOOT_GRADLE_PLUGIN_ARTIFACT_ID));
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> configs, boolean isPrePackagePhase) {
        if (getContext().getGeneratorMode() == GeneratorMode.WATCH) {
            boolean isDevtoolsSecretPresent = ensureSpringDevToolSecretToken(getProject());
            if (!isDevtoolsSecretPresent) {
                log.verbose("Generating the spring devtools token in property: " + DEV_TOOLS_REMOTE_SECRET);
                throw new IllegalStateException("No spring.devtools.remote.secret found in application.properties. Plugin has added it, please re-run goals");
            }
            if (!isPrePackagePhase && isFatJar()) {
                addDevToolsFilesToFatJar(getProject(), detectFatJar());
            }
        }
        return super.customize(configs, isPrePackagePhase);
    }

    @Override
    protected Map<String, String> getEnv(boolean prePackagePhase) {
        return nestedGenerator.getEnv(ppp -> super.getEnv(ppp), prePackagePhase);
    }

    @Override
    protected List<String> getExtraJavaOptions() {
        List<String> opts = super.getExtraJavaOptions();
        final String configuredColor = getConfig(COLOR);
        if (StringUtils.isNotBlank(configuredColor)) {
            opts.add("-Dspring.output.ansi.enabled=" + configuredColor);
        }
        return opts;
    }

    @Override
    protected boolean isFatJar() {
        if (!hasMainClass() && isSpringBootRepackage(getProject())) {
            log.verbose("Using fat jar packaging as the spring boot plugin is using `repackage` goal execution");
            return true;
        }
        return super.isFatJar();
    }

    @Override
    protected String getDefaultWebPort() {
        return "" + SpringBootConfiguration.from(getProject()).getServerPort();
    }

    @Override
    protected AssemblyConfiguration createAssembly() {
        return Optional.ofNullable(nestedGenerator.createAssemblyConfiguration(addAdditionalFiles()))
            .orElse(super.createAssembly());
    }

    @Override
    protected String getBuildWorkdir() {
        return nestedGenerator.getBuildWorkdir();
    }

    @Override
    protected String getFromAsConfigured() {
        return Optional.ofNullable(super.getFromAsConfigured()).orElse(nestedGenerator.getFrom());
    }

    @Override
    protected Arguments getBuildEntryPoint() {
        return nestedGenerator.getBuildEntryPoint();
    }

    @Override
    protected String getDefaultJolokiaPort() {
        return nestedGenerator.getDefaultJolokiaPort();
    }

    @Override
    protected String getDefaultPrometheusPort() {
        return nestedGenerator.getDefaultPrometheusPort();
    }
}
