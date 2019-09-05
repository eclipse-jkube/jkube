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
package io.jshift.generator.webapp.handler;

import java.util.List;

import io.jshift.generator.webapp.AppServerHandler;

/**
 * A custom app server handler used when use explicitely configures the base image
 *
 * @author roland
 * @since 05/10/16
 */
public class CustomAppServerHandler implements AppServerHandler {

    private String from, deploymentDir, command, user;
    private List<String> ports;

    public CustomAppServerHandler(String from, String deploymentDir, String command, String user, List<String> ports) {
        this.from = from;
        this.deploymentDir = deploymentDir;
        this.command = command;
        this.user = user;
        this.ports = ports;
    }

    @Override
    public boolean isApplicable() {
        return true;
    }

    @Override
    public String getName() {
        return "custom";
    }

    @Override
    public String getFrom() {
        return from;
    }

    @Override
    public String getDeploymentDir() {
        return deploymentDir;
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public List<String> exposedPorts() {
        return ports;
    }
}