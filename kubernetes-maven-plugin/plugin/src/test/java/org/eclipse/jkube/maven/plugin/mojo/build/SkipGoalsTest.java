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
package org.eclipse.jkube.maven.plugin.mojo.build;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class SkipGoalsTest {

    @Mock
    private KitLogger log;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MojoExecution mojoExecution;

    @Spy
    @InjectMocks
    BuildMojo buildMojo;

    @BeforeEach
    void setUp() {
      openMocks(this);
    }

    private void setupBuildGoal() throws Exception {
      doNothing().when(buildMojo).init();
      doNothing().when(buildMojo).doExecute();
      when(mojoExecution.getMojoDescriptor().getFullGoalName()).thenReturn("k8s:build");
    }

    @Test
    void should_execute_build_goal_if_skip_false() throws Exception {
        setupBuildGoal();
        // given
        buildMojo.skip = false;
        // when
        buildMojo.execute();
        // then
        verify(buildMojo).doExecute();
    }

    @Test
    void should_log_informative_message_when_build_goal_is_skipped() throws Exception {
        setupBuildGoal();
        // given
        buildMojo.skip = true;
        // when
        buildMojo.execute();
        // then
        verify(buildMojo, never()).doExecute();
        verify(log).info("`%s` goal is skipped.", "k8s:build");
    }

    @Spy
    @InjectMocks
    ApplyMojo applyMojo;

    private void setupApplyGoal() throws Exception {
      doNothing().when(applyMojo).init();
      doNothing().when(applyMojo).executeInternal();
      when(mojoExecution.getMojoDescriptor().getFullGoalName()).thenReturn("k8s:apply");
    }

    @Test
    void should_execute_apply_goal_if_skip_false() throws Exception {
        setupApplyGoal();
        // given
        applyMojo.skip = false;
        // when
        applyMojo.execute();
        // then
        verify(applyMojo).executeInternal();
    }

    @Test
    void should_log_informative_message_when_apply_goal_is_skipped() throws Exception {
        setupApplyGoal();
        // given
        applyMojo.skip = true;
        // when
        applyMojo.execute();
        // then
        verify(applyMojo, never()).executeInternal();
        verify(log).info("`%s` goal is skipped.", "k8s:apply");
    }

}
