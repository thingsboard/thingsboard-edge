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
package org.thingsboard.monitoring.service.integration.impl;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.thingsboard.monitoring.config.integration.CoapIntegrationMonitoringConfig;
import org.thingsboard.monitoring.config.integration.IntegrationMonitoringTarget;
import org.thingsboard.monitoring.config.integration.IntegrationType;
import org.thingsboard.monitoring.service.integration.IntegrationHealthChecker;

import java.io.IOException;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class CoapIntegrationHealthChecker extends IntegrationHealthChecker<CoapIntegrationMonitoringConfig> {

    private CoapClient coapClient;

    public CoapIntegrationHealthChecker(CoapIntegrationMonitoringConfig config, IntegrationMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (coapClient == null) {
            String uri = target.getIntegration().getConfiguration().get("clientConfiguration").get("coapEndpoint").asText();
            coapClient = new CoapClient(uri);
            coapClient.setTimeout((long) config.getRequestTimeoutMs());
            log.debug("Initialized CoAP client for URI {}", uri);
        }
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        CoapResponse response = coapClient.post(payload, MediaTypeRegistry.APPLICATION_JSON);
        CoAP.ResponseCode code = response.getCode();
        if (code.codeClass != CoAP.CodeClass.SUCCESS_RESPONSE.value) {
            throw new IOException("COAP client didn't receive success response from server");
        }
    }

    @Override
    protected void destroyClient() throws Exception {
        if (coapClient != null) {
            coapClient.shutdown();
            coapClient = null;
            log.info("Disconnected CoAP client");
        }
    }

    @Override
    protected IntegrationType getIntegrationType() {
        return IntegrationType.COAP;
    }

}
