/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
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
package org.thingsboard.server.transport.http.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.msg.tools.MaxPayloadSizeExceededException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class PayloadSizeFilter extends OncePerRequestFilter {

    private final Map<String, Long> limits = new LinkedHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public PayloadSizeFilter(String limitsConfiguration) {
        for (String limit : limitsConfiguration.split(";")) {
            try {
                String urlPathPattern = limit.split("=")[0];
                long maxPayloadSize = Long.parseLong(limit.split("=")[1]);
                limits.put(urlPathPattern, maxPayloadSize);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse size limits configuration: " + limitsConfiguration);
            }
        }
        log.info("Initialized payload size filter with configuration: {}" , limitsConfiguration);
    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        for (String url : limits.keySet()) {
            if (pathMatcher.match(url, request.getRequestURI())) {
                if (checkMaxPayloadSizeExceeded(request, response, limits.get(url))) {
                    return;
                }
                break;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean checkMaxPayloadSizeExceeded(HttpServletRequest request, HttpServletResponse response, long maxPayloadSize) throws IOException {
        if (request.getContentLength() > maxPayloadSize) {
            log.info("[{}] [{}] Payload size {} exceeds the limit of {} bytes", request.getRemoteAddr(), request.getRequestURL(), request.getContentLength(), maxPayloadSize);
            handleMaxPayloadSizeExceededException(response, new MaxPayloadSizeExceededException(maxPayloadSize));
            return true;
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private void handleMaxPayloadSizeExceededException(HttpServletResponse response, MaxPayloadSizeExceededException exception) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        JacksonUtil.writeValue(response.getWriter(), exception.getMessage());
    }
}
