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
package org.eclipse.jkube.kit.config.image;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.apache.commons.lang3.SerializationUtils;

/**
 * @author roland
 * @since 02.09.14
 */
public class ImageConfiguration<B extends BuildConfiguration<?>> implements Serializable {

    private String name;

    private String alias;

    private String registry;

    private B build;

    protected ImageConfiguration() {}

    public String getName() {
        return name;
    }


    public String getAlias() {
        return alias;
    }

    public B getBuildConfiguration() {
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
    public static class TypedBuilder<B extends BuildConfiguration<?>, I extends ImageConfiguration<B>> {

        protected final ImageConfiguration<B> config;

        protected TypedBuilder(I config) {
            this.config = config;
        }

        public TypedBuilder<B, I> name(String name) {
            config.name = name;
            return this;
        }

        public TypedBuilder<B, I> alias(String alias) {
            config.alias = alias;
            return this;
        }

        public TypedBuilder<B, I> buildConfig(B buildConfig) {
            config.build = buildConfig;
            return this;
        }

        public TypedBuilder<B, I> registry(String registry) {
            config.registry = registry;
            return this;
        }

        public I build() {
            return (I)config;
        }

    }

    public static class Builder extends TypedBuilder<BuildConfiguration<?>, ImageConfiguration<BuildConfiguration<?>>>{
        public Builder() {
            this(null);
        }

        public Builder(ImageConfiguration<BuildConfiguration<?>> that) {
            super(that == null ? new ImageConfiguration<>() : SerializationUtils.clone(that));
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
