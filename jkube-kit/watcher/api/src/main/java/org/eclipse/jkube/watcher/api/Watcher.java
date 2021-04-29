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
package org.eclipse.jkube.watcher.api;

import java.util.Collection;
import java.util.List;

import org.eclipse.jkube.kit.common.Named;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;

import io.fabric8.kubernetes.api.model.HasMetadata;


public interface Watcher extends Named {

    /**
     * Check whether this watcher should kick in.
     *
     * @return true if the watcher is applicable
     * @param configs all image configurations
     */
    boolean isApplicable(List<ImageConfiguration> configs, Collection<HasMetadata> resources, PlatformMode mode);


    /**
     * Watch the resources and kick a rebuild when they change.
     *
     * @param configs all image configurations
     */
    void watch(List<ImageConfiguration> configs, Collection<HasMetadata> resources, PlatformMode mode) throws Exception;

}
