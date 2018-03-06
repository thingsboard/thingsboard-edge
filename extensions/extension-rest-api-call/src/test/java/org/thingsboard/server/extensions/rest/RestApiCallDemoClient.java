/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.extensions.rest;

import com.sun.net.httpserver.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.stream.Collectors;

public class RestApiCallDemoClient {

    private static final String DEMO_REST_BASIC_AUTH = "/demo-rest-basic-auth";
    private static final String DEMO_REST_NO_AUTH = "/demo-rest-no-auth";
    private static final String USERNAME = "demo";
    private static final String PASSWORD = "demo";
    private static final int HTTP_SERVER_PORT = 8888;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(HTTP_SERVER_PORT), 0);

        HttpContext secureContext = server.createContext(DEMO_REST_BASIC_AUTH, new RestDemoHandler());
        secureContext.setAuthenticator(new BasicAuthenticator("demo-auth") {
            @Override
            public boolean checkCredentials(String user, String pwd) {
                return user.equals(USERNAME) && pwd.equals(PASSWORD);
            }
        });

        server.createContext(DEMO_REST_NO_AUTH, new RestDemoHandler());
        server.setExecutor(null);
        System.out.println("[*] Waiting for messages.");
        server.start();
    }

    private static class RestDemoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestBody;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "utf-8"))) {
                requestBody = br.lines().collect(Collectors.joining(System.lineSeparator()));
            }
            System.out.println("[x] Received body: \n" + requestBody);

            String response = "Hello from demo client!";
            exchange.sendResponseHeaders(200, response.length());
            System.out.println("[x] Sending response: \n" + response);

            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}