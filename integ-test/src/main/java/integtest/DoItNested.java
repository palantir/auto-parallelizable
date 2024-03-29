/*
 * (c) Copyright 2022 Palantir Technologies Inc. All rights reserved.
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

package integtest;

import com.palantir.gradle.autoparallelizable.AutoParallelizable;
import java.io.File;
import java.util.stream.Collectors;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;

@AutoParallelizable
public final class DoItNested {
    public abstract static class DoItNestedTask extends DoItNestedTaskImpl {
        public DoItNestedTask() {
            setDescription("lol");
        }
    }

    public abstract static class AbstractNested {
        @OutputFile
        abstract RegularFileProperty getFileValue();
    }

    public interface Nested {
        @Input
        Property<String> getStringValue();

        @InputFiles
        ConfigurableFileCollection getFilesValue();
    }

    interface DoubleNested {

        @Input
        SetProperty<Integer> getIntsValue();

        @org.gradle.api.tasks.Nested
        Nested getNested();
    }

    interface NestedProperties {
        @org.gradle.api.tasks.Nested
        ListProperty<Nested> getListProperty();

        @org.gradle.api.tasks.Nested
        SetProperty<Nested> getSetProperty();

        @org.gradle.api.tasks.Nested
        MapProperty<String, Nested> getMapProperty();

        @org.gradle.api.tasks.Nested
        Property<Nested> getProperty();
    }

    interface Params {
        @org.gradle.api.tasks.Nested
        DoubleNested getDoubleNested();

        @org.gradle.api.tasks.Nested
        AbstractNested getAbstractNested();

        @InputDirectory
        DirectoryProperty getDirValue();

        @org.gradle.api.tasks.Nested
        NestedProperties getNestedProperties();
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    static void action(Params params) {
        System.out.println("string: "
                + params.getDoubleNested().getNested().getStringValue().get());
        System.out.println("file: "
                + params.getAbstractNested().getFileValue().get().getAsFile().getName());
        System.out.println("dir: " + params.getDirValue().get().getAsFile().getName());
        System.out.println("ints: " + params.getDoubleNested().getIntsValue().get());
        System.out.println("files: "
                + params.getDoubleNested().getNested().getFilesValue().getFiles().stream()
                        .map(File::getName)
                        .collect(Collectors.joining(", ")));
        System.out.println("from property: "
                + params.getNestedProperties()
                        .getProperty()
                        .get()
                        .getStringValue()
                        .get());
        System.out.println("from list: "
                + params.getNestedProperties()
                        .getListProperty()
                        .get()
                        .get(0)
                        .getStringValue()
                        .get());
        System.out.println("from set: "
                + params.getNestedProperties().getSetProperty().get().stream()
                        .findFirst()
                        .get()
                        .getStringValue()
                        .get());
        System.out.println("from map: "
                + params.getNestedProperties()
                        .getMapProperty()
                        .get()
                        .get("map")
                        .getStringValue()
                        .get());
    }

    private DoItNested() {}
}
