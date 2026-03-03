package com.github.SalindaGunarathna.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(Dtos.class)
public @interface Dto {

    String name();

    String packageName() default "";

    String mapperPackageName() default "";

    boolean generateMapper() default true;

    boolean generateToEntity() default true;

    boolean generateUpdate() default false;

    NullHandling nullHandling() default NullHandling.IGNORE_NULLS;

    IncludePolicy include() default IncludePolicy.ANNOTATED_ONLY;

    boolean copyValidation() default false;

    ValidationNamespace validationNamespace() default ValidationNamespace.JAKARTA;

    boolean generateBuilder() default false;
}
