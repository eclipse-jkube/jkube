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
package org.eclipse.jkube.generator.webapp;

import java.util.List;

/**
 * Interface encapsulating a certain application handler
 *
 * @author kameshs
 */
public interface AppServerHandler {

    /**
     * Return true it the handler thinks it should kick in. Typically
     * check for certain app server specific files and or plugins.
     * @return true is this handler should run, false otherwise
     */
    boolean isApplicable();

    /**
     * Name of the server that this appserver handler stands for
     *
     * @return server name
     */
    String getName();

    /**
     * Get the base image to use for this specific app server
     * @return base image name in Docker format.
     */
    String getFrom();

    /**
     * Get the directory with in the base image where to put the applications
     * into.
     *
     * @return the deployment directory fitting the the from() image.
     */
    String getDeploymentDir();

    /**
     * Get the default command to put into the image, which should fit to the
     * image returned from from()
     *
     * @return the default command.
     */
    String getCommand();

    /**
     * Get the user to use. Return null if no specific user directive is required
     */
    String getUser();

    /**
     * A list of ports which are exposed by the base image
     *
     * @return list of ports to expos
     */
    List<String> exposedPorts();
}
