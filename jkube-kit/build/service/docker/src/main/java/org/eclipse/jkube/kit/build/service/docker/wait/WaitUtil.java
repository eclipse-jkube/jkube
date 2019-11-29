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
package org.eclipse.jkube.kit.build.service.docker.wait;

import java.util.Arrays;
import java.util.concurrent.*;


/**
 * @author roland
 * @since 18.10.14
 */
public class WaitUtil {

    // how long to wait at max when doing a http ping
    private static final long DEFAULT_MAX_WAIT = 10 * 1000L;

    // How long to wait between pings
    private static final long WAIT_RETRY_WAIT = 500;


    private WaitUtil() {}

    public static long wait(int wait, Callable<Void> callable) throws ExecutionException, WaitTimeoutException {
        long now = System.currentTimeMillis();
        if (wait > 0) {
            try {
                FutureTask<Void> task = new FutureTask<>(callable);
                task.run();

                task.get(wait, TimeUnit.SECONDS);
            } catch (@SuppressWarnings("unused") InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (@SuppressWarnings("unused") TimeoutException e) {
                throw new WaitTimeoutException("timed out waiting for execution to complete: " + e, delta(now));
            }
        }
        return delta(now);
    }

    public static long wait(Precondition precondition, int maxWait, WaitChecker... checkers) throws WaitTimeoutException, PreconditionFailedException {
        return wait(precondition, maxWait, Arrays.asList(checkers));
    }

    public static long wait(Precondition precondition, int maxWait, Iterable<WaitChecker> checkers) throws WaitTimeoutException, PreconditionFailedException {
        long max = maxWait > 0 ? maxWait : DEFAULT_MAX_WAIT;
        long now = System.currentTimeMillis();
        try {
            do {
                if (!precondition.isOk()) {
                    // Final check, could be that the check just succeeded
                    if (check(checkers)) {
                        return delta(now);
                    }
                    throw new PreconditionFailedException("Precondition failed", delta(now));
                } else {
                    if (check(checkers)) {
                        return delta(now);
                    }
                }
                sleep(WAIT_RETRY_WAIT);
            } while (delta(now) < max);
            throw new WaitTimeoutException("No checker finished successfully", delta(now));
        } finally {
            precondition.cleanup();
            cleanup(checkers);
        }
    }

    private static boolean check(Iterable<WaitChecker> checkers) {
        for (WaitChecker checker : checkers) {
            if (checker.check()) {
                return true;
            }
        }
        return false;
    }

    // Give checkers a possibility to clean up
    private static void cleanup(Iterable<WaitChecker> checkers) {
        for (WaitChecker checker : checkers) {
            checker.cleanUp();
        }
    }

    /**
     * Sleep a bit
     *
     * @param millis how long to sleep in milliseconds
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ...
            Thread.currentThread().interrupt();
        }
    }

    private static long delta(long now) {
        return System.currentTimeMillis() - now;
    }


    /**
     * Simple interfact for checking some preconditions
     */
    public interface Precondition {
        // true if precondition is met
        boolean isOk();
        // cleanup which might be needed if the check is done.
        void cleanup();
    }
}

