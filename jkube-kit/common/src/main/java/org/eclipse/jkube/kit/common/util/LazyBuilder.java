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
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A builder that computes a specific object lazily.
 */
public class LazyBuilder<I, T> {

    private final AtomicReference<T> instance;
    private final Function<I, T> build;

    public LazyBuilder(Function<I, T> build) {
        this.instance = new AtomicReference<>();
        this.build = build;
    }

    public T get(I input) {
        T result = instance.get();
        if (result == null) {
            result = build.apply(input);
            if (!instance.compareAndSet(null, result)) {
                return instance.get();
            }
        }
        return result;
    }

    /**
     * Returns true if the builder was instantiated.
     *
     * @return true if an instance was built, false otherwise.
     */
    public boolean hasInstance() {
        return instance.get() != null;
    }

    public static class VoidLazyBuilder<T> extends LazyBuilder<Void, T> {

          public VoidLazyBuilder(Supplier<T> supplier) {
              super(empty -> supplier.get());
          }

          public T get() {
              return super.get(null);
          }
    }
}
