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

import com.google.auto.service.AutoService;
import com.palantir.goethe.Goethe;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Inject;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkerExecutor;

@AutoService(Processor.class)
public final class AutoParallelizableProcessor extends AbstractProcessor {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(AutoParallelizable.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> _annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        roundEnv.getElementsAnnotatedWith(AutoParallelizable.class).forEach(element -> {
            paralleliseTask((TypeElement) element);
        });

        return false;
    }

    private void paralleliseTask(TypeElement typeElement) {
        List<TypeElement> possibleParams = typeElement.getEnclosedElements().stream()
                .filter(subElement -> subElement.getKind().equals(ElementKind.INTERFACE))
                .map(TypeElement.class::cast)
                .filter(element -> element.getSimpleName().toString().equals("Params"))
                .collect(Collectors.toList());

        TypeElement params = possibleParams.get(0);

        String packageName = processingEnv
                .getElementUtils()
                .getPackageOf(typeElement)
                .getQualifiedName()
                .toString();

        ClassName workParamsClassName = ClassName.get(packageName, typeElement.getSimpleName() + "WorkParams");
        ClassName workActionClassName = ClassName.get(packageName, typeElement.getSimpleName() + "WorkAction");

        emitWorkParams(params, packageName, workParamsClassName);

        emitWorkAction(typeElement, packageName, workParamsClassName);

        emitTaskImpl(typeElement, params, packageName, workActionClassName);
    }

    private void emitWorkParams(TypeElement params, String packageName, ClassName workParamsClassName) {
        TypeSpec workParamsType = TypeSpec.interfaceBuilder(workParamsClassName)
                .addSuperinterface(ClassName.get(WorkParameters.class))
                .addSuperinterface(params.asType())
                .build();

        JavaFile workParams = JavaFile.builder(packageName, workParamsType)
                .skipJavaLangImports(true)
                .build();

        Goethe.formatAndEmit(workParams, processingEnv.getFiler());
    }

    private void emitWorkAction(TypeElement typeElement, String packageName, ClassName workParamsClassName) {
        List<ExecutableElement> possibleExecutes = typeElement.getEnclosedElements().stream()
                .filter(subElement -> subElement.getKind().equals(ElementKind.METHOD))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getSimpleName().toString().equals("action"))
                .collect(Collectors.toList());

        ExecutableElement _action = possibleExecutes.get(0);

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "RedundantModifier")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .build();

        MethodSpec workActionExecute = MethodSpec.methodBuilder("execute")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addCode(CodeBlock.builder()
                        .add("$T.action(getParameters());", typeElement.asType())
                        .build())
                .build();

        TypeSpec workActionType = TypeSpec.classBuilder(typeElement.getSimpleName() + "WorkAction")
                .addModifiers(Modifier.ABSTRACT)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(WorkAction.class), workParamsClassName))
                .addMethod(constructor)
                .addMethod(workActionExecute)
                .build();

        JavaFile workAction = JavaFile.builder(packageName, workActionType)
                .skipJavaLangImports(true)
                .build();

        Goethe.formatAndEmit(workAction, processingEnv.getFiler());
    }

    private void emitTaskImpl(
            TypeElement typeElement, TypeElement params, String packageName, ClassName workActionClassName) {
        MethodSpec workerExecutor = MethodSpec.methodBuilder("getWorkerExecutor")
                .addAnnotation(Inject.class)
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(ClassName.get(WorkerExecutor.class))
                .build();

        CodeBlock.Builder paramsSetters = CodeBlock.builder()
                .add("$N().noIsolation().submit($T.class, params -> {", workerExecutor, workActionClassName)
                .indent();

        params.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .map(ExecutableElement.class::cast)
                .forEach(possibleMethod -> {
                    Name simpleName = possibleMethod.getSimpleName();
                    paramsSetters
                            .add("params.$L().set($L());", simpleName, simpleName)
                            .build();
                });

        MethodSpec execute = MethodSpec.methodBuilder("execute")
                .addAnnotation(TaskAction.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addCode(paramsSetters.unindent().add("});").build())
                .build();

        TypeSpec taskImplType = TypeSpec.classBuilder(typeElement.getSimpleName() + "TaskImpl")
                .addModifiers(Modifier.ABSTRACT)
                .superclass(ClassName.get(DefaultTask.class))
                .addSuperinterface(ClassName.get(params))
                .addMethod(workerExecutor)
                .addMethod(execute)
                .build();

        JavaFile taskImpl = JavaFile.builder(packageName, taskImplType)
                .skipJavaLangImports(true)
                .build();

        Goethe.formatAndEmit(taskImpl, processingEnv.getFiler());
    }
}
