package com.dataheaps.aspectrest;

import com.dataheaps.aspectrest.annotations.*;

/**
 * Created by admin on 16/2/17.
 */
public class Apis implements RestHandler {

    @Override
    public void init() {

    }

    @Get
    @Path("/echo")
    public String echoGet(@FromQueryString @Name("id") String id) {
        return id;
    }

    @Get @Authenticated
    @Path("/echoauth")
    public String echoGetAuth(@FromQueryString @Name("id") String id) {
        return id;
    }

    @Post
    @Path("/echo")
    public String echoPost(@FromBody @Name("id") String id) {
        return id;
    }

    @Post
    @Path("/echoraw")
    public String echoPostRaw(@IsBody @Name("id") String body) {
        return body;
    }
}
