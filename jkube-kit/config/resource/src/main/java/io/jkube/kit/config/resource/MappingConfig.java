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
package io.jkube.kit.config.resource;

public class MappingConfig {

    private String kind;

    private String filenameTypes;

    public String getKind() {
        return kind;
    }

    public String getFilenameTypes() {
        return filenameTypes;
    }

    public String[] getFilenamesAsArray() {
        if (this.filenameTypes == null) {
            return new String[0];
        }
        return filenameTypes.split(",\\s*");
    }

    public boolean isValid() {
        return kind != null &&  filenameTypes != null && filenameTypes.length() > 0;
    }

}
