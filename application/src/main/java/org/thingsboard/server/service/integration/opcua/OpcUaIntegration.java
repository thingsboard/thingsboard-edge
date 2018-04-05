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
package org.thingsboard.server.service.integration.opcua;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.thingsboard.server.service.integration.AbstractIntegration;
import org.thingsboard.server.service.integration.IntegrationContext;
import org.thingsboard.server.service.integration.TbIntegrationInitParams;
import org.thingsboard.server.service.integration.mqtt.basic.BasicMqttIntegration;

import java.util.Arrays;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * Created by Valerii Sosliuk on 3/17/2018.
 */
@Slf4j
public class OpcUaIntegration extends AbstractIntegration<OpcUaIntegrationMsg> {

    private OpcUaClient client;

    @Override
    public void init(TbIntegrationInitParams params) throws Exception {
        super.init(params);
    }

    @Override
    public void process(IntegrationContext context, OpcUaIntegrationMsg msg) {

    }

    private void initClient(OpcUaServerConfiguration configuration) {
        try {

            log.info("Initializing OPC-UA server connection to [{}:{}]!", configuration.getHost(), configuration.getPort());
            CertificateInfo certificate = ConfigurationTools.loadCertificate(configuration.getKeystore());

            SecurityPolicy securityPolicy = SecurityPolicy.valueOf(configuration.getSecurity());
            IdentityProvider identityProvider = configuration.getIdentity().toProvider();

            EndpointDescription[] endpoints = UaTcpStackClient.getEndpoints("opc.tcp://" + configuration.getHost() + ":" + configuration.getPort() + "/").get();

            EndpointDescription endpoint = Arrays.stream(endpoints)
                    .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getSecurityPolicyUri()))
                    .findFirst().orElseThrow(() -> new Exception("no desired endpoints returned"));

            OpcUaClientConfig config = OpcUaClientConfig.builder()
                    .setApplicationName(LocalizedText.english(configuration.getApplicationName()))
                    .setApplicationUri(configuration.getApplicationUri())
                    .setCertificate(certificate.getCertificate())
                    .setKeyPair(certificate.getKeyPair())
                    .setEndpoint(endpoint)
                    .setIdentityProvider(identityProvider)
                    .setRequestTimeout(uint(configuration.getTimeoutInMillis()))
                    .build();

            client = new OpcUaClient(config);
            client.connect().get();
        } catch (Exception e) {
            log.error("Falied to connect to OPC-UA server. Reason: {}", e.getMessage(), e);
        }

    }
}
