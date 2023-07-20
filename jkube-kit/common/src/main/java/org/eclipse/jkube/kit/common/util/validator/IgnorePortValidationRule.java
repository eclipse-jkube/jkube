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

import com.networknt.schema.ValidationMessage;

/**
 * Model to represent ignore validation rules as tuple of json tree path and constraint ruleType
 * for resource descriptor validations
 */
public class IgnorePortValidationRule implements ValidationRule {

    private final String ruleType;

    public IgnorePortValidationRule(String aRuleType) {
        this.ruleType = aRuleType;
    }

    public String getType() {
        return ruleType;
    }

    @Override
    public boolean ignore(ValidationMessage msg) {
        return msg.getType().equalsIgnoreCase(TYPE) &&
                msg.getMessage().contains(": integer found, object expected");
    }
}
