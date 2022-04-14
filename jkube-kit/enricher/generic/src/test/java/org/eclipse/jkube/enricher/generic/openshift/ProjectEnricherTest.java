package org.eclipse.jkube.enricher.generic.openshift;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.api.model.Project;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class ProjectEnricherTest {

    private EnricherContext context;

    public void setExpectations(Properties properties, ResourceConfig resourceConfig) {
        context = JKubeEnricherContext.builder()
                .log(new KitLogger.SilentLogger())
                .resources(resourceConfig)
                .project(JavaProject.builder()
                        .properties(properties)
                        .groupId("group")
                        .artifactId("artifact-id")
                        .build())
                .build();
    }

    @Test
    public void convertNamespaceResourceToProject() {
        // Given
        Properties properties = new Properties();
        properties.put("jkube.enricher.jkube-namespace.namespace", "example");
        setExpectations(properties, new ResourceConfig());
        final KubernetesListBuilder klb = new KubernetesListBuilder();
        // When
        new ProjectEnricher((JKubeEnricherContext) context).create(PlatformMode.openshift, klb);
        // Then
        assertThat(klb.build().getItems()).isEmpty();
    }

}