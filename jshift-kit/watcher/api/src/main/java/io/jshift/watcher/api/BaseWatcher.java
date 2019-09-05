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
package io.jshift.watcher.api;


import io.jshift.kit.common.Configs;
import io.jshift.kit.common.PrefixedLogger;

/**
 * The base class of watchers.
 */
public abstract class BaseWatcher implements Watcher {

    private WatcherContext context;

    private WatcherConfig config;

    private String name;

    protected final PrefixedLogger log;

    public BaseWatcher(WatcherContext context, String name) {
        this.context = context;
        this.config = new WatcherConfig(context.getProject().getProperties(), name, context.getConfig());
        this.name = name;
        this.log = new PrefixedLogger(name, context.getLogger());
    }

    public WatcherContext getContext() {
        return context;
    }

    protected String getConfig(Configs.Key key) {
        return config.get(key);
    }

    protected String getConfig(Configs.Key key, String defaultVal) {
        return config.get(key, defaultVal);
    }

    @Override
    public String getName() {
        return name;
    }

}
