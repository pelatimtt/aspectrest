package com.dataheaps.aspectrest;

import com.owlike.genson.Context;
import com.owlike.genson.Converter;
import com.owlike.genson.stream.ObjectReader;
import com.owlike.genson.stream.ObjectWriter;
import org.joda.time.DateTime;

/**
 * Created by admin on 23/9/16.
 */
public class JodaTimeConverter implements Converter<DateTime> {

    @Override
    public void serialize(DateTime dateTime, ObjectWriter objectWriter, Context context) throws Exception {
        objectWriter.writeString(dateTime.toString());
    }

    @Override
    public DateTime deserialize(ObjectReader objectReader, Context context) throws Exception {
        return null;
    }
}
