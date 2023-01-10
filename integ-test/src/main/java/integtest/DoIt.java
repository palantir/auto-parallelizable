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
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;

@AutoParallelizable
public final class DoIt {
    public abstract static class DoItTask extends DoItTaskImpl {
        public DoItTask() {
            setDescription("lol");
        }
    }

    interface Params {
        @Input
        Property<String> getStringValue();

        @OutputFile
        RegularFileProperty getFileValue();

        @InputDirectory
        DirectoryProperty getDirValue();

        @Input
        SetProperty<Integer> getIntsValue();

        @InputFiles
        ConfigurableFileCollection getFilesValue();

        default String stringsRealValue() {
            return getStringValue().get();
        }
    }

    @SuppressWarnings("checkstyle:RegexpSinglelineJava")
    static void action(Params params) {
        System.out.println("string: " + params.getStringValue().get());
        System.out.println("file: " + params.getFileValue().get().getAsFile().getName());
        System.out.println("dir: " + params.getDirValue().get().getAsFile().getName());
        System.out.println("ints: " + params.getIntsValue().get());
        System.out.println("files: "
                + params.getFilesValue().getFiles().stream().map(File::getName).collect(Collectors.joining(", ")));
    }

    private DoIt() {}
}
