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
package org.eclipse.jkube.quickstart.kit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Collections;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.api.model.Template;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.service.JKubeServiceHub;
import org.eclipse.jkube.kit.resource.helm.HelmConfig;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.ParameterBuilder;
import io.fabric8.openshift.api.model.TemplateBuilder;
import org.apache.commons.io.FileUtils;

public class Main {

  private static final String APP_NAME = "jkube-helm-example";
  private static final String CONFIG_MAP_NAME = APP_NAME + "-config-map";
  private static final String SERVICE_NAME = APP_NAME + "-service";

  public static void main(String[] args) throws IOException {
    final File targetDir = getProjectDir().resolve("target").toFile();
    final File helmSourceDir = new File(targetDir, "helm-sources");
    final File kubernetesHelmInputDir = new File(helmSourceDir, "kubernetes");
    FileUtils.forceMkdir(kubernetesHelmInputDir);

    Files.walkFileTree(getProjectDir().resolve("static").toAbsolutePath(), new SimpleFileVisitor<Path>(){
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        FileUtils.copyFileToDirectory(file.toFile(), kubernetesHelmInputDir);
        return super.visitFile(file, attrs);
      }
    });

    final Template values = new TemplateBuilder()
        .addToParameters(new ParameterBuilder().withName("serviceAccountName").withValue("default").build())
        .addToParameters(new ParameterBuilder().withName("labelsAppName").withValue(APP_NAME).build())
        .addToParameters(new ParameterBuilder()
            .withName("containerName").withValue("{{ .Chart.Name }}").build())
        .addToParameters(new ParameterBuilder().withName("containerImagePullPolicy").withValue("Always").build())
        .addToParameters(new ParameterBuilder().withName("volumeName").withValue("config-map-volume").build())
        .addToParameters(new ParameterBuilder().withName("serviceType").withValue("NodePort").build())
        .addToParameters(new ParameterBuilder().withName("configMapName").withValue(CONFIG_MAP_NAME).build())
        .addToParameters(new ParameterBuilder().withName("helmBuildLogFilename").withValue("helm-generate.log").build())
        .build();

    final KubernetesListBuilder klb = new KubernetesListBuilder()
        .addToItems(
            new ConfigMapBuilder()
                .withNewMetadata().withName("${configMapName}").addToLabels("app", "${labelsAppName}").endMetadata()
                .addToData("${helmBuildLogFilename}", String.format("Chart generated: %s\n", Instant.now().toString()))
                .build()
        )
        .addToItems(new ServiceBuilder()
            .withNewMetadata().withName(SERVICE_NAME).addToLabels("app", "${labelsAppName}").endMetadata()
            .withNewSpec()
            .addToSelector("app", "${labelsAppName}")
            .addNewPort().withPort(8080).withNewTargetPort(8080).endPort()
            .withType("${serviceType}")
            .endSpec()
            .build());
    FileUtils.write(
        new File(kubernetesHelmInputDir, "kubernetes.yml"), Serialization.asYaml(klb.build()), StandardCharsets.UTF_8);

    final HelmConfig helmConfig = HelmConfig.builder()
        .chart(APP_NAME + "-chart")
        .version("1.33.7")
        .templates(Collections.singletonList(values))
        .sourceDir(helmSourceDir.getAbsolutePath())
        .outputDir(new File(targetDir, "helm").getAbsolutePath())
        .tarballOutputDir(targetDir.getAbsolutePath())
        .chartExtension("tar.gz")
        .types(Collections.singletonList(HelmConfig.HelmType.KUBERNETES))
        .build();
    JKubeServiceHub.builder()
        .configuration(JKubeConfiguration.builder()
            .project(JavaProject.builder()
                .baseDirectory(getProjectDir().toFile())
                .build())
            .outputDirectory("target")
            .build())
        .platformMode(RuntimeMode.KUBERNETES)
        .log(new KitLogger.StdoutLogger())
        .build().getHelmService()
        .generateHelmCharts(helmConfig);
  }

  private static Path getProjectDir() {
    final Path currentWorkDir = Paths.get("");
    if (currentWorkDir.toAbsolutePath().endsWith("helm")) {
      return currentWorkDir.toAbsolutePath();
    }
    return currentWorkDir.resolve("kit").resolve("helm");
  }
}
