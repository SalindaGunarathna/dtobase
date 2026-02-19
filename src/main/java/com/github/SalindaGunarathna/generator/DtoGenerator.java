package com.github.SalindaGunarathna.generator;

import com.github.SalindaGunarathna.annotations.Dto;
import com.github.SalindaGunarathna.annotations.IncludeInDto;
import com.github.SalindaGunarathna.annotations.ValidationNamespace;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.Writer;

public class DtoGenerator {

    public static void generate(ProcessingEnvironment env,
                                TypeElement entity,
                                Dto config) {

        try {

            String dtoName = config.name() + "Base";

            String packageName = config.packageName().isEmpty()
                    ? env.getElementUtils().getPackageOf(entity).toString()
                    : config.packageName();

            JavaFileObject file = env.getFiler()
                    .createSourceFile(packageName + "." + dtoName);

            try (Writer writer = file.openWriter()) {

                writer.write("package " + packageName + ";\n\n");
                writer.write("public class " + dtoName + " {\n\n");

                for (Element field : entity.getEnclosedElements()) {

                    if (field.getKind() == ElementKind.FIELD) {

                        IncludeInDto include =
                                field.getAnnotation(IncludeInDto.class);

                        if (include != null) {
                            String[] dtos = include.dtos();
                            String[] value = include.value();

                            if (dtos.length > 0 && value.length > 0) {
                                env.getMessager().printMessage(
                                        Diagnostic.Kind.ERROR,
                                        "@IncludeInDto should use either dtos or value, not both, on field " + field.getSimpleName(),
                                        field
                                );
                                continue;
                            }

                            String[] dtoNames = dtos.length > 0 ? dtos : value;
                            if (dtoNames.length == 0) {
                                continue;
                            }

                            String[] targets = include.targets();
                            boolean[] copyValidation = include.copyValidation();

                            boolean hasTargets = targets != null && targets.length > 0;
                            boolean hasCopyValidation = copyValidation != null && copyValidation.length > 0;

                            if (hasTargets && targets.length != dtoNames.length) {
                                env.getMessager().printMessage(
                                        Diagnostic.Kind.ERROR,
                                        "@IncludeInDto targets length must match dtos/value length on field " + field.getSimpleName(),
                                        field
                                );
                                continue;
                            }

                            if (hasCopyValidation && copyValidation.length != dtoNames.length) {
                                env.getMessager().printMessage(
                                        Diagnostic.Kind.ERROR,
                                        "@IncludeInDto copyValidation length must match dtos/value length on field " + field.getSimpleName(),
                                        field
                                );
                                continue;
                            }

                            for (int i = 0; i < dtoNames.length; i++) {
                                if (dtoNames[i].equals(config.name())) {

                                    String type =
                                            field.asType().toString();
                                    String sourceName =
                                            field.getSimpleName().toString();

                                    String targetName = sourceName;
                                    if (hasTargets) {
                                        String candidate = targets[i];
                                        if (candidate != null && !candidate.trim().isEmpty()) {
                                            targetName = candidate;
                                        }
                                    }

                                    boolean shouldCopyValidation = config.copyValidation();
                                    if (hasCopyValidation) {
                                        shouldCopyValidation = copyValidation[i];
                                    }

                                    if (shouldCopyValidation) {
                                        writeValidationAnnotations(env, field, writer, config);
                                    }

                                    writer.write("    private " + type +
                                            " " + targetName + ";\n");

                                    writer.write("\n    public " + type +
                                            " get" +
                                            capitalize(targetName) +
                                            "() { return " + targetName + "; }\n");

                                    writer.write("    public void set" +
                                            capitalize(targetName) +
                                            "(" + type + " " + targetName +
                                            ") { this." + targetName +
                                            " = " + targetName + "; }\n\n");
                                }
                            }
                        }
                    }
                }

                writer.write("}\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeValidationAnnotations(ProcessingEnvironment env,
                                                   Element field,
                                                   Writer writer,
                                                   Dto config) {
        try {
            String namespacePrefix = config.validationNamespace() == ValidationNamespace.JAKARTA
                    ? "jakarta.validation."
                    : "javax.validation.";

            for (AnnotationMirror mirror : field.getAnnotationMirrors()) {
                String annotationType = mirror.getAnnotationType().toString();
                if (annotationType.startsWith(namespacePrefix)) {
                    writer.write("    " + mirror.toString() + "\n");
                }
            }
        } catch (Exception e) {
            env.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Failed to copy validation annotations: " + e.getMessage(),
                    field
            );
        }
    }

    private static String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }
}
