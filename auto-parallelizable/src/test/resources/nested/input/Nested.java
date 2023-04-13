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
import org.gradle.api.provider.Property;

@AutoParallelizable
public final class Nested {
    public abstract class NestedTask extends NestedTaskImpl {
        public NestedTask() {
            setDescription("lol");
        }
    }

    interface NestedInterface {
        public Property<String> getString();
    }

    interface DoublyNested {

        Property<String> getDoubleString();

        @org.gradle.api.tasks.Nested
        NestedInterface getNestedInterface();
    }

    interface TripleNested {

        Property<String> getTripleString();

        Property<Integer> getTripleInteger();

        @org.gradle.api.tasks.Nested
        DoublyNested getDoublyNestedInterface();
    }

    interface Params {
        Property<String> getSomeString();

        @org.gradle.api.tasks.Nested
        NestedInterface getNestedInterface();

        @org.gradle.api.tasks.Nested
        DoublyNested getDoublyNestedInterface();

        @org.gradle.api.tasks.Nested
        TripleNested getTripleNestedInterface();
    }

    static void action(Params params) {
        System.out.println("Hello " + params.getSomeString().get());
    }
}
