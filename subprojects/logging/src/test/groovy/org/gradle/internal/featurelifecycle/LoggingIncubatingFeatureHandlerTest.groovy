/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.internal.featurelifecycle

import org.gradle.internal.featurelifecycle.FeatureUsage.FeatureType
import spock.lang.Subject

@Subject(LoggingIncubatingFeatureHandler)
class LoggingIncubatingFeatureHandlerTest extends AbstractLoggingFeatureHandlerTest {
    CollectingDeprecatedFeatureHandler delegate = Mock(CollectingDeprecatedFeatureHandler)

    @Override
    FeatureHandler createHandler() {
        return new LoggingIncubatingFeatureHandler(delegate)
    }

    @Override
    FeatureType getFeatureType() {
        return FeatureType.INCUBATING
    }

    def 'only incubating feature would be logged'() {
        when:
        createHandler().featureUsed(new FeatureUsage('message', getClass(), FeatureType.DEPRECATED))

        then:
        outputEventListener.events.empty
        1 * delegate.featureUsed(_)
        0 * _._
    }
}