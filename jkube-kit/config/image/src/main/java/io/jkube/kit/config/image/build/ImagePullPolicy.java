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

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author roland
 * @since 05.11.17
 */
public enum ImagePullPolicy {

    /**
     * Pull always images
     */
    Always,

    /**
     * Pull image only if not present
     */
    IfNotPresent,

    /**
     * Don't auto pull images
     */
    Never;

    public static ImagePullPolicy fromString(String imagePullPolicy) {
        for (ImagePullPolicy policy : values()) {
            if (policy.name().equalsIgnoreCase(imagePullPolicy)) {
                return policy;
            }
        }
        throw new IllegalArgumentException(
            String.format("No policy %s known. Valid values are: %s",
                          imagePullPolicy,
                          Stream.of(values()).map(Enum::name).collect(Collectors.joining(", "))));
    }
}
