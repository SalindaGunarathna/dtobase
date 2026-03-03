package com.github.SalindaGunarathna.annotations;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface ExcludeInDto {
    String[] value() default {};

    String[] dtos() default {};
}
