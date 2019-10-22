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
package io.jkube.kit.common.util.validator;

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
