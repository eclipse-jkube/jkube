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
package org.eclipse.jkube.generator.api;

import java.util.List;

import org.eclipse.jkube.kit.common.Named;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

/**
 * Generator which can create {@link ImageConfiguration} on the fly by examining the build pom.xml
 * @author roland
 */
public interface Generator extends Named {

    /**
     * Check whether this generator should kick in. The check must not examing anything below `target/` as this
     * can not be available when this is called in a pre package phase.
     * @return true if the generator is applicable
     * @param configs all configuration already available
     */
    boolean isApplicable(List<ImageConfiguration> configs);

    /**
     * Provide additional image configurations.
     *
     * @param existingConfigs the already detected and resolved configuration
     * @param prePackagePhase if true this is called in a prepackage phase where no artifacts has been packaged in target/.
     * @return list of image configurations
     */
    List<ImageConfiguration> customize(List<ImageConfiguration> existingConfigs, boolean prePackagePhase);
}





