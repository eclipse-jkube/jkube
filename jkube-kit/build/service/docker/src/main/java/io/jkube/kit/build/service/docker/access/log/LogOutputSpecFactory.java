package io.jkube.kit.build.service.docker.access.log;

import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.build.service.docker.config.LogConfiguration;
import io.jkube.kit.build.service.docker.config.RunImageConfiguration;
import io.jkube.kit.build.service.docker.helper.FormatParameterReplacer;

import java.util.HashMap;
import java.util.Map;


/**
 * @author roland
 * @since 26/09/15
 */
public class LogOutputSpecFactory {
    private static final String DEFAULT_PREFIX_FORMAT = "%a> ";
    private boolean useColor;
    private boolean logStdout;
    private String logDate;

    public LogOutputSpecFactory(boolean useColor, boolean logStdout, String logDate) {
        this.useColor = useColor;
        this.logStdout = logStdout;
        this.logDate = logDate;
    }

    // ================================================================================================

    public LogOutputSpec createSpec(String containerId, ImageConfiguration imageConfiguration) {
        LogOutputSpec.Builder builder = new LogOutputSpec.Builder();
        LogConfiguration logConfig = extractLogConfiguration(imageConfiguration);

        addLogFormat(builder, logConfig);
        addPrefix(builder, logConfig.getPrefix(), imageConfiguration, containerId);
        builder.file(logConfig.getFileLocation())
                .useColor(useColor)
                .logStdout(logStdout)
                .color(logConfig.getColor());

        return builder.build();
    }

    private void addPrefix(LogOutputSpec.Builder builder, String logPrefix, ImageConfiguration imageConfig, String containerId) {
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

    private void addLogFormat(LogOutputSpec.Builder builder, LogConfiguration logConfig) {
        String logFormat = logConfig.getDate() != null ? logConfig.getDate() : logDate;
        if (logFormat != null && logFormat.equalsIgnoreCase("true")) {
            logFormat = "DEFAULT";
        }
        if (logFormat != null) {
            builder.timeFormatter(logFormat);
        }
    }

    private LogConfiguration extractLogConfiguration(ImageConfiguration imageConfiguration) {
        RunImageConfiguration runConfig = imageConfiguration.getRunConfiguration();
        LogConfiguration logConfig = null;
        if (runConfig != null) {
            logConfig = runConfig.getLogConfiguration();
        }
        if (logConfig == null) {
            logConfig = LogConfiguration.DEFAULT;
        }
        return logConfig;
    }
}

