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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import mockit.Expectations;
import mockit.Mocked;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.RunImageConfiguration;
import org.eclipse.jkube.kit.config.image.UlimitConfig;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.CleanupMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.jkube.kit.config.image.build.BuildConfiguration.DEFAULT_CLEANUP;

/**
 * @author roland
 * @since 05/12/14
 */

@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored", "unused"})
class PropertyConfigHandlerTest {
    @Mocked
    private JavaProject javaProject;

    private PropertyConfigHandler configHandler;

    private ImageConfiguration imageConfiguration;

    @BeforeEach
    void setUp() {
        configHandler = new PropertyConfigHandler();
        imageConfiguration = buildAnUnresolvedImage();
    }

    @Test
    void testSkipBuild() {
        assertThat(resolveExternalImageConfig(getSkipTestData(ConfigKey.SKIP_BUILD, false)).getBuildConfiguration().getSkip()).isFalse();
        assertThat(resolveExternalImageConfig(getSkipTestData(ConfigKey.SKIP_BUILD, true)).getBuildConfiguration().getSkip()).isTrue();

        assertThat(resolveExternalImageConfig(mergeArrays(getBaseTestData(), new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.FROM), "busybox"})).getBuildConfiguration().getSkip()).isFalse();
    }

    @Test
    void testSkipRun() {
        assertThat(resolveExternalImageConfig(getSkipTestData(ConfigKey.SKIP_RUN, false)).getRunConfiguration().skip()).isFalse();
        assertThat(resolveExternalImageConfig(getSkipTestData(ConfigKey.SKIP_RUN, true)).getRunConfiguration().skip()).isTrue();

        assertThat(resolveExternalImageConfig(mergeArrays(getBaseTestData(), new String[] {k(ConfigKey.NAME), "image"})).getRunConfiguration().skip()).isFalse();
    }

    @Test
    void testType() {
        assertThat(configHandler.getType()).isNotNull();
    }

    @Test
    void testPortsFromConfigAndProperties() {
        imageConfiguration = ImageConfiguration.builder()
                .external(new HashMap<>())
                .build(BuildConfiguration.builder()
                        .ports(Collections.singletonList("1234"))
                        .addCacheFrom("foo/bar:latest")
                        .build()
                )
                .run(RunImageConfiguration.builder()
                        .ports(Collections.singletonList("jolokia.port:1234"))
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
        assertThat(configs).singleElement()
            .satisfies(config -> assertThat(config)
                .extracting(ImageConfiguration::getRunConfiguration)
                .extracting(RunImageConfiguration::getPorts)
                .asList()
                .containsExactly("9090", "0.0.0.0:80:80", "jolokia.port:1234"))
            .satisfies(config -> assertThat(config)
                .extracting(ImageConfiguration::getBuildConfiguration)
                .extracting(BuildConfiguration::getPorts)
                .asList()
                .containsExactly("9090", "80", "1234"));
    }

    @Test
    void testInvalidPropertyMode() {
        makeExternalConfigUse(PropertyMode.Override);
        imageConfiguration.getExternalConfig().put("mode", "invalid");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> resolveImage(imageConfiguration, props()));
    }

    @Test
    void testRunCommands() {
        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(mergeArrays(getBaseTestData(), new String[] {
                        "docker.from", "base",
                        "docker.name","demo",
                        "docker.run.1", "foo",
                        "docker.run.2", "bar",
                        "docker.run.3", "wibble"})
        ));

        assertThat(configs).singleElement()
                .extracting(ImageConfiguration::getBuildConfiguration)
                .extracting(BuildConfiguration::getRunCmds)
                .asList()
                .containsExactly("xyz", "foo", "bar", "wibble");
    }

    @Test
    void testShell() {
        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(mergeArrays(getBaseTestData(), new String[] {
                        "docker.from", "base",
                        "docker.name","demo",
                        "docker.shell", "/bin/sh -c"}))
        );

        assertThat(configs).singleElement()
                .extracting(ImageConfiguration::getBuildConfiguration)
                .returns(new String[]{"/bin/sh", "-c"}, c -> c.getShell().asStrings().toArray());
    }

    @Test
    void testRunCommandsFromPropertiesAndConfig() {
        imageConfiguration = ImageConfiguration.builder()
                .external(new HashMap<>())
                .build(BuildConfiguration.builder()
                        .runCmds(Arrays.asList("some","ignored","value"))
                        .addCacheFrom("foo/bar:latest")
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

        assertThat(configs).singleElement()
                .extracting(ImageConfiguration::getBuildConfiguration)
                .extracting(BuildConfiguration::getRunCmds)
                .asList()
                .containsExactly("propconf", "withrun", "used");
    }

    @Test
    void testRunCommandsFromConfigAndProperties() {
        imageConfiguration = ImageConfiguration.builder()
                .external(externalMode(PropertyMode.Fallback))
                .build(BuildConfiguration.builder()
                        .runCmds(Arrays.asList("some","configured","value"))
                        .addCacheFrom("foo/bar:latest")
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

        assertThat(configs).singleElement()
                .extracting(ImageConfiguration::getBuildConfiguration)
                .extracting(BuildConfiguration::getRunCmds)
                .asList()
                .containsExactly("some", "configured", "value");
    }

    @Test
    void testEntrypoint() {
        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(mergeArrays(getBaseTestData(), new String[] {
                        "docker.from", "base",
                        "docker.name","demo",
                        "docker.entrypoint", "/entrypoint.sh --from-property"}))
        );

        assertThat(configs).singleElement()
                .extracting(ImageConfiguration::getBuildConfiguration)
                .returns(new String[]{"/entrypoint.sh", "--from-property"}, c -> c.getEntryPoint().asStrings().toArray());
    }

    @Test
    void testBuildFromDockerFileMerged() {

        imageConfiguration = ImageConfiguration.builder()
                .name("myimage")
                .external(externalMode(PropertyMode.Override))
                .build(BuildConfiguration.builder()
                        .dockerFile("/some/path")
                        .addCacheFrom("foo/bar:latest")
                        .build()
                )
                .build();

        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration, props()
        );

        assertThat(configs).hasSize(1);

        BuildConfiguration buildConfiguration = configs.get(0).getBuildConfiguration();
        assertThat(buildConfiguration).isNotNull();
        buildConfiguration.initAndValidate();

        Path absolutePath = Paths.get(".").toAbsolutePath();
        String expectedPath = absolutePath.getRoot() + "some" + File.separator + "path";
        assertThat(buildConfiguration.getDockerFile().getAbsolutePath()).isEqualTo(expectedPath);
    }

    @Test
    void testEnvAndLabels() {
        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(mergeArrays(getBaseTestData(), new String[]{
                        "docker.from", "baase",
                        "docker.name", "demo",
                        "docker.env.HOME", "/tmp",
                        "docker.env.root.dir", "/bla",
                        "docker.labels.version", "1.0.0",
                        "docker.labels.blub.bla.foobar", "yep"
                })));

        assertThat(configs).hasSize(1);
        ImageConfiguration calcConfig = configs.get(0);
        for (Map<String, String> env : new Map[] { calcConfig.getBuildConfiguration().getEnv(),
                calcConfig.getRunConfiguration().getEnv()}) {
          assertThat(env).hasSize(2)
              .contains(
                  entry("HOME", "/tmp"),
                  entry("root.dir", "/bla"));
        }
        for (Map<String, String> labels : new Map[] { calcConfig.getBuildConfiguration().getLabels(),
                calcConfig.getRunConfiguration().getLabels()}) {
          assertThat(labels).hasSize(3)
              .contains(
                  entry("version", "1.0.0"),
                  entry("blub.bla.foobar", "yep"));
        }
    }


    @Test
    void testSpecificEnv() {
        List<ImageConfiguration> configs = resolveImage(
                imageConfiguration,props(mergeArrays(getBaseTestData(), new String[] {
                        "docker.from", "baase",
                        "docker.name","demo",
                        "docker.envBuild.HOME", "/tmp",
                        "docker.envRun.root.dir", "/bla"
               })));

        assertThat(configs).singleElement()
            .satisfies(config -> assertThat(config)
                .extracting(ImageConfiguration::getBuildConfiguration)
                .extracting(BuildConfiguration::getEnv)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .hasSize(1)
                .containsEntry("HOME", "/tmp"))
            .satisfies(config -> assertThat(config)
                .extracting(ImageConfiguration::getRunConfiguration)
                .extracting(RunImageConfiguration::getEnv)
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .hasSize(1)
                .containsEntry("root.dir", "/bla"));
    }

    @Test
    void testNoCleanup() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.CLEANUP), "none", k(ConfigKey.FROM), "base" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertThat(config.getBuildConfiguration().cleanupMode()).isEqualTo(CleanupMode.NONE);
    }

    @Test
    void testNoBuildConfig() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image" };

        ImageConfiguration config = resolveExternalImageConfig(testData);
        assertThat(config.getBuildConfiguration()).isNull();
    }

    @Test
    void testNoCacheDisabled() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.NOCACHE), "false", k(ConfigKey.FROM), "base" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertThat(config.getBuildConfiguration().getNocache()).isFalse();
    }

    @Test
    void testNoCacheEnabled() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.NOCACHE), "true", k(ConfigKey.FROM), "base" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertThat(config.getBuildConfiguration().getNocache()).isTrue();
    }

    @Test
    void testCacheFrom() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.CACHEFROM), "foo/bar:latest", k(ConfigKey.FROM), "base"};

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertThat(config.getBuildConfiguration().getCacheFrom()).isEqualTo(Collections.singletonList("foo/bar:latest"));
    }

    @Test
    void testExtractCacheFrom() {
        // Given
        String cacheFrom1 = "foo/bar:latest";
        String cacheFrom2 = "foo/bar1:0.1.0";
        PropertyConfigHandler propertyConfigHandler = new PropertyConfigHandler();

        // When
        List<String> result = propertyConfigHandler.extractCacheFrom(cacheFrom1, cacheFrom2);

        // Then
        assertThat(result).hasSize(2)
            .containsExactly(cacheFrom1, cacheFrom2);
    }

    @Test
    void testNoOptimise() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.OPTIMISE), "false", k(ConfigKey.FROM), "base" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertThat(config.getBuildConfiguration().optimise()).isFalse();
    }

    @Test
    void testDockerFile() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.DOCKER_FILE), "file", "docker.args.foo", "bar" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertThat(config.getBuildConfiguration()).isNotNull();
    }

    @Test
    void testContextDir() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.CONTEXT_DIR), "dir" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertThat(config.getBuildConfiguration()).isNotNull();
    }

    @Test
    void testFilter() {
        String filter = "@";
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.FROM), "base", k(ConfigKey.FILTER), filter };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertThat(config.getBuildConfiguration().getFilter()).isEqualTo(filter);
    }

    @Test
    void testCleanupDefault() {
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.FROM), "base" };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertThat(config.getBuildConfiguration().cleanupMode().toParameter()).isEqualTo(DEFAULT_CLEANUP);
    }

    @Test
    void testCleanup() {
        CleanupMode mode = CleanupMode.REMOVE;
        String[] testData = new String[] {k(ConfigKey.NAME), "image", k(ConfigKey.FROM), "base", k(ConfigKey.CLEANUP), mode.toParameter() };

        ImageConfiguration config = resolveExternalImageConfig(mergeArrays(getBaseTestData(), testData));
        assertThat(config.getBuildConfiguration().cleanupMode()).isEqualTo(mode);
    }

    @Test
    void testResolve() {
        ImageConfiguration resolved = resolveExternalImageConfig(mergeArrays(getBaseTestData(), getTestData()));

        validateBuildConfiguration(resolved.getBuildConfiguration());
        validateRunConfiguration(resolved.getRunConfiguration());
        //validateWaitConfiguraion(resolved.getRunConfiguration().getWaitConfiguration());
    }

    protected void validateEnv(Map<String, String> env) {
        assertThat(env).containsEntry("HOME", "/Users/roland");
    }

    private ImageConfiguration buildAnUnresolvedImage() {
        return ImageConfiguration.builder()
                .build(BuildConfiguration.builder().build())
                .external(new HashMap<>())
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
        assertThat(resolvedImageConfigs).hasSize(1);

        return resolvedImageConfigs.get(0);
    }

    private void validateBuildConfiguration(BuildConfiguration buildConfig) {
        assertThat(buildConfig)
                .returns(CleanupMode.TRY_TO_REMOVE, BuildConfiguration::cleanupMode)
                .returns("command.sh", c -> c.getCmd().getShell())
                .returns("image", BuildConfiguration::getFrom)
                .returns("image-ext", c -> c.getFromExt().get("name"))
                .returns(a("8080", "8080"), BuildConfiguration::getPorts)
                .returns(a("/vol1", "/foo"), BuildConfiguration::getVolumes)
                .returns("fabric8io@redhat.com", BuildConfiguration::getMaintainer)
                .returns(null, BuildConfiguration::getNocache)
                .returns("Always", BuildConfiguration::getImagePullPolicy)
                .returns(null, c -> c.getAssembly().getUser())
                .returns(null, c -> c.getAssembly().getExportTargetDir());
        /*
         * validate only the descriptor is required and defaults are all used, 'testAssembly' validates
         * all options can be set
         */
        validateEnv(buildConfig.getEnv());
        validateLabels(buildConfig.getLabels());
        validateArgs(buildConfig.getArgs());
        validateBuildOptions(buildConfig.getBuildOptions());
    }

    private void validateArgs(Map<String, String> args) {
        assertThat(args).containsEntry("PROXY", "http://proxy");
    }

    private void validateLabels(Map<String, String> labels) {
        assertThat(labels).containsEntry("com.acme.label", "Hello\"World");
    }

    private void validateBuildOptions(Map<String,String> buildOptions) {
        assertThat(buildOptions).containsEntry("shmsize", "2147483648");
    }

    protected void validateRunConfiguration(RunImageConfiguration runConfig) {
        assertThat(runConfig)
                .returns(a("/foo", "/tmp:/tmp"), c -> c.getVolumeConfiguration().getBind())
                .returns(a("CAP"), RunImageConfiguration::getCapAdd)
                .returns(a("CAP"), RunImageConfiguration::getCapDrop)
                .returns(a("seccomp=unconfined"), RunImageConfiguration::getSecurityOpts)
                .returns("command.sh", c -> c.getCmd().getShell())
                .returns(a("8.8.8.8"), RunImageConfiguration::getDns)
                .returns("host", c -> c.getNetworkingConfig().getStandardMode(null))
                .returns(a("example.com"), RunImageConfiguration::getDnsSearch)
                .returns("domain.com", RunImageConfiguration::getDomainname)
                .returns("entrypoint.sh", c -> c.getEntrypoint().getShell())
                .returns(a("localhost:127.0.0.1"), RunImageConfiguration::getExtraHosts)
                .returns("subdomain", RunImageConfiguration::getHostname)
                .returns(a("redis"), RunImageConfiguration::getLinks)
                .returns(1L, RunImageConfiguration::getMemory)
                .returns(1L, RunImageConfiguration::getMemorySwap)
                .returns(1000000000L, RunImageConfiguration::getCpus)
                .returns(1L, RunImageConfiguration::getCpuShares)
                .returns("0,1", RunImageConfiguration::getCpuSet)
                .returns("/tmp/envProps.txt", RunImageConfiguration::getEnvPropertyFile)
                .returns("/tmp/props.txt", RunImageConfiguration::getPortPropertyFile)
                .returns("8081:8080", c -> c.getPorts().get(0))
                .returns(true, RunImageConfiguration::getPrivileged)
                .returns("tomcat", RunImageConfiguration::getUser)
                .returns(a("from"), c -> c.getVolumeConfiguration().getFrom())
                .returns("foo", RunImageConfiguration::getWorkingDir)
                .returns(4, c -> c.getUlimits().size())
                .returns("/var/lib/mysql:10m", c -> c.getTmpfs().get(0))
                .returns("Never", RunImageConfiguration::getImagePullPolicy)
                .returns(true, RunImageConfiguration::getReadOnly)
                .returns(true, RunImageConfiguration::getAutoRemove)
                .returns("on-failure", c -> c.getRestartPolicy().getName())
                .returns(1, c -> c.getRestartPolicy().getRetry())
                .returns("http://foo.com", c -> c.getWait().getUrl())
                .returns("pattern", c -> c.getWait().getLog())
                .returns("post_start_command", c -> c.getWait().getExec().getPostStart())
                .returns("pre_stop_command", c -> c.getWait().getExec().getPreStop())
                .returns(true, c -> c.getWait().getExec().isBreakOnError())
                .returns(5, c -> c.getWait().getTime())
                .returns(true, c -> c.getWait().getHealthy())
                .returns(0, c -> c.getWait().getExit())
                .returns("green", c -> c.getLog().getColor())
                .returns(true, c -> c.getLog().isEnabled())
                .returns("SRV",  c -> c.getLog().getPrefix())
                .returns("iso8601", c -> c.getLog().getDate())
                .returns("json", c -> c.getLog().getDriver().getName())
                .returns(2, c -> c.getLog().getDriver().getOpts().size())
                .returns("1024", c -> c.getLog().getDriver().getOpts().get("max-size"))
                .returns("10", c -> c.getLog().getDriver().getOpts().get("max-file"));

        assertThat(runConfig.getUlimits()).isNotNull();
        assertUlimitEquals(ulimit("memlock",10,10),runConfig.getUlimits().get(0));
        assertUlimitEquals(ulimit("memlock",null,-1),runConfig.getUlimits().get(1));
        assertUlimitEquals(ulimit("memlock",1024,null),runConfig.getUlimits().get(2));
        assertUlimitEquals(ulimit("memlock",2048,null),runConfig.getUlimits().get(3));
        validateEnv(runConfig.getEnv());
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
        assertThat(actual)
                .hasFieldOrPropertyWithValue("name", expected.getName())
                .hasFieldOrPropertyWithValue("soft", expected.getSoft())
                .hasFieldOrPropertyWithValue("hard", expected.getHard());
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