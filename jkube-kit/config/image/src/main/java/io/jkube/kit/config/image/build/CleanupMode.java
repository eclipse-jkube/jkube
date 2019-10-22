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
package io.jkube.kit.config.image.build;

/**
 * Mode specifying how a cleanup should be performed.
 *
 * @author roland
 * @since 01/03/16
 */
public enum CleanupMode {
    NONE(false, "none"),
    TRY_TO_REMOVE(true, "try"),
    REMOVE(true, "remove");

    private final boolean remove;
    private final String parameter;

    CleanupMode(boolean remove, String parameter) {
        this.remove = remove;
        this.parameter = parameter;
    }

    public static CleanupMode parse(String param) {
        if (param == null || param.equalsIgnoreCase("try")) {
            return TRY_TO_REMOVE;
        } else if (param.equalsIgnoreCase("false") || param.equalsIgnoreCase("none")) {
            return NONE;
        } else if (param.equalsIgnoreCase("true") || param.equalsIgnoreCase("remove")) {
            return REMOVE;
        } else {
            throw new IllegalArgumentException("Invalid clean up mode " + param + " (should be one of: none/try/remove)");
        }
    }

    public boolean isRemove() {
        return remove;
    }

    public String toParameter() {
        return parameter;
    }
}
