package com.dataheaps.aspectrest.annotations;


import com.dataheaps.aspectrest.validation.Validator;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by matteopelati on 30/11/15.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Validate {
    Class<? extends Validator>[] value();
}
