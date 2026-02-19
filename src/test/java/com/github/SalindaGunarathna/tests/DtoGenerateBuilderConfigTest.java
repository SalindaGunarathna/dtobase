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
 * Class-level generateBuilder configuration tests.
 * Verifies builder generation on/off, content, and absence when disabled.
 */
public class DtoGenerateBuilderConfigTest {

    /**
     * Verifies builder is not generated when generateBuilder is not enabled.
     */
    @Test
    void generateBuilder_defaultFalse_skipsBuilder() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\")",
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
        String dto = getGeneratedSource(compilation, "com.example.model.dto.user.UserDto");
        com.google.common.truth.Truth.assertThat(dto)
                .doesNotContain("class Builder");
        com.google.common.truth.Truth.assertThat(dto)
                .doesNotContain("builder()");
    }

    /**
     * Verifies builder is generated when generateBuilder is enabled.
     */
    @Test
    void generateBuilder_true_generatesBuilder() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\", generateBuilder = true)",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\"})",
                "  private String email;",
                "  @IncludeInDto(dtos = {\"UserDto\"})",
                "  private int age;",
                "  public String getEmail() { return email; }",
                "  public void setEmail(String email) { this.email = email; }",
                "  public int getAge() { return age; }",
                "  public void setAge(int age) { this.age = age; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        String dto = getGeneratedSource(compilation, "com.example.model.dto.user.UserDto");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("public static Builder builder()");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("class Builder");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("public Builder email(java.lang.String email)");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("public Builder age(int age)");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("public UserDto build()");
    }

    /**
     * Verifies builder methods use DTO setters (not direct field access).
     */
    @Test
    void generateBuilder_usesSetters() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\", generateBuilder = true)",
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
        String dto = getGeneratedSource(compilation, "com.example.model.dto.user.UserDto");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("dto.setEmail(email)");
    }

    /**
     * Verifies builder is generated only for the DTO that enables generateBuilder.
     */
    @Test
    void generateBuilder_mixedPerDto_onlyGeneratesForEnabled() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserCreateDto\", generateBuilder = true)",
                "@Dto(name = \"UserResponseDto\", generateBuilder = false)",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserCreateDto\", \"UserResponseDto\"})",
                "  private String email;",
                "  public String getEmail() { return email; }",
                "  public void setEmail(String email) { this.email = email; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        String createDto = getGeneratedSource(compilation, "com.example.model.dto.user.UserCreateDto");
        String responseDto = getGeneratedSource(compilation, "com.example.model.dto.user.UserResponseDto");
        com.google.common.truth.Truth.assertThat(createDto)
                .contains("class Builder");
        com.google.common.truth.Truth.assertThat(responseDto)
                .doesNotContain("class Builder");
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
}
