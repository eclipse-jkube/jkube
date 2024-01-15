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
package org.eclipse.jkube.enricher.generic;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.GitUtil;
import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.OpenShiftAnnotations;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitEnricherTest {

    private static final String GIT_REMOTE_URL = "https://github.com:jkubeio/eclipse-jkube-demo-project.git";
    private static final String GIT_BRANCH = "master";
    private static final String GIT_COMMIT_ID = "058bed285de43aac80b5bf9433b9a3a9c3915e19";
    private JKubeEnricherContext context;
    private GitEnricher gitEnricher;
    private MockedStatic<GitUtil> gitUtilMockedStatic;
    private Properties properties;
    private KubernetesListBuilder klb;

    @TempDir
    private File temporaryFolder;

    @BeforeEach
    void setUp() {
        klb = new KubernetesListBuilder().withItems(new ServiceBuilder().build());
        properties = new Properties();
        context = JKubeEnricherContext.builder()
            .project(JavaProject.builder()
                .groupId("org.example")
                .artifactId("test-project")
                .version("0.0.1")
                .baseDirectory(temporaryFolder)
                .properties(properties)
                .build())
            .log(spy(new KitLogger.SilentLogger()))
            .build();
        gitEnricher = new GitEnricher(context);
        gitUtilMockedStatic = mockStatic(GitUtil.class);
    }

    @AfterEach
    void tearDown() {
        gitUtilMockedStatic.close();
    }

    @Test
    void getAnnotations_addedInKubernetesPlatformMode() {
        // Given
        Map<String, String> annotations;

        // When
        annotations = GitEnricher.getAnnotations(PlatformMode.kubernetes, GIT_REMOTE_URL, GIT_BRANCH, GIT_COMMIT_ID, true);

        // Then
        assertJkubeAnnotations(annotations);
    }

    @Test
    void getAnnotations_addedInOpenShiftPlatformMode() {
        // Given
        Map<String, String> annotations;

        // When
        annotations = GitEnricher.getAnnotations(PlatformMode.openshift, GIT_REMOTE_URL, GIT_BRANCH, GIT_COMMIT_ID, true);

        // Then
        assertJkubeAnnotations(annotations);
        assertThat(annotations).containsEntry(OpenShiftAnnotations.VCS_REF.value(), GIT_BRANCH)
            .containsEntry(OpenShiftAnnotations.VCS_URI.value(), GIT_REMOTE_URL);
    }

    @Test
    void getAnnotations_addedWithAllNullValues() {
        // Given
        Map<String, String> annotations;

        // When
        annotations = GitEnricher.getAnnotations(PlatformMode.kubernetes, null, null, null, true);

        // Then
        assertThat(annotations).isEmpty();
    }

    @Test
    void getAnnotations_addedWithNullCommitValues() {
        // Given
        Map<String, String> annotations;

        // When
        annotations = GitEnricher.getAnnotations(PlatformMode.kubernetes, GIT_REMOTE_URL, GIT_BRANCH, null, true);

        // Then
        assertJkubeAnnotationsRemoteUrlAndBranch(annotations);
    }

    @Test
    @DisplayName("no .git repository found, then no git annotations added")
    void create_whenNoGitRepositoryFound_thenNoAnnotationsAdded() {
        // When
        gitEnricher.create(PlatformMode.kubernetes, klb);

        // Then
        HasMetadata result = klb.buildFirstItem();
        assertThat(result)
            .extracting("metadata.annotations")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .doesNotContainKey("jkube.eclipse.org/git-branch")
            .doesNotContainKey("jkube.eclipse.org/git-commit")
            .doesNotContainKey("jkube.eclipse.org/git-url");
    }

    static Stream<Arguments> controllerResources() {
        return Stream.of(
            arguments(new ServiceBuilder().build()),
            arguments(new DeploymentBuilder().build()),
            arguments(new DeploymentConfigBuilder().build()),
            arguments(new ReplicaSetBuilder().build()),
            arguments(new ReplicationControllerBuilder().build()),
            arguments(new DaemonSetBuilder().build()),
            arguments(new StatefulSetBuilder().build()),
            arguments(new JobBuilder().build())
        );
    }

    @ParameterizedTest(name = "Git annotations should be added to {0}")
    @MethodSource("controllerResources")
    void create_whenResourceProvided_thenAddGitAnnotations(HasMetadata h) throws IOException {
        // Given
        givenValidGitRepositoryExistsInProject("origin");
        klb = new KubernetesListBuilder().withItems(h);

        // When
        gitEnricher.create(PlatformMode.kubernetes, klb);

        // Then
        HasMetadata result = klb.buildFirstItem();
        assertThat(result)
            .extracting("metadata.annotations")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("jkube.eclipse.org/git-branch", "test-branch")
            .containsEntry("jkube.eclipse.org/git-commit", "testcommitid")
            .containsEntry("jkube.eclipse.org/git-url", "https://example.com/foo.git");
    }

    @Nested
    @DisplayName("given a valid .git repository exists")
    class ValidGitRepositoryExists {
        @BeforeEach
        void setUp() throws IOException {
            givenValidGitRepositoryExistsInProject("origin");
        }

        @Test
        @DisplayName("failed to extract git information, then no annotations added")
        void whenFailedToExtractGitInformation_thenNoGitAnnotationsAdded() {
            // Given
            IOException expectedExceptionToBeThrown = new IOException("Failed to read git repository");
            gitUtilMockedStatic.when(() -> GitUtil.getGitRepository(temporaryFolder)).thenThrow(expectedExceptionToBeThrown);

            // When
            gitEnricher.create(PlatformMode.kubernetes, klb);

            // Then
            HasMetadata result = klb.buildFirstItem();
            assertThat(result)
                .extracting("metadata.annotations")
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .doesNotContainKey("jkube.eclipse.org/git-branch")
                .doesNotContainKey("jkube.eclipse.org/git-commit")
                .doesNotContainKey("jkube.eclipse.org/git-url");
            verify(context.getLog()).error("jkube-git: Cannot extract Git information for adding to annotations: java.io.IOException: Failed to read git repository", expectedExceptionToBeThrown);
        }

        @Test
        @DisplayName("jkube.remoteName=upstream, then fetch git-url from specified remote")
        void whenGitRemotePropertySpecified_thenUseProvidedGitRemoteInAnnotations() throws IOException {
            // Given
            givenValidGitRepositoryExistsInProject("upstream");
            properties.put("jkube.remoteName", "upstream");

            // When
            gitEnricher.create(PlatformMode.kubernetes, klb);

            // Then
            HasMetadata result = klb.buildFirstItem();
            assertThat(result)
                .extracting("metadata.annotations")
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("jkube.eclipse.org/git-branch", "test-branch")
                .containsEntry("jkube.eclipse.org/git-commit", "testcommitid")
                .containsEntry("jkube.eclipse.org/git-url", "https://example.com/foo.git");
        }

        @Test
        @DisplayName("git remote not found, then do not add git-url annotation")
        void whenRemoteNotFound_thenGitUrlAnnotationNotAdded() throws IOException {
            // Given
            givenValidGitRepositoryExistsInProject("test");

            // When
            gitEnricher.create(PlatformMode.kubernetes, klb);

            // Then
            HasMetadata result = klb.buildFirstItem();
            assertThat(result)
                .extracting("metadata.annotations")
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("jkube.eclipse.org/git-branch", "test-branch")
                .containsEntry("jkube.eclipse.org/git-commit", "testcommitid")
                .doesNotContainKey("jkube.eclipse.org/git-url");
            verify(context.getLog()).warn("jkube-git: Could not detect any git remote");
        }

        @Test
        @DisplayName("OpenShift vcs-uri, vcs-ref annotations added in OpenShift Platform")
        void whenPlatformModeOpenShift_thenAddOpenShiftVcsAnnotations() {
            // When
            gitEnricher.create(PlatformMode.openshift, klb);

            // Then
            HasMetadata result = klb.buildFirstItem();
            assertThat(result)
                .extracting("metadata.annotations")
                .asInstanceOf(InstanceOfAssertFactories.MAP)
                .containsEntry("app.openshift.io/vcs-ref", "test-branch")
                .containsEntry("app.openshift.io/vcs-uri", "https://example.com/foo.git");
        }
    }

    private void assertJkubeAnnotations(Map<String, String> annotations) {
        assertJkubeAnnotationsRemoteUrlAndBranch(annotations);
        assertThat(annotations).containsEntry(JKubeAnnotations.GIT_COMMIT.value(true), GIT_COMMIT_ID);
    }

    private void assertJkubeAnnotationsRemoteUrlAndBranch(Map<String, String> annotations) {
      assertThat(annotations)
          .containsEntry("jkube.io/git-url", GIT_REMOTE_URL)
          .containsEntry("jkube.io/git-branch", GIT_BRANCH);
    }

    private void givenValidGitRepositoryExistsInProject(String remoteName) throws IOException {
        Repository gitRepository = mock(Repository.class);
        StoredConfig storedConfig = mock(StoredConfig.class);
        when(gitRepository.getBranch()).thenReturn("test-branch");
        when(gitRepository.getConfig()).thenReturn(storedConfig);
        when(storedConfig.getString("remote", remoteName, "url")).thenReturn("https://example.com/foo.git");
        gitUtilMockedStatic.when(() -> GitUtil.findGitFolder(temporaryFolder)).thenReturn(new File(temporaryFolder, ".git"));
        gitUtilMockedStatic.when(() -> GitUtil.getGitRepository(temporaryFolder)).thenReturn(gitRepository);
        gitUtilMockedStatic.when(() -> GitUtil.getGitCommitId(gitRepository)).thenReturn("testcommitid");
    }
}
