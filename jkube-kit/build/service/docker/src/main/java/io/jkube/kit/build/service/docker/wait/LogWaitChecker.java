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
package io.jkube.kit.build.service.docker.wait;

import io.jkube.kit.build.service.docker.access.DockerAccess;
import io.jkube.kit.build.service.docker.access.log.LogGetHandle;
import io.jkube.kit.common.KitLogger;

import java.util.concurrent.CountDownLatch;

/**
 * @author roland
 * @since 25/03/2017
 */
public class LogWaitChecker implements WaitChecker, LogWaitCheckerCallback {

    private final String containerId;
    private final String logPattern;
    private final KitLogger log;

    private final CountDownLatch latch;
    private final LogGetHandle logHandle;

    public LogWaitChecker(final String logPattern, final DockerAccess dockerAccess, final String containerId, final KitLogger log) {
        this.containerId = containerId;
        this.logPattern = logPattern;
        this.log = log;

        this.latch = new CountDownLatch(1);
        this.logHandle = dockerAccess.getLogAsync(containerId, new LogMatchCallback(log, this, logPattern));
    }

    @Override
    public void matched() {
        latch.countDown();
        log.info("Pattern '%s' matched for container %s", logPattern, containerId);
    }

    @Override
    public boolean check() {
        return latch.getCount() == 0;
    }

    @Override
    public void cleanUp() {
        if (logHandle != null) {
            logHandle.finish();
        }
    }

    @Override
    public String getLogLabel() {
        return "on log out '" + logPattern + "'";
    }
}