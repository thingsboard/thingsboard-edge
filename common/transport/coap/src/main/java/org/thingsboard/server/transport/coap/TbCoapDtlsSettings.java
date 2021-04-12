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
package org.thingsboard.server.transport.coap;

import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@ConditionalOnProperty(prefix = "transport.coap.dtls", value = "enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnExpression("'${transport.type:null}'=='null' || ('${transport.type}'=='local' && '${transport.coap.enabled}'=='true')")
@Component
public class TbCoapDtlsSettings {

    @Value("${transport.coap.bind_address}")
    private String host;

    @Value("${transport.coap.bind_port}")
    private Integer port;

    @Value("${transport.coap.dtls.mode}")
    private String mode;

    @Value("${transport.coap.dtls.key_store}")
    private String keyStoreFile;

    @Value("${transport.coap.dtls.key_store_password}")
    private String keyStorePassword;

    @Value("${transport.coap.dtls.key_password}")
    private String keyPassword;

    @Value("${transport.coap.dtls.key_alias}")
    private String keyAlias;

    @Value("${transport.coap.dtls.skip_validity_check_for_client_cert}")
    private boolean skipValidityCheckForClientCert;

    @Value("${transport.coap.dtls.x509.dtls_session_inactivity_timeout}")
    private long dtlsSessionInactivityTimeout;

    @Value("${transport.coap.dtls.x509.dtls_session_report_timeout}")
    private long dtlsSessionReportTimeout;

    @Autowired
    private TransportService transportService;

    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    public DtlsConnectorConfig dtlsConnectorConfig() throws UnknownHostException {
        Optional<SecurityMode> securityModeOpt = SecurityMode.parse(mode);
        if (securityModeOpt.isEmpty()) {
            log.warn("Incorrect configuration of securityMode {}", mode);
            throw new RuntimeException("Failed to parse mode property: " + mode + "!");
        } else {
            DtlsConnectorConfig.Builder configBuilder = new DtlsConnectorConfig.Builder();
            configBuilder.setAddress(getInetSocketAddress());
            String keyStoreFilePath = Resources.getResource(keyStoreFile).getPath();
            SslContextUtil.Credentials serverCredentials = loadServerCredentials(keyStoreFilePath);
            SecurityMode securityMode = securityModeOpt.get();
            if (securityMode.equals(SecurityMode.NO_AUTH)) {
                configBuilder.setClientAuthenticationRequired(false);
                configBuilder.setServerOnly(true);
            } else {
                configBuilder.setAdvancedCertificateVerifier(
                        new TbCoapDtlsCertificateVerifier(
                                transportService,
                                serviceInfoProvider,
                                dtlsSessionInactivityTimeout,
                                dtlsSessionReportTimeout,
                                skipValidityCheckForClientCert
                        )
                );
            }
            configBuilder.setIdentity(serverCredentials.getPrivateKey(), serverCredentials.getCertificateChain(),
                    Collections.singletonList(CertificateType.X_509));
            return configBuilder.build();
        }
    }

    private SslContextUtil.Credentials loadServerCredentials(String keyStoreFilePath) {
        try {
            return SslContextUtil.loadCredentials(keyStoreFilePath, keyAlias, keyStorePassword.toCharArray(),
                    keyPassword.toCharArray());
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to load serverCredentials due to: ", e);
        }
    }

    private void loadTrustedCertificates(DtlsConnectorConfig.Builder config, String keyStoreFilePath) {
        StaticNewAdvancedCertificateVerifier.Builder trustBuilder = StaticNewAdvancedCertificateVerifier.builder();
        try {
            Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates(
                    keyStoreFilePath, keyAlias,
                    keyStorePassword.toCharArray());
            trustBuilder.setTrustedCertificates(trustedCertificates);
            if (trustBuilder.hasTrusts()) {
                config.setAdvancedCertificateVerifier(trustBuilder.build());
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to load trusted certificates due to: ", e);
        }
    }

    private InetSocketAddress getInetSocketAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(host);
        return new InetSocketAddress(addr, port);
    }

    private enum SecurityMode {
        X509,
        NO_AUTH;

        static Optional<SecurityMode> parse(String name) {
            SecurityMode mode = null;
            if (name != null) {
                for (SecurityMode securityMode : SecurityMode.values()) {
                    if (securityMode.name().equalsIgnoreCase(name)) {
                        mode = securityMode;
                        break;
                    }
                }
            }
            return Optional.ofNullable(mode);
        }

    }

}