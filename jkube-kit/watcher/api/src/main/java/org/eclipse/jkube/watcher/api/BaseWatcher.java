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
package org.eclipse.jkube.watcher.api;


import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.PrefixedLogger;

/**
 * The base class of watchers.
 */
public abstract class BaseWatcher implements Watcher {

    private final WatcherContext context;

    private final WatcherConfig config;

    private final String name;

    protected final PrefixedLogger log;

    public BaseWatcher(WatcherContext context, String name) {
        this.context = context;
        this.config = new WatcherConfig(context.getBuildContext().getProject().getProperties(), name, context.getConfig());
        this.name = name;
        this.log = new PrefixedLogger(name, context.getLogger());
    }

    public WatcherContext getContext() {
        return context;
    }

    protected String getConfig(Configs.Config key) {
        return config.get(key);
    }

    @Override
    public String getName() {
        return name;
    }

}
