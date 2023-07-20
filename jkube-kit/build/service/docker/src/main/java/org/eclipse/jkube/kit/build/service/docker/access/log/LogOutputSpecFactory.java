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
package org.eclipse.jkube.kit.build.service.docker.access.log;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.LogConfiguration;
import org.eclipse.jkube.kit.config.image.RunImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.helper.FormatParameterReplacer;

import java.util.HashMap;
import java.util.Map;


/**
 * @author roland
 * @since 26/09/15
 */
public class LogOutputSpecFactory {
    private static final String DEFAULT_PREFIX_FORMAT = "%a> ";
    private final boolean useColor;
    private final boolean logStdout;
    private final String logDate;

    public LogOutputSpecFactory(boolean useColor, boolean logStdout) {
        this(useColor, logStdout, null);
    }

    public LogOutputSpecFactory(boolean useColor, boolean logStdout, String logDate) {
        this.useColor = useColor;
        this.logStdout = logStdout;
        this.logDate = logDate;
    }

    // ================================================================================================

    public LogOutputSpec createSpec(String containerId, ImageConfiguration imageConfiguration) {
        LogOutputSpec.LogOutputSpecBuilder builder = LogOutputSpec.builder();
        LogConfiguration logConfig = extractLogConfiguration(imageConfiguration);

        addLogFormat(builder, logConfig);
        addPrefix(builder, logConfig.getPrefix(), imageConfiguration, containerId);
        builder.file(logConfig.getFileLocation())
                .useColor(useColor)
                .logStdout(logStdout)
                .colorString(logConfig.getColor());

        return builder.build();
    }

    private void addPrefix(LogOutputSpec.LogOutputSpecBuilder builder, String logPrefix, ImageConfiguration imageConfig, String containerId) {
        String prefixFormat = logPrefix;
        if (prefixFormat == null) {
            prefixFormat = DEFAULT_PREFIX_FORMAT;
        }
        FormatParameterReplacer formatParameterReplacer = new FormatParameterReplacer(getPrefixFormatParameterLookups(imageConfig, containerId));
        builder.prefix(formatParameterReplacer.replace(prefixFormat));
    }

    private Map<String, FormatParameterReplacer.Lookup> getPrefixFormatParameterLookups(final ImageConfiguration imageConfig, final String containerId) {
        Map<String, FormatParameterReplacer.Lookup> ret = new HashMap<>();

        ret.put("z", () -> "");
        ret.put("c", () -> containerId.substring(0, 6));
        ret.put("C", () -> containerId);
        ret.put("a", () -> {
            String alias = imageConfig.getAlias();
            if (alias != null) {
                return alias;
            }
            return containerId.substring(0, 6);
        });
        ret.put("n", imageConfig::getName);

        return ret;
    }

    private void addLogFormat(LogOutputSpec.LogOutputSpecBuilder builder, LogConfiguration logConfig) {
        String logFormat = logConfig.getDate() != null ? logConfig.getDate() : logDate;
        if (logFormat != null && logFormat.equalsIgnoreCase("true")) {
            logFormat = "DEFAULT";
        }
        if (logFormat != null) {
            builder.timeFormatterString(logFormat);
        }
    }

    private LogConfiguration extractLogConfiguration(ImageConfiguration imageConfiguration) {
        RunImageConfiguration runConfig = imageConfiguration.getRunConfiguration();
        LogConfiguration logConfig = null;
        if (runConfig != null) {
            logConfig = runConfig.getLog();
        }
        if (logConfig == null) {
            logConfig = LogConfiguration.DEFAULT;
        }
        return logConfig;
    }
}

