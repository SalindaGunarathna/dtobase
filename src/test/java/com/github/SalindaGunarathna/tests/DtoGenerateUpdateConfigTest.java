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
 * Class-level generateUpdate and nullHandling configuration tests.
 * Verifies updateEntity generation, null-handling behavior, and compile-time failures.
 */
public class DtoGenerateUpdateConfigTest {

    /**
     * Verifies updateEntity is not generated when generateUpdate is not enabled.
     */
    @Test
    void generateUpdate_defaultFalse_skipsUpdateEntity() {
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
        String mapper = getGeneratedSource(compilation, "com.example.model.mapper.user.UserDtoMapper");
        com.google.common.truth.Truth.assertThat(mapper)
                .doesNotContain("updateEntity(");
    }

    /**
     * Verifies updateEntity is generated when generateUpdate is enabled.
     */
    @Test
    void generateUpdate_true_generatesUpdateEntity() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\", generateUpdate = true)",
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
        String mapper = getGeneratedSource(compilation, "com.example.model.mapper.user.UserDtoMapper");
        com.google.common.truth.Truth.assertThat(mapper)
                .contains("updateEntity(");
    }

    /**
     * Verifies default nullHandling=IGNORE_NULLS adds null checks for reference fields,
     * and does not add null checks for primitive fields.
     */
    @Test
    void nullHandling_defaultIgnoreNulls_addsNullChecksForReferencesOnly() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\", generateUpdate = true)",
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
        String mapper = getGeneratedSource(compilation, "com.example.model.mapper.user.UserDtoMapper");
        com.google.common.truth.Truth.assertThat(mapper)
                .contains("if(dto.getEmail() != null)");
        com.google.common.truth.Truth.assertThat(mapper)
                .contains("entity.setEmail(dto.getEmail());");
        com.google.common.truth.Truth.assertThat(mapper)
                .contains("entity.setAge(dto.getAge());");
        com.google.common.truth.Truth.assertThat(mapper)
                .doesNotContain("dto.getAge() != null");
    }

    /**
     * Verifies nullHandling=SET_NULLS updates reference fields without null checks.
     */
    @Test
    void nullHandling_setNulls_skipsNullChecks() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "import com.github.SalindaGunarathna.annotations.NullHandling;",
                "@Dto(name = \"UserDto\", generateUpdate = true, nullHandling = NullHandling.SET_NULLS)",
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
        String mapper = getGeneratedSource(compilation, "com.example.model.mapper.user.UserDtoMapper");
        com.google.common.truth.Truth.assertThat(mapper)
                .doesNotContain("if(dto.getEmail() != null)");
        com.google.common.truth.Truth.assertThat(mapper)
                .contains("entity.setEmail(dto.getEmail());");
    }

    /**
     * Verifies compilation fails when updateEntity requires a missing entity setter.
     */
    @Test
    void generateUpdate_missingSetter_failsCompilation() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\", generateUpdate = true, generateToEntity = false)",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\"})",
                "  private String email;",
                "  public String getEmail() { return email; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("setEmail");
    }

    /**
     * Verifies compilation fails when updateEntity uses a setter with mismatched types.
     */
    @Test
    void generateUpdate_setterTypeMismatch_failsCompilation() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\", generateUpdate = true, generateToEntity = false)",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\"})",
                "  private String email;",
                "  public String getEmail() { return email; }",
                "  public void setEmail(Integer email) { }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("incompatible types");
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

