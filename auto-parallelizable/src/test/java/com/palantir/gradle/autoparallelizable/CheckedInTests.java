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

package com.palantir.gradle.autoparallelizable;

import static com.google.testing.compile.CompilationSubject.assertThat;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import groovy.transform.CompileStatic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaFileObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@CompileStatic
class CheckedInTests {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package (?<package>[\\w.]+);");

    @ParameterizedTest
    @MethodSource("allCheckedInTests")
    void checked_in_tests(Path testRoot) {
        Path testInputs = testRoot.resolve("input");

        JavaFileObject[] inputs = childrenOf(testInputs).stream()
                .map(CheckedInTests::javaFileObjectFromFile)
                .toArray(JavaFileObject[]::new);

        Compilation compilation = Compiler.javac()
                .withProcessors(new AutoParallelizableProcessor())
                .compile(inputs);

        assertThat(compilation).succeededWithoutWarnings();

        Set<String> seenFiles = new HashSet<>();

        childrenOf(testRoot.resolve("output")).forEach(output -> {
            seenFiles.add(output.getFileName().toString());
            assertThat(compilation)
                    .generatedSourceFile(fullyQualifiedName(output))
                    .contentsAsUtf8String()
                    .isEqualTo(readString(output));
        });

        compilation.generatedSourceFiles().stream()
                .filter(javaFileObject ->
                        !seenFiles.contains(Iterables.getLast(Splitter.on('/').splitToList(javaFileObject.getName()))))
                .forEach(javaFileObject -> {
                    throw new RuntimeException(
                            "Produced source file output " + javaFileObject.getName() + " which was not expected");
                });
    }

    private static List<Path> allCheckedInTests() {
        return childrenOf(Paths.get("src/test/resources"));
    }

    private static List<Path> childrenOf(Path directory) {
        try (Stream<Path> children = Files.list(directory)) {
            return children.collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JavaFileObject javaFileObjectFromFile(Path path) {
        return JavaFileObjects.forSourceString(fullyQualifiedName(path), readString(path));
    }

    private static String fullyQualifiedName(Path path) {
        String text = readString(path);

        Matcher matcher = PACKAGE_PATTERN.matcher(text);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not find package in file " + path);
        }

        return matcher.group("package") + "." + path.getFileName().toString().replace(".java", "");
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
