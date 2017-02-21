package com.dataheaps.aspectrest.serializers;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import org.joda.time.DateTime;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by admin on 22/2/17.
 */
public class GensonSerializer implements Serializer {

    Genson genson = new GensonBuilder()
            .useClassMetadata(false)
            .exclude("@class")
            .withConverter(new JodaTimeConverter(), DateTime.class)
            .useDateAsTimestamp(true)
            .create();

    @Override
    public Object deserialize(InputStream i, Class<?> type) {
        return genson.deserialize(i, type);
    }

    @Override
    public byte[] serialize(Object i) {
        return genson.serialize(i).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getContentType() {
        return "application/json; charset=utf-8";
    }
}
