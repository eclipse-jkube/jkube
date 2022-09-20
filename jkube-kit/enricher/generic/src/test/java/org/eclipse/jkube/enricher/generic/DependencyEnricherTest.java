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
package org.eclipse.jkube.enricher.generic;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.eclipse.jkube.kit.enricher.api.model.KindAndName;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DependencyEnricherTest {

    private JKubeEnricherContext context;

    private JavaProject project;

    // Some resource files related to test case placed in resources/ directory:
    private static final String OVERRIDE_FRAGMENT_FILE = "/jenkins-kubernetes-cm.yml";
    private static final String ARTIFACT_FILE_PATH = "/jenkins-4.0.41.jar";

    @Before
    public void setUp() {
        context = mock(JKubeEnricherContext.class);
        Configuration configuration = mock(Configuration.class);
        when(context.getConfiguration()).thenReturn(configuration);
        when(context.getLog()).thenReturn(new KitLogger.SilentLogger());
        project =mock(JavaProject.class);
    }
    @Test
    public void checkDuplicatesInResource() throws Exception {
        // Generate given Resources
        KubernetesListBuilder aBuilder = createResourcesForTest();
        // Enrich
        KubernetesList aResourceList = enrichResources(aBuilder);
        // Assert
        assertThat(aResourceList.getItems()).isNotNull();
        assertThat(checkUniqueResources(aResourceList.getItems())).isTrue();
    }

    private KubernetesList enrichResources(KubernetesListBuilder aBuilder) throws URISyntaxException {
        DependencyEnricher enricher = new DependencyEnricher(context);
        enricher.create(PlatformMode.kubernetes, aBuilder);
        enricher.enrich(PlatformMode.kubernetes, aBuilder);
        return aBuilder.build();
    }

    private KubernetesListBuilder createResourcesForTest() throws IOException, URISyntaxException {
        setupExpectations();
        List<File> resourceList = new ArrayList<>();

        resourceList.add(new File(Paths.get(getClass().getResource(OVERRIDE_FRAGMENT_FILE).toURI()).toAbsolutePath().toString()));



        /*
         * Our override file also contains a ConfigMap item with name jenkins, load it while
         * loading Kubernetes resources.
         */
        return KubernetesResourceUtil.readResourceFragmentsFrom(
                PlatformMode.kubernetes,
                KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING,
                project.getName(),
                resourceList.toArray(new File[resourceList.size()]));
    }

    private void setupExpectations() {
        // Setup Mock behaviour
        when(context.getDependencies(true)).thenReturn(getDummyArtifacts());
    }

    private List<Dependency> getDummyArtifacts() {
        List<Dependency> artifacts = new ArrayList<>();

        File aFile = new File(getClass().getResource(ARTIFACT_FILE_PATH).getFile());
        Dependency artifact = Dependency.builder().groupId("g1").artifactId("a1").version("v1")
            .type("jar").scope("compile").file(aFile).build();
        artifacts.add(artifact);
        return artifacts;
    }

    private boolean checkUniqueResources(List<HasMetadata> resourceList) {
        Map<KindAndName, Integer> resourceMap = new HashMap<>();
        for(int index = 0; index < resourceList.size(); index++) {
            KindAndName aKey = new KindAndName(resourceList.get(index));
            if(resourceMap.containsKey(aKey))
                return false;
            resourceMap.put(aKey, index);
        }
        return true;
    }
}
