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
package org.eclipse.jkube.kit.build.service.docker.access.log;

import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;

import java.util.HashMap;
import java.util.Map;

/**
 * @author roland
 * @since 25/11/14
 */
public class LogDispatcher {

    private Map<String,LogGetHandle> logHandles;

    private DockerAccess dockerAccess;

    public LogDispatcher(DockerAccess dockerAccess) {
        this.dockerAccess = dockerAccess;
        logHandles = new HashMap<>();
    }

    public synchronized void trackContainerLog(String containerId, LogOutputSpec spec)  {
        LogGetHandle handle = dockerAccess.getLogAsync(containerId, new DefaultLogCallback(spec));
        logHandles.put(containerId, handle);
    }

    public synchronized void fetchContainerLog(String containerId, LogOutputSpec spec) {
        dockerAccess.getLogSync(containerId, new DefaultLogCallback(spec));
    }

    public synchronized void untrackAllContainerLogs() {
        for (Map.Entry<String,LogGetHandle> logHandlesEntry : logHandles.entrySet()) {
            LogGetHandle handle = logHandlesEntry.getValue();
            handle.finish();
        }
        logHandles.clear();
    }


    public static LogDispatcher getLogDispatcher(Map<String, Object> pluginContext, ServiceHub hub, String logDispatcherContextKey) {
        LogDispatcher dispatcher = (LogDispatcher) pluginContext.get(logDispatcherContextKey);
        if (dispatcher == null) {
            dispatcher = new LogDispatcher(hub.getDockerAccess());
            pluginContext.put(logDispatcherContextKey, dispatcher);
        }
        return dispatcher;
    }

    // =======================================================================================


}
