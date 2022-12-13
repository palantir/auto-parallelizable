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
        JavaFileObject source = JavaFileObjects.forSourceString 'app.Something', /* language=java */ '''
            package app;

            import com.palantir.gradle.autoparallelizable.AutoParallelizable;
            import org.gradle.api.provider.Property;
            
            @AutoParallelizable
            public final class Something {
                public abstract class SomethingTask extends SomethingTaskImpl {
                    public SomethingTask() {
                        setDescription("lol");
                    }
                }
                
                interface Params {
                    Property<String> getName();
                }
                
                static void execute(Params params) {
                    System.out.println("Hello " + params.getName().get());
                }
            }
        '''.stripIndent(true)

        Compilation compilation = Compiler.javac()
                .withProcessors(new AutoParallelizableProcessor())
                .compile(source)

        assertThat(compilation).succeeded()

        assertThat(compilation)
                .generatedSourceFile("app.SomethingWorkParams")
                .contentsAsString(StandardCharsets.UTF_8)
                // language=java
                .isEqualTo '''
                    package app;

                    import org.gradle.workers.WorkParameters;

                    interface SomethingWorkParams extends WorkParameters, Something.Params {}
                '''.stripIndent(true).stripLeading()

        assertThat(compilation)
                .generatedSourceFile("app.SomethingWorkAction")
                .contentsAsString(StandardCharsets.UTF_8)
                // language=java
                .isEqualTo '''
                    package app;

                    import java.lang.Override;
                    import org.gradle.workers.WorkAction;

                    abstract class SomethingWorkAction implements WorkAction<SomethingWorkParams> {
                        @Override
                        public final void execute() {
                            Something.execute(getParameters());
                        }
                    }
                '''.stripIndent(true).stripLeading()

        assertThat(compilation)
                .generatedSourceFile("app.SomethingTaskImpl")
                .contentsAsString(StandardCharsets.UTF_8)
                // language=java
                .isEqualTo '''
                    package app;

                    import javax.inject.Inject;
                    import org.gradle.api.DefaultTask;
                    import org.gradle.api.tasks.TaskAction;
                    import org.gradle.workers.WorkerExecutor;

                    abstract class SomethingTaskImpl extends DefaultTask {
                        @Inject
                        protected abstract WorkerExecutor getWorkerExecutor();
                        
                        @TaskAction
                        public final void execute() {
                            getWorkerExecutor().noIsolation().submit(SomethingWorkAction.class, params -> {
                                params.getName().set(getName());
                            });
                        }
                    }
                '''.stripIndent(true).stripLeading()
    }
}
