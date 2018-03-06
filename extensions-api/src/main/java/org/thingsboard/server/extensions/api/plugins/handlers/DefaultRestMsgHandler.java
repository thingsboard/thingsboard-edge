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
package org.thingsboard.server.extensions.api.plugins.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.rest.PluginRestMsg;

import javax.servlet.ServletException;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class DefaultRestMsgHandler implements RestMsgHandler {

    protected final ObjectMapper jsonMapper = new ObjectMapper();

    @Override
    public void process(PluginContext ctx, PluginRestMsg msg) {
        try {
            log.debug("[{}] Processing REST msg: {}", ctx.getPluginId(), msg);
            HttpMethod method = msg.getRequest().getMethod();
            switch (method) {
                case GET:
                    handleHttpGetRequest(ctx, msg);
                    break;
                case POST:
                    handleHttpPostRequest(ctx, msg);
                    break;
                case DELETE:
                    handleHttpDeleteRequest(ctx, msg);
                    break;
                default:
                    msg.getResponseHolder().setErrorResult(new HttpRequestMethodNotSupportedException(method.name()));
            }
            log.debug("[{}] Processed REST msg.", ctx.getPluginId());
        } catch (Exception e) {
            log.warn("[{}] Exception during REST msg processing: {}", ctx.getPluginId(), e.getMessage(), e);
            msg.getResponseHolder().setErrorResult(e);
        }
    }

    protected void handleHttpGetRequest(PluginContext ctx, PluginRestMsg msg) throws ServletException {
        msg.getResponseHolder().setErrorResult(new HttpRequestMethodNotSupportedException(HttpMethod.GET.name()));
    }

    protected void handleHttpPostRequest(PluginContext ctx, PluginRestMsg msg) throws ServletException {
        msg.getResponseHolder().setErrorResult(new HttpRequestMethodNotSupportedException(HttpMethod.POST.name()));
    }

    protected void handleHttpDeleteRequest(PluginContext ctx, PluginRestMsg msg) throws ServletException {
        msg.getResponseHolder().setErrorResult(new HttpRequestMethodNotSupportedException(HttpMethod.DELETE.name()));
    }

}
