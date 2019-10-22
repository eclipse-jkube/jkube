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
package io.jkube.generator.webapp;

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
