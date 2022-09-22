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
package org.eclipse.jkube.wildfly.jar.generator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.eclipse.jkube.wildfly.jar.generator.WildflyJARGenerator.JBOSS_MAVEN_DIST;
import static org.eclipse.jkube.wildfly.jar.generator.WildflyJARGenerator.JBOSS_MAVEN_REPO;
import static org.eclipse.jkube.wildfly.jar.generator.WildflyJARGenerator.PLUGIN_OPTIONS;

/**
 * @author roland
 */

class WildflyJARGeneratorTest {

    @TempDir
    Path temporaryFolder;

    @Mocked
    private GeneratorContext context;

    @Mocked
    private JavaProject project;

    @Test
    void notApplicable() {
        WildflyJARGenerator generator = new WildflyJARGenerator(createGeneratorContext());
        assertThat(generator.isApplicable(Collections.emptyList())).isFalse();
    }

    // To be revisited if we enable jolokia and prometheus.
    @Test
    void getEnv() {
        WildflyJARGenerator generator = new WildflyJARGenerator(createGeneratorContext());
        Map<String, String> extraEnv = generator.getEnv(true);
        assertThat(extraEnv).isNotNull().hasSize(4);
    }
    
    @Test
    void getExtraOptions() {
        WildflyJARGenerator generator = new WildflyJARGenerator(createGeneratorContext());
        List<String> extraOptions = generator.getExtraJavaOptions();
        assertThat(extraOptions).isNotNull()
            .singleElement().isEqualTo("-Djava.net.preferIPv4Stack=true");
    }
    
    @Test
    void slimServer(@Mocked final JavaProject project) throws IOException {
      Map<String, Object> options = new HashMap<>();
      Map<String, String> pluginOptions = new HashMap<>();
      options.put(PLUGIN_OPTIONS, pluginOptions);
      pluginOptions.put(JBOSS_MAVEN_DIST, null);
      pluginOptions.put(JBOSS_MAVEN_REPO, "target" + File.separator + "myrepo");

      Path targetDir = Files.createDirectory(temporaryFolder.resolve("target"));
      Files.createDirectory(targetDir.resolve("myrepo"));

      GeneratorContext ctx = contextForSlimServer(project, options, temporaryFolder);
      WildflyJARGenerator generator = new WildflyJARGenerator(ctx);

      List<String> extraOptions = generator.getExtraJavaOptions();
      assertThat(extraOptions).isNotNull()
          .hasSize(2)
          .satisfies(option -> assertThat(option).first()
              .isEqualTo("-Djava.net.preferIPv4Stack=true"))
          .satisfies(option -> assertThat(option).last()
              .isEqualTo("-Dmaven.repo.local=/deployments/myrepo"));

      List<AssemblyFileSet> fileSets = generator.addAdditionalFiles();
      assertThat(fileSets).isNotEmpty()
          .last()
          .hasFieldOrPropertyWithValue("directory", targetDir.toFile())
          .extracting(AssemblyFileSet::getIncludes).asList()
          .singleElement()
          .isEqualTo("myrepo");
    }
    
    @Test
    void slimServerAbsoluteDir(@Mocked final JavaProject project) throws IOException {
      Map<String, Object> options = new HashMap<>();
      Map<String, String> pluginOptions = new HashMap<>();

      Path targetDir = Files.createDirectory(temporaryFolder.resolve("target"));
      Path repoDir = Files.createDirectory(targetDir.resolve("myrepo"));

      options.put(PLUGIN_OPTIONS, pluginOptions);
      pluginOptions.put(JBOSS_MAVEN_DIST, null);
      pluginOptions.put(JBOSS_MAVEN_REPO, repoDir.toString());

      GeneratorContext ctx = contextForSlimServer(project, options, null);
      WildflyJARGenerator generator = new WildflyJARGenerator(ctx);

      List<String> extraOptions = generator.getExtraJavaOptions();
      assertThat(extraOptions).isNotNull()
          .hasSize(2)
          .satisfies(option -> assertThat(option).first()
              .isEqualTo("-Djava.net.preferIPv4Stack=true"))
          .satisfies(option -> assertThat(option).last()
              .isEqualTo("-Dmaven.repo.local=/deployments/myrepo"));

      List<AssemblyFileSet> fileSets = generator.addAdditionalFiles();
      assertThat(fileSets).isNotEmpty()
          .last()
          .hasFieldOrPropertyWithValue("directory", targetDir.toFile())
          .extracting(AssemblyFileSet::getIncludes).asList()
          .singleElement()
          .isEqualTo("myrepo");
    }
    
    @Test
    void slimServerNoDir(@Mocked final JavaProject project) throws Exception {
      Map<String, Object> options = new HashMap<>();
      Map<String, String> pluginOptions = new HashMap<>();

      Path targetDir = Files.createDirectory(temporaryFolder.resolve("target"));
      Path repoDir = targetDir.resolve("myrepo");

      options.put(PLUGIN_OPTIONS, pluginOptions);
      pluginOptions.put(JBOSS_MAVEN_DIST, null);
      pluginOptions.put(JBOSS_MAVEN_REPO, repoDir.toString());

      GeneratorContext ctx = contextForSlimServer(project, options, null);
      WildflyJARGenerator generator = new WildflyJARGenerator(ctx);

      List<String> extraOptions = generator.getExtraJavaOptions();
      assertThat(extraOptions).isNotNull()
          .hasSize(2)
          .satisfies(option -> assertThat(option).first()
              .isEqualTo("-Djava.net.preferIPv4Stack=true"))
          .satisfies(option -> assertThat(option).last()
              .isEqualTo("-Dmaven.repo.local=/deployments/myrepo"));

      assertThatRuntimeException()
          .isThrownBy(generator::addAdditionalFiles)
          .withMessage("Error, WildFly bootable JAR generator can't retrieve generated maven local cache, directory "
              + repoDir + " doesn't exist.");
    }
    
    @Test
    void slimServerNoRepo(@Mocked final JavaProject project) {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap<>();
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_DIST, null);
        GeneratorContext ctx = contextForSlimServer(project, options, null);
        WildflyJARGenerator generator = new WildflyJARGenerator(ctx);
        List<String> extraOptions = generator.getExtraJavaOptions();
        assertThat(extraOptions).isNotNull()
            .singleElement()
            .isEqualTo("-Djava.net.preferIPv4Stack=true");
    }
    
    @Test
    void slimServerNoDist(@Mocked final JavaProject project) {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap<>();
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_REPO, "myrepo");
        GeneratorContext ctx = contextForSlimServer(project, options, null);
        WildflyJARGenerator generator = new WildflyJARGenerator(ctx);
        List<String> extraOptions = generator.getExtraJavaOptions();
        assertThat(extraOptions).isNotNull()
            .singleElement()
            .isEqualTo("-Djava.net.preferIPv4Stack=true");
    }
    
    @Test
    void slimServerFalseDist(@Mocked final JavaProject project) {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap<>();
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_REPO, "myrepo");
        pluginOptions.put(JBOSS_MAVEN_DIST, "false");
        GeneratorContext ctx = contextForSlimServer(project, options, null);
        WildflyJARGenerator generator = new WildflyJARGenerator(ctx);
        List<String> extraOptions = generator.getExtraJavaOptions();
        assertThat(extraOptions).isNotNull()
            .singleElement()
            .isEqualTo("-Djava.net.preferIPv4Stack=true");
    }
    
    @Test
    void slimServerTrueDist(@Mocked final JavaProject project) {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap<>();
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_REPO, "myrepo");
        pluginOptions.put(JBOSS_MAVEN_DIST, "true");
        GeneratorContext ctx = contextForSlimServer(project, options, null);
        WildflyJARGenerator generator = new WildflyJARGenerator(ctx);
        List<String> extraOptions = generator.getExtraJavaOptions();
        assertThat(extraOptions).isNotNull()
            .hasSize(2)
            .satisfies(o -> assertThat(o).first()
                .isEqualTo("-Djava.net.preferIPv4Stack=true"))
            .satisfies(o -> assertThat(o).last()
                .isEqualTo("-Dmaven.repo.local=/deployments/myrepo"));
    }
    
    private GeneratorContext contextForSlimServer(JavaProject project, Map<String, Object> bootableJarConfig, Path dir) {
      Plugin plugin = Plugin.builder().artifactId("wildfly-jar-maven-plugin").groupId("org.wildfly.plugins")
          .configuration(bootableJarConfig).build();
      List<Plugin> lst = new ArrayList<>();
      lst.add(plugin);

      if (dir == null) {
        new Expectations() {{
            project.getPlugins();
            result = lst;
            context.getProject();
            result = project;
          }};
      } else {
        new Expectations() {{
            project.getPlugins();
            result = lst;
            project.getBaseDirectory();
            result = dir.toFile();
            context.getProject();
            result = project;
          }};
      }
      return context;
    }

    private GeneratorContext createGeneratorContext() {
        new Expectations() {{
            context.getProject(); result = project;
            project.getOutputDirectory(); result = temporaryFolder.toFile();
            project.getPlugins(); result = Collections.emptyList(); minTimes = 0;
            project.getVersion(); result = "1.0.0"; minTimes = 0;
        }};
        return context;
    }
}
