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
package io.jkube.kit.build.service.docker.helper;

import io.jkube.kit.common.ExternalCommand;
import io.jkube.kit.common.KitLogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command for extracting the environment information emitted by e.g. 'docker-machine env' as
 * a map.
 *
 * @since 14/09/16
 */
abstract public class EnvCommand extends ExternalCommand {

    private final Map<String, String> env = new HashMap<>();

    private final String prefix;

    public EnvCommand(KitLogger log, String prefix) {
        super(log);
        this.prefix = prefix;
    }

    @Override
    protected void processLine(String line) {
        if (log.isDebugEnabled()) {
            log.verbose("%s", line);
        }
        if (line.startsWith(prefix)) {
            setEnvironmentVariable(line.substring(prefix.length()));
        }
    }

    private final Pattern ENV_VAR_PATTERN = Pattern.compile("^\\s*(?<key>[^=]+)=\"?(?<value>.*?)\"?\\s*$");

    // parse line like SET DOCKER_HOST=tcp://192.168.99.100:2376
    private void setEnvironmentVariable(String line) {
        Matcher matcher = ENV_VAR_PATTERN.matcher(line);
        if (matcher.matches()) {
            String key = matcher.group("key");
            String value = matcher.group("value");
            log.debug("Env: %s=%s",key,value);
            env.put(key, value);
        }
    }

    public Map<String, String> getEnvironment() throws IOException {
        execute();
        return env;
    }
}
