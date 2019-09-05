package io.jshift.kit.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class KindFilenameMapperUtil {

    public static Map<String, List<String>> loadMappings() {

        final String location = "/META-INF/jshift/kind-filename-type-mapping-default.adoc";
        final String locationMappingProperties =
                EnvUtil.getEnvVarOrSystemProperty("jshift.mapping", "/META-INF/jshift/kind-filename-type-mapping-default.properties");

        try (final InputStream mappingFile = loadContent(location); final InputStream mappingPropertiesFile = loadContent(locationMappingProperties)) {
            final AsciiDocParser asciiDocParser = new AsciiDocParser();
            final Map<String, List<String>> defaultMapping = asciiDocParser.serializeKindFilenameTable(mappingFile);

            if (mappingPropertiesFile != null) {
                PropertiesMappingParser propertiesMappingParser = new PropertiesMappingParser();
                defaultMapping.putAll(propertiesMappingParser.parse(mappingPropertiesFile));
            }

            return defaultMapping;

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static InputStream loadContent(String location) {
        InputStream resourceAsStream = KindFilenameMapperUtil.class.getResourceAsStream(location);

        if (resourceAsStream == null) {
            final File locationFile = new File(location);

            try {
                return new FileInputStream(locationFile);
            } catch (FileNotFoundException e) {
                return null;
            }
        }

        return resourceAsStream;
    }

}

