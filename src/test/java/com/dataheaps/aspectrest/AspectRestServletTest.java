package com.dataheaps.aspectrest;

import com.google.common.collect.ImmutableMap;
import com.owlike.genson.Genson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by admin on 16/2/17.
 */
public class AspectRestServletTest {

    static final int PORT = 23457;
    static final String USER_AGENT = "Mozilla/5.0 (iPad; U; CPU OS 3_2_1 like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko) Mobile/7B405";

    static Server server;

    @BeforeClass
    public static void setup() throws Exception {


        server = new Server(PORT);

        AspectRestServlet restServlet = new AspectRestServlet();
        restServlet.setModules(ImmutableMap.of("apis", new Apis()));
        restServlet.setAuthenticators(ImmutableMap.of("auth", new Auth()));

        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(new ServletHolder(restServlet), "/");

        server.setHandler(servletHandler);
        server.start();


//        serverThread = new Thread() {
//
//            @Override
//            public void run() {
//                try {
//
//                    server = new Server(PORT);
//
//                    AspectRestServlet restServlet = new AspectRestServlet();
//                    restServlet.setModules(ImmutableMap.of("apis", new Apis()));
//                    restServlet.setAuthenticators(ImmutableMap.of("auth", new Auth()));
//
//                    ServletHandler servletHandler = new ServletHandler();
//                    servletHandler.addServletWithMapping(new ServletHolder(restServlet), "/");
//
//                    server.setHandler(servletHandler);
//                    server.start();
//
//                }
//                catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        };
//
//        serverThread.start();

    }

    @AfterClass
    public static void shutdown() throws Exception {
        server.stop();
    }


    @Test
    public void testGet() throws Exception {

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet("http://localhost:" + PORT + "/apis/echo?id=test");

        request.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(request);

        assert (response.getStatusLine().getStatusCode() == 200);
        String resp = new Genson().deserialize(response.getEntity().getContent(), String.class);
        assert (resp.equals("test"));

    }

    @Test
    public void testGetPath() throws Exception {

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet("http://localhost:" + PORT + "/apis/echo/test");

        request.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(request);

        assert (response.getStatusLine().getStatusCode() == 200);
        String resp = new Genson().deserialize(response.getEntity().getContent(), String.class);
        assert (resp.equals("test"));

    }

    @Test
    public void testGetQs() throws Exception {

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet("http://localhost:" + PORT + "/apis/echorawqs?id=test");

        request.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(request);

        assert (response.getStatusLine().getStatusCode() == 200);
        String resp = new Genson().deserialize(response.getEntity().getContent(), String.class);
        assert (resp.equals("id=test"));

    }


    @Test
    public void testPost() throws Exception {

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost("http://localhost:" + PORT + "/apis/echo");
        request.setEntity(new StringEntity(new Genson().serialize(
                ImmutableMap.of("id", "test")
        )));

        request.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(request);

        assert (response.getStatusLine().getStatusCode() == 200);
        String resp = new Genson().deserialize(response.getEntity().getContent(), String.class);
        assert (resp.equals("test"));


    }

    @Test
    public void testPostRaw() throws Exception {

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost("http://localhost:" + PORT + "/apis/echoraw");
        request.setEntity(new StringEntity("test"));

        request.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(request);

        assert (response.getStatusLine().getStatusCode() == 200);
        String resp = new Genson().deserialize(response.getEntity().getContent(), String.class);
        assert (resp.equals("test"));


    }


    @Test
    public void testAuthGet() throws Exception {

        HttpClient client = HttpClientBuilder.create().build();

        HttpPost authRequest = new HttpPost("http://localhost:" + PORT + "/auth/login");
        authRequest.setEntity(new StringEntity(new Genson().serialize(
                ImmutableMap.of("username", "test@test.com", "password", "test", "rememberme", false)
        )));
        authRequest.addHeader("User-Agent", USER_AGENT);
        HttpResponse authResponse = client.execute(authRequest);

        assert (authResponse.getStatusLine().getStatusCode() == 200);


        HttpGet request = new HttpGet("http://localhost:" + PORT + "/apis/echoauth?id=test");

        request.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(request);

        assert (response.getStatusLine().getStatusCode() == 200);
        String resp = new Genson().deserialize(response.getEntity().getContent(), String.class);
        assert (resp.equals("test"));

    }

    @Test
    public void testAuthGetFail() throws Exception {

        HttpClient client = HttpClientBuilder.create().build();

        HttpGet request = new HttpGet("http://localhost:" + PORT + "/apis/echoauth?id=test");

        request.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(request);

        assert (response.getStatusLine().getStatusCode() != 200);

    }

    @Test
    public void testValidate() throws Exception {

        HttpClient client = HttpClientBuilder.create().build();

        HttpGet request = new HttpGet("http://localhost:" + PORT + "/apis/validate?id=test@gmail.com");

        request.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(request);

        assert (response.getStatusLine().getStatusCode() == 200);

    }

    @Test
    public void testValidateFailure() throws Exception {

        HttpClient client = HttpClientBuilder.create().build();

        HttpGet request = new HttpGet("http://localhost:" + PORT + "/apis/validate?id=gmail.com");

        request.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(request);

        assert (response.getStatusLine().getStatusCode() != 200);

    }


}