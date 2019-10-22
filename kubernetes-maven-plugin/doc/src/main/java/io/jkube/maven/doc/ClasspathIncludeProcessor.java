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
package io.jkube.maven.doc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;

/**
 * @author roland
 * @since 11/07/16
 */
public class ClasspathIncludeProcessor extends IncludeProcessor {

    @Override
    public boolean handles(String target) {
        return target.startsWith("classpath:");
    }

    @Override
    public void process(Document document,
                        PreprocessorReader reader,
                        String target,
                        Map<String, Object> attributes) {
        List<String> content = readContent(target);
        for (int i = content.size() - 1; i >= 0; i--) {
            String line = content.get(i);
            // See also https://github.com/asciidoctor/asciidoctorj/issues/437#issuecomment-192669617
            // Seems to be a hack to avoid mangling of paragraphes
            if (line.trim().equals("")) {
                line = " ";
            }
            reader.push_include(line, target, target, 1, attributes);
        }
    }

    private List<String> readContent(String target) {
        String resourcePath = target.substring("classpath:".length());
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("No resource " + target + " could be found in the classpath");
        }

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
        String line;
        List<String> lines = new ArrayList<>();
        try {
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return lines;
    }
}
