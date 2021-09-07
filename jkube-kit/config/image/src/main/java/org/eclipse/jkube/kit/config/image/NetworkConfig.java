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
package org.eclipse.jkube.kit.config.image;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Network config encapsulating network specific configuration
 * @author roland
 * @since 29/07/16
 */
@Getter
@EqualsAndHashCode
public class NetworkConfig implements Serializable {

    private final String name;
    private final Mode mode;
    private final List<String> aliases;

    public NetworkConfig() {
        this(null, null, null);
    }

    @Builder
    public NetworkConfig(Mode mode, String name, List<String> aliases) {
        this.name = name;
        this.mode = mode;
        this.aliases = Optional.ofNullable(aliases).orElse(new ArrayList<>());
    }
    // Use by Maven to add flattened <alias> entries
    // See  http://blog.sonatype.com/2011/03/configuring-plugin-goals-in-maven-3/
    public void addAlias(String alias) {
        aliases.add(alias);
    }

    /**
     * Legacy constructor using the ;&lt;net;&gt; config
     * @param net net, encapsulating mode and name.
     */
    public static NetworkConfig fromLegacyNetSpec(String net) {
        Mode mode = null;
        String name = null;
        if (net != null) {
            mode = extractMode(net);
            if (mode == Mode.container) {
                name = net.substring(Mode.container.name().length() + 1);
            } else if (mode == Mode.custom) {
                name = net;
            }
        }
        return NetworkConfig.builder().mode(mode).name(name).build();
    }

    private static Mode extractMode(String mode) {
        if (mode != null && mode.length() > 0) {
            try {
                return Mode.valueOf(mode.toLowerCase());
            } catch (IllegalArgumentException exp) { /* could be a custom mode, too */ }
            if (mode.toLowerCase().startsWith(Mode.container.name() + ":")) {
                return Mode.container;
            } else {
                return Mode.custom;
            }
        }
        return null;
    }

    public boolean isCustomNetwork() {
        return (mode != null && mode == Mode.custom) || (mode == null && name != null);
    }

    public boolean isStandardNetwork() {
        return mode != null && mode != Mode.custom;
    }

    public String getStandardMode(String containerId) {
        if (isCustomNetwork()) {
            throw new IllegalArgumentException("Custom network for network '" + name +
                    "' can not be used as standard mode");
        }
        if (mode == null) {
            return null;
        }
        return mode.name().toLowerCase() + (mode == Mode.container ? ":" + containerId : "");
    }

    public String getContainerAlias() {
        return mode == Mode.container ? name : null;
    }

    public String getCustomNetwork() {
        return mode == Mode.custom || mode == null ? name : null;
    }

    public boolean hasAliases() {
        return aliases != null && !aliases.isEmpty();
    }


    // Mode used for determining the network
    public enum Mode {
        none,
        bridge,
        host,
        container,
        custom;
    }

    public static class NetworkConfigBuilder {

        public NetworkConfigBuilder modeString(String modeString){
            mode = Optional.ofNullable(modeString).map(Mode::valueOf).orElse(null);
            return this;
        }

        public NetworkConfig build() {
            return mode == null && name == null && aliases == null ?
                null : new NetworkConfig(mode, name, aliases);
        }

    }
}

