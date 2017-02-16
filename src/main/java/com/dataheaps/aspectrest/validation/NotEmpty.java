package com.dataheaps.aspectrest.validation;

/**
 * Created by matteopelati on 30/11/15.
 */
public class NotEmpty implements Validator<String> {

    @Override
    public void validate(String name, String value) throws IllegalArgumentException {
        if (value.trim().isEmpty())
            throw new IllegalArgumentException("The field " + name + " cannot be empty");
    }
}
