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
package io.jkube.maven.enricher.api.util;

import java.math.BigDecimal;

/**
 * Utility methods for using durations according to Docker/Go format (https://golang.org/pkg/time/#ParseDuration).
 */
public class GoTimeUtil {


    private static final String[] TIME_UNITS = {"ns", "us", "µs", "ms", "s", "m", "h"};
    private static final long[] UNIT_MULTIPLIERS = {1, 1000, 1_000, 1_000_000, 1_000_000_000, 60L * 1_000_000_000, 3600L * 1_000_000_000};

    private GoTimeUtil() {}

    /**
     * Parses a duration string anr returns its value in seconds.
     *
     * @param duration duration in string
     * @return returns integer value
     */
    public static Integer durationSeconds(String duration) {
        BigDecimal ns = durationNs(duration);
        if (ns == null) {
            return null;
        }

        BigDecimal sec = ns.divide(new BigDecimal(1_000_000_000));
        if (sec.compareTo(new BigDecimal(Integer.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException("Integer Overflow");
        }
        return sec.intValue();
    }

    /**
     * Parses a duration string anr returns its value in nanoseconds.
     *
     * @param durationP duration as a string value
     * @return BigDecimal value of time
     */
    public static BigDecimal durationNs(String durationP) {
        if (durationP == null) {
            return null;
        }
        String duration = durationP.trim();
        if (duration.length() == 0) {
            return null;
        }

        int unitPos = 1;
        while (unitPos < duration.length() && (Character.isDigit(duration.charAt(unitPos)) || duration.charAt(unitPos) == '.')) {
            unitPos++;
        }

        if (unitPos >= duration.length()) {
            throw new IllegalArgumentException("Time unit not found in string: " + duration);
        }

        String tail = duration.substring(unitPos);

        Long multiplier = null;
        Integer unitEnd = null;
        for(int i=0; i<TIME_UNITS.length; i++) {
            if (tail.startsWith(TIME_UNITS[i])) {
                multiplier = UNIT_MULTIPLIERS[i];
                unitEnd = unitPos + TIME_UNITS[i].length();
                break;
            }
        }

        if (multiplier == null) {
            throw new IllegalArgumentException("Unknown time unit in string: " + duration);
        }

        BigDecimal value = new BigDecimal(duration.substring(0, unitPos));
        value = value.multiply(BigDecimal.valueOf(multiplier));

        String remaining = duration.substring(unitEnd);
        BigDecimal remainingValue = durationNs(remaining);
        if (remainingValue != null) {
            value = value.add(remainingValue);
        }

        return value;
    }

}
