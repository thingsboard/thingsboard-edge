/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.extensions.rest.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.thingsboard.server.extensions.api.component.Plugin;
import org.thingsboard.server.extensions.api.plugins.AbstractPlugin;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.rest.action.RestApiCallPluginAction;

import java.util.Base64;

@Plugin(name = "REST API Call Plugin", actions = {RestApiCallPluginAction.class},
        descriptor = "RestApiCallPluginDescriptor.json", configuration = RestApiCallPluginConfiguration.class)
@Slf4j
public class RestApiCallPlugin extends AbstractPlugin<RestApiCallPluginConfiguration> {

    private static final String BASIC_AUTH_METHOD = "BASIC_AUTH";
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String AUTHORIZATION_HEADER_FORMAT = "Basic %s";
    private static final String CREDENTIALS_TEMPLATE = "%s:%s";
    private static final String BASE_URL_TEMPLATE = "%s%s:%d%s";
    private RestApiCallMsgHandler handler;
    private String baseUrl;
    private HttpHeaders headers = new HttpHeaders();

    @Override
    public void init(RestApiCallPluginConfiguration configuration) {
        String host = configuration.getHost();
        host = host.trim();
        if (host.contains("://")) {
            host = host.substring(host.lastIndexOf('/') + 1, host.length());
        }

        this.baseUrl = String.format(
                BASE_URL_TEMPLATE,
                configuration.getProtocol(),
                host,
                configuration.getPort(),
                configuration.getBasePath());

        if (configuration.getAuthMethod().equals(BASIC_AUTH_METHOD)) {
            String userName = configuration.getUserName();
            String password = configuration.getPassword();
            String credentials = String.format(CREDENTIALS_TEMPLATE, userName, password);
            byte[] token = Base64.getEncoder().encode(credentials.getBytes());
            this.headers.add(AUTHORIZATION_HEADER_NAME, String.format(AUTHORIZATION_HEADER_FORMAT, new String(token)));
        }

        if (configuration.getHeaders() != null) {
            configuration.getHeaders().forEach(h -> {
                log.debug("Adding header to request object. Key = {}, Value = {}", h.getKey(), h.getValue());
                this.headers.add(h.getKey(), h.getValue());
            });
        }

        init();
    }

    private void init() {
        this.handler = new RestApiCallMsgHandler(baseUrl, headers);
    }

    @Override
    protected RuleMsgHandler getRuleMsgHandler() {
        return handler;
    }

    @Override
    public void resume(PluginContext ctx) {
        init();
    }

    @Override
    public void suspend(PluginContext ctx) {
        log.debug("Suspend method was called, but no impl provided!");
    }

    @Override
    public void stop(PluginContext ctx) {
        log.debug("Stop method was called, but no impl provided!");
    }
}
