package com.dataheaps.aspectrest;

/**
 * Created by admin on 17/3/18.
 */
public interface RestErrorHandler {
    RestResponse handle(Throwable e);
}
