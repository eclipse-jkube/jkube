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
package org.eclipse.jkube.kit.common.util;

/**
 * A builder that computes a specific object lazily.
 */
public abstract class LazyBuilder<T> {

    private volatile T instance;

    public LazyBuilder() {
    }

    public T get() {
        T result = instance;
        if (result == null) {
            synchronized (this) {
                result = instance;
                if (result == null) {
                    instance = result = build();
                }
            }
        }
        return result;
    }

    protected abstract T build();

}
