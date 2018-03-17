package com.dataheaps.aspectrest;


import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dataheaps.aspectrest.annotations.Authenticated;
import com.dataheaps.aspectrest.annotations.Delete;
import com.dataheaps.aspectrest.annotations.FromBody;
import com.dataheaps.aspectrest.annotations.FromPath;
import com.dataheaps.aspectrest.annotations.FromQueryString;
import com.dataheaps.aspectrest.annotations.Get;
import com.dataheaps.aspectrest.annotations.Head;
import com.dataheaps.aspectrest.annotations.IsBody;
import com.dataheaps.aspectrest.annotations.IsPath;
import com.dataheaps.aspectrest.annotations.IsQueryString;
import com.dataheaps.aspectrest.annotations.Name;
import com.dataheaps.aspectrest.annotations.Path;
import com.dataheaps.aspectrest.annotations.Post;
import com.dataheaps.aspectrest.annotations.Priority;
import com.dataheaps.aspectrest.annotations.Put;
import com.dataheaps.aspectrest.modules.auth.AuthModule;
import com.dataheaps.aspectrest.serializers.GensonSerializer;
import com.dataheaps.aspectrest.serializers.Serializer;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by matteopelati on 26/11/15.
 */
public class AspectRestServlet extends HttpServlet {

    static ThreadLocal<Object> localContext = new ThreadLocal<>();

    public static Object getContext() {
        return localContext.get();
    }

    static final String CONTENT_TYPE = "Content-Type";
    static final String CONTENT_LENGTH = "Content-Length";
    static final String CACHE_CONTROL = "Cache-Control";

    static final SimpleDateFormat INPUT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static final Logger logger = LoggerFactory.getLogger(AspectRestServlet.class);

    @Getter @Setter Map<String, RestHandler> modules = new HashMap<>();
    @Getter @Setter Map<String, AuthModule> authenticators = new HashMap<>();
    @Getter @Setter Map<String, String> headers = new HashMap<>();
    @Getter @Setter RestErrorHandler errorHandler;

    SortedSet<RestServiceDescriptor> serviceTree = new TreeSet<>();
    List<AuthModule> authenticatorTree = new ArrayList<>();
    Object context;
    Serializer serializer;
    ValidatorFactory factory;
    ExecutableValidator validator;

    public AspectRestServlet() {
        serializer = new GensonSerializer();
    }

    public AspectRestServlet(boolean validate) {
        serializer = new GensonSerializer();
        if (validate) {
            factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator().forExecutables();
        }
    }

    public AspectRestServlet(boolean validate, Serializer serializer, Object context) {
        this.context = context;
        this.serializer = serializer;
        if (validate) {
            factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator().forExecutables();
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            for (Map.Entry<String, RestHandler> service : modules.entrySet())
                addService(service.getKey(), service.getValue());
            for (Map.Entry<String, AuthModule> service : authenticators.entrySet())
                addAuthService(service.getKey(), service.getValue());
        }
        catch (IllegalAccessException e) {
            throw new ServletException(e);
        }
    }

    Set<RestSource> getRestSource(Parameter p) {

        Set<RestSource> restSource = new HashSet<>();

        if (p.isAnnotationPresent(FromPath.class))
            restSource.add(RestSource.PATH_ARG);
        if (p.isAnnotationPresent(IsPath.class))
            restSource.add(RestSource.PATH_ALL);
        if (p.isAnnotationPresent(IsBody.class))
            restSource.add(RestSource.BODY_ALL);
        if (p.isAnnotationPresent(FromBody.class))
            restSource.add(RestSource.BODY_ARG);
        if (p.isAnnotationPresent(FromQueryString.class))
            restSource.add(RestSource.QS_ARG);
        if (p.isAnnotationPresent(IsQueryString.class))
            restSource.add(RestSource.QS_ALL);

        return restSource;
    }

    RestMethod getRestMethod(Method m) {
        if (m.isAnnotationPresent(Get.class))
            return RestMethod.GET;
        else if (m.isAnnotationPresent(Head.class))
            return RestMethod.HEAD;
        if (m.isAnnotationPresent(Delete.class))
            return RestMethod.DELETE;
        if (m.isAnnotationPresent(Post.class))
            return RestMethod.POST;
        if (m.isAnnotationPresent(Put.class))
            return RestMethod.PUT;
        else return null;
    }

    void addService(String path, RestHandler handler) throws IllegalAccessException {
        addService(path, handler, serviceTree);
    }

    void addAuthService(String path, AuthModule authHandler) throws IllegalAccessException {
        addService(path, authHandler, serviceTree);
        authenticatorTree.add(authHandler);
    }

    void addService(String path, RestHandler service, SortedSet services) throws IllegalArgumentException {

        service.init();

        String basePath = path;
        for (Method m: service.getClass().getMethods()) {

            RestMethod verb = getRestMethod(m);
            if (verb == null) continue;

            m.setAccessible(true);
            Path methodPath = m.getAnnotation(Path.class);
            if (methodPath == null)
                throw new IllegalArgumentException(
                        String.format("A path must be specified for method %s.%s", service.getClass().getCanonicalName(), m.getName())
                );

            Priority priority = m.getAnnotation(Priority.class);
            boolean authenticated = m.isAnnotationPresent(Authenticated.class);

            List<RestServiceDescriptor.ArgIndex> argIndexes = new ArrayList<>();

            for (int ctr=0;ctr<m.getParameters().length;ctr++) {

                Parameter p = m.getParameters()[ctr];
                Set<RestSource> restSource = getRestSource(p);
                if (restSource == null) continue;

                Name name = p.getAnnotation(Name.class);
                if (name == null)
                    throw new IllegalArgumentException("A name must be specified for a parameter" + service.getClass().getCanonicalName() + "." + m.getName());

                argIndexes.add(new RestServiceDescriptor.ArgIndex(
                        restSource, ctr, name.value(), p.getType()
                ));
            }

            String servicePath = StringUtils.join("/", basePath, methodPath.value().isEmpty() ? "" : StringUtils.join("/", methodPath.value())).replaceAll("/+", "/");
            
//            Paths.get("/" + basePath, methodPath.value() + "/").normalize().toString();

            RestServiceDescriptor descriptor = new RestServiceDescriptor(
                    priority != null ? priority.value() : 0,
                    Pattern.compile("^" + servicePath + "$"),
                    service, m, authenticated, verb,
                    argIndexes
            );

            logger.info(String.format(
                    "Registered handler for path %s mapped to %s.%s",
                    servicePath, m.getDeclaringClass().getCanonicalName(), m.getName()
            ));

            services.add(descriptor);

        }
    }

    void sendResponse(Object o, HttpServletResponse resp) throws IOException {

        byte[] respBuffer = serializer.serialize(o);
        resp.setHeader(CONTENT_TYPE, serializer.getContentType());
        resp.setHeader(CONTENT_LENGTH, Integer.toString(respBuffer.length));

        resp.getOutputStream().write(respBuffer);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getOutputStream().flush();
        resp.getOutputStream().close();
    }

    Object convertString(String source, Class type) throws IllegalArgumentException {

        if (source == null)
            return null;

        else if (type.equals(String.class))
            return source;

        else if (type.equals(Long.class))
            return Long.parseLong(source);
        else if (type.equals(long.class))
            return Long.parseLong(source);

        else if (type.equals(Integer.class))
            return Integer.parseInt(source);
        else if (type.equals(int.class))
            return Integer.parseInt(source);

        else if (type.equals(Float.class))
            return Float.parseFloat(source);
        else if (type.equals(float.class))
            return Float.parseFloat(source);

        else if (type.equals(Double.class))
            return Double.parseDouble(source);
        else if (type.equals(double.class))
            return Double.parseDouble(source);

        else if (type.equals(Boolean.class))
            return Boolean.parseBoolean(source);
        else if (type.equals(boolean.class))
            return Boolean.parseBoolean(source);

        else if (type.equals(Date.class)) {
            try {
                return INPUT_DATE_FORMAT.parse(source);
            }
            catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        }

        else if (type.equals(DateTime.class)) {
            try {
                return DateTime.parse(source);
            }
            catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        else if (type.isEnum())
            return Enum.valueOf(type, source);
        else
            throw new IllegalArgumentException("Cannot convertString \"" + source + "\" to type " + type.toString());
    }

    Map<String,String> parseQueryString(String qs) {

        if (qs == null || qs.isEmpty())
            return Collections.EMPTY_MAP;

        Map<String,String> values = new HashMap<>();
        for (String token : qs.split("&")) {
            if (token.trim().isEmpty()) continue;
            String[] kv = token.split("=");
            if (kv.length >= 2)
                values.put(kv[0].trim(), URLDecoder.decode(kv[1].trim()));
        }

        return values;
    }

    RestRequest getServiceDescriptor(RestMethod verb, String path, String qs, Map<String,String> parsedQs) throws FileNotFoundException, IllegalArgumentException {

        for (RestServiceDescriptor d : serviceTree) {

            if (!d.verb.equals(verb))
                continue;
            Matcher m = d.path.matcher(path);
            logger.debug(String.format("Matching request %s with %s", path, d.path.toString()));
            if (!m.matches())
                continue;

            Object[] argVals = new Object[d.method.getParameterCount()];
            for (RestServiceDescriptor.ArgIndex i : d.argsIndexes) {

                if (i.source.contains(RestSource.PATH_ARG) && argVals[i.index] == null) {
                    argVals[i.index] = convertString(m.group(i.name), d.method.getParameterTypes()[i.index]);
                }
                else if (i.source.contains(RestSource.PATH_ALL) && argVals[i.index] == null) {
                    argVals[i.index] = path;
                }
                else if (i.source.contains(RestSource.QS_ARG) && argVals[i.index] == null) {
                    argVals[i.index] = convertString(parsedQs.get(i.name), d.method.getParameterTypes()[i.index]);
                }
                else if (i.source.contains(RestSource.QS_ALL) && argVals[i.index] == null) {
                    argVals[i.index] = qs;
                }
            }

            return new RestRequest(d, argVals);

        }

        throw new FileNotFoundException("Path " + path + " not found");
    }

    void validateParameters(RestServiceDescriptor d, Object[] args) throws IllegalAccessException, InstantiationException {

        if (validator == null)
            return;

        Set<ConstraintViolation<Object>> violations = validator.validateParameters(d.service, d.method, args);

        if (violations == null || violations.isEmpty())
            return;

        StringBuffer res = new StringBuffer();
        for (ConstraintViolation<Object> v: violations) {
            res.append(v.getMessage() + ";");
        }

        throw new ValidationException(res.toString());
    }

    void fillBodyParameters(InputStream body, RestRequest request) throws IOException {

        byte[] bytes = IOUtils.toByteArray(body);
        Map inputMap = null;

        for (RestServiceDescriptor.ArgIndex i : request.descriptor.argsIndexes) {

            if (i.source.contains(RestSource.BODY_ARG) && request.args[i.index] == null) {
                if (inputMap == null)
                    inputMap = (Map) serializer.deserialize(new ByteArrayInputStream(bytes), Map.class);
                request.args[i.index] = inputMap.get(i.name);
            }
            else if (i.source.contains(RestSource.BODY_ALL) && request.args[i.index] == null) {
                request.args[i.index] = serializer.deserialize(new ByteArrayInputStream(bytes), i.argClass);
            }
        }

    }

    void checkAuthenticated() throws IllegalAccessException {

        for (AuthModule authHandler : authenticatorTree) {
            try {
                Object profile = authHandler.checkAuthenticated();
                ServletContext.currentUser.set(profile);
                return;
            }
            catch (Exception e) {

            }
        }
        throw new IllegalAccessException();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(RestMethod.GET, req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(RestMethod.POST, req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(RestMethod.PUT, req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(RestMethod.DELETE, req, resp);
    }

    void handle(RestMethod method, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {

        try {

            logger.info(method.toString() + " " + httpServletRequest.getServletPath());

            ServletContext.respose.set(httpServletResponse);
            ServletContext.request.set(httpServletRequest);
            localContext.set(context);

            httpServletResponse.setHeader(CACHE_CONTROL, "no-cache");
            for (Map.Entry<String,String> e : headers.entrySet())
                httpServletResponse.setHeader(e.getKey(), e.getValue());

            Object result = null;
            RestRequest restRequest = getServiceDescriptor(
                    method,
                   httpServletRequest.getServletPath().replaceAll("/+", "/"),
//                    Paths.get(httpServletRequest.getServletPath()).normalize().toString(),
                    httpServletRequest.getQueryString(),
                    parseQueryString(httpServletRequest.getQueryString())
            );

            if (restRequest.descriptor.authenticated)
                checkAuthenticated();

            if (method == RestMethod.POST || method == RestMethod.PUT || method == RestMethod.DELETE)
                fillBodyParameters(httpServletRequest.getInputStream(), restRequest);

            validateParameters(restRequest.descriptor, restRequest.args);
            result = restRequest.descriptor.method.invoke(restRequest.descriptor.service, restRequest.args);
            sendResponse(result, httpServletResponse);

        }
        catch (FileNotFoundException e) {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
        catch (IllegalAccessException e) {
            httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
        catch (NoSuchElementException e) {
            httpServletResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, e.getMessage());
        }
        catch (Exception e) {

            if (errorHandler != null) {
                RestError err = errorHandler.handle(e);
                httpServletResponse.sendError(err.status, err.message);
            }
            else {
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }

    }


}
