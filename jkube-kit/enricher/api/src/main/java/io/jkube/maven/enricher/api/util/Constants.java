package io.jkube.maven.enricher.api.util;

/**
 */
// TODO-F8SPEC : Should be move the to AppCatalog mojo and must not be in the general available util package
// Also consider whether the Constants class pattern makes (should probably change to real enums ???)
public class Constants {
    public static final String RESOURCE_SOURCE_URL_ANNOTATION = "maven.jkube.io/source-url";
    public static final String RESOURCE_APP_CATALOG_ANNOTATION = "maven.jkube.io/app-catalog";
}

