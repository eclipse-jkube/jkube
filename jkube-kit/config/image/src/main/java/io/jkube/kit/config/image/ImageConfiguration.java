package io.jkube.kit.config.image;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.jkube.kit.config.image.build.BuildConfiguration;
import org.apache.commons.lang3.SerializationUtils;

/**
 * @author roland
 * @since 02.09.14
 */
public class ImageConfiguration implements Serializable {

    private String name;

    private String alias;

    private String registry;

    private BuildConfiguration build;

    // Used for injection
    public ImageConfiguration() {}

    public String getName() {
        return name;
    }


    public String getAlias() {
        return alias;
    }

    public BuildConfiguration getBuildConfiguration() {
        return build;
    }


    public String getDescription() {
        return String.format("[%s] %s", new ImageName(name).getFullName(), (alias != null ? "\"" + alias + "\"" : "")).trim();
    }

    public String getRegistry() {
        return registry;
    }

    @Override
    public String toString() {
        return String.format("ImageConfiguration {name='%s', alias='%s'}", name, alias);
    }

    public String[] validate(NameFormatter nameFormatter) {
        name = nameFormatter.format(name);
        List<String> apiVersions = new ArrayList<>();
        if (build != null) {
            apiVersions.add(build.validate());
        }
        return apiVersions.stream().filter(Objects::nonNull).toArray(String[]::new);
    }

    // =========================================================================
    // Builder for image configurations

    public static class Builder {
        protected ImageConfiguration config;

        public Builder()  {
            this(null);
        }

        public Builder(ImageConfiguration that) {
            if (that == null) {
                this.config = new ImageConfiguration();
            } else {
                this.config = SerializationUtils.clone(that);
            }
        }

        public Builder name(String name) {
            config.name = name;
            return this;
        }

        public Builder alias(String alias) {
            config.alias = alias;
            return this;
        }

        public Builder buildConfig(BuildConfiguration buildConfig) {
            config.build = buildConfig;
            return this;
        }

        public Builder registry(String registry) {
            config.registry = registry;
            return this;
        }

        public ImageConfiguration build() {
            return config;
        }
    }

    // =====================================================================
    /**
     * Format an image name by replacing certain placeholders
     */
    public interface NameFormatter {
        String format(String name);

        NameFormatter IDENTITY = name -> name;
    }
}
