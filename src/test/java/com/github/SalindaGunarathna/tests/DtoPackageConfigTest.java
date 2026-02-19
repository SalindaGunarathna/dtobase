package com.github.SalindaGunarathna.tests;

import com.github.SalindaGunarathna.processor.DtoProcessor;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;

/**
 * Class-level package configuration tests for DTO generation.
 * Verifies default package derivation and custom overrides for DTO and mapper packages.
 */
public class DtoPackageConfigTest {

    /**
     * Verifies default package locations when packageName and mapperPackageName are not set.
     */
    @Test
    void defaultPackages_areDerivedFromEntityPackage() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserCreateDto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserCreateDto\"})",
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
                .generatedSourceFile("com.example.model.dto.user.UserCreateDto")
                .contentsAsUtf8String()
                .contains("package com.example.model.dto.user;");
        assertThat(compilation)
                .generatedSourceFile("com.example.model.mapper.user.UserCreateDtoMapper")
                .contentsAsUtf8String()
                .contains("package com.example.model.mapper.user;");
    }

    /**
     * Verifies that an explicit packageName is used for the generated DTO.
     */
    @Test
    void customDtoPackage_isRespected() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserCreateDto\", packageName = \"com.custom.dto\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserCreateDto\"})",
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
                .generatedSourceFile("com.custom.dto.UserCreateDto")
                .contentsAsUtf8String()
                .contains("package com.custom.dto;");
    }

    /**
     * Verifies that an explicit mapperPackageName is used for the generated mapper.
     */
    @Test
    void customMapperPackage_isRespected() {
        JavaFileObject source = JavaFileObjects.forSourceLines(
                "com.example.model.User",
                "package com.example.model;",
                "import com.github.SalindaGunarathna.annotations.Dto;",
                "import com.github.SalindaGunarathna.annotations.IncludeInDto;",
                "@Dto(name = \"UserCreateDto\", mapperPackageName = \"com.custom.mapper\")",
                "public class User {",
                "  @IncludeInDto(dtos = {\"UserCreateDto\"})",
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
                .generatedSourceFile("com.custom.mapper.UserCreateDtoMapper")
                .contentsAsUtf8String()
                .contains("package com.custom.mapper;");
    }
}
