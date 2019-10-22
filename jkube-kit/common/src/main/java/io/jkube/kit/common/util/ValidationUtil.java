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
package io.jkube.kit.common.util;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;

import javax.validation.ConstraintViolation;
import java.util.Set;

/**
 */
public class ValidationUtil {
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
