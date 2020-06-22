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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A builder that computes a specific object lazily.
 */
public class LazyBuilder<T> {

    private final AtomicReference<T> instance;
    private final Supplier<T> build;

    public LazyBuilder(Supplier<T> build) {
        this.instance = new AtomicReference<>();
        this.build = build;
    }

    public T get() {
        T result = instance.get();
        if (result == null) {
            result = build.get();
            if (!instance.compareAndSet(null, result)) {
                return instance.get();
            }
        }
        return result;
    }

}
