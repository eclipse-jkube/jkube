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
package io.jkube.generator.karaf;

import java.util.ArrayList;
import java.util.List;

import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.common.Configs;
import io.jkube.kit.common.util.MavenUtil;
import io.jkube.kit.config.image.build.Arguments;
import io.jkube.kit.config.image.build.AssemblyConfiguration;
import io.jkube.kit.config.image.build.BuildConfiguration;
import io.jkube.generator.api.FromSelector;
import io.jkube.generator.api.GeneratorContext;
import io.jkube.generator.api.support.BaseGenerator;
import org.apache.commons.lang3.StringUtils;

import static io.jkube.kit.config.image.build.util.BuildLabelUtil.addSchemaLabels;

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
        ImageConfiguration.Builder imageBuilder = new ImageConfiguration.Builder();
        BuildConfiguration.Builder buildBuilder = new BuildConfiguration.Builder()
            .ports(extractPorts())
            .cmd(new Arguments(getConfig(Config.cmd)));
        addSchemaLabels(buildBuilder, getContext().getProject(), log);
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
               MavenUtil.hasPluginOfAnyGroupId(getProject(), KARAF_MAVEN_PLUGIN_ARTIFACT_ID);
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
