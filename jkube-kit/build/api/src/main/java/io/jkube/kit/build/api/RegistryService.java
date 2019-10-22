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
package io.jkube.kit.build.api;

import java.io.IOException;

import io.jkube.kit.config.image.ImageConfiguration;
import io.jkube.kit.config.image.build.ImagePullPolicy;


/**
 * @author roland
 * @since 17.10.18
 */
public interface RegistryService {

    void pushImage(ImageConfiguration imageConfig, int retries, boolean skipTag, RegistryContext registryContext) throws IOException;

    void pullImage(String image, ImagePullPolicy policy, RegistryContext registryContext) throws IOException;
}
