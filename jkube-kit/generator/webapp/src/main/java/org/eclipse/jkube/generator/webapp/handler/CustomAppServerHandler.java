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
package org.eclipse.jkube.generator.webapp.handler;

import java.util.List;

import org.eclipse.jkube.generator.webapp.AppServerHandler;

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