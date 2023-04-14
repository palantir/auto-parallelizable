/*
 * (c) Copyright 2023 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app;

import com.palantir.gradle.autoparallelizable.AutoParallelizable;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Nested;

@AutoParallelizable
public final class Abstract {
    public abstract class AbstractTask extends AbstractTaskImpl {
        public AbstractTask() {
            setDescription("lol");
        }
    }

    interface Nested {
        public Property<String> getString();
    }

    abstract static class AbstractParams {
        public abstract Property<String> getSettableNonNestedString();

        public abstract Property<String> getStringWithParameters(String parameter);

        private Property<String> getPrivateProperty() {
            return null;
        }

        public abstract Nested getNonNestedNonSettableProperty();
    }

    interface Params {
        @org.gradle.api.tasks.Nested
        AbstractParams getAbstractParams();
    }

    static void action(Params params) {
        System.out.println("Hello "
                + params.getAbstractParams().getSettableNonNestedString().get());
    }
}
