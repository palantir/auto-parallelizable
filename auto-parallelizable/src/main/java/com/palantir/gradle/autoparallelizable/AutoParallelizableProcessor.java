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

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.google.common.collect.Iterables;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic.Kind;

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
        Optional<TypeElement> maybeParams = verifyParamsElement(typeElement);

        if (maybeParams.isEmpty()) {
            return;
        }

        TypeElement params = maybeParams.get();

        if (!verifyActionMethod(typeElement, params)) {
            return;
        }

        String packageName = processingEnv
                .getElementUtils()
                .getPackageOf(typeElement)
                .getQualifiedName()
                .toString();

        ClassName workParamsClassName = ClassName.get(packageName, typeElement.getSimpleName() + "WorkParams");
        ClassName workActionClassName = ClassName.get(packageName, typeElement.getSimpleName() + "WorkAction");

        Emitter emitter = new Emitter(processingEnv.getFiler(), packageName, typeElement);

        emitWorkParams(emitter, params, workParamsClassName);

        emitWorkAction(emitter, typeElement, workParamsClassName);

        emitTaskImpl(emitter, typeElement, params, workActionClassName);
    }

    private Optional<TypeElement> verifyParamsElement(TypeElement typeElement) {
        List<Element> possibleParams = typeElement.getEnclosedElements().stream()
                .filter(element -> element.getSimpleName().toString().equals("Params"))
                .collect(Collectors.toList());

        if (possibleParams.isEmpty()) {
            error(typeElement, "Could not find interface named 'Params' in class " + typeElement.getQualifiedName());
            return Optional.empty();
        }

        Element paramElement = possibleParams.get(0);

        if (!isPackagePrivate(paramElement)) {
            error(paramElement, "Params type must be package-private");
            return Optional.empty();
        }

        if (!paramElement.getKind().equals(ElementKind.INTERFACE)) {
            error(
                    paramElement,
                    "Params type must be an interface - was a "
                            + paramElement.getKind().toString().toLowerCase(Locale.ROOT));
            return Optional.empty();
        }

        return Optional.of((TypeElement) paramElement);
    }

    private boolean verifyActionMethod(TypeElement typeElement, TypeElement params) {
        List<ExecutableElement> possibleActions = findActionMethod(typeElement);
        if (possibleActions.isEmpty()) {
            error(typeElement, "There must be a 'static void action(Params)' method that performs the task action");
            return false;
        }

        ExecutableElement action = possibleActions.get(0);

        boolean successful = true;

        if (!action.getModifiers().contains(Modifier.STATIC)) {
            error(action, "The 'action' method must be static");
            successful = false;
        }

        if (!action.getReturnType().getKind().equals(TypeKind.VOID)) {
            error(action, "The 'action' method must return void");
            successful = false;
        }

        long numberOfParamsArguments = action.getParameters().stream()
                .filter(actionParameter -> isSameType(actionParameter, params))
                .count();
        if (numberOfParamsArguments == 0) {
            error(action, "The 'action' method must have a Params argument");
            successful = false;
        }

        if (numberOfParamsArguments > 1) {
            error(action, "The 'action' method must have at most one Params argument");
            successful = false;
        }

        List<? extends VariableElement> remainingParameters = action.getParameters().stream()
                .filter(actionParameter -> !isSameType(actionParameter, params))
                .collect(Collectors.toList());
        for (VariableElement remainingParameter : remainingParameters) {
            boolean hasInjectAnnotation = isInjectable(remainingParameter);
            if (!hasInjectAnnotation) {
                error(
                        remainingParameter,
                        "Any non 'Param' annotation must be marked with the @AutoParallelizable.Inject annotation");
                successful = false;
            }
        }

        if (!isPackagePrivate(action)) {
            error(action, "The 'action' method must be package-private");
            successful = false;
        }

        if (!action.getThrownTypes().isEmpty()) {
            error(action, "The 'action' method must not throw any exceptions");
            successful = false;
        }

        return successful;
    }

    private static boolean isInjectable(VariableElement parameter) {
        return MoreElements.isAnnotationPresent(parameter, AutoParallelizable.Inject.class);
    }

    private static boolean isNestedProperty(Element element) {
        return MoreElements.isAnnotationPresent(element, "org.gradle.api.tasks.Nested");
    }

    private static List<ExecutableElement> findActionMethod(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(subElement -> subElement.getKind().equals(ElementKind.METHOD))
                .map(ExecutableElement.class::cast)
                .filter(element -> element.getSimpleName().toString().equals("action"))
                .collect(Collectors.toList());
    }

    private boolean isSameType(Element first, Element second) {
        return processingEnv.getTypeUtils().isSameType(first.asType(), second.asType());
    }

    private boolean isPackagePrivate(Element element) {
        Set<Modifier> modifiers = element.getModifiers();

        return !(modifiers.contains(Modifier.PUBLIC)
                || modifiers.contains(Modifier.PRIVATE)
                || modifiers.contains(Modifier.PROTECTED));
    }

    private void emitWorkParams(Emitter emitter, TypeElement params, ClassName workParamsClassName) {
        TypeSpec workParamsType = TypeSpec.interfaceBuilder(workParamsClassName)
                .addSuperinterface(ClassName.get("org.gradle.workers", "WorkParameters"))
                .addSuperinterface(params.asType())
                .build();

        emitter.emit(workParamsType);
    }

    private void emitWorkAction(Emitter emitter, TypeElement typeElement, ClassName workParamsClassName) {
        ExecutableElement actionMethod = Iterables.getOnlyElement(findActionMethod(typeElement));
        List<MethodSpec> injectableMethods = actionMethod.getParameters().stream()
                .filter(AutoParallelizableProcessor::isInjectable)
                .map(injectable ->
                        injectMethod(ClassName.get(injectable.asType()), getMethodNameBasedOnType(injectable)))
                .collect(Collectors.toList());

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "$S", "RedundantModifier")
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .build();

        String format = actionMethod.getParameters().stream()
                .map(parameter ->
                        isInjectable(parameter) ? getMethodNameBasedOnType(parameter) + "()" : "getParameters()")
                .collect(Collectors.joining(", ", "$T.action(", ");"));

        MethodSpec workActionExecute = MethodSpec.methodBuilder("execute")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addCode(CodeBlock.builder().add(format, typeElement.asType()).build())
                .build();

        TypeSpec workActionType = TypeSpec.classBuilder(typeElement.getSimpleName() + "WorkAction")
                .addModifiers(Modifier.ABSTRACT)
                .addSuperinterface(ParameterizedTypeName.get(
                        ClassName.get("org.gradle.workers", "WorkAction"), workParamsClassName))
                .addMethods(injectableMethods)
                .addMethod(constructor)
                .addMethod(workActionExecute)
                .build();

        emitter.emit(workActionType);
    }

    private static String getMethodNameBasedOnType(VariableElement injectable) {
        return "get" + MoreTypes.asElement(injectable.asType()).getSimpleName();
    }

    private void emitTaskImpl(
            Emitter emitter, TypeElement typeElement, TypeElement params, ClassName workActionClassName) {
        MethodSpec workerExecutor =
                injectMethod(ClassName.get("org.gradle.workers", "WorkerExecutor"), "getWorkerExecutor");

        CodeBlock.Builder paramsSetters = CodeBlock.builder()
                .add("$N().noIsolation().submit($T.class, params -> {", workerExecutor, workActionClassName)
                .indent();

        handleParamsLikeElement(paramsSetters, "params", "this", params);

        MethodSpec execute = MethodSpec.methodBuilder("execute")
                .addAnnotation(ClassName.get("org.gradle.api.tasks", "TaskAction"))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addCode(paramsSetters.unindent().add("});").build())
                .build();

        TypeSpec taskImplType = TypeSpec.classBuilder(typeElement.getSimpleName() + "TaskImpl")
                .addModifiers(Modifier.ABSTRACT)
                .superclass(ClassName.get("org.gradle.api", "DefaultTask"))
                .addSuperinterface(ClassName.get(params))
                .addMethod(workerExecutor)
                .addMethod(execute)
                .build();

        emitter.emit(taskImplType);
    }

    private void handleParamsLikeElement(
            CodeBlock.Builder builder, String writerContext, String readerContext, TypeElement paramsLikeElement) {
        paramsLikeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .map(ExecutableElement.class::cast)
                .forEach(possibleMethod -> {
                    if (possibleMethod.getModifiers().contains(Modifier.DEFAULT)) {
                        return;
                    }

                    if (isNestedProperty(possibleMethod)) {
                        TypeElement nestedParamsLikeElement = MoreTypes.asTypeElement(possibleMethod.getReturnType());
                        String newContextSuffix = possibleMethod.getSimpleName().toString() + "()";
                        handleParamsLikeElement(
                                builder,
                                writerContext + "." + newContextSuffix,
                                readerContext + "." + newContextSuffix,
                                nestedParamsLikeElement);
                        return;
                    }

                    Name simpleName = possibleMethod.getSimpleName();

                    String returnType = possibleMethod.getReturnType().toString();

                    String setterMethod = "set";

                    if (returnType.endsWith("ConfigurableFileCollection")) {
                        setterMethod = "from";
                    }

                    builder.add(
                            "$L.$L().$L($L.$L());", writerContext, simpleName, setterMethod, readerContext, simpleName);
                });
    }

    private static MethodSpec injectMethod(TypeName returns, String methodName) {
        return MethodSpec.methodBuilder(methodName)
                .addAnnotation(ClassName.get("javax.inject", "Inject"))
                .addModifiers(Modifier.PROTECTED, Modifier.ABSTRACT)
                .returns(returns)
                .build();
    }

    private void error(Element element, String error) {
        processingEnv.getMessager().printMessage(Kind.ERROR, error, element);
    }
}
