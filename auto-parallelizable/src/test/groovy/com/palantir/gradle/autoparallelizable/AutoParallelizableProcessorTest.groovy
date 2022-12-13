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

package com.palantir.gradle.autoparallelizable


import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import javax.tools.JavaFileObject
import java.nio.charset.StandardCharsets

import static com.google.testing.compile.CompilationSubject.assertThat

@CompileStatic
class AutoParallelizableProcessorTest {
    @Test
    void test() {
        JavaFileObject source = JavaFileObjects.forSourceString 'com.lol.Lol', /* language=java */ '''
            import com.palantir.gradle.autoparallelizable.AutoParallelizable;
            
            @AutoParallelizable
            class Lol {}
        '''.stripIndent(true)

        Compilation compilation = Compiler.javac()
                .withProcessors(new AutoParallelizableProcessor())
                .compile(source)

        assertThat(compilation).succeeded()

        assertThat(compilation)
                .generatedSourceFile("Blah")
                .contentsAsString(StandardCharsets.UTF_8)
                // language=java
                .isEqualTo '''
                    class Blah {}
                '''.stripIndent(true).strip()
    }
}
