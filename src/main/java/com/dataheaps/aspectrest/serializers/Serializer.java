package com.dataheaps.aspectrest.serializers;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by admin on 22/2/17.
 */
public interface Serializer {

    Object deserialize(InputStream i, Class<?> type);
    byte[] serialize(Object i);
    String getContentType();

}
