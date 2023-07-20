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
package org.eclipse.jkube.generator.webapp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.webapp.handler.JettyAppSeverHandler;
import org.eclipse.jkube.generator.webapp.handler.TomcatAppSeverHandler;
import org.eclipse.jkube.generator.webapp.handler.WildFlyAppSeverHandler;

/**
 * @author kameshs
 */
class AppServerDetector {

    private final List<? extends org.eclipse.jkube.generator.webapp.AppServerHandler> serverHandlers;
    private final AppServerHandler defaultHandler;
    private final HashMap<String, AppServerHandler> serverHandlerMap;

    AppServerDetector(GeneratorContext generatorContext) {
        // Add new handlers to this list for new appservers
        defaultHandler = new TomcatAppSeverHandler(generatorContext);
        serverHandlers = Arrays.asList(
            new JettyAppSeverHandler(generatorContext),
            new WildFlyAppSeverHandler(generatorContext),
            defaultHandler
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