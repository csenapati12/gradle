/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.tasks.diagnostics.internal.graph.nodes;

import org.gradle.api.artifacts.result.ComponentSelectionDescriptor;
import org.gradle.api.artifacts.result.ComponentSelectionReason;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.ComponentSelectionDescriptorInternal;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.ComponentSelectionReasonInternal;

import java.util.List;

public abstract class SelectionReasonHelper {
    public static String getReasonDescription(ComponentSelectionReason reason) {
        String description = reason.getDescription();
        if (reason.isExpected() && !((ComponentSelectionReasonInternal) reason).hasCustomDescriptions()) {
            description = null;
        }
        return description;
    }

    public static String getLastCustomDescription(ComponentSelectionReason reason) {
        List<ComponentSelectionDescriptor> descriptions = reason.getDescriptions();
        String result = null;
        for (ComponentSelectionDescriptor description : descriptions) {
            if (((ComponentSelectionDescriptorInternal)description).hasCustomDescription()) {
                result = description.getDescription();
            }
        }
        return result;
    }
}
