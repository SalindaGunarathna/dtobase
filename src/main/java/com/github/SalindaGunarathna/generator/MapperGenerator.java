package com.github.SalindaGunarathna.generator;

import com.github.SalindaGunarathna.annotations.Dto;
import com.github.SalindaGunarathna.annotations.ExcludeInDto;
import com.github.SalindaGunarathna.annotations.IncludeInDto;
import com.github.SalindaGunarathna.annotations.IncludePolicy;
import com.github.SalindaGunarathna.annotations.NullHandling;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapperGenerator {

    private static class FieldMapping {
        private final String sourceName;
        private final String targetName;
        private final boolean primitive;

        private FieldMapping(String sourceName, String targetName, boolean primitive) {
            this.sourceName = sourceName;
            this.targetName = targetName;
            this.primitive = primitive;
        }
    }

    public static void generate(ProcessingEnvironment env,
                                TypeElement entity,
                                Dto config) {

        try {
            String entityName = entity.getSimpleName().toString();
            String entityPackageName = env.getElementUtils().getPackageOf(entity).toString();
            String entityQualifiedName = entity.getQualifiedName().toString();
            String entityNameLower = entityName.toLowerCase(Locale.ROOT);

            String dtoName = config.name();
            String dtoClassName = dtoName;
            String mapperName = dtoName + "Mapper";

            String dtoPackageName = config.packageName().isEmpty()
                    ? defaultPackage(entityPackageName, "dto", entityNameLower)
                    : config.packageName();

            String mapperPackageName = config.mapperPackageName().isEmpty()
                    ? defaultPackage(entityPackageName, "mapper", entityNameLower)
                    : config.mapperPackageName();

            String entityTypeForSource = mapperPackageName.equals(entityPackageName)
                    ? entityName
                    : entityQualifiedName;

            String dtoTypeForSource = mapperPackageName.equals(dtoPackageName)
                    ? dtoClassName
                    : dtoPackageName + "." + dtoClassName;

            List<FieldMapping> mappings = new ArrayList<>();
            IncludePolicy includePolicy = config.include();

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

                    if (includePolicy == IncludePolicy.ANNOTATED_ONLY) {
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
                        boolean hasTargets = targets != null && targets.length > 0;

                        if (hasTargets && targets.length != dtoNames.length) {
                            env.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "@IncludeInDto targets length must match dtos/value length on field " + field.getSimpleName(),
                                    field
                            );
                            continue;
                        }

                        for (int i = 0; i < dtoNames.length; i++) {
                            if (dtoNames[i].equals(config.name())) {
                                String sourceName = field.getSimpleName().toString();
                                String targetName = sourceName;

                                if (hasTargets) {
                                    String candidate = targets[i];
                                    if (candidate != null && !candidate.trim().isEmpty()) {
                                        targetName = candidate;
                                    }
                                }

                                boolean isPrimitive = field.asType().getKind().isPrimitive();
                                mappings.add(new FieldMapping(sourceName, targetName, isPrimitive));
                                break;
                            }
                        }
                        continue;
                    }

                    String sourceName = field.getSimpleName().toString();
                    String targetName = sourceName;

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
                        boolean hasTargets = targets != null && targets.length > 0;

                        if (hasTargets && targets.length != dtoNames.length) {
                            env.getMessager().printMessage(
                                    Diagnostic.Kind.ERROR,
                                    "@IncludeInDto targets length must match dtos/value length on field " + field.getSimpleName(),
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
                                    break;
                                }
                            }
                        }
                    }

                    boolean isPrimitive = field.asType().getKind().isPrimitive();
                    mappings.add(new FieldMapping(sourceName, targetName, isPrimitive));
                }
            }

            JavaFileObject file = env.getFiler()
                    .createSourceFile(mapperPackageName + "." + mapperName);

            try (Writer writer = file.openWriter()) {

                writer.write("package " + mapperPackageName + ";\n\n");

                writer.write("public class " + mapperName + " {\n\n");

                // toDto
                writer.write("    public static " + dtoTypeForSource +
                        " toDto(" + entityTypeForSource + " entity) {\n");

                writer.write("        if(entity == null) return null;\n");
                writer.write("        " + dtoTypeForSource +
                        " dto = new " + dtoTypeForSource +
                        "();\n");

                for (FieldMapping mapping : mappings) {
                    writer.write("        dto.set" +
                            capitalize(mapping.targetName) +
                            "(entity.get" +
                            capitalize(mapping.sourceName) +
                            "());\n");
                }

                writer.write("        return dto;\n    }\n\n");

                if (config.generateToEntity()) {
                    writer.write("    public static " + entityTypeForSource +
                            " toEntity(" + dtoTypeForSource + " dto) {\n");

                    writer.write("        if(dto == null) return null;\n");
                    writer.write("        " + entityTypeForSource +
                            " entity = new " + entityTypeForSource +
                            "();\n");

                    for (FieldMapping mapping : mappings) {
                        writer.write("        entity.set" +
                                capitalize(mapping.sourceName) +
                                "(dto.get" +
                                capitalize(mapping.targetName) +
                                "());\n");
                    }

                    writer.write("        return entity;\n    }\n\n");
                }

                if (config.generateUpdate()) {
                    boolean ignoreNulls = config.nullHandling() == NullHandling.IGNORE_NULLS;

                    writer.write("    public static void updateEntity(" +
                            entityTypeForSource + " entity, " +
                            dtoTypeForSource + " dto) {\n");

                    writer.write("        if(entity == null || dto == null) return;\n");

                    for (FieldMapping mapping : mappings) {
                        String getter = "dto.get" + capitalize(mapping.targetName) + "()";

                        if (ignoreNulls && !mapping.primitive) {
                            writer.write("        if(" + getter + " != null) {\n");
                            writer.write("            entity.set" +
                                    capitalize(mapping.sourceName) +
                                    "(" + getter + ");\n");
                            writer.write("        }\n");
                        } else {
                            writer.write("        entity.set" +
                                    capitalize(mapping.sourceName) +
                                    "(" + getter + ");\n");
                        }
                    }

                    writer.write("    }\n\n");
                }

                writer.write("}\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
