/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jkube.enricher.generic;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.jkube.kit.config.resource.JkubeAnnotations;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * @author kameshs
 */
public class MavenIssueManagementEnricherTest {

    @Mocked
    private MavenEnricherContext context;

    @Test
    public void testMavenIssueManagementAll() {

        final MavenProject project = new MavenProject();
        final IssueManagement issueManagement = new IssueManagement();
        issueManagement.setSystem("GitHub");
        issueManagement.setUrl("https://github.com/reactiverse/vertx-maven-plugin/issues/");
        project.setIssueManagement(issueManagement);
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        enricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);
        Assert.assertEquals(2, scmAnnotations.size());
        assertEquals("GitHub",
                scmAnnotations.get(JkubeAnnotations.ISSUE_SYSTEM.value()));
        assertEquals("https://github.com/reactiverse/vertx-maven-plugin/issues/",
                scmAnnotations.get(JkubeAnnotations.ISSUE_TRACKER_URL.value()));
    }

    @Test
    public void testMavenIssueManagementOnlySystem() {

        final MavenProject project = new MavenProject();
        final IssueManagement issueManagement = new IssueManagement();
        issueManagement.setSystem("GitHub");
        project.setIssueManagement(issueManagement);
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        enricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);
        assertTrue(scmAnnotations.isEmpty());
    }

    @Test
    public void testMavenIssueManagementOnlyUrl() {

        final MavenProject project = new MavenProject();
        final IssueManagement issueManagement = new IssueManagement();
        issueManagement.setUrl("https://github.com/fabric8io/jkube-maven-plugin/issues/");
        project.setIssueManagement(issueManagement);
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        enricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);
        assertTrue(scmAnnotations.isEmpty());
    }


    @Test
    public void testMavenNoIssueManagement() {

        final MavenProject project = new MavenProject();
        // Setup mock behaviour
        new Expectations() {
            {
                {
                    context.getProject();
                    result = project;
                }
            }
        };

        MavenIssueManagementEnricher enricher = new MavenIssueManagementEnricher(context);
        KubernetesListBuilder builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().withNewMetadata().withName("foo").endMetadata().build());

        enricher.create(PlatformMode.kubernetes, builder);

        Map<String, String> scmAnnotations = builder.buildFirstItem().getMetadata().getAnnotations();
        assertNotNull(scmAnnotations);
        assertTrue(scmAnnotations.isEmpty());
    }


}
