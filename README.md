# dtobase

A lightweight Java annotation processor that generates DTOs and mappers from your entity classes.

## Why it exists
Manual DTOs and mappers are repetitive, error-prone, and easy to drift from the entity model. This processor:
- Generates DTO classes from annotated entities
- Generates mapper classes between entity and DTOs
- Supports multiple DTOs per entity with per-field inclusion rules
- Keeps update semantics explicit and safe

## Features
- Multiple DTOs per entity via repeatable `@Dto`
- Field inclusion control with `@IncludeInDto`
- Optional mapper generation (default on)
- Optional `toEntity` and `updateEntity` generation
- Null-handling strategy for update mapping
- Optional builder generation
- Optional validation annotation copying (Jakarta or Javax)
- Package defaults for DTOs and mappers

## Requirements
- Java 17
- Maven or Gradle build with annotation processing enabled

## Installation (Maven)
Add the dependency and the annotation processor path.

```xml
<dependencies>
  <dependency>
    <groupId>io.github.salindagunarathna</groupId>
    <artifactId>dtobase</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
  </dependency>
</dependencies>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.13.0</version>
      <configuration>
        <annotationProcessorPaths>
          <path>
            <groupId>io.github.salindagunarathna</groupId>
            <artifactId>dtobase</artifactId>
            <version>1.0-SNAPSHOT</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
```

Replace the version with your published release version.

## Quick Start

### 1) Annotate your entity
```java
@Dto(name = "UserCreateDto")
@Dto(name = "UserUpdateDto", generateUpdate = true)
@Dto(name = "UserResponseDto")
public class User {

  @IncludeInDto(dtos = {"UserCreateDto", "UserUpdateDto", "UserResponseDto"})
  private String email;

  @IncludeInDto(dtos = {"UserResponseDto"}, targets = {"displayName"})
  private String name;

  // getters/setters...
}
```

### 2) Use generated classes
```java
UserCreateDto dto = new UserCreateDto();
User entity = UserCreateDtoMapper.toEntity(dto);

UserUpdateDto update = new UserUpdateDto();
UserUpdateDtoMapper.updateEntity(entity, update);
```

### 3) Generated outputs
Generated sources appear under:
```
 target/generated-sources/annotations
```

## Default Package Rules
When `packageName` or `mapperPackageName` are empty:
- DTO package: `<entity package>.dto.<entitylower>`
- Mapper package: `<entity package>.mapper.<entitylower>`

Example for `com.example.model.User`:
- DTOs: `com.example.model.dto.user.*`
- Mappers: `com.example.model.mapper.user.*`

## Class-Level Configuration (`@Dto`)

| Option | Default | Description |
| --- | --- | --- |
| `name` | required | DTO class name (used exactly as provided) |
| `packageName` | `""` | DTO package override |
| `mapperPackageName` | `""` | Mapper package override |
| `generateMapper` | `true` | Generate mapper class |
| `generateToEntity` | `true` | Generate `toEntity` method |
| `generateUpdate` | `false` | Generate `updateEntity` method |
| `nullHandling` | `IGNORE_NULLS` | Update behavior for nulls |
| `copyValidation` | `false` | Copy validation annotations to DTO |
| `validationNamespace` | `JAKARTA` | Use Jakarta or Javax annotations |
| `generateBuilder` | `false` | Generate DTO builder |

### Null handling
- `IGNORE_NULLS`: skips updating entity fields when DTO value is null
- `SET_NULLS`: sets entity fields even when DTO value is null

## Field-Level Configuration (`@IncludeInDto`)

| Option | Description |
| --- | --- |
| `dtos` | List of DTO names to include this field |
| `value` | Alias for `dtos` (use only one) |
| `targets` | Rename field per DTO (same length as `dtos`) |
| `copyValidation` | Per-DTO override for validation copying |

### Alias usage
```java
@IncludeInDto(dtos = {"UserCreateDto", "UserUpdateDto", "UserResponseDto"})
private String email;

@IncludeInDto({"UserCreateDto", "UserUpdateDto", "UserResponseDto"})
private String username;
```

### Notes
- `dtos` and `value` are mutually exclusive.
- `targets` length must match `dtos` length.
- If a target name is empty, it falls back to the source field name.

## Builder Generation
Enable per DTO:
```java
@Dto(name = "UserDto", generateBuilder = true)
```
Generated builder:
```java
UserDto dto = UserDto.builder()
  .email("a@b.com")
  .build();
```
Builder method names respect `targets` (renamed fields).

## Validation Copying
- Default is **off**.
- Enable with `@Dto(copyValidation = true)`.
- Per-field override via `@IncludeInDto(copyValidation = {...})`.
- Namespace: `JAKARTA` or `JAVAX`.

## Testing
All tests are documented in:
- `TEST_CATALOG.md`

## Known Requirements
Mappers rely on standard JavaBean getters/setters. If accessors are missing or mismatched, compilation fails.


