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
package org.eclipse.jkube.kit.build.service.docker.helper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Enum holding the possible values avalaible for auto-pulling.
 *
 * @author roland
 * @since 01/03/15
 */
public enum AutoPullMode {

    ON(true, "on", "true"),
    ONCE(true, "once"),
    OFF(false, "off", "false"),
    ALWAYS(true, "always");

    private final Set<String> values = new HashSet<>();
    private final boolean doPullIfNotPresent;

    AutoPullMode(boolean doPullIfNotPresent, String... vals) {
        this.doPullIfNotPresent = doPullIfNotPresent;
        Collections.addAll(values, vals);
    }

    public boolean doPullIfNotPresent() {
        return doPullIfNotPresent;
    }

    public boolean alwaysPull() {
        return (this == ONCE || this == ALWAYS);
    }

        public static AutoPullMode fromString(String val) {
        String valNorm = val.toLowerCase();
        for (AutoPullMode mode : values()) {
            if (mode.values.contains(valNorm)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid auto-pull mode " + val + ". Please use 'on', 'off', 'once' or 'always'.");
    }
}
