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
package org.eclipse.jkube.kit.common;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author roland
 * @since 24.10.18
 */
public class TimeUtil {
    private TimeUtil() { }

    /**
     * Calculate the duration between now and the given time
     *
     * Taken mostly from http://stackoverflow.com/a/5062810/207604 . Kudos to @dblevins
     *
     * @param start starting time (in milliseconds)
     * @return time in seconds
     *
     */
    public static String formatDurationTill(long start) {
        long duration = System.currentTimeMillis() - start;
        StringBuilder res = new StringBuilder();

        TimeUnit current = HOURS;

        while (duration > 0) {
            long temp = current.convert(duration, MILLISECONDS);

            if (temp > 0) {
                duration -= current.toMillis(temp);
                res.append(temp).append(" ").append(current.name().toLowerCase());
                if (temp < 2) res.deleteCharAt(res.length() - 1);
                res.append(", ");
            }
            if (current == SECONDS) {
                break;
            }
            current = TimeUnit.values()[current.ordinal() - 1];
        }
        if (res.lastIndexOf(", ") < 0) {
            return duration + " " + MILLISECONDS.name().toLowerCase();
        }
        res.deleteCharAt(res.length() - 2);
        int i = res.lastIndexOf(", ");
        if (i > 0) {
            res.deleteCharAt(i);
            res.insert(i, " and");
        }

        return res.toString();
    }

}
