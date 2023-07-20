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
package org.eclipse.jkube.kit.enricher.api.util;

import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.Enricher;

import java.util.List;

public class Misc {

    private Misc() {}

    public static List<Enricher> filterEnrichers(ProcessorConfig config, List<Enricher> enrichers) {
        return config.prepareProcessors(enrichers, "enricher");
    }
}
