package com.dataheaps.aspectrest;

import com.dataheaps.aspectrest.annotations.*;
import org.hibernate.validator.constraints.Email;

import javax.validation.constraints.*;
import javax.validation.constraints.NotNull;

/**
 * Created by admin on 16/2/17.
 */
public class Apis implements RestHandler {

    @Override
    public void init() {

    }

    @Get
    @Path("/echo/(?<id>.+)")
    public String echoGetPath(@FromPath @Name("id") String id) {
        return id;
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

    @Get
    @Path("/echorawqs")
    public String echoPostRawQs(@IsQueryString @Name("id") String body) {
        return body;
    }

    @Get
    @Path("/validate")
    public String validate(@FromQueryString @Name("id") @NotNull @Email String body) {
        return body;
    }


}
