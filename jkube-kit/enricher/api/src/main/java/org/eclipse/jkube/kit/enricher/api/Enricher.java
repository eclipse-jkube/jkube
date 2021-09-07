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
package org.eclipse.jkube.kit.enricher.api;

import org.eclipse.jkube.kit.common.Named;
import org.eclipse.jkube.kit.config.resource.PlatformMode;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

/**
 * Interface describing enrichers which add to kubernetes descriptors
 *
 * @author roland
 * @since 01/04/16
 */
public interface Enricher extends Named {

    /**
     * Add default resources when they are missing. Each enricher should be responsible
     * for a certain kind of resource and should detect whether a default resource
     * should be added. This should be done defensive, so that when an object is
     * already set it must not be overwritten. This method is only called on resources which are
     * associated with the artefact to build. This is determined that the resource is named like the artifact
     * to build.
     *
     * @param platformMode platform mode for generated resource descriptors
     * @param builder the build to examine and add to
     */
    void create(PlatformMode platformMode, KubernetesListBuilder builder);

    /**
     * Final customization of the overall resource descriptor. Fine tuning happens here.
     * @param platformMode platform mode for generated resource descriptors
     * @param builder list to customer used to customize
     */
    void enrich(PlatformMode platformMode, KubernetesListBuilder builder);
}
