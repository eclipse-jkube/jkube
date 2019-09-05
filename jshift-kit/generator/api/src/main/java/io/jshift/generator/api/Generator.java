/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.generator.api;

import java.util.List;

import io.jshift.kit.build.service.docker.ImageConfiguration;
import io.jshift.kit.common.Named;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Generator which can create {@link ImageConfiguration} on the fly by examining the build pom.xml
 * @author roland
 * @since 15/05/16
 */
public interface Generator extends Named {

    /**
     * Check whether this generator should kick in. The check must not examing anything below `target/` as this
     * can not be available when this is called in a pre package phase.
     * @return true if the generator is applicable
     * @param configs all configuration already available
     */
    boolean isApplicable(List<ImageConfiguration> configs) throws MojoExecutionException;

    /**
     * Provide additional image configurations.
     *
     * @param existingConfigs the already detected and resolved configuration
     * @param prePackagePhase if true this is called in a prepackage phase where no artifacts has been packaged in target/.
     * @return list of image configurations
     */
    List<ImageConfiguration> customize(List<ImageConfiguration> existingConfigs, boolean prePackagePhase) throws MojoExecutionException;
}





