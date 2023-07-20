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
package org.eclipse.jkube.kit.config.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class MappingConfig {

    private String kind;
    private String apiVersion;
    private String filenameTypes;

    public String[] getFilenamesAsArray() {
        if (this.filenameTypes == null) {
            return new String[0];
        }
        return filenameTypes.split(",\\s*");
    }

    public boolean isValid() {
        return kind != null && filenameTypes != null && filenameTypes.length() > 0;
    }
}
