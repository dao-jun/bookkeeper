/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.bookkeeper.http.vertx;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.bookkeeper.http.HttpServer;
import org.apache.bookkeeper.http.service.HttpEndpointService;
import org.apache.bookkeeper.http.service.HttpServiceRequest;
import org.apache.bookkeeper.http.service.HttpServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Http Handler for Vertx based Http Server.
 */
public abstract class VertxAbstractHandler implements Handler<RoutingContext> {

    private static final Logger LOG = LoggerFactory.getLogger(VertxAbstractHandler.class);

    /**
     * Process the request using the given httpEndpointService.
     */
    @SuppressFBWarnings("DCN_NULLPOINTER_EXCEPTION")
    void processRequest(HttpEndpointService httpEndpointService, RoutingContext context) {
        HttpServerRequest httpRequest = context.request();
        HttpServerResponse httpResponse = context.response();
        HttpServiceRequest request = new HttpServiceRequest()
            .setMethod(convertMethod(httpRequest))
            .setParams(convertParams(httpRequest))
            .setBody(context.body().asString());
        HttpServiceResponse response;
        try {
            response = httpEndpointService.handle(request);
        } catch (NullPointerException | IllegalArgumentException e) {
            LOG.error("Vertx handler failed to process request {}, cause {}", request.toString(), e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            LOG.error("Exception in vertx handler", e);
            throw new RuntimeException(e);
        }
        httpResponse.setStatusCode(response.getStatusCode());
        if (response.getContentType() != null) {
            httpResponse.putHeader(HttpHeaderNames.CONTENT_TYPE, response.getContentType());
        }
        httpResponse.end(response.getBody());
    }

    /**
     * Convert http request parameters to a map.
     */
    @SuppressWarnings("unchecked")
    Map<String, String> convertParams(HttpServerRequest request) {
        Map<String, String> map = new HashMap<>();
        Iterator<Map.Entry<String, String>> iterator = request.params().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    /**
     * Convert http request method to the method that
     * can be recognized by HttpServer.
     */
    HttpServer.Method convertMethod(HttpServerRequest request) {
        HttpMethod method = request.method();
        if (HttpMethod.POST.equals(method)) {
            return HttpServer.Method.POST;
        } else if (HttpMethod.DELETE.equals(method)) {
            return HttpServer.Method.DELETE;
        } else if (HttpMethod.PUT.equals(method)) {
            return HttpServer.Method.PUT;
        }
        return HttpServer.Method.GET;
    }
}
