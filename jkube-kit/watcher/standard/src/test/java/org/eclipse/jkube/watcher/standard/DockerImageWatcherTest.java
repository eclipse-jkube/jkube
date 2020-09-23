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
package org.eclipse.jkube.watcher.standard;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.jkube.kit.build.service.docker.WatchService;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.access.ClusterAccess;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DockerImageWatcherTest {
    @Mocked
    WatchService.ImageWatcher imageWatcher;

    @Mocked
    KitLogger logger;

    @Mocked
    ClusterAccess clusterAccess;

    @Mocked
    KubernetesClient kubernetesClient;

    @Before
    public void init() {
        new Expectations() {{

            clusterAccess.getNamespace();
            result = "default";

            clusterAccess.createDefaultClient();
            result = kubernetesClient;

            kubernetesClient.pods().inNamespace(anyString).withLabelSelector((LabelSelector)any).list();
            result = new PodListBuilder()
                    .addToItems(new PodBuilder()
                            .withNewMetadata()
                            .withName("testpod")
                            .endMetadata().build())
                    .build();
        }};
    }

    @Test
    public void testExecuteCommandInPod() {
        // Given
        new Expectations() {{
            imageWatcher.getPostExec();
            result = "ls -lt /deployments";
        }};
        Set<HasMetadata> resources = getMockedResourceList();

        // When
        DockerImageWatcher.executeCommandInPod(imageWatcher, resources, clusterAccess, logger, 0);

        // Then
        new Verifications() {{
           kubernetesClient.pods().inNamespace("default").withLabelSelector(new LabelSelectorBuilder().withMatchLabels(Collections.singletonMap("foo", "bar")).build()).list();
           times = 1;

           kubernetesClient.pods().inNamespace("default").withName("testpod").exec(new String[]{"ls", "-lt", "/deployments"});
           times = 1;
        }};
    }

    @Test
    public void testCopyFileToPod() throws IOException {
        // Given
        Set<HasMetadata> resources = getMockedResourceList();
        File fileToCopy = Files.createTempFile("text", "txt").toFile();

        // When
        DockerImageWatcher.copyFileToPod(fileToCopy, resources, clusterAccess, logger, 0);

        // Then
        new Verifications() {{
            LabelSelector labelSelector = new LabelSelectorBuilder()
                    .withMatchLabels(Collections.singletonMap("foo", "bar"))
                    .build();
            kubernetesClient.pods().inNamespace("default").withLabelSelector(labelSelector).list();
            times = 1;

            kubernetesClient.pods().inNamespace("default").withName("testpod").readingInput((FileInputStream)any).writingOutput((OutputStream)any).exec("tar", "-xf", "-", "-C", "/");
            times = 1;
        }};
    }

    private Set<HasMetadata> getMockedResourceList() {
        Set<HasMetadata> resources = new HashSet<>();
        resources.add(new DeploymentBuilder()
                .withNewMetadata().withName("foo").endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels("foo", "bar")
                .endSelector()
                .endSpec()
                .build());
        return resources;
    }
}
