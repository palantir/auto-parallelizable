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
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
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

        TypeSpec type = TypeSpec.interfaceBuilder(typeElement.getSimpleName() + "WorkParams")
                .addSuperinterface(ClassName.get(WorkParameters.class))
                .addSuperinterface(params.asType())
                .build();

        String packageName = processingEnv
                .getElementUtils()
                .getPackageOf(typeElement)
                .getQualifiedName()
                .toString();

        JavaFile workParams = JavaFile.builder(packageName, type).build();

        Goethe.formatAndEmit(workParams, processingEnv.getFiler());
    }
}
