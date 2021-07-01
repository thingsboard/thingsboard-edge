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
package org.thingsboard.server.transport.lwm2m.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServerBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MInMemoryBootstrapConfigStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2mDefaultBootstrapSessionManager;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.DefaultLwM2mTransportService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mNetworkConfig.getCoapConfig;

@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' && '${transport.lwm2m.bootstrap.enable:false}'=='true') || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled:false}'=='true'&& '${transport.lwm2m.bootstrap.enable:false}'=='true')")
@RequiredArgsConstructor
public class LwM2MTransportBootstrapService {
    private boolean pskMode = false;

    private final LwM2MTransportServerConfig serverConfig;
    private final LwM2MTransportBootstrapConfig bootstrapConfig;
    private final LwM2MBootstrapSecurityStore lwM2MBootstrapSecurityStore;
    private final LwM2MInMemoryBootstrapConfigStore lwM2MInMemoryBootstrapConfigStore;

    private LeshanBootstrapServer server;

    @PostConstruct
    public void init() {
        log.info("Starting LwM2M transport bootstrap server...");
        this.server = getLhBootstrapServer();
        this.server.start();
        log.info("Started LwM2M transport bootstrap server.");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping LwM2M transport bootstrap server!");
        server.destroy();
        log.info("LwM2M transport bootstrap server stopped!");
    }

    public LeshanBootstrapServer getLhBootstrapServer() {
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();
        builder.setLocalAddress(bootstrapConfig.getHost(), bootstrapConfig.getPort());
        builder.setLocalSecureAddress(bootstrapConfig.getSecureHost(), bootstrapConfig.getSecurePort());

        /* Create CoAP Config */
        builder.setCoapConfig(getCoapConfig(bootstrapConfig.getPort(), bootstrapConfig.getSecurePort(), serverConfig));

        /* Define model provider (Create Models )*/

        /*  Create credentials */
        this.setServerWithCredentials(builder);

//        /** Set securityStore with new ConfigStore */
//        builder.setConfigStore(lwM2MInMemoryBootstrapConfigStore);

        /* SecurityStore */
        builder.setSecurityStore(lwM2MBootstrapSecurityStore);


        /* Create and Set DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedSupportedGroupsOnly(serverConfig.isRecommendedSupportedGroups());
        dtlsConfig.setRecommendedCipherSuitesOnly(serverConfig.isRecommendedCiphers());
        dtlsConfig.setSupportedCipherSuites(this.pskMode ? DefaultLwM2mTransportService.PSK_CIPHER_SUITES : DefaultLwM2mTransportService.RPK_OR_X509_CIPHER_SUITES);

        /* Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        BootstrapSessionManager sessionManager = new LwM2mDefaultBootstrapSessionManager(lwM2MBootstrapSecurityStore);
        builder.setSessionManager(sessionManager);

        /* Create BootstrapServer */
        return builder.build();
    }

    private void setServerWithCredentials(LeshanBootstrapServerBuilder builder) {
        try {
            if (serverConfig.getKeyStoreValue() != null) {
                KeyStore keyStoreServer = serverConfig.getKeyStoreValue();
                if (this.setBuilderX509(builder)) {
                    X509Certificate rootCAX509Cert = (X509Certificate) keyStoreServer.getCertificate(serverConfig.getRootCertificateAlias());
                    if (rootCAX509Cert != null) {
                        X509Certificate[] trustedCertificates = new X509Certificate[1];
                        trustedCertificates[0] = rootCAX509Cert;
                        builder.setTrustedCertificates(trustedCertificates);
                    } else {
                        /* by default trust all */
                        builder.setTrustedCertificates(new X509Certificate[0]);
                    }
                }
            } else {
                /* by default trust all */
                builder.setTrustedCertificates(new X509Certificate[0]);
                log.info("Unable to load X509 files for BootStrapServer");
                this.pskMode = true;
            }
        } catch (KeyStoreException ex) {
            log.error("[{}] Unable to load X509 files server", ex.getMessage());
        }
    }

    private boolean setBuilderX509(LeshanBootstrapServerBuilder builder) {
        try {
            X509Certificate serverCertificate = (X509Certificate) serverConfig.getKeyStoreValue().getCertificate(this.bootstrapConfig.getCertificateAlias());
            PrivateKey privateKey = (PrivateKey) serverConfig.getKeyStoreValue().getKey(this.bootstrapConfig.getCertificateAlias(), serverConfig.getCertificatePassword() == null ? null : serverConfig.getCertificatePassword().toCharArray());
            PublicKey publicKey = serverCertificate.getPublicKey();
            if (privateKey != null && privateKey.getEncoded().length > 0 && publicKey != null && publicKey.getEncoded().length > 0) {
                builder.setPublicKey(serverCertificate.getPublicKey());
                builder.setPrivateKey(privateKey);
                builder.setCertificateChain(new X509Certificate[]{serverCertificate});
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            log.error("[{}] Unable to load KeyStore  files server", ex.getMessage());
            return false;
        }
    }

}
