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
 * Class-level include policy and @ExcludeInDto behavior tests.
 */
public class DtoIncludePolicyConfigTest {

    /**
     * Verifies ALL_FIELDS includes unannotated fields.
     */
    @Test
    void includePolicy_allFields_includesUnannotatedFields() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludePolicy;",
                "@Dto(name = \"UserDto\", include = IncludePolicy.ALL_FIELDS)",
                "public class User {",
                "  private String email;",
                "  private String password;",
                "  public String getEmail() { return email; }",
                "  public void setEmail(String email) { this.email = email; }",
                "  public String getPassword() { return password; }",
                "  public void setPassword(String password) { this.password = password; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        String dto = getGeneratedSource(compilation, "com.example.model.dto.user.UserDto");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("private java.lang.String email;");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("private java.lang.String password;");
        String mapper = getGeneratedSource(compilation, "com.example.model.mapper.user.UserDtoMapper");
        com.google.common.truth.Truth.assertThat(mapper)
                .contains("dto.setEmail(entity.getEmail())");
        com.google.common.truth.Truth.assertThat(mapper)
                .contains("dto.setPassword(entity.getPassword())");
    }

    /**
     * Verifies @ExcludeInDto excludes a field for a specific DTO.
     */
    @Test
    void includePolicy_allFields_excludeInDto_excludesForDto() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.ExcludeInDto;",
                "import com.github.SalindaGunarathna.annotations.IncludePolicy;",
                "@Dto(name = \"UserDto\", include = IncludePolicy.ALL_FIELDS)",
                "public class User {",
                "  private String email;",
                "  @ExcludeInDto(dtos = {\"UserDto\"})",
                "  private String password;",
                "  public String getEmail() { return email; }",
                "  public void setEmail(String email) { this.email = email; }",
                "  public String getPassword() { return password; }",
                "  public void setPassword(String password) { this.password = password; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        String dto = getGeneratedSource(compilation, "com.example.model.dto.user.UserDto");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("private java.lang.String email;");
        com.google.common.truth.Truth.assertThat(dto)
                .doesNotContain("password");
        String mapper = getGeneratedSource(compilation, "com.example.model.mapper.user.UserDtoMapper");
        com.google.common.truth.Truth.assertThat(mapper)
                .doesNotContain("Password");
    }

    /**
     * Verifies empty @ExcludeInDto excludes the field from all DTOs.
     */
    @Test
    void excludeInDto_empty_excludesAllDtos() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.ExcludeInDto;",
                "import com.github.SalindaGunarathna.annotations.IncludePolicy;",
                "@Dto(name = \"UserCreateDto\", include = IncludePolicy.ALL_FIELDS)",
                "@Dto(name = \"UserSummaryDto\", include = IncludePolicy.ALL_FIELDS)",
                "public class User {",
                "  @ExcludeInDto",
                "  private String secret;",
                "  private String email;",
                "  public String getSecret() { return secret; }",
                "  public void setSecret(String secret) { this.secret = secret; }",
                "  public String getEmail() { return email; }",
                "  public void setEmail(String email) { this.email = email; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        String createDto = getGeneratedSource(compilation, "com.example.model.dto.user.UserCreateDto");
        String summaryDto = getGeneratedSource(compilation, "com.example.model.dto.user.UserSummaryDto");
        com.google.common.truth.Truth.assertThat(createDto)
                .contains("email");
        com.google.common.truth.Truth.assertThat(createDto)
                .doesNotContain("secret");
        com.google.common.truth.Truth.assertThat(summaryDto)
                .contains("email");
        com.google.common.truth.Truth.assertThat(summaryDto)
                .doesNotContain("secret");
    }

    /**
     * Verifies @IncludeInDto targets apply without excluding other DTOs in ALL_FIELDS mode.
     */
    @Test
    void includePolicy_allFields_includeInDto_targets_overrideWithoutFiltering() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "import com.github.SalindaGunarathna.annotations.IncludePolicy;",
                "@Dto(name = \"UserCreateDto\", include = IncludePolicy.ALL_FIELDS)",
                "@Dto(name = \"UserSummaryDto\", include = IncludePolicy.ALL_FIELDS)",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserSummaryDto\"}, targets = {\"displayName\"})",
                "  private String name;",
                "  public String getName() { return name; }",
                "  public void setName(String name) { this.name = name; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        String createDto = getGeneratedSource(compilation, "com.example.model.dto.user.UserCreateDto");
        String summaryDto = getGeneratedSource(compilation, "com.example.model.dto.user.UserSummaryDto");
        com.google.common.truth.Truth.assertThat(createDto)
                .contains("private java.lang.String name;");
        com.google.common.truth.Truth.assertThat(createDto)
                .doesNotContain("displayName");
        com.google.common.truth.Truth.assertThat(summaryDto)
                .contains("private java.lang.String displayName;");
    }

    /**
     * Verifies using both dtos and value in @ExcludeInDto fails compilation.
     */
    @Test
    void excludeInDto_dtosAndValueTogether_failsCompilation() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.ExcludeInDto;",
                "import com.github.SalindaGunarathna.annotations.IncludePolicy;",
                "@Dto(name = \"UserDto\", include = IncludePolicy.ALL_FIELDS)",
                "public class User {",
                "  @ExcludeInDto(dtos = {\"UserDto\"}, value = {\"UserDto\"})",
                "  private String email;",
                "  public String getEmail() { return email; }",
                "  public void setEmail(String email) { this.email = email; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("either dtos or value");
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
