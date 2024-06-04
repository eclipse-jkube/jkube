/*
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
package org.eclipse.jkube.springboot.watcher;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import org.eclipse.jkube.kit.common.JKubeException;
import org.eclipse.jkube.kit.common.util.ClassUtil;
import org.eclipse.jkube.kit.common.util.IoUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.eclipse.jkube.kit.common.util.SpringBootConfiguration;
import org.eclipse.jkube.kit.common.util.SpringBootUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.service.PodLogService;
import org.eclipse.jkube.kit.config.service.PortForwardService;
import org.eclipse.jkube.springboot.RemoteSpringBootDevtoolsCommand;
import org.eclipse.jkube.watcher.api.BaseWatcher;
import org.eclipse.jkube.watcher.api.WatcherContext;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import org.apache.commons.lang3.StringUtils;

import static org.eclipse.jkube.kit.common.util.SpringBootUtil.DEV_TOOLS_REMOTE_SECRET;
import static org.eclipse.jkube.kit.common.util.SpringBootUtil.getSpringBootPluginConfiguration;
import static org.eclipse.jkube.springboot.SpringBootDevtoolsUtils.getSpringBootDevToolsJar;

public class SpringBootWatcher extends BaseWatcher {

    private final PortForwardService portForwardService;

    public SpringBootWatcher(WatcherContext watcherContext) {
        super(watcherContext, "spring-boot");
        portForwardService = new PortForwardService(watcherContext.getLogger());
    }

    @Override
    public boolean isApplicable(List<ImageConfiguration> configs, Collection<HasMetadata> resources, PlatformMode mode) {
        return JKubeProjectUtil.hasPluginOfAnyArtifactId(getContext().getBuildContext().getProject(), SpringBootUtil.SPRING_BOOT_MAVEN_PLUGIN_ARTIFACT_ID) ||
         JKubeProjectUtil.hasPluginOfAnyArtifactId(getContext().getBuildContext().getProject(), SpringBootUtil.SPRING_BOOT_GRADLE_PLUGIN_ARTIFACT_ID);
    }

    @Override
    public void watch(List<ImageConfiguration> configs, String namespace, Collection<HasMetadata> resources, PlatformMode mode) {
        final NamespacedKubernetesClient kubernetes;
        if (namespace != null) {
            kubernetes = getContext().getJKubeServiceHub().getClient().adapt(NamespacedKubernetesClient.class).inNamespace(namespace);
        } else {
            kubernetes = getContext().getJKubeServiceHub().getClient().adapt(NamespacedKubernetesClient.class);
        }

        PodLogService.PodLogServiceContext logContext = PodLogService.PodLogServiceContext.builder()
                .log(log)
                .newPodLog(getContext().getNewPodLogger())
                .oldPodLog(getContext().getOldPodLogger())
                .build();

        new PodLogService(logContext).tailAppPodsLogs(
            kubernetes,
            namespace,
            resources, false, null, true, null, false);

        String url = getPortForwardUrl(kubernetes, resources);
        if (url != null) {
            runRemoteSpringApplication(url);
        } else {
            throw new IllegalStateException("Unable to open a channel to the remote pod.");
        }
    }

    String getPortForwardUrl(NamespacedKubernetesClient kubernetes, final Collection<HasMetadata> resources) {
        LabelSelector selector = KubernetesHelper.extractPodLabelSelector(resources);
        if (selector == null) {
            log.warn("Unable to determine a selector for application pods");
            return null;
        }

        final SpringBootConfiguration springBootConfiguration = SpringBootConfiguration.from(
          getContext().getBuildContext().getProject());
        int localPort = IoUtil.getFreeRandomPort();
        int containerPort = springBootConfiguration.getServerPort();
        portForwardService.forwardPortAsync(kubernetes, selector, containerPort, localPort);

        String scheme = StringUtils.isNotBlank(springBootConfiguration.getServerKeystore()) ?
          "https://" : "http://";
        String contextPath = StringUtils.isNotBlank(springBootConfiguration.getServerContextPath()) ?
          springBootConfiguration.getServerContextPath() : "";
        return scheme + "localhost:" + localPort + contextPath;
    }

    private void runRemoteSpringApplication(String url) {
        log.info("Running RemoteSpringApplication against endpoint: " + url);

        final String remoteSecret = validateSpringBootDevtoolsSettings();

        final List<URLClassLoader> classLoaders = new ArrayList<>();
        ClassLoader pluginClassLoader = getClass().getClassLoader();
        if (pluginClassLoader instanceof URLClassLoader) {
            classLoaders.add((URLClassLoader) pluginClassLoader);
        }
        try (URLClassLoader projectClassLoader = ClassUtil.createProjectClassLoader(
                    getContext().getBuildContext().getProject().getCompileClassPathElements(), log)
        ) {
            classLoaders.add(projectClassLoader);

            final String devToolsPath = getSpringBootDevToolsJar(getContext().getBuildContext().getProject())
                  .getCanonicalPath();
            final String classPath = classLoaders.stream().flatMap(cl -> Stream.of(cl.getURLs())).map(u -> {
                try {
                    URI uri = u.toURI();
                    File file = new File(uri);
                    return file.getCanonicalPath();
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to create classpath: " + e, e);
                }
            }).collect(Collectors.joining(File.pathSeparator, "", File.pathSeparator))
              // Add dev tools to the classpath (the main class is not read from BOOT-INF/lib)
              .concat(devToolsPath);
            RemoteSpringBootDevtoolsCommand command = new RemoteSpringBootDevtoolsCommand(classPath, remoteSecret, url, log);

            try {
                command.execute();
            } catch (Exception e) {
                throw new JKubeException("Failed to run RemoteSpringApplication: " + e, e);
            }
        } catch (IOException e) {
            log.warn("Instructed to use project classpath, but cannot. Continuing build if we can: ", e);
        }
    }

    private String validateSpringBootDevtoolsSettings() {
        final Map<String, Object> configuration = getSpringBootPluginConfiguration(getContext().getBuildContext().getProject());
        if(configuration != null && (!configuration.containsKey("excludeDevtools") || !configuration.get("excludeDevtools").equals("false"))) {
            log.warn("devtools need to be included in repacked archive, please set <excludeDevtools> to false in plugin configuration");
            throw new IllegalStateException("devtools needs to be included in fat jar");
        }

        Properties properties = SpringBootUtil.getSpringBootApplicationProperties(
            JKubeProjectUtil.getClassLoader(getContext().getBuildContext().getProject()));
        String remoteSecret = properties.getProperty(DEV_TOOLS_REMOTE_SECRET, System.getProperty(DEV_TOOLS_REMOTE_SECRET));
        if (StringUtils.isBlank(remoteSecret)) {
            log.warn("There is no `%s` property defined in your src/main/resources/application.properties. Please add one!", DEV_TOOLS_REMOTE_SECRET);
            throw new IllegalStateException("No " + DEV_TOOLS_REMOTE_SECRET + " property defined in application.properties or system properties");
        }
        return properties.getProperty(DEV_TOOLS_REMOTE_SECRET);
    }
}

