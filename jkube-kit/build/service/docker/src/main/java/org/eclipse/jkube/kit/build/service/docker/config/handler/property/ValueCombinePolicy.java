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
package org.eclipse.jkube.kit.build.service.docker.config.handler.property;

import org.apache.commons.lang3.StringUtils;

/**
 * Dictates how to combine values from different sources. See {@link PropertyConfigHandler} for details.
 */
public enum ValueCombinePolicy {
    /**
     * The prioritized value fully replaces any other values.
     */
    Replace,

    /**
     * All provided values are merged. This only makes sense for complex types such as lists and maps.
     */
    Merge;

    public static ValueCombinePolicy fromString(String valueCombinePolicy) {
        for (ValueCombinePolicy policy : values()) {
            if (policy.name().equalsIgnoreCase(valueCombinePolicy)) {
                return policy;
            }
        }
        throw new IllegalArgumentException(String.format("No value combine policy %s known. Valid values are: %s",
                valueCombinePolicy, StringUtils.join(values(), ", ")));
    }
}
