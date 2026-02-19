package com.github.SalindaGunarathna.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Dtos {
    Dto[] value();
}
