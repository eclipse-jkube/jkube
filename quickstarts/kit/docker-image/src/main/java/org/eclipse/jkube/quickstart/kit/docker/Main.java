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
package org.eclipse.jkube.quickstart.kit.docker;

import org.eclipse.jkube.kit.build.service.docker.DockerAccessFactory;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.ServiceHub;
import org.eclipse.jkube.kit.build.service.docker.ServiceHubFactory;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpecFactory;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.JKubeConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.BuildServiceConfig;
import org.eclipse.jkube.kit.config.service.JKubeServiceException;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;

import java.io.File;

public class Main {
  public static void main(String[] args) {
    final KitLogger kitLogger = new KitLogger.StdoutLogger();
    kitLogger.info("Initiating default JKube configuration and required services...");

    kitLogger.info(" - Creating DockerAccessContext");
    final DockerAccessFactory.DockerAccessContext dac = DockerAccessFactory.DockerAccessContext.builder()
        .projectProperties(System.getProperties()).skipMachine(false).maxConnections(100).log(kitLogger)
        .build();
    kitLogger.info(" - Creating Docker Service Hub");
    final ServiceHub serviceHub = new ServiceHubFactory().createServiceHub(
        new DockerAccessFactory().createDockerAccess(dac),
        kitLogger,
        new LogOutputSpecFactory(false, true));
    kitLogger.info(" - Creating Docker Build Service Configuration");
    final BuildServiceConfig dockerBuildServiceConfig = new BuildServiceConfig.Builder().build();
    kitLogger.info(" - Creating configuration for JKube");
    final JKubeConfiguration configuration = JKubeConfiguration.builder()
        .project(JavaProject.builder().build())
        .outputDirectory(new File("target").getAbsolutePath())
        .build();

    kitLogger.info("Creating configuration for example Docker Image");
    final ImageConfiguration imageConfiguration = ImageConfiguration.builder()
        .build(BuildConfiguration.builder().from("java").build())
        .build();
    try (
        JKubeServiceHub jKubeServiceHub = new JKubeServiceHub.Builder()
            .log(kitLogger)
            .configuration(configuration)
            .platformMode(RuntimeMode.kubernetes)
            .dockerServiceHub(serviceHub)
            .buildServiceConfig(dockerBuildServiceConfig)
            .build()) {
      jKubeServiceHub.getBuildService().build(imageConfiguration);
//      jKubeServiceHub.getApplyService().applyResource();
    } catch (JKubeServiceException ex) {
      kitLogger.error("Error occurred: '%s'", ex.getMessage());
    }
  }
  //
  //
  //  private static BuildServiceConfig initBuildServiceConfig() throws DependencyResolutionRequiredException {
  //    return new BuildServiceConfig.Builder()
  //        .dockerBuildContext(getBuildContext())
  //        .dockerMavenBuildContext(createMojoParameters())
  //        .buildRecreateMode(BuildRecreateMode.fromParameter(buildRecreate))
  //        .openshiftBuildStrategy(buildStrategy)
  //        .openshiftPullSecret(openshiftPullSecret)
  //        .s2iBuildNameSuffix(s2iBuildNameSuffix)
  //        .s2iImageStreamLookupPolicyLocal(s2iImageStreamLookupPolicyLocal)
  //        .forcePullEnabled(forcePull)
  //        .imagePullManager(getImagePullManager(imagePullPolicy, autoPull))
  //        .buildDirectory(project.getBuild().getDirectory())
  //        .attacher((classifier, destFile) -> {
  //          if (destFile.exists()) {
  //            projectHelper.attachArtifact(project, "yml", classifier, destFile);
  //          }
  //        })
  //        .enricherTask(builder -> {
  //          EnricherManager enricherManager = new EnricherManager(resources, getEnricherContext(), MavenUtil.getCompileClasspathElementsIfRequested(project, useProjectClasspath));
  //          enricherManager.enrich(PlatformMode.kubernetes, builder);
  //          enricherManager.enrich(PlatformMode.openshift, builder);
  //        })
  //        .build();
  //  }
}
