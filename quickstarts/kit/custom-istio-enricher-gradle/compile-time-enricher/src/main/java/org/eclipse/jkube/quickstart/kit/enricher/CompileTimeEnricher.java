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
package org.eclipse.jkube.quickstart.kit.enricher;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

public class CompileTimeEnricher extends BaseEnricher {

    public CompileTimeEnricher(JKubeEnricherContext enricherContext) {
        super(enricherContext, "compile-time-enricher");
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
        log.info("This is the compile-time-enricher running");
    }
}
