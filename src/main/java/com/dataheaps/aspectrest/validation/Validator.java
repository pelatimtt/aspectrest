package com.dataheaps.aspectrest.validation;

/**
 * Created by matteopelati on 30/11/15.
 */
public interface Validator<T> {
    void validate(String name, T value) throws IllegalArgumentException;
}
