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
package org.eclipse.jkube.kit.common.util.validator;

import io.fabric8.kubernetes.schema.validator.ValidationMessage;

/**
 * Model to represent ignore validation rules as tuple of json tree path and constraint ruleType
 * for resource descriptor validations
 */
public class IgnorePortValidationRule implements ValidationRule {

    @Override
    public boolean ignore(ValidationMessage validationMessage) {
        return validationMessage.getLevel() == ValidationMessage.Level.ERROR &&
          (validationMessage.getSchema() != null && validationMessage.getSchema().endsWith("/io.k8s.apimachinery.pkg.util.intstr.IntOrString")) &&
          validationMessage.getMessage().matches(".+/(targetPort|port)'] Instance type \\(integer\\) does not match any allowed primitive type \\(allowed: \\[\\\"string\\\"\\]\\)$");
    }
}
