package io.jshift.kit.config.resource;

import java.util.ArrayList;
import java.util.List;

public class ConfigMap {

    private String name;
    private List<ConfigMapEntry> entries = new ArrayList<>();

    public void addEntry(ConfigMapEntry configMapEntry) {
        this.entries.add(configMapEntry);
    }

    public List<ConfigMapEntry> getEntries() {
        return entries;
    }

    /**
     * Set the name of ConfigMap.
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return the name of ConfigMap.
     * @return
     */
    public String getName() {
        return name;
    }
}


