package com.dataheaps.aspectrest;

import com.dataheaps.aspectrest.modules.auth.AbstractAuthModule;

import java.io.IOException;

/**
 * Created by admin on 16/2/17.
 */
public class Auth extends AbstractAuthModule<String> {

    @Override
    protected String authenticate(String identity, String password) throws IllegalAccessException, IOException {
        if (identity.equals("test@test.com") && password.equals("test"))
            return identity;
        throw new IllegalAccessException();
    }

    @Override
    protected String getUserToken(String identity, String password, String profile) throws IllegalAccessException, IOException {
        return identity + "_" + password;
    }

    @Override
    protected String authenticateWithToken(String token) throws IllegalAccessException, IOException {
        if (token.equals("test@test.com_test"))
            return "test@test.com";
        throw new IllegalAccessException();
    }
}
