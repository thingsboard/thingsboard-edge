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
package org.thingsboard.server.service.lwm2m;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MServerSecurityConfigDefault;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.transport.lwm2m.config.LwM2MSecureServerConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core') && '${transport.lwm2m.enabled:false}'=='true'")
public class LwM2MServiceImpl implements LwM2MService {

    private final LwM2MTransportServerConfig serverConfig;
    private final Optional<LwM2MTransportBootstrapConfig> bootstrapConfig;

    @Override
    public LwM2MServerSecurityConfigDefault getServerSecurityInfo(boolean bootstrapServer) {
        LwM2MSecureServerConfig bsServerConfig = bootstrapServer ? bootstrapConfig.orElse(null) : serverConfig;
        if (bsServerConfig!= null) {
            LwM2MServerSecurityConfigDefault result = getServerSecurityConfig(bsServerConfig);
            result.setBootstrapServerIs(bootstrapServer);
            return result;
        }
        else {
            return  null;
        }
    }

    private LwM2MServerSecurityConfigDefault getServerSecurityConfig(LwM2MSecureServerConfig bsServerConfig) {
        LwM2MServerSecurityConfigDefault bsServ = new LwM2MServerSecurityConfigDefault();
        bsServ.setShortServerId(bsServerConfig.getId());
        bsServ.setHost(bsServerConfig.getHost());
        bsServ.setPort(bsServerConfig.getPort());
        bsServ.setSecurityHost(bsServerConfig.getSecureHost());
        bsServ.setSecurityPort(bsServerConfig.getSecurePort());
        byte[] publicKeyBase64 = getPublicKey(bsServerConfig);
        if (publicKeyBase64 == null) {
            bsServ.setServerPublicKey("");
        } else {
            bsServ.setServerPublicKey(Base64.encodeBase64String(publicKeyBase64));
        }
        byte[] certificateBase64 = getCertificate(bsServerConfig);
        if (certificateBase64 == null) {
            bsServ.setServerCertificate("");
        } else {
            bsServ.setServerCertificate(Base64.encodeBase64String(certificateBase64));
        }
        return bsServ;
    }

    private byte[] getPublicKey(LwM2MSecureServerConfig config) {
        try {
            SslCredentials sslCredentials = config.getSslCredentials();
            if (sslCredentials != null) {
                return sslCredentials.getPublicKey().getEncoded();
            }
        } catch (Exception e) {
            log.trace("Failed to fetch public key from key store!", e);
        }
        return null;
    }

    private byte[] getCertificate(LwM2MSecureServerConfig config) {
        try {
            SslCredentials sslCredentials = config.getSslCredentials();
            if (sslCredentials != null) {
                return sslCredentials.getCertificateChain()[0].getEncoded();
            }
        } catch (Exception e) {
            log.trace("Failed to fetch certificate from key store!", e);
        }
        return null;
    }
}

