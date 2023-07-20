/*
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
package org.eclipse.jkube.kit.common.util;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;

import javax.validation.ConstraintViolation;
import java.util.Set;

/**
 */
public class ValidationUtil {

    private ValidationUtil() { }

    public static String createValidationMessage(Set<ConstraintViolation<?>> constraintViolations) {
        if (constraintViolations.isEmpty()) {
            return "No Constraint Validations!";
        }
        StringBuilder builder = new StringBuilder("Constraint Validations: ");
        for (ConstraintViolation<?> violation : constraintViolations) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            Object leafBean = violation.getLeafBean();
            if (leafBean instanceof HasMetadata) {
                HasMetadata hasMetadata = (HasMetadata) leafBean;
                ObjectMeta metadata = hasMetadata.getMetadata();
                if (metadata != null) {
                    leafBean = "" + hasMetadata.getKind() + ": " + metadata;
                }
            }
            builder.append(violation.getPropertyPath() + " " + violation.getMessage() + " on bean: " + leafBean);
        }
        return builder.toString();
    }
}
