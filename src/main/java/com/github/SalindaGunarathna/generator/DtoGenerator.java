package com.github.SalindaGunarathna.generator;

import com.github.SalindaGunarathna.annotations.Dto;
import com.github.SalindaGunarathna.annotations.ExcludeInDto;
import com.github.SalindaGunarathna.annotations.IncludeInDto;
import com.github.SalindaGunarathna.annotations.IncludePolicy;
import com.github.SalindaGunarathna.annotations.ValidationNamespace;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DtoGenerator {

    private static class FieldSpec {
        private final String type;
        private final String name;

        private FieldSpec(String type, String name) {
            this.type = type;
            this.name = name;
        }
    }

    public static void generate(ProcessingEnvironment env,
                                TypeElement entity,
                                Dto config) {

        try {

            String dtoName = config.name();

            String entityPackage = env.getElementUtils().getPackageOf(entity).toString();
            String entityNameLower = entity.getSimpleName().toString().toLowerCase(Locale.ROOT);
            String defaultPackageName = defaultPackage(entityPackage, "dto", entityNameLower);

            String packageName = config.packageName().isEmpty()
                    ? defaultPackageName
                    : config.packageName();

            JavaFileObject file = env.getFiler()
                    .createSourceFile(packageName + "." + dtoName);

            try (Writer writer = file.openWriter()) {

                writer.write("package " + packageName + ";\n\n");
                writer.write("public class " + dtoName + " {\n\n");

                List<FieldSpec> dtoFields = new ArrayList<>();

                for (Element field : entity.getEnclosedElements()) {

                    if (field.getKind() == ElementKind.FIELD) {

                        IncludeInDto include = field.getAnnotation(IncludeInDto.class);
                        ExcludeInDto exclude = field.getAnnotation(ExcludeInDto.class);

                        if (exclude != null) {
                            String[] dtos = exclude.dtos();
                            String[] value = exclude.value();

                            if (dtos.length > 0 && value.length > 0) {
                                env.getMessager().printMessage(
                                        Diagnostic.Kind.ERROR,
                                        "@ExcludeInDto should use either dtos or value, not both, on field " + field.getSimpleName(),
                                        field
                                );
                                continue;
                            }

                            String[] dtoNames = dtos.length > 0 ? dtos : value;
                            if (dtoNames.length == 0) {
                                continue;
                            }

                            boolean excludedForThisDto = false;
                            for (String dto : dtoNames) {
                                if (dto.equals(config.name())) {
                                    excludedForThisDto = true;
                                    break;
                                }
                            }

                            if (excludedForThisDto) {
                                continue;
                            }
                        }

                        if (config.include() == IncludePolicy.ANNOTATED_ONLY) {
                            if (include == null) {
                                continue;
                            }

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

                                    String rawType = field.asType().toString();
                                    String type = stripTypeAnnotations(rawType);
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

                                    dtoFields.add(new FieldSpec(type, targetName));
                                }
                            }
                            continue;
                        }

                        String rawType = field.asType().toString();
                        String type = stripTypeAnnotations(rawType);
                        String sourceName = field.getSimpleName().toString();

                        String targetName = sourceName;
                        boolean shouldCopyValidation = config.copyValidation();

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

                            if (dtoNames.length > 0) {
                                for (int i = 0; i < dtoNames.length; i++) {
                                    if (dtoNames[i].equals(config.name())) {
                                        if (hasTargets) {
                                            String candidate = targets[i];
                                            if (candidate != null && !candidate.trim().isEmpty()) {
                                                targetName = candidate;
                                            }
                                        }
                                        if (hasCopyValidation) {
                                            shouldCopyValidation = copyValidation[i];
                                        }
                                        break;
                                    }
                                }
                            }
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

                        dtoFields.add(new FieldSpec(type, targetName));
                    }
                }

                if (config.generateBuilder()) {
                    writeBuilder(writer, dtoName, dtoFields);
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

    private static void writeBuilder(Writer writer, String dtoName, List<FieldSpec> fields)
            throws Exception {
        writer.write("    public static Builder builder() { return new Builder(); }\n\n");
        writer.write("    public static class Builder {\n");
        writer.write("        private final " + dtoName + " dto = new " + dtoName + "();\n\n");

        for (FieldSpec field : fields) {
            writer.write("        public Builder " + field.name +
                    "(" + field.type + " " + field.name + ") {\n");
            writer.write("            dto.set" + capitalize(field.name) +
                    "(" + field.name + ");\n");
            writer.write("            return this;\n");
            writer.write("        }\n\n");
        }

        writer.write("        public " + dtoName + " build() { return dto; }\n");
        writer.write("    }\n\n");
    }

    private static String stripTypeAnnotations(String type) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < type.length()) {
            char c = type.charAt(i);
            if (c == '@') {
                i++;
                while (i < type.length()) {
                    char ch = type.charAt(i);
                    if (Character.isWhitespace(ch) || ch == '(') {
                        break;
                    }
                    i++;
                }
                if (i < type.length() && type.charAt(i) == '(') {
                    int depth = 1;
                    i++;
                    while (i < type.length() && depth > 0) {
                        char ch = type.charAt(i);
                        if (ch == '(') {
                            depth++;
                        } else if (ch == ')') {
                            depth--;
                        }
                        i++;
                    }
                }
                while (i < type.length() && Character.isWhitespace(type.charAt(i))) {
                    i++;
                }
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString().trim().replaceAll("\\s+", " ");
    }

    private static String defaultPackage(String basePackage, String segment, String entityLowerName) {
        if (basePackage == null || basePackage.isEmpty()) {
            return segment + "." + entityLowerName;
        }
        return basePackage + "." + segment + "." + entityLowerName;
    }

    private static String capitalize(String s) {
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }
}
