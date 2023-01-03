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

import org.junit.jupiter.api.Test

import static com.google.testing.compile.CompilationSubject.assertThat;

import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler
import com.google.testing.compile.JavaFileObjects
import groovy.transform.CompileStatic

import javax.tools.JavaFileObject

@CompileStatic
class BadInputsTest {
    @Test
    void 'Params interface must exist'() {
        assertErrorProducedByFile "Could not find interface named 'Params' in class app.Test", /* language=java */ '''
            @AutoParallelizable
            public final class Test {
                static void action() {}
            }
        '''
    }

    @Test
    void 'Params must be an interface'() {
        assertErrorProducedByFile "Params type must be an interface - was a class", /* language=java */ '''
            @AutoParallelizable
            public final class Test{
                class Params {}
    
                static void action(Params params) {} 
            }
        '''
    }

    @Test
    void 'action method must exist'() {
        assertErrorProducedByFile "There must be a 'static void action(Params)' method that performs the task action", /* language=java */ '''
            @AutoParallelizable
            public final class Test {
                interface Params {}
            }
        '''
    }

    private static void assertErrorProducedByFile(String error, String file) {
        String modifiedFile = /*language=java */ """
            package app;
            
            import com.palantir.gradle.autoparallelizable.AutoParallelizable;
        """.stripIndent(true) + file.stripIndent(true)

        JavaFileObject noAction = JavaFileObjects.forSourceString 'app.Test', modifiedFile

        Compilation compilation = Compiler.javac()
                .withProcessors(new AutoParallelizableProcessor())
                .compile(noAction)

        assertThat(compilation).failed()

        assertThat(compilation).hadErrorContaining(error)
    }
}
