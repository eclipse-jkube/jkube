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
package org.eclipse.jkube.springboot.enricher;


public class SpringBoot2HealthCheckEnricherTest extends AbstractSpringBootHealthCheckEnricherTestSupport {

    @Override
    protected String getSpringBootVersion() {
        return "2.0.0.RELEASE";
    }

    @Override
    protected String getActuatorDefaultBasePath() {
        return "/actuator";
    }
}
