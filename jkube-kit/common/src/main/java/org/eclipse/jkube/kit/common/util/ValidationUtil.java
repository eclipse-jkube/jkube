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
package org.eclipse.jkube.kit.common.util;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.validator.ResourceValidator;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.File;
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
            builder.append(violation.getPropertyPath()).append(" ").append(violation.getMessage()).append(" on bean: ").append(leafBean);
        }
        return builder.toString();
    }

    public static void validateIfRequired(File resourceDir, ResourceClassifier classifier, KitLogger log,
                                    boolean skipResourceValidation, boolean failOnValidationError) {
        try {
            if (!skipResourceValidation) {
                new ResourceValidator(resourceDir, classifier, log).validate();
            }
        } catch (ConstraintViolationException e) {
            if (failOnValidationError) {
                log.error("[[R]]" + e.getMessage() + "[[R]]");
                log.error("[[R]]use \"jkube.skipResourceValidation=true\" option to skip the validation[[R]]");
                throw new IllegalStateException("Failed to generate kubernetes descriptor");
            } else {
                log.warn("[[Y]]" + e.getMessage() + "[[Y]]");
            }
        } catch (Exception e) {
            if (failOnValidationError) {
                throw new IllegalStateException("Failed to validate resources", e);
            } else {
                log.warn("Failed to validate resources: %s", e.getMessage());
            }
        }
    }
}
