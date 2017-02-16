package com.dataheaps.aspectrest.modules.auth;

import com.dataheaps.aspectrest.RestHandler;

import java.io.IOException;

/**
 * Created by matteopelati on 29/11/15.
 */
public interface AuthModule<T> extends RestHandler {

    T checkAuthenticated() throws IllegalAccessException, IOException;

}
