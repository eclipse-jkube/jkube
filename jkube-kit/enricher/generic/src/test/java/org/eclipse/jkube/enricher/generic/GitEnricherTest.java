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

import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.OpenShiftAnnotations;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GitEnricherTest {

    private static final String GIT_REMOTE_URL = "https://github.com:jkubeio/eclipse-jkube-demo-project.git";
    private static final String GIT_BRANCH = "master";
    private static final String GIT_COMMIT_ID = "058bed285de43aac80b5bf9433b9a3a9c3915e19";

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

    private void assertJkubeAnnotations(Map<String, String> annotations) {
        assertJkubeAnnotationsRemoteUrlAndBranch(annotations);
        assertThat(annotations).containsEntry(JKubeAnnotations.GIT_COMMIT.value(true), GIT_COMMIT_ID);
    }

    private void assertJkubeAnnotationsRemoteUrlAndBranch(Map<String, String> annotations) {
      assertThat(annotations)
          .containsEntry("jkube.io/git-url", GIT_REMOTE_URL)
          .containsEntry("jkube.io/git-branch", GIT_BRANCH);
    }
}
