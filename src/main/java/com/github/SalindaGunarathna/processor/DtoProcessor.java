package com.github.SalindaGunarathna.processor;


import com.github.SalindaGunarathna.annotations.Dto;
import com.github.SalindaGunarathna.annotations.Dtos;
import com.github.SalindaGunarathna.generator.DtoGenerator;
import com.github.SalindaGunarathna.generator.MapperGenerator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.util.*;

@SupportedAnnotationTypes({
        "com.github.SalindaGunarathna.annotations.Dto",
        "com.github.SalindaGunarathna.annotations.Dtos"
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class DtoProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<Element> targets = new HashSet<>();
        targets.addAll(roundEnv.getElementsAnnotatedWith(Dto.class));
        targets.addAll(roundEnv.getElementsAnnotatedWith(Dtos.class));

        for (Element element : targets) {
            if (!(element instanceof TypeElement)) continue;
            TypeElement entity = (TypeElement) element;

            for (Dto config : entity.getAnnotationsByType(Dto.class)) {
                DtoGenerator.generate(processingEnv, entity, config);

                if (config.generateMapper()) {
                    MapperGenerator.generate(processingEnv, entity, config);
                }
            }
        }

        return true;
    }
}

