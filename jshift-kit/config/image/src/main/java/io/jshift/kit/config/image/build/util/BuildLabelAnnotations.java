package io.jshift.kit.config.image.build.util;

public enum BuildLabelAnnotations {
    BUILD_DATE("build-date"),
    NAME("name"),
    DESCRIPTION("description"),
    USAGE("usage"),
    URL("url"),
    VCS_URL("vcs-url"),
    VCS_REF("vcs-ref"),
    VENDOR("vendor"),
    VERSION("version"),
    SCHEMA_VERSION("schema-version");

    private final String annotation;

    BuildLabelAnnotations(String anno) {
        this.annotation = "org.label-schema." + anno;
    }

    public String value() {
        return annotation;
    }

    @Override
    public String toString() {
        return value();
    }
}
