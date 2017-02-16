package com.dataheaps.aspectrest.modules.auth;

import com.dataheaps.aspectrest.ServletContext;
import com.dataheaps.aspectrest.annotations.*;
import com.dataheaps.aspectrest.validation.Email;
import com.dataheaps.aspectrest.validation.NotEmpty;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.Setter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by matteopelati on 27/11/15.
 */


public abstract class AbstractAuthModule<T> implements AuthModule<T> {

    @Getter @Setter String encryptionKey = AbstractAuthModule.class.toString();
    @Getter @Setter int sessionTimeout = 60*60;

    Cache<String, T> sessionCache;
    SecretKey secretKey;

    @Override
    public void init() {

        sessionCache = CacheBuilder.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();
        secretKey = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.US_ASCII), "AES");
    }

    void writeSessionCookie(String sessionId) {
        Cookie sessionCookie = new Cookie(this.getClass().getCanonicalName() + ".SESSION_ID", sessionId);
        sessionCookie.setMaxAge(sessionTimeout);
        sessionCookie.setPath("/");
        ServletContext.getRespose().addCookie(sessionCookie);
    }

    @Post @Path("login")
    public T login(
            @FromBody @Name("username") @NotNull @Validate(Email.class) String username,
            @FromBody @Name("password") @NotNull @Validate(NotEmpty.class) String password,
            @FromBody @Name("rememberme") @NotNull Boolean rememberMe
    ) throws IllegalAccessException, IOException {

        T profile = authenticate(username, password);
        String sessionId = UUID.randomUUID().toString();
        writeSessionCookie(sessionId);

        String token = null;
        if (rememberMe && (token = getUserToken(username, password, profile)) != null) {
            Cookie userCookie = new Cookie(this.getClass().getCanonicalName() + ".AUTH_TOKEN", token);
            userCookie.setMaxAge(60*60*24*365);
            userCookie.setPath("/");
            ServletContext.getRespose().addCookie(userCookie);
        }

        sessionCache.put(sessionId, profile);

        return profile;
    }


    @Override
    public T checkAuthenticated() throws IllegalAccessException, IOException {

        String sessionId = getCookie(this.getClass().getCanonicalName() + ".SESSION_ID");
        if (sessionId != null) {
            T currentUser = sessionCache.getIfPresent(sessionId);
            if (currentUser != null) {
                writeSessionCookie(sessionId);
                return currentUser;
            }
        }

        String authToken = getCookie(this.getClass().getCanonicalName() + ".AUTH_TOKEN");
        if (authToken == null)
            throw new IllegalAccessException();

        T profile = authenticateWithToken(authToken);

        sessionId = UUID.randomUUID().toString();
        writeSessionCookie(sessionId);
        sessionCache.put(sessionId, profile);

        return profile;

    }

    String getCookie(String key) {
        String res = null;
        Cookie[] cookies = ServletContext.getRequest().getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (c.getName().equals(key)) {
                res = c.getValue();
                break;
            }
        }
        return res;
    }

    @Get @Path("logout")
    public void logout() {

        Cookie sessionCookie = new Cookie(this.getClass().getCanonicalName() + ".SESSION_ID", "");
        sessionCookie.setMaxAge(0);
        sessionCookie.setPath("/");
        ServletContext.getRespose().addCookie(sessionCookie);

        Cookie userCookie = new Cookie(this.getClass().getCanonicalName() + ".AUTH_TOKEN", "");
        userCookie.setMaxAge(0);
        userCookie.setPath("/");
        ServletContext.getRespose().addCookie(userCookie);

    }

    protected abstract T authenticate(String identity, String password) throws IllegalAccessException, IOException;
    protected abstract String getUserToken(String identity, String password, T profile) throws IllegalAccessException, IOException;
    protected abstract T authenticateWithToken(String token) throws IllegalAccessException, IOException;

}
