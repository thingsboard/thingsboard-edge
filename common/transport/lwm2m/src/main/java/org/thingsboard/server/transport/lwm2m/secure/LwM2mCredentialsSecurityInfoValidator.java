/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.secure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredentials;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.RPKClientCredentials;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.eclipse.leshan.core.SecurityMode.NO_SEC;
import static org.eclipse.leshan.core.SecurityMode.PSK;
import static org.eclipse.leshan.core.SecurityMode.RPK;
import static org.eclipse.leshan.core.SecurityMode.X509;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class LwM2mCredentialsSecurityInfoValidator {

    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;

    public EndpointSecurityInfo getEndpointSecurityInfo(String endpoint, LwM2mTransportUtil.LwM2mTypeServer keyValue) {
        CountDownLatch latch = new CountDownLatch(1);
        final EndpointSecurityInfo[] resultSecurityStore = new EndpointSecurityInfo[1];
        context.getTransportService().process(ValidateDeviceLwM2MCredentialsRequestMsg.newBuilder().setCredentialsId(endpoint).build(),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                        String credentialsBody = msg.getCredentials();
                        resultSecurityStore[0] = createSecurityInfo(endpoint, credentialsBody, keyValue);
                        resultSecurityStore[0].setMsg(msg);
                        resultSecurityStore[0].setDeviceProfile(msg.getDeviceProfile());
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.trace("[{}] [{}] Failed to process credentials ", endpoint, e);
                        resultSecurityStore[0] = createSecurityInfo(endpoint, null, null);
                        latch.countDown();
                    }
                });
        try {
            latch.await(config.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Failed to await credentials!", e);
        }
        return resultSecurityStore[0];
    }

    /**
     * Create new SecurityInfo
     * @param endpoint -
     * @param jsonStr -
     * @param keyValue -
     * @return SecurityInfo
     */
    private EndpointSecurityInfo createSecurityInfo(String endpoint, String jsonStr, LwM2mTransportUtil.LwM2mTypeServer keyValue) {
        EndpointSecurityInfo result = new EndpointSecurityInfo();
        LwM2MCredentials credentials = JacksonUtil.fromString(jsonStr, LwM2MCredentials.class);
        if (credentials != null) {
            if (keyValue.equals(LwM2mTransportUtil.LwM2mTypeServer.BOOTSTRAP)) {
                result.setBootstrapCredentialConfig(credentials.getBootstrap());
                if (LwM2MSecurityMode.PSK.equals(credentials.getClient().getSecurityConfigClientMode())) {
                    PSKClientCredentials pskClientConfig = (PSKClientCredentials) credentials.getClient();
                    endpoint = StringUtils.isNotEmpty(pskClientConfig.getEndpoint()) ? pskClientConfig.getEndpoint() : endpoint;
                }
                result.setEndpoint(endpoint);
                result.setSecurityMode(credentials.getBootstrap().getBootstrapServer().getSecurityMode());
            } else {
                switch (credentials.getClient().getSecurityConfigClientMode()) {
                    case NO_SEC:
                        createClientSecurityInfoNoSec(result);
                        break;
                    case PSK:
                        createClientSecurityInfoPSK(result, endpoint, credentials.getClient());
                        break;
                    case RPK:
                        createClientSecurityInfoRPK(result, endpoint, credentials.getClient());
                        break;
                    case X509:
                        createClientSecurityInfoX509(result, endpoint, credentials.getClient());
                        break;
                    default:
                        break;
                }
            }
        }
        return result;
    }

    private void createClientSecurityInfoNoSec(EndpointSecurityInfo result) {
        result.setSecurityInfo(null);
        result.setSecurityMode(NO_SEC);
    }

    private void createClientSecurityInfoPSK(EndpointSecurityInfo result, String endpoint, LwM2MClientCredentials clientCredentialsConfig) {
        PSKClientCredentials pskConfig = (PSKClientCredentials) clientCredentialsConfig;
        if (StringUtils.isNotEmpty(pskConfig.getIdentity())) {
            try {
                if (pskConfig.getKey() != null && pskConfig.getKey().length > 0) {
                    endpoint = StringUtils.isNotEmpty(pskConfig.getEndpoint()) ? pskConfig.getEndpoint() : endpoint;
                    if (endpoint != null && !endpoint.isEmpty()) {
                        result.setSecurityInfo(SecurityInfo.newPreSharedKeyInfo(endpoint, pskConfig.getIdentity(), pskConfig.getKey()));
                        result.setSecurityMode(PSK);
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Missing PSK key: " + e.getMessage());
            }
        } else {
            log.error("Missing PSK identity");
        }
    }

    private void createClientSecurityInfoRPK(EndpointSecurityInfo result, String endpoint, LwM2MClientCredentials clientCredentialsConfig) {
        RPKClientCredentials rpkConfig = (RPKClientCredentials) clientCredentialsConfig;
        try {
            if (rpkConfig.getKey() != null) {
                PublicKey key = SecurityUtil.publicKey.decode(rpkConfig.getKey());
                result.setSecurityInfo(SecurityInfo.newRawPublicKeyInfo(endpoint, key));
                result.setSecurityMode(RPK);
            } else {
                log.error("Missing RPK key");
            }
        } catch (IllegalArgumentException | IOException | GeneralSecurityException e) {
            log.error("RPK: Invalid security info content: " + e.getMessage());
        }
    }

    private void createClientSecurityInfoX509(EndpointSecurityInfo result, String endpoint, LwM2MClientCredentials clientCredentialsConfig) {
        result.setSecurityInfo(SecurityInfo.newX509CertInfo(endpoint));
        result.setSecurityMode(X509);
    }
}
