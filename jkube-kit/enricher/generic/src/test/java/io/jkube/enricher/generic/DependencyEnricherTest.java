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
package io.jkube.enricher.generic;

/*
 * @author rohan
 * @since 06/11/17
 */

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.jkube.kit.config.image.ImageConfiguration;
import io.jkube.kit.config.resource.GroupArtifactVersion;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import io.jkube.maven.enricher.api.model.Dependency;
import io.jkube.maven.enricher.api.model.KindAndName;
import io.jkube.maven.enricher.api.util.KubernetesResourceUtil;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DependencyEnricherTest {

    @Mocked
    private MavenEnricherContext context;

    @Mocked
    private ImageConfiguration imageConfiguration;

    @Mocked
    private MavenProject project;

    // Some resource files related to test case placed in resources/ directory:
    private final String overrideFragementFile = "/jenkins-kubernetes-cm.yml";
    private final String artifactFilePath = "/jenkins-4.0.41.jar";

    @Test
    public void checkDuplicatesInResource() throws Exception {
        // Generate given Resources
        KubernetesListBuilder aBuilder = createResourcesForTest();
        // Enrich
        KubernetesList aResourceList = enrichResources(aBuilder);
        // Assert
        assertTrue(aResourceList.getItems() != null);
        assertEquals(checkUniqueResources(aResourceList.getItems()), true);
    }

    private KubernetesList enrichResources(KubernetesListBuilder aBuilder) {
        DependencyEnricher enricher = new DependencyEnricher(context);
        enricher.create(PlatformMode.kubernetes, aBuilder);
        enricher.enrich(PlatformMode.kubernetes, aBuilder);
        return aBuilder.build();
    }

    private KubernetesListBuilder createResourcesForTest() throws IOException, URISyntaxException {
        setupExpectations();
        List<File> resourceList = new ArrayList<>();

        resourceList.add(new File(Paths.get(getClass().getResource(overrideFragementFile).toURI()).toAbsolutePath().toString()));



        /*
         * Our override file also contains a ConfigMap item with name jenkins, load it while
         * loading Kubernetes resources.
         */
        KubernetesListBuilder builder = KubernetesResourceUtil.readResourceFragmentsFrom(
                PlatformMode.kubernetes,
                KubernetesResourceUtil.DEFAULT_RESOURCE_VERSIONING,
                project.getName(),
                resourceList.toArray(new File[resourceList.size()]));
        return builder;
    }

    private void setupExpectations() {
        // Setup Mock behaviour
        new Expectations() {{

            context.getDependencies(true);
            result = getDummyArtifacts();
        }};
    }

    private List<Dependency> getDummyArtifacts() {
        List<Dependency> artifacts = new ArrayList<>();


        File aFile = new File(getClass().getResource(artifactFilePath).getFile());
        Dependency artifact = new Dependency(new GroupArtifactVersion("g1", "a1", "v1"),"jar", "compile", aFile);
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
