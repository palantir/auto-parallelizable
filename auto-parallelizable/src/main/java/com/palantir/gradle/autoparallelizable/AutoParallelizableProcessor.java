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

import com.palantir.goethe.Goethe;
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
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

final class AutoParallelizableProcessor extends AbstractProcessor {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(AutoParallelizable.class.getCanonicalName());
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

        TypeSpec workParamsType = TypeSpec.interfaceBuilder(workParamsClassName)
                .addSuperinterface(ClassName.get(WorkParameters.class))
                .addSuperinterface(params.asType())
                .build();

        JavaFile workParams = JavaFile.builder(packageName, workParamsType).build();

        Goethe.formatAndEmit(workParams, processingEnv.getFiler());

        List<ExecutableElement> possibleExecutes = typeElement.getEnclosedElements().stream()
                .filter(subElement -> subElement.getKind().equals(ElementKind.METHOD))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getSimpleName().toString().equals("execute"))
                .collect(Collectors.toList());

        ExecutableElement _execute = possibleExecutes.get(0);

        MethodSpec workActionExecute = MethodSpec.methodBuilder("execute")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addCode(CodeBlock.builder()
                        .add("$T.execute(getParameters());", typeElement.asType())
                        .build())
                .build();

        TypeSpec workActionType = TypeSpec.classBuilder(typeElement.getSimpleName() + "WorkAction")
                .addModifiers(Modifier.ABSTRACT)
                .addSuperinterface(ParameterizedTypeName.get(ClassName.get(WorkAction.class), workParamsClassName))
                .addMethod(workActionExecute)
                .build();

        JavaFile workAction = JavaFile.builder(packageName, workActionType).build();

        Goethe.formatAndEmit(workAction, processingEnv.getFiler());
    }
}
