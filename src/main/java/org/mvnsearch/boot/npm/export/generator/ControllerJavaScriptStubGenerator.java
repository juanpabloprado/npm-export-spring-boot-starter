package org.mvnsearch.boot.npm.export.generator;

import org.intellij.lang.annotations.Language;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller JavaScript Stub generator
 *
 * @author linux_china
 */
public class ControllerJavaScriptStubGenerator {
    private Class<?> controllerClass;
    private String jsClassName;
    private List<Method> requestMethods;
    /**
     * class set for typedef
     */
    private Set<Class<?>> clazzSet = new HashSet<>();
    private String basePath;

    public ControllerJavaScriptStubGenerator(Class<?> controllerClass) {
        this.controllerClass = controllerClass;
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(controllerClass, RequestMapping.class);
        if (requestMapping != null) {
            String[] basePaths = requestMapping.value();
            if (basePaths.length > 0) {
                this.basePath = basePaths[0];
            }
        }
        this.requestMethods = Arrays.stream(this.controllerClass.getMethods())
                .filter(method -> {
                    return AnnotationUtils.findAnnotation(method, RequestMapping.class) != null;
                }).collect(Collectors.toList());
        this.jsClassName = controllerClass.getSimpleName();
    }

    public String generate(String baseUrl) {
        @Language("JavaScript")
        String global = "const axios = require('axios');\n" +
                "\n" +
                "let isBrowser = new Function('try {return this===window;}catch(e){ return false;}');\n" +
                "let isNode = new Function('try {return this===global;}catch(e){return false;}');\n" +
                "\n" +
                "axios.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';\n" +
                "\n" +
                "/**\n" +
                " * @param uri {string}\n" +
                " * @param pathVariables {object}\n" +
                " * @return {string}\n" +
                " */\n" +
                "function formatUri(uri, pathVariables) {\n" +
                "    let newUri = uri;\n" +
                "    for (const name in pathVariables) {\n" +
                "        newUri = newUri.replace('*{' + name + '}', pathVariables[name])\n" +
                "                .replace('{' + name + '}', pathVariables[name])\n" +
                "    }\n" +
                "    return newUri;\n" +
                "}\n";
        @Language(value = "JavaScript", suffix = "}")
        String classDeclare = "class XxxxController {\n" +
                "    constructor() {\n" +
                "        if (isBrowser()) {\n" +
                "            this.baseUrl = '';\n" +
                "        } else {\n" +
                "            this.baseUrl = 'http://localhost:8080'\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    /**\n" +
                "     * set base url\n" +
                "     * @param baseUrl base url\n" +
                "     * @returns {XxxxController}\n" +
                "     */\n" +
                "    setBaseUrl(baseUrl) {\n" +
                "        this.baseUrl = baseUrl;\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    /**\n" +
                "     * set JWT Token\n" +
                "     * @param token token token\n" +
                "     * @return {XxxxController}\n" +
                "     */\n" +
                "    setJwtToken(token) {\n" +
                "        this.jwtToken = token;\n" +
                "        return this;\n" +
                "    }\n" +
                "\n" +
                "    /**\n" +
                "     * set config filter\n" +
                "     * @param filter {function}\n" +
                "     * @return {XxxxController}\n" +
                "     */\n" +
                "    setConfigFilter(filter) {\n" +
                "        this.configFilter = filter;\n" +
                "        return this;\n" +
                "    }\n";
        StringBuilder builder = new StringBuilder();
        builder.append(global);
        builder.append(classDeclare.replaceAll("XxxxController", jsClassName));
        for (Method requestMethod : requestMethods) {
            builder.append(toJsCode(generateMethodStub(requestMethod)) + "\n");
        }
        builder.append("}\n\n");
        builder.append("module.exports = new UserController();\n");
        return builder.toString();
    }

    public JsHttpStubMethod generateMethodStub(Method method) {
        JsHttpStubMethod stubMethod = new JsHttpStubMethod();
        stubMethod.setName(method.getName());
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        String[] paths = null;
        RequestMethod requestMethod = null;
        if (getMapping != null) {
            paths = getMapping.value();
            requestMethod = RequestMethod.GET;
        } else if (postMapping != null) {
            paths = postMapping.value();
            requestMethod = RequestMethod.POST;
        } else if (requestMapping != null) {
            paths = requestMapping.value();
            RequestMethod[] requestMethods = requestMapping.method();
            if (requestMethods.length > 0) {
                requestMethod = requestMethods[0];
            }
        }
        stubMethod.setMethod(requestMethod == null ? RequestMethod.POST : requestMethod);
        if (paths != null && paths.length > 0) {
            stubMethod.setUri(paths[0]);
        }
        if (basePath != null && !basePath.isEmpty()) {
            stubMethod.setUri(basePath + stubMethod.getUri());
        }
        Parameter[] parameters = method.getParameters();
        if (parameters.length > 0) {
            for (Parameter parameter : parameters) {
                JsParam jsParam = new JsParam();
                jsParam.setName(parameter.getName());
                jsParam.setType(parameter.getType());
                PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                if (pathVariable != null) {
                    String value = pathVariable.value();
                    if (value.isEmpty()) {
                        value = jsParam.getName();
                    }
                    jsParam.setPathVariableName(value);
                }
                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                if (requestParam != null) {
                    String value = requestParam.value();
                    if (value.isEmpty()) {
                        value = jsParam.getName();
                    }
                    jsParam.setRequestParamName(value);
                }
                RequestHeader requestHeader = parameter.getAnnotation(RequestHeader.class);
                if (requestHeader != null) {
                    String value = requestHeader.value();
                    jsParam.setHttpHeaderName(value);
                }
                RequestBody requestBody = parameter.getAnnotation(RequestBody.class);
                if (requestBody != null) {
                    jsParam.setBodyData(true);
                    Class<?> bodyType = parameter.getType();
                    if (bodyType.isAssignableFrom(String.class)) {
                        stubMethod.setRequestContentType("text/plain");
                    } else if (bodyType.isAssignableFrom(ByteBuffer.class)
                            || bodyType.isAssignableFrom(byte[].class)) {
                        stubMethod.setRequestContentType("application/octet-stream");
                    } else {
                        stubMethod.setRequestContentType("application/json");
                    }
                }
                stubMethod.addParam(jsParam);
            }
        }
        //return type

        Type genericReturnType = method.getGenericReturnType();
        stubMethod.setReturnType(parseInferredClass(genericReturnType));

        return stubMethod;
    }

    public static Class<?> parseInferredClass(Type genericType) {
        Class<?> inferredClass = null;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) genericType;
            Type[] typeArguments = type.getActualTypeArguments();
            if (typeArguments.length > 0) {
                final Type typeArgument = typeArguments[0];
                if (typeArgument instanceof ParameterizedType) {
                    inferredClass = (Class<?>) ((ParameterizedType) typeArgument).getActualTypeArguments()[0];
                } else {
                    inferredClass = (Class<?>) typeArgument;
                }
            }
        }
        if (inferredClass == null && genericType instanceof Class) {
            inferredClass = (Class<?>) genericType;
        }
        return inferredClass;
    }

    public String toJsCode(JsHttpStubMethod stubMethod) {
        StringBuilder builder = new StringBuilder();
        builder.append("/**\n");
        builder.append("*\n");
        for (JsParam param : stubMethod.getParams()) {
            if (param.isFromRequestSide()) {
                builder.append("* @param {" + param.getJsType() + "}\n");
            }
        }
        builder.append("* @return {Promise<" + stubMethod.getJsReturnType() + ">}\n");
        builder.append("*/\n");
        builder.append(stubMethod.getName() + "(");
        if (!stubMethod.getParams().isEmpty()) {
            String paramsDeclare = stubMethod.getParams().stream()
                    .filter(JsParam::isFromRequestSide)
                    .map(JsParam::getName)
                    .collect(Collectors.joining(", "));
            builder.append(paramsDeclare);
        }
        builder.append("){\n");
        builder.append("  let config = {\n");
        if (stubMethod.hasPathVariable()) {
            builder.append("    url: this.baseUrl + formatUri('" + stubMethod.getUri() + "'," + formatPathVariables(stubMethod) + "),\n");
        } else {
            builder.append("    url: this.baseUrl + '" + stubMethod.getUri() + "',\n");
        }
        builder.append("    headers: " + formatHttpHeaders(stubMethod) + ",\n");
        if (stubMethod.isPlainBody()) {
            builder.append("     data: " + stubMethod.getRequestBodyParam().getName() + ",\n");
        } else {
            if (stubMethod.hasRequestParam()) {
                if (stubMethod.getMethod().equals(RequestMethod.GET)) {
                    builder.append("    params: " + formatRequesterParams(stubMethod) + ",\n");
                } else {
                    builder.append("    data: " + formatRequesterParams(stubMethod) + ",\n");
                }
            }
        }
        builder.append("    method: '" + stubMethod.getMethod().name().toLowerCase() + "'};\n");
        builder.append("  if(this.jwtToken != null) {config.headers['Authorization'] = 'Bearer ' + this.jwtToken; }\n");
        builder.append("  if(this.configFilter != null) {config = this.configFilter(config);}\n");
        builder.append("  return axios(config).then(response => {return response.data;});\n");
        builder.append("}\n");
        return builder.toString();
    }

    public String formatPathVariables(JsHttpStubMethod stubMethod) {
        return "{" + stubMethod.getParams().stream()
                .filter(param -> param.getPathVariableName() != null)
                .map(param -> {
                    return "\"" + param.getPathVariableName() + "\": " + param.getName();
                })
                .collect(Collectors.joining(", ")) + "}";
    }

    public String formatHttpHeaders(JsHttpStubMethod stubMethod) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        if (stubMethod.getRequestContentType() != null) {
            builder.append("\"Content-Type\": \"" + stubMethod.getRequestContentType() + "\"");
        }
        if (stubMethod.hasHttpHeader()) {
            builder.append(",").append(stubMethod.getParams().stream()
                    .filter(param -> param.getPathVariableName() != null)
                    .map(param -> {
                        return "\"" + param.getPathVariableName() + "\": " + param.getName();
                    })
                    .collect(Collectors.joining(", ")));
        }
        builder.append("}");
        return builder.toString();
    }

    public String formatRequesterParams(JsHttpStubMethod stubMethod) {
        return "{" + stubMethod.getParams().stream()
                .filter(param -> param.getPathVariableName() != null)
                .map(param -> {
                    return "\"" + param.getPathVariableName() + "\": " + param.getName();
                })
                .collect(Collectors.joining(", ")) + "}";
    }

}