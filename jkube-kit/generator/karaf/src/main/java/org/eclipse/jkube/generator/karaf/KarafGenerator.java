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
package org.eclipse.jkube.generator.karaf;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jkube.kit.config.image.build.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.generator.api.FromSelector;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.api.support.BaseGenerator;
import org.apache.commons.lang3.StringUtils;

public class KarafGenerator extends BaseGenerator {

    private static final String KARAF_MAVEN_PLUGIN_ARTIFACT_ID = "karaf-maven-plugin";

    public KarafGenerator(GeneratorContext context) {
        super(context, "karaf", new FromSelector.Default(context,"karaf"));
    }

    private enum Config implements Configs.Key {
        baseDir        {{ d = "/deployments/"; }},
        user           {{ d = "jboss:jboss:jboss"; }},
        cmd            {{ d = "/deployments/deploy-and-run.sh"; }},
        webPort        {{ d = "8181"; }},
        jolokiaPort    {{ d = "8778"; }};

        public String def() { return d; } protected String d;
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> configs, boolean prePackagePhase) {
        final ImageConfiguration.Builder imageBuilder = new ImageConfiguration.Builder();
        final BuildConfiguration.Builder buildBuilder = new BuildConfiguration.Builder();

        buildBuilder.ports(extractPorts()).cmd(new Arguments(getConfig(Config.cmd)));

        addSchemaLabels(buildBuilder, log);
        addFrom(buildBuilder);
        if (!prePackagePhase) {
            buildBuilder.assembly(createAssembly());
        }
        addLatestTagIfSnapshot(buildBuilder);
        imageBuilder
            .name(getImageName())
            .alias(getAlias())
            .buildConfig(buildBuilder.build());
        configs.add(imageBuilder.build());
        return configs;
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddImageConfiguration(configs) &&
                JKubeProjectUtil.hasPluginOfAnyArtifactId(getProject(), KARAF_MAVEN_PLUGIN_ARTIFACT_ID);
    }

    protected List<String> extractPorts() {
        List<String> answer = new ArrayList<>();
        addPortIfValid(answer, getConfig(Config.webPort));
        addPortIfValid(answer, getConfig(Config.jolokiaPort));
        return answer;
    }

    private void addPortIfValid(List<String> list, String port) {
        if (StringUtils.isNotBlank(port)) {
            list.add(port);
        }
    }

    private AssemblyConfiguration createAssembly() {
        return new AssemblyConfiguration.Builder()
            .targetDir(getConfig(Config.baseDir))
            .user(getConfig(Config.user))
            .descriptorRef("karaf")
            .build();
    }
}
