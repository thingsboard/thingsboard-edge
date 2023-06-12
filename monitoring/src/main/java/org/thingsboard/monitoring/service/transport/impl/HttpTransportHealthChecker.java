/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.monitoring.service.transport.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.monitoring.config.transport.HttpTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;

import java.time.Duration;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class HttpTransportHealthChecker extends TransportHealthChecker<HttpTransportMonitoringConfig> {

    private RestTemplate restTemplate;

    protected HttpTransportHealthChecker(HttpTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (restTemplate == null) {
            restTemplate = new RestTemplateBuilder()
                    .setConnectTimeout(Duration.ofMillis(config.getRequestTimeoutMs()))
                    .setReadTimeout(Duration.ofMillis(config.getRequestTimeoutMs()))
                    .build();
            log.debug("Initialized HTTP client");
        }
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        String accessToken = target.getDevice().getCredentials().getCredentialsId();
        restTemplate.postForObject(target.getBaseUrl() + "/api/v1/" + accessToken + "/telemetry", payload, String.class);
    }

    @Override
    protected void destroyClient() throws Exception {}

    @Override
    protected TransportType getTransportType() {
        return TransportType.HTTP;
    }

}
