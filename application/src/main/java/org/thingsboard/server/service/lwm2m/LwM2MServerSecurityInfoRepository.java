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
package org.thingsboard.server.service.lwm2m;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.util.Hex;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.lwm2m.ServerSecurityConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MSecureServerConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.KeySpec;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true') || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core'")
public class LwM2MServerSecurityInfoRepository {

    private final LwM2MTransportServerConfig serverConfig;
    private final LwM2MTransportBootstrapConfig bootstrapConfig;

    /**
     * @param securityMode
     * @param bootstrapServer
     * @return ServerSecurityConfig more value is default: Important - port, host, publicKey
     */
    public ServerSecurityConfig getServerSecurityInfo(SecurityMode securityMode, boolean bootstrapServer) {
        ServerSecurityConfig result = getServerSecurityConfig(bootstrapServer ? bootstrapConfig : serverConfig, securityMode);
        result.setBootstrapServerIs(bootstrapServer);
        return result;
    }

    private ServerSecurityConfig getServerSecurityConfig(LwM2MSecureServerConfig serverConfig, SecurityMode securityMode) {
        ServerSecurityConfig bsServ = new ServerSecurityConfig();
        bsServ.setServerId(serverConfig.getId());
        switch (securityMode) {
            case NO_SEC:
                bsServ.setHost(serverConfig.getHost());
                bsServ.setPort(serverConfig.getPort());
                bsServ.setServerPublicKey("");
                break;
            case PSK:
                bsServ.setHost(serverConfig.getSecureHost());
                bsServ.setPort(serverConfig.getSecurePort());
                bsServ.setServerPublicKey("");
                break;
            case RPK:
            case X509:
                bsServ.setHost(serverConfig.getSecureHost());
                bsServ.setPort(serverConfig.getSecurePort());
                bsServ.setServerPublicKey(getPublicKey(serverConfig.getCertificateAlias(), this.serverConfig.getPublicX(), this.serverConfig.getPublicY()));
                break;
            default:
                break;
        }
        return bsServ;
    }

    private String getPublicKey(String alias, String publicServerX, String publicServerY) {
        String publicKey = getServerPublicKeyX509(alias);
        return publicKey != null ? publicKey : getRPKPublicKey(publicServerX, publicServerY);
    }

    private String getServerPublicKeyX509(String alias) {
        try {
            X509Certificate serverCertificate = (X509Certificate) serverConfig.getKeyStoreValue().getCertificate(alias);
            return Hex.encodeHexString(serverCertificate.getEncoded());
        } catch (CertificateEncodingException | KeyStoreException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getRPKPublicKey(String publicServerX, String publicServerY) {
        try {
            /** Get Elliptic Curve Parameter spec for secp256r1 */
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);
            if (publicServerX != null && !publicServerX.isEmpty() && publicServerY != null && !publicServerY.isEmpty()) {
                /** Get point values */
                byte[] publicX = Hex.decodeHex(publicServerX.toCharArray());
                byte[] publicY = Hex.decodeHex(publicServerY.toCharArray());
                /** Create key specs */
                KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                        parameterSpec);
                /** Get keys */
                PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
                if (publicKey != null && publicKey.getEncoded().length > 0) {
                    return Hex.encodeHexString(publicKey.getEncoded());
                }
            }
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.error("[{}] Failed generate Server RPK for profile", e.getMessage());
            throw new RuntimeException(e);
        }
        return null;
    }
}

