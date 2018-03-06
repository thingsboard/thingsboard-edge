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
package org.thingsboard.server.extensions.api.plugins.rest;

import lombok.Data;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.thingsboard.server.extensions.api.plugins.PluginConstants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Function;

@Data
public class RestRequest {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final RequestEntity<byte[]> requestEntity;
    private final HttpServletRequest request;

    public HttpMethod getMethod() {
        return requestEntity.getMethod();
    }

    public String getRequestBody() {
        return new String(requestEntity.getBody(), UTF8);
    }

    public String[] getPathParams() {
        String requestUrl = request.getRequestURL().toString();
        int index = requestUrl.indexOf(PluginConstants.PLUGIN_URL_PREFIX);
        String[] pathParams = requestUrl.substring(index + PluginConstants.PLUGIN_URL_PREFIX.length()).split("/");
        String[] result = new String[pathParams.length - 2];
        System.arraycopy(pathParams, 2, result, 0, result.length);
        return result;
    }

    public String getParameter(String paramName) throws ServletException {
        return getParameter(paramName, null);
    }

    public String getParameter(String paramName, String defaultValue) throws ServletException {
        String paramValue = request.getParameter(paramName);
        if (StringUtils.isEmpty(paramValue)) {
            if (defaultValue == null) {
                throw new MissingServletRequestParameterException(paramName, "String");
            } else {
                return defaultValue;
            }
        } else {
            return paramValue;
        }
    }

    public Optional<Long> getLongParamValue(String paramName) {
        return getParamValue(paramName, s -> Long.valueOf(s));
    }

    public Optional<Integer> getIntParamValue(String paramName) {
        return getParamValue(paramName, s -> Integer.valueOf(s));
    }

    public <T> Optional<T> getParamValue(String paramName, Function<String, T> function) {
        String paramValue = request.getParameter(paramName);
        if (paramValue != null) {
            return Optional.of(function.apply(paramValue));
        } else {
            return Optional.empty();
        }
    }
}
