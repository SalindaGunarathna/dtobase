package com.github.SalindaGunarathna.tests;

import com.github.SalindaGunarathna.processor.DtoProcessor;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;

/**
 * Class-level DTO name configuration tests.
 * Ensures the generated DTO class uses the exact @Dto(name) without suffix changes.
 */
public class DtoNameConfigTest {

    /**
     * Verifies the DTO class name and mapper references use the provided name exactly.
     */
    @Test
    void dtoName_isUsedAsProvided() {
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
        assertThat(compilation)
                .generatedSourceFile("com.example.model.dto.user.UserDto")
                .contentsAsUtf8String()
                .contains("public class UserDto");
        assertThat(compilation)
                .generatedSourceFile("com.example.model.mapper.user.UserDtoMapper")
                .contentsAsUtf8String()
                .contains("UserDto");
        assertThat(compilation)
                .generatedSourceFile("com.example.model.mapper.user.UserDtoMapper")
                .contentsAsUtf8String()
                .doesNotContain("UserDtoBase");
    }
}
