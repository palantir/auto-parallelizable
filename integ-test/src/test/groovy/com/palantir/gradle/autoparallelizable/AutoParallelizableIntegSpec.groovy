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

import nebula.test.IntegrationSpec


class AutoParallelizableIntegSpec extends IntegrationSpec {
    def 'ensure it works'() {
        file('file')
        directory('dir')

        // language=gradle
        buildFile << '''
            import integtest.DoIt.DoItTask
            
            task doIt(type: DoItTask) {
                stringValue = 'heh'
                fileValue = file('file')
                dirValue = file('dir')
                intsValue = [1, 2 ,3] 
                filesValue.from(file('lol1'), file('lol2'))
            }
        '''.stripIndent(true)

        when:
        def stdout = runTasksSuccessfully('doIt').standardOutput

        then:
        stdout.contains 'string: heh'
        stdout.contains 'file: file'
        stdout.contains 'dir: dir'
        stdout.contains 'ints: [1, 2, 3]'
        stdout.contains 'files: lol1, lol2'
    }

    def "@Inject parameters can be specified and injected"() {
        file('file')
        directory('dir')

        // language=gradle
        buildFile << '''
            import integtest.DoItInjectedParameter.DoItInjectedParameterTask
            
            task doIt(type: DoItInjectedParameterTask) {
                stringValue = 'heh'
                fileValue = file('file')
                dirValue = file('dir')
                intsValue = [1, 2 ,3] 
                filesValue.from(file('lol1'), file('lol2'))
            }
        '''.stripIndent(true)

        when:
        def stdout = runTasksSuccessfully('doIt', '-Pautoparallelizable-inject-test=yes').standardOutput

        then:
        stdout.contains 'provider: yes'
        stdout.contains 'string: heh'
        stdout.contains 'file: file'
        stdout.contains 'dir: dir'
        stdout.contains 'ints: [1, 2, 3]'
        stdout.contains 'files: lol1, lol2'
    }

    def 'make sure it is incremental'() {
        /* language=gradle */
        buildFile << '''
            apply plugin: 'java'
            
            dependencies {
                implementation gradleApi()
                
                annotationProcessor buildscript.configurations.classpath.dependencies
                compileOnly buildscript.configurations.classpath.dependencies
            }
            
            println configurations.annotationProcessor.resolve()
        '''.stripIndent(true)

        // language=java
        writeJavaSourceFile '''
            package app;
            
            @com.palantir.gradle.autoparallelizable.AutoParallelizable
            public final class MyCustom {
                interface Params {}
                static void action(Params _params) {}
            } 
        '''.stripIndent(true)

        // language=java
        writeJavaSourceFile '''
            package app;
            public final class Lol {
                public static final String LOL = "foobar";
            }
        '''.stripIndent(true)

        when:
        // First build is a clean and so needs a full recompile
        println runTasks('compileJava').standardOutput

        // Change an unrelated file
        // language=java
        writeJavaSourceFile '''
            package app;
            public final class Lol {}
        '''.stripIndent(true)

        // Second build should not say that it requires a full rebuild
        def stdout = runTasks('compileJava').standardOutput
        println stdout

        then:
        !stdout.contains('Full recompilation')
        !stdout.contains('is not incremental')

        stdout.contains('Incremental compilation of 1 classes')
    }
}
