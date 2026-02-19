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
 * Field-level edge case tests for @IncludeInDto.
 * Covers non-matching DTOs, duplicate targets, and generic type preservation.
 */
public class DtoFieldLevelEdgeCasesTest {

    /**
     * Verifies fields are skipped when DTO name does not match any configured target.
     */
    @Test
    void includeInDto_nonMatchingDtoName_skipsField() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"OtherDto\"})",
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

    /**
     * Verifies duplicate target names cause compilation to fail (duplicate field).
     */
    @Test
    void includeInDto_duplicateTargets_failsCompilation() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\"}, targets = {\"name\"})",
                "  private String email;",
                "  @IncludeInDto(dtos = {\"UserDto\"}, targets = {\"name\"})",
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

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("already defined");
    }

    /**
     * Verifies generic field types are preserved in the generated DTO.
     */
    @Test
    void includeInDto_genericTypes_arePreserved() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import java.util.List;",
                "import java.util.Map;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserDto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserDto\"})",
                "  private List<String> tags;",
                "  @IncludeInDto(dtos = {\"UserDto\"})",
                "  private Map<String, Integer> scores;",
                "  public List<String> getTags() { return tags; }",
                "  public void setTags(List<String> tags) { this.tags = tags; }",
                "  public Map<String, Integer> getScores() { return scores; }",
                "  public void setScores(Map<String, Integer> scores) { this.scores = scores; }",
                "}"
        );

        Compilation compilation = Compiler.javac()
                .withProcessors(new DtoProcessor())
                .compile(source);

        assertThat(compilation).succeeded();
        String dto = getGeneratedSource(compilation, "com.example.model.dto.user.UserDto");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("java.util.List<java.lang.String>");
        com.google.common.truth.Truth.assertThat(dto)
                .contains("java.util.Map<java.lang.String,java.lang.Integer>");
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

