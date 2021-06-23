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
package org.eclipse.jkube.wildfly.generator;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.common.JavaProject;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jkube.generator.api.support.AbstractPortsExtractor;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.AssemblyFileSet;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import static org.eclipse.jkube.wildfly.jar.generator.WildflyGeneratorCommon.JBOSS_MAVEN_DIST;
import static org.eclipse.jkube.wildfly.jar.generator.WildflyGeneratorCommon.JBOSS_MAVEN_REPO;
import static org.eclipse.jkube.wildfly.jar.generator.WildflyGeneratorCommon.PLUGIN_OPTIONS;
import static org.eclipse.jkube.wildfly.jar.generator.WildflyGeneratorCommon.SERVER_OPTION;
import org.eclipse.jkube.wildfly.jar.enricher.WildflyJARHealthCheckEnricher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author roland
 */
public class WildflyGeneratorTest {

    @Mocked
    private GeneratorContext context;

    @Mocked
    private JavaProject project;

    @Test
    public void notApplicable() throws IOException {
        WildflyGenerator generator = new WildflyGenerator(createGeneratorContext(Collections.emptyMap()));
        assertFalse(generator.isApplicable((List<ImageConfiguration>) Collections.EMPTY_LIST));
    }

    @Test
    public void applicable() throws IOException {
        Map<String, Object> options = new HashMap<>();
        options.put(SERVER_OPTION, null);
        WildflyGenerator generator = new WildflyGenerator(createGeneratorContext(options));
        assertTrue(generator.isApplicable((List<ImageConfiguration>) Collections.EMPTY_LIST));
    }
    
    @Test
    public void applicable2() throws IOException {
        Map<String, Object> options = new HashMap<>();
        Map<String, Object> serverOptions = new HashMap<>();
        serverOptions.put("enabled", "true");
        options.put(SERVER_OPTION, serverOptions);
        WildflyGenerator generator = new WildflyGenerator(createGeneratorContext(options));
        assertTrue(generator.isApplicable((List<ImageConfiguration>) Collections.EMPTY_LIST));
    }

    @Test
    public void notApplicable2() throws IOException {
        Map<String, Object> options = new HashMap<>();
        Map<String, Object> serverOptions = new HashMap<>();
        serverOptions.put("enabled", "false");
        options.put(SERVER_OPTION, serverOptions);
        WildflyGenerator generator = new WildflyGenerator(createGeneratorContext(options));
        assertFalse(generator.isApplicable((List<ImageConfiguration>) Collections.EMPTY_LIST));
    }
    
    @Test
    public void customize() throws IOException {
        Map<String, Object> options = new HashMap<>();
        options.put(SERVER_OPTION, null);
        WildflyGenerator generator = new WildflyGenerator(createGeneratorContext(options));
        final List<ImageConfiguration> configs = new ArrayList<>();
        generator.customize(configs, true);
        assertEquals(1, configs.size());
        assertEquals(1, configs.get(0).getBuildConfiguration().getPorts().size());
        assertTrue(configs.get(0).getBuildConfiguration().getPorts().contains("8080"));
    }
    
    @Test
    public void webPort() throws IOException {
        Map<String, Object> options = new HashMap<>();
        options.put(SERVER_OPTION, null);
        final Map<String, Map<String, Object>> config = createFakeConfig("{"
                + "\"webPort\":\"9999\""
                + "}");
        WildflyGenerator generator = new WildflyGenerator(contextWithConfig(options, config));
        final List<ImageConfiguration> configs = new ArrayList<>();
        generator.customize(configs, true);
        assertEquals(1, configs.size());
        assertEquals(1, configs.get(0).getBuildConfiguration().getPorts().size());
        assertTrue(configs.get(0).getBuildConfiguration().getPorts().contains("9999"));
    }

    @Test
    public void serverDir() throws IOException {
        Path tmpDir = Files.createTempDirectory("wildfly-test-project1");
        Path targetDir = tmpDir.resolve("target");
        Path serverDir = targetDir.resolve("foo");
        Files.createDirectories(serverDir);
        Map<String, Object> options = new HashMap<>();
        options.put(SERVER_OPTION, null);
        final Map<String, Map<String, Object>> config = createFakeConfig("{"
                + "\"serverDirectoryName\":\"foo\""
                + "}");

        WildflyGenerator generator = new WildflyGenerator(contextWithServerDirectory(options, targetDir, config));
        AssemblyConfiguration ac = generator.createServerAssembly();
        assertEquals(ac.getTargetDir(), "/opt");
        assertEquals(ac.getUser(), "jboss:root:jboss");
    }
    
    @Test
    public void serverDirFail() throws IOException {
        Path tmpDir = Files.createTempDirectory("wildfly-test-project2");
        Path targetDir = tmpDir.resolve("target");
        Path serverDir = targetDir.resolve("foo");
        Files.createDirectories(serverDir);
        Files.createFile(serverDir.resolve("jboss-modules.jar"));
        Files.createDirectories(serverDir.resolve("bin"));
        Files.createDirectories(serverDir.resolve("standalone"));
        Files.createDirectories(serverDir.resolve("modules"));
        Map<String, Object> options = new HashMap<>();
        options.put(SERVER_OPTION, null);
        final Map<String, Map<String, Object>> config = createFakeConfig("{"
                + "\"serverDirectoryName\":\"bar\""
                + "}");

        WildflyGenerator generator = new WildflyGenerator(contextWithServerDirectory(options, targetDir, config));
        boolean failure = false;
        try {
            AssemblyConfiguration ac = generator.createServerAssembly();
            failure = true;
        } catch (Throwable ex) {
            // XXX OK Expected.
        }
        if (failure) {
            throw new RuntimeException("Test should have failed");
        }
    }

    @Test
    public void scanServerDir() throws IOException {
        Path tmpDir = Files.createTempDirectory("wildfly-test-project3");
        Path targetDir = tmpDir.resolve("target");
        Path serverDir = targetDir.resolve("foo");
        Files.createDirectories(serverDir);
        Files.createFile(serverDir.resolve("jboss-modules.jar"));
        Files.createDirectories(serverDir.resolve("bin"));
        Files.createDirectories(serverDir.resolve("standalone"));
        Files.createDirectories(serverDir.resolve("modules"));
        Map<String, Object> options = new HashMap<>();
        options.put(SERVER_OPTION, null);
        final Map<String, Map<String, Object>> config = Collections.emptyMap();

        WildflyGenerator generator = new WildflyGenerator(contextWithServerDirectory(options, targetDir, config));
        AssemblyConfiguration ac = generator.createServerAssembly();
        assertEquals(ac.getTargetDir(), "/opt");
        assertEquals(ac.getUser(), "jboss:root:jboss");
    }

    @Test
    public void scanServerDirFail() throws IOException {
        Path tmpDir = Files.createTempDirectory("wildfly-test-project4");
        Path targetDir = tmpDir.resolve("target");
        Path serverDir = targetDir.resolve("foo");
        Files.createDirectories(serverDir);
        Files.createDirectories(serverDir.resolve("bin"));
        Files.createDirectories(serverDir.resolve("standalone"));
        Files.createDirectories(serverDir.resolve("modules"));
        Map<String, Object> options = new HashMap<>();
        options.put(SERVER_OPTION, null);
        final Map<String, Map<String, Object>> config = Collections.emptyMap();

        WildflyGenerator generator = new WildflyGenerator(contextWithServerDirectory(options, targetDir, config));
        boolean failure = false;
        try {
            AssemblyConfiguration ac = generator.createServerAssembly();
            failure = true;
        } catch (Throwable ex) {
            // XXX OK Expected.
        }
        if (failure) {
            throw new RuntimeException("Test should have failed");
        }
    }
    
    @Test
    public void scanServerDirFail2() throws IOException {
        Path tmpDir = Files.createTempDirectory("wildfly-test-project5");
        Path targetDir = tmpDir.resolve("target");
        Path serverDir1 = targetDir.resolve("foo");
        Files.createDirectories(serverDir1);
        Files.createFile(serverDir1.resolve("jboss-modules.jar"));
        Files.createDirectories(serverDir1.resolve("bin"));
        Files.createDirectories(serverDir1.resolve("standalone"));
        Files.createDirectories(serverDir1.resolve("modules"));
        Path serverDir2 = targetDir.resolve("bar");
        Files.createDirectories(serverDir2);
        Files.createFile(serverDir2.resolve("jboss-modules.jar"));
        Files.createDirectories(serverDir2.resolve("bin"));
        Files.createDirectories(serverDir2.resolve("standalone"));
        Files.createDirectories(serverDir2.resolve("modules"));
        Map<String, Object> options = new HashMap<>();
        options.put(SERVER_OPTION, null);
        final Map<String, Map<String, Object>> config = Collections.emptyMap();

        WildflyGenerator generator = new WildflyGenerator(contextWithServerDirectory(options, targetDir, config));
        boolean failure = false;
        try {
            AssemblyConfiguration ac = generator.createServerAssembly();
            failure = true;
        } catch (Throwable ex) {
            // XXX OK Expected.
        }
        if (failure) {
            throw new RuntimeException("Test should have failed");
        }
    }

    @Test
    public void getOptions() throws IOException {
        WildflyGenerator generator = new WildflyGenerator(createGeneratorContext(Collections.emptyMap()));
        List<String> extraOptions = generator.getOptions();
        assertNotNull(extraOptions);
        assertEquals(0, extraOptions.size());
    }

    @Test
    public void slimServer(@Mocked final JavaProject project) throws IOException {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap();
        options.put(SERVER_OPTION, null);
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_DIST, null);
        pluginOptions.put(JBOSS_MAVEN_REPO, "target" + File.separator + "myrepo");
        //
        Path tmpDir = Files.createTempDirectory("bootable-jar-test-project");
        Path targetDir = tmpDir.resolve("target");
        Path repoDir = targetDir.resolve("myrepo");
        Files.createDirectories(repoDir);
        try {
            GeneratorContext ctx = contextForSlimServer(project, options, tmpDir);
            WildflyGenerator generator = new WildflyGenerator(ctx);
            List<String> extraOptions = generator.getOptions();
            assertNotNull(extraOptions);
            assertEquals(1, extraOptions.size());
            assertEquals("-Dmaven.repo.local=/opt/server-maven-repo", extraOptions.get(0));
            List<AssemblyFileSet> files = generator.getAdditionalFiles();
            assertFalse(files.isEmpty());
            AssemblyFileSet set = files.get(files.size() - 1);
            assertEquals(repoDir.toFile(), set.getDirectory());
            assertEquals(1, set.getIncludes().size());
            assertEquals("**", set.getIncludes().get(0));
        } finally {
            Files.delete(repoDir);
            Files.delete(targetDir);
            Files.delete(tmpDir);
        }
    }

    @Test
    public void slimServerAbsoluteDir(@Mocked final JavaProject project) throws IOException {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap();
        Path tmpDir = Files.createTempDirectory("bootable-jar-test-project2");
        Path targetDir = tmpDir.resolve("target");
        Path repoDir = targetDir.resolve("myrepo");
        Files.createDirectories(repoDir);
        options.put(SERVER_OPTION, null);
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_DIST, null);
        pluginOptions.put(JBOSS_MAVEN_REPO, repoDir.toString());
        try {
            GeneratorContext ctx = contextForSlimServer(project, options, null);
            WildflyGenerator generator = new WildflyGenerator(ctx);
            List<String> extraOptions = generator.getOptions();
            assertNotNull(extraOptions);
            assertEquals(1, extraOptions.size());
            assertEquals("-Dmaven.repo.local=/opt/server-maven-repo", extraOptions.get(0));
            List<AssemblyFileSet> files = generator.getAdditionalFiles();
            assertFalse(files.isEmpty());
            AssemblyFileSet set = files.get(files.size() - 1);
            assertEquals(repoDir.toFile(), set.getDirectory());
            assertEquals(1, set.getIncludes().size());
            assertEquals("**", set.getIncludes().get(0));
        } finally {
            Files.delete(repoDir);
            Files.delete(targetDir);
            Files.delete(tmpDir);
        }
    }

    @Test
    public void slimServerNoDir(@Mocked final JavaProject project) throws Exception {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap();
        Path tmpDir = Files.createTempDirectory("bootable-jar-test-project2");
        Path targetDir = tmpDir.resolve("target");
        Path repoDir = targetDir.resolve("myrepo");
        options.put(SERVER_OPTION, null);
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_DIST, null);
        pluginOptions.put(JBOSS_MAVEN_REPO, repoDir.toString());
        try {
            GeneratorContext ctx = contextForSlimServer(project, options, null);
            WildflyGenerator generator = new WildflyGenerator(ctx);
            List<String> extraOptions = generator.getOptions();
            assertNotNull(extraOptions);
            assertEquals(1, extraOptions.size());
            assertEquals("-Dmaven.repo.local=/opt/server-maven-repo", extraOptions.get(0));
            Exception result = assertThrows(Exception.class, () -> {
                generator.getAdditionalFiles();
                fail("Test should have failed, no directory for maven repo");
            });
            assertEquals("Error, WildFly bootable JAR generator can't retrieve "
                    + "generated maven local cache, directory " + repoDir + " doesn't exist.", result.getMessage());
        } finally {
            Files.delete(tmpDir);
        }
    }

    @Test
    public void slimServerNoRepo(@Mocked final JavaProject project) throws IOException {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap();
        options.put(SERVER_OPTION, null);
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_DIST, null);
        GeneratorContext ctx = contextForSlimServer(project, options, null);
        WildflyGenerator generator = new WildflyGenerator(ctx);
        List<String> extraOptions = generator.getOptions();
        assertNotNull(extraOptions);
        assertEquals(0, extraOptions.size());
    }

    @Test
    public void slimServerNoDist(@Mocked final JavaProject project) throws IOException {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap();
        options.put(SERVER_OPTION, null);
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_REPO, "myrepo");
        GeneratorContext ctx = contextForSlimServer(project, options, null);
        WildflyGenerator generator = new WildflyGenerator(ctx);
        List<String> extraOptions = generator.getOptions();
        assertNotNull(extraOptions);
        assertEquals(0, extraOptions.size());
    }

    @Test
    public void slimServerFalseDist(@Mocked final JavaProject project) throws IOException {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap();
        options.put(SERVER_OPTION, null);
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_REPO, "myrepo");
        pluginOptions.put(JBOSS_MAVEN_DIST, "false");
        GeneratorContext ctx = contextForSlimServer(project, options, null);
        WildflyGenerator generator = new WildflyGenerator(ctx);
        List<String> extraOptions = generator.getOptions();
        assertNotNull(extraOptions);
        assertEquals(0, extraOptions.size());
    }

    @Test
    public void slimServerTrueDist(@Mocked final JavaProject project) throws IOException {
        Map<String, Object> options = new HashMap<>();
        Map<String, String> pluginOptions = new HashMap();
        options.put(SERVER_OPTION, null);
        options.put(PLUGIN_OPTIONS, pluginOptions);
        pluginOptions.put(JBOSS_MAVEN_REPO, "myrepo");
        pluginOptions.put(JBOSS_MAVEN_DIST, "true");
        GeneratorContext ctx = contextForSlimServer(project, options, null);
        WildflyGenerator generator = new WildflyGenerator(ctx);
        List<String> extraOptions = generator.getOptions();
        assertNotNull(extraOptions);
        assertEquals(1, extraOptions.size());
        assertEquals("-Dmaven.repo.local=/opt/server-maven-repo", extraOptions.get(0));
    }

    private GeneratorContext contextForSlimServer(JavaProject project, Map<String, Object> bootableJarconfig, Path dir) {
        Plugin plugin
                = Plugin.builder().artifactId(WildflyJARHealthCheckEnricher.BOOTABLE_JAR_ARTIFACT_ID).
                        groupId(WildflyJARHealthCheckEnricher.BOOTABLE_JAR_GROUP_ID).configuration(bootableJarconfig).build();
        List<Plugin> lst = new ArrayList<>();
        lst.add(plugin);
        ProcessorConfig c = new ProcessorConfig(null, null, Collections.emptyMap());
        if (dir == null) {
            new Expectations() {
                {
                    project.getPlugins();
                    result = lst;
                    context.getProject();
                    result = project;
                }
            };
        } else {
            new Expectations() {
                {
                    project.getPlugins();
                    result = lst;
                    project.getBaseDirectory();
                    result = dir.toFile();
                    context.getProject();
                    result = project;
                }
            };
        }
        return context;
    }

    private GeneratorContext createGeneratorContext(Map<String, Object> config) throws IOException {
        Plugin plugin
                = Plugin.builder().artifactId(WildflyJARHealthCheckEnricher.BOOTABLE_JAR_ARTIFACT_ID).
                        groupId(WildflyJARHealthCheckEnricher.BOOTABLE_JAR_GROUP_ID).configuration(config).build();
        List<Plugin> lst = new ArrayList<>();
        lst.add(plugin);
        new Expectations() {
            {
                project.getPlugins();
                result = lst;
                context.getProject();
                result = project;
                project.getVersion();
                result = "1.0.0";
                minTimes = 0;
            }
        };
        return context;
    }

    private Map<String, Map<String, Object>> createFakeConfig(String config) {
        try {
            Map<String, Object> wildflyMap = AbstractPortsExtractor.JSON_MAPPER.readValue(config, Map.class);
            Map<String, Map<String, Object>> generatorConfigMap = new HashMap<>();
            generatorConfigMap.put("wildfly", wildflyMap);

            Map<String, Object> generatorMap = new HashMap<>();
            generatorMap.put("config", generatorConfigMap);

            Map<String, Object> pluginConfigurationMap = new HashMap<>();
            pluginConfigurationMap.put("generator", generatorMap);

            return generatorConfigMap;
        } catch (JsonProcessingException jsonProcessingException) {
            jsonProcessingException.printStackTrace();
        }
        return null;
    }

    private GeneratorContext contextWithServerDirectory(Map<String, Object> bootableJarconfig, Path dir,
            Map<String, Map<String, Object>> jkubeConfig) {
        Plugin plugin
                = Plugin.builder().artifactId(WildflyJARHealthCheckEnricher.BOOTABLE_JAR_ARTIFACT_ID).
                        groupId(WildflyJARHealthCheckEnricher.BOOTABLE_JAR_GROUP_ID).configuration(bootableJarconfig).build();
        List<Plugin> lst = new ArrayList<>();
        lst.add(plugin);
        ProcessorConfig c = new ProcessorConfig(null, null, jkubeConfig);
        new Expectations() {
            {
                project.getPlugins();
                result = lst;
                project.getBuildDirectory();
                result = dir.toFile();
                context.getProject();
                result = project;
                context.getConfig();
                result = c;
            }
        };
        return context;
    }
    
    private GeneratorContext contextWithConfig(Map<String, Object> bootableJarconfig,
            Map<String, Map<String, Object>> jkubeConfig) {
        Plugin plugin
                = Plugin.builder().artifactId(WildflyJARHealthCheckEnricher.BOOTABLE_JAR_ARTIFACT_ID).
                        groupId(WildflyJARHealthCheckEnricher.BOOTABLE_JAR_GROUP_ID).configuration(bootableJarconfig).build();
        List<Plugin> lst = new ArrayList<>();
        lst.add(plugin);
        ProcessorConfig c = new ProcessorConfig(null, null, jkubeConfig);
        new Expectations() {
            {
                project.getPlugins();
                result = lst;
                context.getProject();
                result = project;
                context.getConfig();
                result = c;
                project.getVersion();
                result = "1.0.0";
                minTimes = 0;
            }
        };
        return context;
    }
}
