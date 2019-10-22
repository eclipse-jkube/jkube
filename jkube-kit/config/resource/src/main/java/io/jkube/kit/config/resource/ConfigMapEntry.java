package io.jkube.kit.config.resource;

import java.util.Objects;

public class ConfigMapEntry {

    private String name;
    private String value;
    private String file;

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getFile() {
        return file;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ConfigMapEntry that = (ConfigMapEntry) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(value, that.value) &&
                Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, file);
    }
}

