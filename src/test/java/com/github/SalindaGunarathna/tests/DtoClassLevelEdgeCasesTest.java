package com.github.SalindaGunarathna.tests;

import com.github.SalindaGunarathna.processor.DtoProcessor;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.IOException;

import static com.google.testing.compile.CompilationSubject.assertThat;

/**
 * Class-level edge case tests.
 * Covers mapper generation precedence, cross-package type usage, type-use annotations, and builder with targets.
 */
public class DtoClassLevelEdgeCasesTest {

    /**
     * Verifies generateMapper=false prevents mapper generation even if update/toEntity are enabled.
     */
    @Test
    void generateMapper_false_overridesOtherMapperOptions() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\", generateMapper = false, generateUpdate = true, generateToEntity = true)",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\"})",
                "  private String email;",
                "  public String getEmail() { return email; }",
                "  public void setEmail(String email) { this.email = email; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        com.google.common.truth.Truth.assertThat(hasGeneratedFile(compilation, "com/example/model/mapper/user/UserDtoMapper.java"))
                .isFalse();
    }

    /**
     * Verifies cross-package mapper uses fully-qualified entity and DTO types.
     */
    @Test
    void mapper_crossPackage_usesQualifiedTypes() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\", packageName = \"com.custom.dto\", mapperPackageName = \"com.custom.mapper\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\"})",
                "  private String email;",
                "  public String getEmail() { return email; }",
                "  public void setEmail(String email) { this.email = email; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        String mapper = getGeneratedSource(compilation, "com.custom.mapper.UserDtoMapper");
        com.google.common.truth.Truth.assertThat(mapper)
                .contains("com.example.model.User");
        com.google.common.truth.Truth.assertThat(mapper)
                .contains("com.custom.dto.UserDto");
    }

    /**
     * Verifies type-use annotations are stripped from generated DTO field types.
     */
    @Test
    void typeUseAnnotations_areStrippedFromFieldTypes() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import java.lang.annotation.ElementType;",
                "import java.lang.annotation.Retention;",
                "import java.lang.annotation.RetentionPolicy;",
                "import java.lang.annotation.Target;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Target(ElementType.TYPE_USE)",
                "@Retention(RetentionPolicy.RUNTIME)",
                "@interface TypeUseAnn {}",
                "@Dto(name = \"UserDto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\"})",
                "  private java.util.List<@TypeUseAnn String> names;",
                "  public java.util.List<String> getNames() { return names; }",
                "  public void setNames(java.util.List<String> names) { this.names = names; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        String dto = getGeneratedSource(compilation, "com.example.model.dto.user.UserDto");
        com.google.common.truth.Truth.assertThat(dto)
                .doesNotContain("@TypeUseAnn");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("java.util.List<java.lang.String>");
    }

    /**
     * Verifies builder method names respect @IncludeInDto targets.
     */
    @Test
    void generateBuilder_respectsTargetsForMethodNames() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\", generateBuilder = true)",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\"}, targets = {\"mail\"})",
                "  private String email;",
                "  public String getEmail() { return email; }",
                "  public void setEmail(String email) { this.email = email; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        String dto = getGeneratedSource(compilation, "com.example.model.dto.user.UserDto");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("public Builder mail(java.lang.String mail)");
        com.google.common.truth.Truth.assertThat(dto)
                .doesNotContain("public Builder email(");
    }

    private static String getGeneratedSource(Compilation compilation, String qualifiedName) {
        JavaFileObject file = compilation.generatedSourceFile(qualifiedName)
                .orElseThrow(() -> new AssertionError("Missing generated source: " + qualifiedName));
        try {
            return file.getCharContent(false).toString();
        } catch (IOException e) {
            throw new AssertionError("Failed to read generated source: " + qualifiedName, e);
        }
    }

    private static boolean hasGeneratedFile(Compilation compilation, String suffix) {
        return compilation.generatedSourceFiles().stream()
                .map(file -> file.toUri().toString().replace("\\", "/"))
                .anyMatch(path -> path.endsWith(suffix));
    }
}
