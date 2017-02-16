package com.dataheaps.aspectrest.validation;

import java.util.regex.Pattern;

/**
 * Created by matteopelati on 7/12/15.
 */
public class PhoneNumber implements Validator<String> {

    static Pattern pattern = Pattern.compile("\\+[0-9]+");

    @Override
    public void validate(String name, String value) throws IllegalArgumentException {
        if (!pattern.matcher(value).matches())
            throw new IllegalArgumentException("The field " + name + " is not a valid phone number");
    }
}
