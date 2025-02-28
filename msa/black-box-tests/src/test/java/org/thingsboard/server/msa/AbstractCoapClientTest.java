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
package org.thingsboard.server.msa;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.californium.elements.config.IntegerDefinition;
import org.eclipse.californium.elements.config.TcpConfig;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.msg.session.FeatureType;
@Slf4j
public abstract class AbstractCoapClientTest extends AbstractContainerTest{

    private static final String COAP_BASE_URL = "coap://localhost:5683/api/v1/";
    private static final long CLIENT_REQUEST_TIMEOUT = 60000L;


    private static final String COAP_CLIENT_TEST = "COAP_CLIENT_TEST.";
    private static final IntegerDefinition COAP_PORT_DEF = CoapConfig.COAP_PORT;

    private static final ModuleDefinitionsProvider MODULE_DEFINITIONS_PROVIDER = new ModuleDefinitionsProvider() {

        @Override
        public String getModule() {
            return COAP_CLIENT_TEST;
        }

        @Override
        public void applyDefinitions(Configuration config) {
            TcpConfig.register();
            config.set(COAP_PORT_DEF, 5683);
        }
    };

    protected CoapClient client;

    protected byte[] createCoapClientAndPublish(String deviceName) throws Exception {
        String provisionRequestMsg = createTestProvisionMessage(deviceName);
        Configuration.addDefaultModule(MODULE_DEFINITIONS_PROVIDER);
        String featureTokenUrl = COAP_BASE_URL + FeatureType.PROVISION.name().toLowerCase();
        client = new CoapClient(featureTokenUrl);
        try {
            return client.setTimeout(CLIENT_REQUEST_TIMEOUT)
                    .post(provisionRequestMsg.getBytes(), MediaTypeRegistry.APPLICATION_JSON)
                    .getPayload();
        } catch (NullPointerException e){
            log.error("createCoapClientAndPublish, deviceName [{}], provisionRequestMsg: [{}]", deviceName, provisionRequestMsg);
            return null;
        }
    }


    protected void disconnect() {
        if (client != null) {
            client.shutdown();
        }
    }

    private String createTestProvisionMessage(String deviceName) {
        ObjectNode provisionRequest = JacksonUtil.newObjectNode();
        provisionRequest.put("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.put("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        if (deviceName != null) {
            provisionRequest.put("deviceName", deviceName);
        }
        return provisionRequest.toString();
    }
}


