package com.dataheaps.aspectrest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by matteopelati on 29/11/15.
 */
public class ServletContext {

    static ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();
    static ThreadLocal<HttpServletResponse> respose = new ThreadLocal<>();
    static ThreadLocal currentUser = new ThreadLocal();

    public static HttpServletRequest getRequest() {
        return request.get();
    }
    public static HttpServletResponse getRespose() {
        return respose.get();
    }
    public static <T> T getCurrentUser() {
        return (T) currentUser.get();
    }
}
