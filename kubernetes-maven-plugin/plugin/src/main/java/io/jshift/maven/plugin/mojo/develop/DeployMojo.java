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
package io.jshift.maven.plugin.mojo.develop;

import io.jshift.maven.plugin.mojo.build.ApplyMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * This goal forks the install goal then applies the generated kubernetes resources to the current cluster.
 *
 * Note that the goals k8s:resource and k8s:build must be bound to the proper execution phases.
 *
 * @author roland
 * @since 09/06/16
 */

@Mojo(name = "deploy", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.VALIDATE)
@Execute(phase = LifecyclePhase.INSTALL)
public class DeployMojo extends ApplyMojo {
    @Parameter(property = "docker.skip.deploy", defaultValue = "false")
    protected boolean skipDeploy;

    @Override
    public void executeInternal() throws MojoExecutionException {
        if (skipDeploy) {
            return;
        }

        super.executeInternal();
    }
}
