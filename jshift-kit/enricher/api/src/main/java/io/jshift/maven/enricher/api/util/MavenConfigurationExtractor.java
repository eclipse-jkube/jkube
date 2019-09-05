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
package io.jshift.maven.enricher.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class MavenConfigurationExtractor {

    private MavenConfigurationExtractor() {
    }

    /**
     * Transforms the Dom object into a Map.
     * This Map can contain pairs key/value where value can be a simple type, another Map (Inner objects)
     * and a list of simple types.
     *
     * Currently it is NOT supported List of complex objects.
     * @param root object.
     * @return Map of DOM structure.
     */
    public static Map<String, Object> extract(Xpp3Dom root) {
        if (root == null) {
            return new HashMap<>();
        }

        return getElement(root);
    }

    private static Map<String, Object> getElement(Xpp3Dom element) {

        final Map<String, Object> conf = new HashMap<>();

        final Xpp3Dom[] currentElements = element.getChildren();

        for (Xpp3Dom currentElement: currentElements) {
            if (isSimpleType(currentElement)) {

                if (isAListOfElements(conf, currentElement)) {
                    addAsList(conf, currentElement);
                } else {
                    conf.put(currentElement.getName(), currentElement.getValue());
                }
            } else {
                conf.put(currentElement.getName(), getElement(currentElement));
            }
        }

        return conf;

    }

    private static void addAsList(Map<String, Object> conf, Xpp3Dom currentElement) {
        final Object insertedValue = conf.get(currentElement.getName());
        if (insertedValue instanceof List) {
            ((List) insertedValue).add(currentElement.getValue());
        } else {
            final List<String> list = new ArrayList<>();
            list.add((String) insertedValue);
            list.add(currentElement.getValue());
            conf.put(currentElement.getName(), list);
        }
    }

    private static boolean isAListOfElements(Map<String, Object> conf, Xpp3Dom currentElement) {
        return conf.containsKey(currentElement.getName());
    }

    private static boolean isSimpleType(Xpp3Dom currentElement) {
        return currentElement.getChildCount() == 0;
    }
}
