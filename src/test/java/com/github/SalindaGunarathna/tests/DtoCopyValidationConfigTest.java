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
 * Class-level copyValidation and validationNamespace configuration tests.
 * Verifies default behavior, explicit enable/disable, namespace selection, and per-field overrides.
 */
public class DtoCopyValidationConfigTest {

    /**
     * Verifies default copyValidation=false does not copy validation annotations.
     */
    @Test
    void copyValidation_defaultFalse_doesNotCopyAnnotations() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "import jakarta.validation.constraints.NotBlank;",
                "@Dto(name = \"UserDto\")",
                "public class User {",
                "  @NotBlank",
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
                .doesNotContain("@jakarta.validation.constraints.NotBlank");
    }

    /**
     * Verifies copyValidation=true copies jakarta.validation annotations by default.
     */
    @Test
    void copyValidation_true_copiesJakartaAnnotations() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "import jakarta.validation.constraints.NotBlank;",
                "@Dto(name = \"UserDto\", copyValidation = true)",
                "public class User {",
                "  @NotBlank",
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
                .contains("@jakarta.validation.constraints.NotBlank");
    }

    /**
     * Verifies validationNamespace=JAVAX copies javax.validation annotations (and not jakarta).
     */
    @Test
    void validationNamespace_javax_copiesOnlyJavaxAnnotations() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "import com.github.SalindaGunarathna.annotations.ValidationNamespace;",
                "import javax.validation.constraints.NotBlank;",
                "import jakarta.validation.constraints.Size;",
                "@Dto(name = \"UserDto\", copyValidation = true, validationNamespace = ValidationNamespace.JAVAX)",
                "public class User {",
                "  @NotBlank",
                "  @Size(min = 3, max = 20)",
                "  @IncludeInDto(dtos = {\"UserDto\"})",
                "  private String username;",
                "  public String getUsername() { return username; }",
                "  public void setUsername(String username) { this.username = username; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        String dto = getGeneratedSource(compilation, "com.example.model.dto.user.UserDto");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("@javax.validation.constraints.NotBlank");
        com.google.common.truth.Truth.assertThat(dto)
                .doesNotContain("@jakarta.validation.constraints.Size");
    }

    /**
     * Verifies per-field copyValidation override can disable copying for a specific DTO.
     */
    @Test
    void copyValidation_perFieldOverride_disablesForTargetDto() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "import jakarta.validation.constraints.NotBlank;",
                "@Dto(name = \"UserCreateDto\", copyValidation = true)",
                "@Dto(name = \"UserUpdateDto\", copyValidation = true)",
                "public class User {",
                "  @NotBlank",
                "  @IncludeInDto(dtos = {\"UserCreateDto\", \"UserUpdateDto\"}, copyValidation = {true, false})",
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
        String updateDto = getGeneratedSource(compilation, "com.example.model.dto.user.UserUpdateDto");
        com.google.common.truth.Truth.assertThat(createDto)
                .contains("@jakarta.validation.constraints.NotBlank");
        com.google.common.truth.Truth.assertThat(updateDto)
                .doesNotContain("@jakarta.validation.constraints.NotBlank");
    }

    /**
     * Verifies per-field copyValidation override length mismatch triggers compilation error.
     */
    @Test
    void copyValidation_overrideLengthMismatch_failsCompilation() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "import jakarta.validation.constraints.NotBlank;",
                "@Dto(name = \"UserCreateDto\", copyValidation = true)",
                "@Dto(name = \"UserUpdateDto\", copyValidation = true)",
                "public class User {",
                "  @NotBlank",
                "  @IncludeInDto(dtos = {\"UserCreateDto\", \"UserUpdateDto\"}, copyValidation = {true})",
                "  private String email;",
                "  public String getEmail() { return email; }",
                "  public void setEmail(String email) { this.email = email; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("copyValidation length must match");
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
