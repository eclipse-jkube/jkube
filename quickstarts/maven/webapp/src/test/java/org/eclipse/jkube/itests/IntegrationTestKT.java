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
package org.eclipse.jkube.itests;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.fabric8.kubernetes.assertions.Assertions.assertThat;

/**
 * Tests that the Kubernetes resources (Services, Replication Controllers and
 * Pods) can be provisioned and start up correctly.
 *
 * This test creates a new Kubernetes Namespace for the duration of the test.
 * For more information see: http://fabric8.io/guide/testing.html
 */
@RunWith(Arquillian.class)
public class IntegrationTestKT {

	@ArquillianResource
	protected KubernetesClient kubernetes;

	@Test
	public void testRunningPodStaysUp() throws Exception {
		assertThat(kubernetes).deployments().pods().isPodReadyForPeriod();
	}
}