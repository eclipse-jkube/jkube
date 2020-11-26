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
package org.eclipse.jkube.kit.build.service.docker.config.handler.property;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.LogConfiguration;
import org.eclipse.jkube.kit.config.image.RestartPolicy;
import org.eclipse.jkube.kit.config.image.RunImageConfiguration;
import org.eclipse.jkube.kit.config.image.UlimitConfig;
import org.eclipse.jkube.kit.config.image.WaitConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.CleanupMode;
import org.junit.Before;
import org.junit.Test;

import static org.eclipse.jkube.kit.config.image.build.BuildConfiguration.DEFAULT_CLEANUP;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author roland
 * @since 05/12/14
 */

public class PropertyConfigHandlerTest {
    @Mocked
    private JavaProject javaProject;

    private PropertyConfigHandler configHandler;

    private ImageConfiguration imageConfiguration;

    @Before
    public void setUp() throws Exception {
        configHandler = new PropertyConfigHandler();
        imageConfiguration = buildAnUnresolvedImage();
    }

    @Test
    public void testSkipBuild() {
        assertFalse(resolveExternalImageConfig(getSkipTestData(ConfigKey.SKIP_BUILD, false)).getBuildConfiguration().getSkip());
        assertTrue(resolveExternalImageConfig(getSkipTestData(ConfigKey.SKIP_BUILD, true)).getBuildConfiguration().getSkip());

        assertFalse(resolveExternalImageConfig(mergeArrays(getBaseTestData(), new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.FROM), "busybox"})).getBuildConfiguration().getSkip());
    }

    @Test
    public void testSkipRun() {
        assertFalse(resolveExternalImageConfig(getSkipTestData(ConfigKey.SKIP_RUN, false)).getRunConfiguration().skip());
        assertTrue(resolveExternalImageConfig(getSkipTestData(ConfigKey.SKIP_RUN, true)).getRunConfiguration().skip());

        assertFalse(resolveExternalImageConfig(mergeArrays(getBaseTestData(), new String[] {k(ConfigKey.NAME), "image"})).getRunConfiguration().skip());
    }

    @Test
    public void testType() throws Exception {
        assertNotNull(configHandler.getType());
    }

    @Test
    public void testPortsFromConfigAndProperties() {
        imageConfiguration = ImageConfiguration.builder()
                .external(new HashMap<String, String>())
                .build(BuildConfiguration.builder()
                        .ports(Arrays.asList("1234"))
                        .cacheFrom((Arrays.asList("foo/bar:latest")))
                        .build()
                )
                .run(RunImageConfiguration.builder()
                        .ports(Arrays.asList("jolokia.port:1234"))
                        .build()
                )
                .build();

        makeExternalConfigUse(PropertyMode.Override);

        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(
                        "docker.name","demo",
                        "docker.ports.1", "9090",
                        "docker.ports.2", "0.0.0.0:80:80",
                        "docker.from", "busybox"
                ));
        assertEquals(1,configs.size());
        RunImageConfiguration runConfig = configs.get(0).getRunConfiguration();
        List<String> portsAsList = runConfig.getPorts();
        String[] ports = new ArrayList<>(portsAsList).toArray(new String[portsAsList.size()]);
        assertArrayEquals(new String[] {
                "9090",
                "0.0.0.0:80:80",
                "jolokia.port:1234"
        },ports);
        BuildConfiguration buildConfig = configs.get(0).getBuildConfiguration();
        ports = new ArrayList<>(buildConfig.getPorts()).toArray(new String[buildConfig.getPorts().size()]);
        assertArrayEquals(new String[]{"9090", "80", "1234"}, ports);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPropertyMode() {
        makeExternalConfigUse(PropertyMode.Override);
        imageConfiguration.getExternalConfig().put("mode", "invalid");

        resolveImage(imageConfiguration,props());
    }

    @Test
    public void testRunCommands() {
        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(mergeArrays(getBaseTestData(), new String[] {
                        "docker.from", "base",
                        "docker.name","demo",
                        "docker.run.1", "foo",
                        "docker.run.2", "bar",
                        "docker.run.3", "wibble"})
        ));

        assertEquals(1, configs.size());

        BuildConfiguration buildConfig = configs.get(0).getBuildConfiguration();
        String[] runCommands = new ArrayList<>(buildConfig.getRunCmds()).toArray(new String[buildConfig.getRunCmds().size()]);
        assertArrayEquals(new String[]{"xyz", "foo", "bar", "wibble"}, runCommands);
    }

    @Test
    public void testShell() {
        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(mergeArrays(getBaseTestData(), new String[] {
                        "docker.from", "base",
                        "docker.name","demo",
                        "docker.shell", "/bin/sh -c"}))
        );

        assertEquals(1, configs.size());

        BuildConfiguration buildConfig = configs.get(0).getBuildConfiguration();
        String[] shell = new ArrayList<>(buildConfig.getShell().asStrings()).toArray(new String[buildConfig.getShell().asStrings().size()]);
        assertArrayEquals(new String[]{"/bin/sh", "-c"}, shell);
    }

    @Test
    public void testRunCommandsFromPropertiesAndConfig() {
        imageConfiguration = ImageConfiguration.builder()
                .external(new HashMap<String, String>())
                .build(BuildConfiguration.builder()
                        .runCmds(Arrays.asList("some","ignored","value"))
                        .cacheFrom((Arrays.asList("foo/bar:latest")))
                        .build()
                )
                .build();

        makeExternalConfigUse(PropertyMode.Override);

        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(
                        "docker.from", "base",
                        "docker.name","demo",
                        "docker.run.1", "propconf",
                        "docker.run.2", "withrun",
                        "docker.run.3", "used")
        );

        assertEquals(1, configs.size());

        BuildConfiguration buildConfig = configs.get(0).getBuildConfiguration();
        String[] runCommands = new ArrayList<>(buildConfig.getRunCmds()).toArray(new String[buildConfig.getRunCmds().size()]);
        assertArrayEquals(new String[]{"propconf", "withrun", "used"}, runCommands);
    }

    @Test
    public void testRunCommandsFromConfigAndProperties() {
        imageConfiguration = ImageConfiguration.builder()
                .external(externalMode(PropertyMode.Fallback))
                .build(BuildConfiguration.builder()
                        .runCmds(Arrays.asList("some","configured","value"))
                        .cacheFrom((Arrays.asList("foo/bar:latest")))
                        .build()
                )
                .build();

        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(
                        "docker.from", "base",
                        "docker.name","demo",
                        "docker.run.1", "this",
                        "docker.run.2", "is",
                        "docker.run.3", "ignored")
        );

        assertEquals(1, configs.size());

        BuildConfiguration buildConfig = configs.get(0).getBuildConfiguration();
        String[] runCommands = new ArrayList<>(buildConfig.getRunCmds()).toArray(new String[buildConfig.getRunCmds().size()]);
        assertArrayEquals(new String[]{"some", "configured", "value"}, runCommands);
    }

    @Test
    public void testEntrypoint() {
        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(mergeArrays(getBaseTestData(), new String[] {
                        "docker.from", "base",
                        "docker.name","demo",
                        "docker.entrypoint", "/entrypoint.sh --from-property"}))
        );

        assertEquals(1, configs.size());

        BuildConfiguration buildConfig = configs.get(0).getBuildConfiguration();
        assertArrayEquals(new String[]{"/entrypoint.sh", "--from-property"}, buildConfig.getEntryPoint().asStrings().toArray());
    }

    private RunImageConfiguration getRunImageConfiguration(List<ImageConfiguration> configs) {
        assertEquals(1, configs.size());
        return configs.get(0).getRunConfiguration();
    }

    @Test
    public void testBuildFromDockerFileMerged() {
        imageConfiguration = ImageConfiguration.builder()
                .name("myimage")
                .external(externalMode(PropertyMode.Override))
                .build(BuildConfiguration.builder()
                        .dockerFile("/some/path")
                        .cacheFrom((Collections.singletonList("foo/bar:latest")))
                        .build()
                )
                .build();

        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration, props()
        );

        assertEquals(1, configs.size());

        BuildConfiguration buildConfiguration = configs.get(0).getBuildConfiguration();
        assertNotNull(buildConfiguration);
        buildConfiguration.initAndValidate();

        Path absolutePath = Paths.get(".").toAbsolutePath();
        String expectedPath = absolutePath.getRoot() + "some" + File.separator + "path";
        assertEquals(expectedPath, buildConfiguration.getDockerFile().getAbsolutePath());
    }

    @Test
    public void testEnvAndLabels() throws Exception {
        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(mergeArrays(getBaseTestData(), new String[]{
                        "docker.from", "baase",
                        "docker.name", "demo",
                        "docker.env.HOME", "/tmp",
                        "docker.env.root.dir", "/bla",
                        "docker.labels.version", "1.0.0",
                        "docker.labels.blub.bla.foobar", "yep"
                })));

        assertEquals(1,configs.size());
        ImageConfiguration calcConfig = configs.get(0);
        for (Map env : new Map[] { calcConfig.getBuildConfiguration().getEnv(),
                calcConfig.getRunConfiguration().getEnv()}) {
            assertEquals(2,env.size());
            assertEquals("/tmp",env.get("HOME"));
            assertEquals("/bla",env.get("root.dir"));
        }
        for (Map labels : new Map[] { calcConfig.getBuildConfiguration().getLabels(),
                calcConfig.getRunConfiguration().getLabels()}) {
            assertEquals(3, labels.size());
            assertEquals("1.0.0", labels.get("version"));
            assertEquals("yep", labels.get("blub.bla.foobar"));
        }
    }


    @Test
    public void testSpecificEnv() throws Exception {
        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(mergeArrays(getBaseTestData(), new String[] {
                        "docker.from", "baase",
                        "docker.name","demo",
                        "docker.envBuild.HOME", "/tmp",
                        "docker.envRun.root.dir", "/bla"
               })));

        assertEquals(1,configs.size());
        ImageConfiguration calcConfig = configs.get(0);

        Map<String, String> env;

        env = calcConfig.getBuildConfiguration().getEnv();
        assertEquals(1,env.size());
        assertEquals("/tmp",env.get("HOME"));

        env = calcConfig.getRunConfiguration().getEnv();
        assertEquals(1,env.size());
        assertEquals("/bla",env.get("root.dir"));
    }

    @Test
    public void testNoCleanup() throws Exception {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.CLEANUP), "none", k(ConfigKey.FROM), "base" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertEquals(CleanupMode.NONE, config.getBuildConfiguration().cleanupMode());
    }

    @Test
    public void testNoBuildConfig() throws Exception {
        String[] testData = new String[] {k(ConfigKey.NAME), "image" };

        ImageConfiguration config = resolveExternalImageConfig(testData);
        assertNull(config.getBuildConfiguration());
    }

    @Test
    public void testNoCacheDisabled() throws Exception {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.NOCACHE), "false", k(ConfigKey.FROM), "base" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertEquals(false, config.getBuildConfiguration().getNocache());
    }

    @Test
    public void testNoCacheEnabled() throws Exception {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.NOCACHE), "true", k(ConfigKey.FROM), "base" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertEquals(true, config.getBuildConfiguration().getNocache());
    }

    @Test
    public void testCacheFrom() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.CACHEFROM), "foo/bar:latest", k(ConfigKey.FROM), "base"};

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertEquals(Collections.singletonList("foo/bar:latest"), config.getBuildConfiguration().getCacheFrom());
    }

    @Test
    public void testExtractCacheFrom() {
        // Given
        String cacheFrom1 = "foo/bar:latest";
        String cacheFrom2 = "foo/bar1:0.1.0";
        PropertyConfigHandler propertyConfigHandler = new PropertyConfigHandler();

        // When
        List<String> result = propertyConfigHandler.extractCacheFrom(cacheFrom1, cacheFrom2);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(cacheFrom1, result.get(0));
        assertEquals(cacheFrom2, result.get(1));
    }

    @Test
    public void testNoOptimise() throws Exception {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.OPTIMISE), "false", k(ConfigKey.FROM), "base" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertFalse(config.getBuildConfiguration().optimise());
    }

    @Test
    public void testDockerFile() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.DOCKER_FILE), "file", "docker.args.foo", "bar" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertNotNull(config.getBuildConfiguration());
    }

    @Test
    public void testContextDir() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.CONTEXT_DIR), "dir" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertNotNull(config.getBuildConfiguration());
    }

    @Test
    public void testFilter() {
        String filter = "@";
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.FROM), "base", k(ConfigKey.FILTER), filter };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertEquals(filter, config.getBuildConfiguration().getFilter());
    }

    @Test
    public void testCleanupDefault() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.FROM), "base" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertEquals(DEFAULT_CLEANUP, config.getBuildConfiguration().cleanupMode().toParameter());
    }

    @Test
    public void testCleanup() {
        CleanupMode mode = CleanupMode.REMOVE;
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.FROM), "base", k(ConfigKey.CLEANUP), mode.toParameter() };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertEquals(mode, config.getBuildConfiguration().cleanupMode());
    }

    @Test
    public void testResolve() {
        ImageConfiguration resolved = resolveExternalImageConfig(mergeArrays(getBaseTestData(), getTestData()));

        validateBuildConfiguration(resolved.getBuildConfiguration());
        validateRunConfiguration(resolved.getRunConfiguration());
        //validateWaitConfiguraion(resolved.getRunConfiguration().getWaitConfiguration());
    }

    protected void validateEnv(Map<String, String> env) {
        assertTrue(env.containsKey("HOME"));
        assertEquals("/Users/roland", env.get("HOME"));
    }

    private ImageConfiguration buildAnUnresolvedImage() {
        return ImageConfiguration.builder()
                .build(BuildConfiguration.builder().build())
                .external(new HashMap<String, String>())
                .build();
    }

    private Map<String, String> externalMode(PropertyMode mode) {
        Map<String, String> external = new HashMap<>();
        if(mode != null) {
            external.put("type", "properties");
            external.put("mode", mode.name());
        }
        return external;
    }

    private void makeExternalConfigUse(PropertyMode mode) {
        Map<String, String> external = imageConfiguration.getExternalConfig();
        external.put("type", "properties");
        if(mode != null) {
            external.put("mode", mode.name());
        } else {
            external.remove("mode");
        }
    }

    private List<ImageConfiguration> resolveImage(ImageConfiguration image, final Properties properties) {
        //MavenProject project = mock(MavenProject.class);
        //when(project.getProperties()).thenReturn(properties);
        new Expectations() {{
            javaProject.getProperties(); result = properties;
            javaProject.getBaseDirectory(); minTimes = 0; maxTimes = 1; result = new File("./");
        }};

        return configHandler.resolve(image, javaProject);
    }

    private ImageConfiguration resolveExternalImageConfig(String[] testData) {
        Map<String, String> external = new HashMap<>();
        external.put("type", "props");

        ImageConfiguration config = ImageConfiguration.builder().name("image")
                .alias("alias")
                .external(external)
                .build(BuildConfiguration.builder().build())
                .build();

        List<ImageConfiguration> resolvedImageConfigs = resolveImage(config, props(testData));
        assertEquals(1, resolvedImageConfigs.size());

        return resolvedImageConfigs.get(0);
    }

    private void validateBuildConfiguration(BuildConfiguration buildConfig) {
        assertEquals(CleanupMode.TRY_TO_REMOVE, buildConfig.cleanupMode());
        assertEquals("command.sh", buildConfig.getCmd().getShell());
        assertEquals("image", buildConfig.getFrom());
        assertEquals("image-ext", buildConfig.getFromExt().get("name"));
        assertEquals("8080", buildConfig.getPorts().get(0));
        assertEquals("8080", buildConfig.getPorts().get(1));
        assertEquals("/vol1", buildConfig.getVolumes().get(0));
        assertEquals("/foo", buildConfig.getVolumes().get(1));
        assertEquals("fabric8io@redhat.com",buildConfig.getMaintainer());
        assertNull(buildConfig.getNocache());
        assertEquals("Always", buildConfig.getImagePullPolicy());

        validateEnv(buildConfig.getEnv());
        validateLabels(buildConfig.getLabels());
        validateArgs(buildConfig.getArgs());
        validateBuildOptions(buildConfig.getBuildOptions());
        /*
         * validate only the descriptor is required and defaults are all used, 'testAssembly' validates
         * all options can be set
         */
        AssemblyConfiguration assemblyConfig = buildConfig.getAssembly();
        assertNull(assemblyConfig.getUser());
        assertNull(assemblyConfig.getExportTargetDir());
    }

    private void validateArgs(Map<String, String> args) {
        assertEquals("http://proxy",args.get("PROXY"));
    }

    private void validateLabels(Map<String, String> labels) {
        assertEquals("Hello\"World",labels.get("com.acme.label"));
    }

    private void validateBuildOptions(Map<String,String> buildOptions) {
        assertEquals("2147483648", buildOptions.get("shmsize"));
    }

    protected void validateRunConfiguration(RunImageConfiguration runConfig) {
        assertEquals(a("/foo", "/tmp:/tmp"), runConfig.getVolumeConfiguration().getBind());
        assertEquals(a("CAP"), runConfig.getCapAdd());
        assertEquals(a("CAP"), runConfig.getCapDrop());
        assertEquals(a("seccomp=unconfined"), runConfig.getSecurityOpts());
        assertEquals("command.sh", runConfig.getCmd().getShell());
        assertEquals(a("8.8.8.8"), runConfig.getDns());
        assertEquals("host",runConfig.getNetworkingConfig().getStandardMode(null));
        assertEquals(a("example.com"), runConfig.getDnsSearch());
        assertEquals("domain.com", runConfig.getDomainname());
        assertEquals("entrypoint.sh", runConfig.getEntrypoint().getShell());
        assertEquals(a("localhost:127.0.0.1"), runConfig.getExtraHosts());
        assertEquals("subdomain", runConfig.getHostname());
        assertEquals(a("redis"), runConfig.getLinks());
        assertEquals((Long) 1L, runConfig.getMemory());
        assertEquals((Long) 1L, runConfig.getMemorySwap());
        assertEquals((Long) 1000000000L, runConfig.getCpus());
        assertEquals((Long) 1L, runConfig.getCpuShares());
        assertEquals("0,1", runConfig.getCpuSet());
        assertEquals("/tmp/envProps.txt",runConfig.getEnvPropertyFile());
        assertEquals("/tmp/props.txt", runConfig.getPortPropertyFile());
        assertEquals("8081:8080", runConfig.getPorts().get(0));
        assertEquals(true, runConfig.getPrivileged());
        assertEquals("tomcat", runConfig.getUser());
        assertEquals(a("from"), runConfig.getVolumeConfiguration().getFrom());
        assertEquals("foo", runConfig.getWorkingDir());
        assertNotNull( runConfig.getUlimits());
        assertEquals(4, runConfig.getUlimits().size());
        assertUlimitEquals(ulimit("memlock",10,10),runConfig.getUlimits().get(0));
        assertUlimitEquals(ulimit("memlock",null,-1),runConfig.getUlimits().get(1));
        assertUlimitEquals(ulimit("memlock",1024,null),runConfig.getUlimits().get(2));
        assertUlimitEquals(ulimit("memlock",2048,null),runConfig.getUlimits().get(3));
        assertEquals("/var/lib/mysql:10m", runConfig.getTmpfs().get(0));
        assertEquals(1, runConfig.getTmpfs().size());
        assertEquals("Never", runConfig.getImagePullPolicy());
        assertEquals(true, runConfig.getReadOnly());
        assertEquals(true, runConfig.getAutoRemove());

        validateEnv(runConfig.getEnv());

        // not sure it's worth it to implement 'equals/hashcode' for these
        RestartPolicy policy = runConfig.getRestartPolicy();
        assertEquals("on-failure", policy.getName());
        assertEquals(1, policy.getRetry());

        WaitConfiguration wait = runConfig.getWait();
        assertEquals("http://foo.com", wait.getUrl());
        assertEquals("pattern", wait.getLog());
        assertEquals("post_start_command", wait.getExec().getPostStart());
        assertEquals("pre_stop_command", wait.getExec().getPreStop());
        assertTrue(wait.getExec().isBreakOnError());
        assertEquals(5, wait.getTime().intValue());
        assertTrue(wait.getHealthy());
        assertEquals(0, wait.getExit().intValue());

        LogConfiguration config = runConfig.getLog();
        assertEquals("green", config.getColor());
        assertTrue(config.isEnabled());
        assertEquals("SRV", config.getPrefix());
        assertEquals("iso8601", config.getDate());
        assertEquals("json",config.getDriver().getName());
        assertEquals(2, config.getDriver().getOpts().size());
        assertEquals("1024", config.getDriver().getOpts().get("max-size"));
        assertEquals("10", config.getDriver().getOpts().get("max-file"));
    }

    private UlimitConfig ulimit(String name, Integer hard, Integer soft) {
        return new UlimitConfig(name, hard, soft);
    }

    private Properties props(String ... args) {
        Properties ret = new Properties();
        for (int i = 0; i < args.length; i += 2) {
            ret.setProperty(args[i], args[i + 1]);
        }
        return ret;
    }

    private String[] getTestAssemblyData() {
        return new String[] {
                k(ConfigKey.FROM), "busybox",
                k(ConfigKey.ASSEMBLY_BASEDIR), "/basedir",
                k(ConfigKey.ASSEMBLY_USER), "user",
                k(ConfigKey.NAME), "image",
        };
    }

    private String[] getTestData() {
        return new String[] {
                k(ConfigKey.ALIAS), "alias",
                k(ConfigKey.BIND) + ".1", "/foo",
                k(ConfigKey.BIND) + ".2", "/tmp:/tmp",
                k(ConfigKey.CAP_ADD) + ".1", "CAP",
                k(ConfigKey.CAP_DROP) + ".1", "CAP",
                k(ConfigKey.SECURITY_OPTS) + ".1", "seccomp=unconfined",
                k(ConfigKey.CPUS), "1000000000",
                k(ConfigKey.CPUSET), "0,1",
                k(ConfigKey.CPUSHARES), "1",
                k(ConfigKey.CMD), "command.sh",
                k(ConfigKey.DNS) + ".1", "8.8.8.8",
                k(ConfigKey.NET), "host",
                k(ConfigKey.DNS_SEARCH) + ".1", "example.com",
                k(ConfigKey.DOMAINNAME), "domain.com",
                k(ConfigKey.ENTRYPOINT), "entrypoint.sh",
                k(ConfigKey.ENV) + ".HOME", "/Users/roland",
                k(ConfigKey.ARGS) + ".PROXY", "http://proxy",
                k(ConfigKey.LABELS) + ".com.acme.label", "Hello\"World",
                k(ConfigKey.BUILD_OPTIONS) + ".shmsize", "2147483648",
                k(ConfigKey.ENV_PROPERTY_FILE), "/tmp/envProps.txt",
                k(ConfigKey.EXTRA_HOSTS) + ".1", "localhost:127.0.0.1",
                k(ConfigKey.FROM), "image",
                k(ConfigKey.FROM_EXT) + ".name", "image-ext",
                k(ConfigKey.FROM_EXT) + ".kind", "kind",
                k(ConfigKey.HOSTNAME), "subdomain",
                k(ConfigKey.LINKS) + ".1", "redis",
                k(ConfigKey.MAINTAINER), "fabric8io@redhat.com",
                k(ConfigKey.MEMORY), "1",
                k(ConfigKey.MEMORY_SWAP), "1",
                k(ConfigKey.NAME), "image",
                k(ConfigKey.PORT_PROPERTY_FILE), "/tmp/props.txt",
                k(ConfigKey.PORTS) + ".1", "8081:8080",
                k(ConfigKey.PRIVILEGED), "true",
                k(ConfigKey.REGISTRY), "registry",
                k(ConfigKey.RESTART_POLICY_NAME), "on-failure",
                k(ConfigKey.RESTART_POLICY_RETRY), "1",
                k(ConfigKey.USER), "tomcat",
                k(ConfigKey.ULIMITS)+".1", "memlock=10:10",
                k(ConfigKey.ULIMITS)+".2", "memlock=:-1",
                k(ConfigKey.ULIMITS)+".3", "memlock=1024:",
                k(ConfigKey.ULIMITS)+".4", "memlock=2048",
                k(ConfigKey.VOLUMES) + ".1", "/foo",
                k(ConfigKey.VOLUMES_FROM) + ".1", "from",
                k(ConfigKey.WAIT_EXEC_PRE_STOP), "pre_stop_command",
                k(ConfigKey.WAIT_EXEC_POST_START), "post_start_command",
                k(ConfigKey.WAIT_EXEC_BREAK_ON_ERROR), "true",
                k(ConfigKey.WAIT_LOG), "pattern",
                k(ConfigKey.WAIT_HEALTHY), "true",
                k(ConfigKey.WAIT_TIME), "5",
                k(ConfigKey.WAIT_EXIT), "0",
                k(ConfigKey.WAIT_URL), "http://foo.com",
                k(ConfigKey.LOG_PREFIX), "SRV",
                k(ConfigKey.LOG_COLOR), "green",
                k(ConfigKey.LOG_ENABLED), "true",
                k(ConfigKey.LOG_DATE), "iso8601",
                k(ConfigKey.LOG_DRIVER_NAME), "json",
                k(ConfigKey.LOG_DRIVER_OPTS) + ".max-size", "1024",
                k(ConfigKey.LOG_DRIVER_OPTS) + ".max-file", "10",
                k(ConfigKey.WORKING_DIR), "foo",
                k(ConfigKey.TMPFS) + ".1", "/var/lib/mysql:10m",
                k(ConfigKey.IMAGE_PULL_POLICY_BUILD), "Always",
                k(ConfigKey.IMAGE_PULL_POLICY_RUN), "Never",
                k(ConfigKey.READ_ONLY), "true",
                k(ConfigKey.AUTO_REMOVE), "true",
        };
    }

    private String[] getSkipTestData(ConfigKey key, boolean value) {
        String[] baseData = getBaseTestData();
        String[] data = new String[baseData.length + 6];
        System.arraycopy(baseData, 0, data, 0, baseData.length);

        data[baseData.length] = k(ConfigKey.NAME);
        data[baseData.length + 1] = "image";
        data[baseData.length + 2] = k(key);
        data[baseData.length + 3] = String.valueOf(value);
        data[baseData.length + 4] = k(ConfigKey.FROM);
        data[baseData.length + 5] = "busybox";
        return data;
    }

    private String k(ConfigKey from) {
        return from.asPropertyKey();
    }

    private void assertUlimitEquals(UlimitConfig expected, UlimitConfig actual){
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getSoft(), actual.getSoft());
        assertEquals(expected.getHard(), actual.getHard());
    }

    private List<String> a(String ... args) {
        return Arrays.asList(args);
    }

    private String[] getBaseTestData() {
        return new String[] {
                "docker.name", "docker-project",
                "docker.from", "docker-from:ladocker",
                "docker.cachefrom", "docker-image:ladocker",
                "docker.args.foo", "bar",
                "docker.labels.foo", "l1",
                "docker.ports.p1", "8080",
                "docker.run.0", "xyz",
                "docker.volumes.0", "/vol1",
                "docker.tags.0", "0.1.0"
        };
    }

    private String[] mergeArrays(String[] a, String[] b) {
        String[] mergedArr = new String[a.length + b.length];
        int mergedIndex = 0;
        for (String s : a) mergedArr[mergedIndex++] = s;
        for (String s : b) mergedArr[mergedIndex++] = s;
        return mergedArr;
    }
}