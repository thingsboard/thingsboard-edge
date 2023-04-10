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
package org.thingsboard.server.transport.lwm2m.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapSessionManager;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServerBuilder;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.queue.util.TbLwM2mBootstrapTransportComponent;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2mDefaultBootstrapSessionManager;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.TbLwM2MDtlsBootstrapCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MInMemoryBootstrapConfigStore;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.security.cert.X509Certificate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CURVES_ONLY;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_ROLE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole.SERVER_ONLY;
import static org.thingsboard.server.transport.lwm2m.server.DefaultLwM2mTransportService.PSK_CIPHER_SUITES;
import static org.thingsboard.server.transport.lwm2m.server.DefaultLwM2mTransportService.RPK_OR_X509_CIPHER_SUITES;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MNetworkConfig.getCoapConfig;

@Slf4j
@Component
@TbLwM2mBootstrapTransportComponent
@RequiredArgsConstructor
public class LwM2MTransportBootstrapService {

    private final LwM2MTransportServerConfig serverConfig;
    private final LwM2MTransportBootstrapConfig bootstrapConfig;
    private final LwM2MBootstrapSecurityStore lwM2MBootstrapSecurityStore;
    private final LwM2MInMemoryBootstrapConfigStore lwM2MInMemoryBootstrapConfigStore;
    private final TransportService transportService;
    private final TbLwM2MDtlsBootstrapCertificateVerifier certificateVerifier;
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
        try {
            log.info("Stopping LwM2M transport bootstrap server!");
            server.destroy();
            log.info("LwM2M transport bootstrap server stopped!");
        } catch (Exception e) {
            log.error("Failed to gracefully stop the LwM2M transport bootstrap server!", e);
        }
    }

    public LeshanBootstrapServer getLhBootstrapServer() {
        LeshanBootstrapServerBuilder builder = new LeshanBootstrapServerBuilder();
        builder.setLocalAddress(bootstrapConfig.getHost(), bootstrapConfig.getPort());
        builder.setLocalSecureAddress(bootstrapConfig.getSecureHost(), bootstrapConfig.getSecurePort());

        /* Create CoAP Config */
        builder.setCoapConfig(getCoapConfig(bootstrapConfig.getPort(), bootstrapConfig.getSecurePort(), serverConfig));


        /* Create and Set DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder(getCoapConfig(bootstrapConfig.getPort(), bootstrapConfig.getSecurePort(), serverConfig));

        dtlsConfig.set(DTLS_RECOMMENDED_CURVES_ONLY, serverConfig.isRecommendedSupportedGroups());
        dtlsConfig.set(DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, serverConfig.isRecommendedCiphers());
        dtlsConfig.set(DTLS_RETRANSMISSION_TIMEOUT, serverConfig.getDtlsRetransmissionTimeout(), MILLISECONDS);
        dtlsConfig.set(DTLS_ROLE, SERVER_ONLY);
        setServerWithCredentials(builder, dtlsConfig);

        /* Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        /* Set securityStore with new ConfigStore */
        builder.setConfigStore(lwM2MInMemoryBootstrapConfigStore);

        /* SecurityStore */
        builder.setSecurityStore(lwM2MBootstrapSecurityStore);


        BootstrapSessionManager sessionManager = new LwM2mDefaultBootstrapSessionManager(lwM2MBootstrapSecurityStore, lwM2MInMemoryBootstrapConfigStore, transportService);
        builder.setSessionManager(sessionManager);

        /* Create BootstrapServer */
        return builder.build();
    }

    private void setServerWithCredentials(LeshanBootstrapServerBuilder builder, DtlsConnectorConfig.Builder dtlsConfig) {
        if (this.bootstrapConfig.getSslCredentials() != null) {
            SslCredentials sslCredentials = this.bootstrapConfig.getSslCredentials();
            builder.setPublicKey(sslCredentials.getPublicKey());
            builder.setPrivateKey(sslCredentials.getPrivateKey());
            builder.setCertificateChain(sslCredentials.getCertificateChain());
            dtlsConfig.setAdvancedCertificateVerifier(certificateVerifier);
            dtlsConfig.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, RPK_OR_X509_CIPHER_SUITES);
        } else {
            /* by default trust all */
            builder.setTrustedCertificates(new X509Certificate[0]);
            log.info("Unable to load X509 files for BootStrap Server");
            dtlsConfig.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, PSK_CIPHER_SUITES);
        }
    }
}
