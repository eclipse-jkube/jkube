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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.jkube.generator.webapp.handler.JettyAppSeverHandler;
import io.jkube.generator.webapp.handler.TomcatAppSeverHandler;
import io.jkube.generator.webapp.handler.WildFlyAppSeverHandler;
import org.apache.maven.project.MavenProject;

/**
 * @author kameshs
 */
class AppServerDetector {

    private final List<? extends io.jkube.generator.webapp.AppServerHandler> serverHandlers;
    private final AppServerHandler defaultHandler;
    private final HashMap<String, AppServerHandler> serverHandlerMap;

    AppServerDetector(MavenProject project) {
        // Add new handlers to this list for new appservers
        serverHandlers =
            Arrays.asList(
                new JettyAppSeverHandler(project),
                new WildFlyAppSeverHandler(project),
                defaultHandler = new TomcatAppSeverHandler(project)
                         );
        serverHandlerMap = new HashMap<>();
        for (AppServerHandler handler : serverHandlers) {
            serverHandlerMap.put(handler.getName(), handler);
        }
    }

    AppServerHandler detect(String server) {
        if (server != null && serverHandlerMap.containsKey(server)) {
            return serverHandlerMap.get(server);
        }
        for (AppServerHandler handler : serverHandlers) {
            if (handler.isApplicable()) {
                return handler;
            }
        }
        return defaultHandler;
    }
}