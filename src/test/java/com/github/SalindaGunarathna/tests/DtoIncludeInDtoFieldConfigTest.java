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
 * Field-level @IncludeInDto configuration tests.
 * Verifies dto selection, target renaming, exclusion behavior, and error cases.
 */
public class DtoIncludeInDtoFieldConfigTest {

    /**
     * Verifies fields are included only for DTOs listed in @IncludeInDto.dtos.
     */
    @Test
    void includeInDto_dtos_filtersFieldsPerDto() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserCreateDto\")",
                "@Dto(name = \"UserSummaryDto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserCreateDto\"})",
                "  private String email;",
                "  @IncludeInDto(dtos = {\"UserSummaryDto\"})",
                "  private String displayName;",
                "  public String getEmail() { return email; }",
                "  public void setEmail(String email) { this.email = email; }",
                "  public String getDisplayName() { return displayName; }",
                "  public void setDisplayName(String displayName) { this.displayName = displayName; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        String createDto = getGeneratedSource(compilation, "com.example.model.dto.user.UserCreateDto");
        String summaryDto = getGeneratedSource(compilation, "com.example.model.dto.user.UserSummaryDto");
        com.google.common.truth.Truth.assertThat(createDto)
                .contains("private java.lang.String email;");
        com.google.common.truth.Truth.assertThat(createDto)
                .doesNotContain("displayName");
        com.google.common.truth.Truth.assertThat(summaryDto)
                .contains("private java.lang.String displayName;");
        com.google.common.truth.Truth.assertThat(summaryDto)
                .doesNotContain("email");
    }

    /**
     * Verifies @IncludeInDto.value works as an alias for dtos.
     */
    @Test
    void includeInDto_value_aliasWorks() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\")",
                "public class User {",
                "  @IncludeInDto({\"UserDto\"})",
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
                .contains("private java.lang.String email;");
    }

    /**
     * Verifies target renaming maps field names correctly per DTO.
     */
    @Test
    void includeInDto_targets_renamesFields() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\")",
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
                .contains("private java.lang.String mail;");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("getMail()");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("setMail");
    }

    /**
     * Verifies empty target values fall back to the original field name.
     */
    @Test
    void includeInDto_targets_emptyFallsBackToSourceName() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\"}, targets = {\"\"})",
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
                .contains("private java.lang.String email;");
        com.google.common.truth.Truth.assertThat(dto)
                .doesNotContain("private java.lang.String ;");
    }

    /**
     * Verifies using both dtos and value results in a compilation error.
     */
    @Test
    void includeInDto_dtosAndValueTogether_failsCompilation() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\"}, value = {\"UserDto\"})",
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

    /**
     * Verifies targets length mismatch triggers a compilation error.
     */
    @Test
    void includeInDto_targetsLengthMismatch_failsCompilation() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\", \"UserDto2\"}, targets = {\"mail\"})",
                "  private String email;",
                "  public String getEmail() { return email; }",
                "  public void setEmail(String email) { this.email = email; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("targets length must match");
    }

    /**
     * Verifies fields not annotated with @IncludeInDto are excluded.
     */
    @Test
    void includeInDto_unannotatedField_isExcluded() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\"})",
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
                .contains("email");
        com.google.common.truth.Truth.assertThat(dto)
                .doesNotContain("password");
    }

    /**
     * Verifies mapper uses renamed target field for both toDto and toEntity.
     */
    @Test
    void includeInDto_targets_updatesMapperFieldNames() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\")",
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
        String mapper = getGeneratedSource(compilation, "com.example.model.mapper.user.UserDtoMapper");
        com.google.common.truth.Truth.assertThat(mapper)
                .contains("dto.setMail(entity.getEmail())");
        com.google.common.truth.Truth.assertThat(mapper)
                .contains("entity.setEmail(dto.getMail())");
    }

    /**
     * Verifies multiple targets map correctly for multiple DTOs.
     */
    @Test
    void includeInDto_multipleTargets_mapCorrectly() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserCreateDto\")",
                "@Dto(name = \"UserSummaryDto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserCreateDto\", \"UserSummaryDto\"}, targets = {\"email\", \"mail\"})",
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
        String summaryDto = getGeneratedSource(compilation, "com.example.model.dto.user.UserSummaryDto");
        com.google.common.truth.Truth.assertThat(createDto)
                .contains("private java.lang.String email;");
        com.google.common.truth.Truth.assertThat(summaryDto)
                .contains("private java.lang.String mail;");
    }

    /**
     * Verifies empty dtos/value arrays cause the field to be skipped.
     */
    @Test
    void includeInDto_emptyDtos_skipsField() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {})",
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
                .doesNotContain("email");
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
