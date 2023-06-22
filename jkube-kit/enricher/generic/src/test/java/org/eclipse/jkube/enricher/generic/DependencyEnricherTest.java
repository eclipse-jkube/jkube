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
import org.eclipse.jkube.kit.enricher.api.model.KindAndName;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceFragments;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DependencyEnricherTest {

    private JKubeEnricherContext context;

    // Some resource files related to test case placed in resources/ directory:
    private static final String OVERRIDE_FRAGMENT_FILE = "/jenkins-kubernetes-cm.yml";
    private static final String ARTIFACT_FILE_PATH = "/jenkins-4.0.41.jar";

    @BeforeEach
    void setUp() {
        context = JKubeEnricherContext.builder()
          .project(JavaProject.builder()
            .name("the-project")
            .dependenciesWithTransitive(Collections.singletonList(Dependency.builder()
              .groupId("g1").artifactId("a1").version("v1")
              .type("jar").scope("compile")
              .file(new File(getClass().getResource(ARTIFACT_FILE_PATH).getFile()))
              .build()))
            .build())
          .log(new KitLogger.SilentLogger())
          .build();
    }

    @Test
    void checkDuplicatesInResource() throws Exception {
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
        List<File> resourceList = new ArrayList<>();
        resourceList.add(new File(Paths.get(getClass().getResource(OVERRIDE_FRAGMENT_FILE).toURI()).toAbsolutePath().toString()));
        /*
         * Our override file also contains a ConfigMap item with name jenkins, load it while
         * loading Kubernetes resources.
         */
        return KubernetesResourceFragments.readResourceFragmentsFrom(
                KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING,
                "the-project",
                resourceList.toArray(new File[0]));
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
