package com.dataheaps.aspectrest.validation;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by matteopelati on 30/11/15.
 */
public class Email implements Validator<String> {

    static Pattern emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @Override
    public void validate(String name, String value) throws IllegalArgumentException {
        Matcher m = emailPattern.matcher(value);
        if (!m.matches())
            throw new IllegalArgumentException("The field " + name + " does not have a valid email format");
    }
}
