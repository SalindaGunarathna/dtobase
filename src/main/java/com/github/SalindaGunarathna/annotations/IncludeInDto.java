package com.github.SalindaGunarathna.annotations;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface IncludeInDto {
    String[] value() default {};

    String[] dtos() default {};

    String[] targets() default {};

    boolean[] copyValidation() default {};
}
