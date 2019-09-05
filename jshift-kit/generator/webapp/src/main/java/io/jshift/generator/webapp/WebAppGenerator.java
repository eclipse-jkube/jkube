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
package io.jshift.generator.webapp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jshift.kit.build.service.docker.ImageConfiguration;
import io.jshift.kit.common.Configs;
import io.jshift.kit.common.util.MavenUtil;
import io.jshift.kit.config.image.build.Arguments;
import io.jshift.kit.config.image.build.AssemblyConfiguration;
import io.jshift.kit.config.image.build.BuildConfiguration;
import io.jshift.kit.config.image.build.OpenShiftBuildStrategy;
import io.jshift.generator.api.GeneratorContext;
import io.jshift.generator.api.support.BaseGenerator;
import io.jshift.generator.webapp.handler.CustomAppServerHandler;
import io.jshift.kit.config.resource.RuntimeMode;

import static io.jshift.kit.config.image.build.util.BuildLabelUtil.addSchemaLabels;


/**
 * A generator for WAR apps
 *
 * @author kameshs
 */
public class WebAppGenerator extends BaseGenerator {

    private enum Config implements Configs.Key {
        // App server to use (like 'tomcat', 'jetty', 'wildfly'
        server,

        // Directory where to deploy to
        targetDir,

        // Unix user under which the war should be installed. If null, the default image user is used
        user,

        // Command to execute. If null, the base image default command is used
        cmd,

        // Context path under which the app will be available
        path {{ d = "/"; }},

        // Ports to expose as a command separated list
        ports;

        protected String d;

        public String def() { return d; }
    }

    public WebAppGenerator(GeneratorContext context) {
        super(context, "webapp");
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs) {
        return shouldAddImageConfiguration(configs) &&
               MavenUtil.hasPlugin(getProject(), "org.apache.maven.plugins", "maven-war-plugin");
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> configs, boolean prePackagePhase) {
        if (getContext().getRuntimeMode() == RuntimeMode.openshift &&
            getContext().getStrategy() == OpenShiftBuildStrategy.s2i &&
            !prePackagePhase) {
            throw new IllegalArgumentException("S2I not yet supported for the webapp-generator. Use -Dfabric8.mode=kubernetes or " +
                                               "-Dfabric8.build.strategy=docker for OpenShift mode. Please refer to the reference manual at " +
                                               "https://maven.jshift.io for details about build modes.");
        }

        // Late initialization to avoid unnecessary directory scanning
        AppServerHandler handler = getAppServerHandler(getContext());

        log.info("Using %s as base image for webapp",handler.getFrom());

        ImageConfiguration.Builder imageBuilder = new ImageConfiguration.Builder();

        BuildConfiguration.Builder buildBuilder = new BuildConfiguration.Builder()
            .from(getFrom(handler))
            .ports(handler.exposedPorts())
            .cmd(new Arguments(getDockerRunCommand(handler)))
            .env(getEnv(handler));

        addSchemaLabels(buildBuilder, getContext().getProject(), log);
        if (!prePackagePhase) {
            buildBuilder.assembly(createAssembly(handler));
        }
        addLatestTagIfSnapshot(buildBuilder);
        imageBuilder
            .name(getImageName())
            .alias(getAlias())
            .buildConfig(buildBuilder.build());
        configs.add(imageBuilder.build());

        return configs;
    }

    private AppServerHandler getAppServerHandler(GeneratorContext context) {
        String from = super.getFromAsConfigured();
        if (from != null) {
            // If a base image is provided use this exclusively and dont do a custom lookup
            return createCustomAppServerHandler(from);
        } else {
            return new AppServerDetector(context.getProject()).detect(getConfig(Config.server));
        }
    }

    private AppServerHandler createCustomAppServerHandler(String from) {
        String user = getConfig(Config.user);
        String deploymentDir = getConfig(Config.targetDir, "/deployments");
        String command = getConfig(Config.cmd);
        List<String> ports = Arrays.asList(getConfig(Config.ports, "8080").split("\\s*,\\s*"));
        return new CustomAppServerHandler(from, deploymentDir, command, user, ports);
    }

    protected Map<String, String> getEnv(AppServerHandler handler) {
        Map<String, String> defaultEnv = new HashMap<>();
        defaultEnv.put("DEPLOY_DIR", getDeploymentDir(handler));
        return defaultEnv;
    }

    private AssemblyConfiguration createAssembly(AppServerHandler handler) {
        String path = getConfig(Config.path);
        if (path.equals("/")) {
            path = "ROOT";
        }
        getProject().getProperties().setProperty("jshift.generator.webapp.path",path);
        AssemblyConfiguration.Builder builder = new AssemblyConfiguration.Builder()
                .targetDir(getDeploymentDir(handler))
                .descriptorRef("webapp");
        String user = getUser(handler);
        if (user != null) {
            builder.user(user);
        }
        return builder.build();
    }

    // To be called **only** from customize() as they require an already
    // initialized appServerHandler:
    protected String getFrom(AppServerHandler handler) {
        return handler.getFrom();
    }

    private String getDockerRunCommand(AppServerHandler handler) {
        String cmd = getConfig(Config.cmd);
        return cmd != null ? cmd : handler.getCommand();
    }

    private String getDeploymentDir(AppServerHandler handler) {
        String deploymentDir = getConfig(Config.targetDir);
        return deploymentDir != null ? deploymentDir : handler.getDeploymentDir();
    }

    private String getUser(AppServerHandler handler) {
        String user = getConfig(Config.user);
        return user != null ? user : handler.getUser();
    }
}
