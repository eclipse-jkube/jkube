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
package org.eclipse.jkube.kit.enricher.api.visitor;

import java.util.List;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.Enricher;

public abstract class SelectorVisitor<T> extends TypedVisitor<T> {

    private static ThreadLocal<ProcessorConfig> configHolder = new ThreadLocal<>();

    final List<Enricher> enrichers;

    SelectorVisitor(List<Enricher> enrichers) {
        this.enrichers = enrichers;
    }

    public static void setProcessorConfig(ProcessorConfig config) {
        configHolder.set(config);
    }

    public static void clearProcessorConfig() {
        configHolder.remove();
    }

    protected static ProcessorConfig getConfig() {
        ProcessorConfig ret = configHolder.get();
        if (ret == null) {
            throw new IllegalArgumentException("Internal: No ProcessorConfig set");
        }
        return ret;
    }
}

