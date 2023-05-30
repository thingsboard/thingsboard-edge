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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.thingsboard.monitoring.client.Lwm2mClient;
import org.thingsboard.monitoring.config.transport.Lwm2mTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class Lwm2mTransportHealthChecker extends TransportHealthChecker<Lwm2mTransportMonitoringConfig> {

    private Lwm2mClient lwm2mClient;

    protected Lwm2mTransportHealthChecker(Lwm2mTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initClient() throws Exception {
        if (lwm2mClient == null || lwm2mClient.getLeshanClient() == null || lwm2mClient.isDestroyed()) {
            String endpoint = target.getDevice().getCredentials().getCredentialsId();
            lwm2mClient = new Lwm2mClient(target.getBaseUrl(), endpoint);
            lwm2mClient.initClient();
            log.debug("Initialized LwM2M client for endpoint '{}'", endpoint);
        }
    }

    @Override
    protected void sendTestPayload(String payload) throws Exception {
        lwm2mClient.send(payload, 0);
    }

    @Override
    protected String createTestPayload(String testValue) {
        return testValue;
    }

    @Override
    protected void destroyClient() throws Exception {
        if (lwm2mClient != null) {
            lwm2mClient.destroy();
            lwm2mClient = null;
        }
    }

    @Override
    protected TransportType getTransportType() {
        return TransportType.LWM2M;
    }

}
