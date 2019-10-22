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
package io.jkube.kit.common.util.validator;

import com.networknt.schema.ValidationMessage;

public class IgnoreResourceMemoryLimitRule implements ValidationRule {
    private final String ruleType;

    public IgnoreResourceMemoryLimitRule(String aRuleType) { this.ruleType = aRuleType; }

    public String getType() { return ruleType; }

    @Override
    public boolean ignore(ValidationMessage aValidationMsg) {
        return aValidationMsg.getType().equalsIgnoreCase(TYPE)
                && aValidationMsg.getMessage().contains("string found, object expected");
    }
}
