package io.jshift.springboot.watcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.io.Closeables;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.jshift.kit.build.service.docker.ImageConfiguration;
import io.jshift.kit.common.Configs;
import io.jshift.kit.common.KitLogger;
import io.jshift.kit.common.PrefixedLogger;
import io.jshift.kit.common.util.ClassUtil;
import io.jshift.kit.common.util.IoUtil;
import io.jshift.kit.common.util.KubernetesHelper;
import io.jshift.kit.common.util.MavenUtil;
import io.jshift.kit.common.util.SpringBootConfigurationHelper;
import io.jshift.kit.common.util.SpringBootUtil;
import io.jshift.kit.config.resource.JshiftAnnotations;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.kit.config.service.PodLogService;
import io.jshift.kit.config.service.PortForwardService;
import io.jshift.maven.enricher.api.util.KubernetesResourceUtil;
import io.jshift.watcher.api.BaseWatcher;
import io.jshift.watcher.api.WatcherContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;

import static io.jshift.kit.common.util.SpringBootConfigurationHelper.DEV_TOOLS_REMOTE_SECRET;

public class SpringBootWatcher extends BaseWatcher {


    private final PortForwardService portForwardService;

    // Available configuration keys
    private enum Config implements Configs.Key {

        // The time to wait for the service to be exposed (by the expose controller)
        serviceUrlWaitTimeSeconds {{ d = "5"; }};

        public String def() { return d; } protected String d;
    }

    public SpringBootWatcher(WatcherContext watcherContext) {
        super(watcherContext, "spring-boot");
        portForwardService = new PortForwardService(watcherContext.getKubernetesClient(), watcherContext.getLogger());
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs, Set<HasMetadata> resources, PlatformMode mode) {
        return MavenUtil.hasPluginOfAnyGroupId(getContext().getProject(), SpringBootConfigurationHelper.SPRING_BOOT_MAVEN_PLUGIN_ARTIFACT_ID);
    }

    @Override
    public void watch(List<ImageConfiguration> configs, Set<HasMetadata> resources, PlatformMode mode) throws Exception {
        KubernetesClient kubernetes = getContext().getKubernetesClient();

        PodLogService.PodLogServiceContext logContext = new PodLogService.PodLogServiceContext.Builder()
                .log(log)
                .newPodLog(getContext().getNewPodLogger())
                .oldPodLog(getContext().getOldPodLogger())
                .build();

        new PodLogService(logContext).tailAppPodsLogs(kubernetes, getContext().getClusterConfiguration().getNamespace(), resources, false, null, true, null, false);

        String url = getServiceExposeUrl(kubernetes, resources);
        if (url == null) {
            url = getPortForwardUrl(resources);
        }

        if (url != null) {
            runRemoteSpringApplication(url);
        } else {
            throw new IllegalStateException("Unable to open a channel to the remote pod.");
        }
    }

    private String getPortForwardUrl(final Set<HasMetadata> resources) throws Exception {
        LabelSelector selector = KubernetesResourceUtil.getPodLabelSelector(resources);
        if (selector == null) {
            log.warn("Unable to determine a selector for application pods");
            return null;
        }

        Properties properties = SpringBootUtil.getSpringBootApplicationProperties(MavenUtil.getCompileClassLoader(getContext().getProject()));
        SpringBootConfigurationHelper propertyHelper = new SpringBootConfigurationHelper(SpringBootUtil.getSpringBootVersion(getContext().getProject()));

        int port = IoUtil.getFreeRandomPort();
        int containerPort = propertyHelper.getServerPort(properties);
        portForwardService.forwardPortAsync(getContext().getLogger(), selector, containerPort, port);

        return createForwardUrl(propertyHelper, properties, port);
    }

    private String createForwardUrl(SpringBootConfigurationHelper propertyHelper, Properties properties, int localPort) {
        String scheme = StringUtils.isNotBlank(properties.getProperty(propertyHelper.getServerKeystorePropertyKey())) ? "https://" : "http://";
        String contextPath = properties.getProperty(propertyHelper.getServerContextPathPropertyKey(), "");
        return scheme + "localhost:" + localPort + contextPath;
    }

    private String getServiceExposeUrl(KubernetesClient kubernetes, Set<HasMetadata> resources) throws InterruptedException {
        long serviceUrlWaitTimeSeconds = Configs.asInt(getConfig(Config.serviceUrlWaitTimeSeconds));
        for (HasMetadata entity : resources) {
            if (entity instanceof Service) {
                Service service = (Service) entity;
                String name = KubernetesHelper.getName(service);
                Resource<Service, DoneableService> serviceResource = kubernetes.services().inNamespace(getContext().getClusterConfiguration().getNamespace()).withName(name);
                String url = null;
                // lets wait a little while until there is a service URL in case the exposecontroller is running slow
                for (int i = 0; i < serviceUrlWaitTimeSeconds; i++) {
                    if (i > 0) {
                        Thread.sleep(1000);
                    }
                    Service s = serviceResource.get();
                    if (s != null) {
                        url = KubernetesHelper.getOrCreateAnnotations(s).get(JshiftAnnotations.SERVICE_EXPOSE_URL);
                        if (StringUtils.isNotBlank(url)) {
                            break;
                        }
                    }
                    if (!isExposeService(service)) {
                        break;
                    }
                }

                // lets not wait for other services
                serviceUrlWaitTimeSeconds = 1;
                if (StringUtils.isNotBlank(url) && url.startsWith("http")) {
                    return url;
                }
            }
        }

        log.info("No exposed service found for connecting the dev tools");
        return null;
    }

    private boolean isExposeService(Service service) {
        String expose = KubernetesHelper.getLabels(service).get("expose");
        return expose != null && expose.toLowerCase().equals("true");
    }

    private void runRemoteSpringApplication(String url) {
        log.info("Running RemoteSpringApplication against endpoint: " + url);

        String remoteSecret = validateSpringBootDevtoolsSettings();

        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader pluginClassLoader = (URLClassLoader) classLoader;
            try(
                    URLClassLoader projectClassLoader =
                            ClassUtil.createProjectClassLoader(getContext().getProject().getCompileClasspathElements(), log)) {

                URLClassLoader[] classLoaders = {projectClassLoader, pluginClassLoader};

                StringBuilder buffer = new StringBuilder("java -cp ");
                int count = 0;
                for (URLClassLoader urlClassLoader : classLoaders) {
                    URL[] urLs = urlClassLoader.getURLs();
                    for (URL u : urLs) {
                        if (count++ > 0) {
                            buffer.append(File.pathSeparator);
                        }
                        try {
                            URI uri = u.toURI();
                            File file = new File(uri);
                            buffer.append(file.getCanonicalPath());
                        } catch (Exception e) {
                            throw new IllegalStateException("Failed to create classpath: " + e, e);
                        }
                    }
                }

                // Add dev tools to the classpath (the main class is not read from BOOT-INF/lib)
                try {
                    File devtools = getSpringBootDevToolsJar(getContext().getProject());
                    buffer.append(File.pathSeparator);
                    buffer.append(devtools.getCanonicalPath());
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to include devtools in the classpath: " + e, e);
                }

                buffer.append(" -Dspring.devtools.remote.secret=");
                buffer.append(remoteSecret);
                buffer.append(" org.springframework.boot.devtools.RemoteSpringApplication ");
                buffer.append(url);

                try {
                    String command = buffer.toString();
                    log.debug("Running: " + command);
                    final Process process = Runtime.getRuntime().exec(command);

                    final AtomicBoolean outputEnabled = new AtomicBoolean(true);
                    Runtime.getRuntime().addShutdownHook(new Thread("jshift:watch [spring-boot] shutdown hook") {
                        @Override
                        public void run() {
                            log.info("Terminating the Spring remote client...");
                            outputEnabled.set(false);
                            process.destroy();
                        }
                    });
                    KitLogger logger = new PrefixedLogger("Spring-Remote", log);
                    Thread stdOutPrinter = startOutputProcessor(logger, process.getInputStream(), false, outputEnabled);
                    Thread stdErrPrinter = startOutputProcessor(logger, process.getErrorStream(), true, outputEnabled);
                    int status = process.waitFor();
                    stdOutPrinter.join();
                    stdErrPrinter.join();
                    if (status != 0) {
                        log.warn("Process returned status: %s", status);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to run RemoteSpringApplication: " + e, e);
                }
            } catch (DependencyResolutionRequiredException e) {
                log.warn("Instructed to use project classpath, but cannot. Continuing build if we can: ", e);
            } catch (IOException e) {
                log.warn("Instructed to use project classpath, but cannot. Continuing build if we can: ", e);
            }
        } else {
            throw new IllegalStateException("ClassLoader must be a URLClassLoader but it is: " + classLoader.getClass().getName());
        }
    }

    protected Thread startOutputProcessor(final KitLogger logger, final InputStream inputStream, final boolean error, final AtomicBoolean outputEnabled) throws IOException {
        Thread printer = new Thread() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (outputEnabled.get()) {
                            if (error) {
                                logger.error("%s", line);
                            } else {
                                logger.info("%s", line);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (outputEnabled.get()) {
                        logger.error("Failed to process " + (error ? "stderr" : "stdout") + " from spring-remote process: " + e);
                    }
                } finally {
                    Closeables.closeQuietly(reader);
                }
            }
        };

        printer.start();
        return printer;
    }

    private File getSpringBootDevToolsJar(MavenProject project) throws IOException {
        String version = SpringBootUtil.getSpringBootDevToolsVersion(project).orElseThrow(() -> new IllegalStateException("Unable to find the spring-boot version"));
        return getContext().getFabric8ServiceHub().getArtifactResolverService().resolveArtifact(SpringBootConfigurationHelper.SPRING_BOOT_GROUP_ID, SpringBootConfigurationHelper.SPRING_BOOT_DEVTOOLS_ARTIFACT_ID, version, "jar");
    }

    private String validateSpringBootDevtoolsSettings() throws IllegalStateException {
        String configuration = MavenUtil.getPlugin(getContext().getProject(), null, SpringBootConfigurationHelper.SPRING_BOOT_MAVEN_PLUGIN_ARTIFACT_ID).getConfiguration().toString();
        if(!configuration.contains("<excludeDevtools>false</excludeDevtools>")) {
            log.warn("devtools need to be included in repacked archive, please set <excludeDevtools> to false in plugin configuration");
            throw new IllegalStateException("devtools needs to be included in fat jar");
        }

        Properties properties = SpringBootUtil.getSpringBootApplicationProperties(MavenUtil.getCompileClassLoader(getContext().getProject()));
        String remoteSecret = properties.getProperty(DEV_TOOLS_REMOTE_SECRET, System.getProperty(DEV_TOOLS_REMOTE_SECRET));
        if (StringUtils.isBlank(remoteSecret)) {
            log.warn("There is no `%s` property defined in your src/main/resources/application.properties. Please add one!", DEV_TOOLS_REMOTE_SECRET);
            throw new IllegalStateException("No " + DEV_TOOLS_REMOTE_SECRET + " property defined in application.properties or system properties");
        }
        return properties.getProperty(DEV_TOOLS_REMOTE_SECRET);
    }

}

