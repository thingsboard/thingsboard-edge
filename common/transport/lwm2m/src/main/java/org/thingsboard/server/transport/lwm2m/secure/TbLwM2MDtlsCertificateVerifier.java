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
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.StaticCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredentials;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MCredentials;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbMainSecurityStore;

import javax.annotation.PostConstruct;
import javax.security.auth.x500.X500Principal;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import static org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer.CLIENT;

@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class TbLwM2MDtlsCertificateVerifier implements NewAdvancedCertificateVerifier {

    private final TbLwM2MDtlsSessionStore sessionStorage;
    private final LwM2MTransportServerConfig config;
    private final LwM2mCredentialsSecurityInfoValidator securityInfoValidator;
    private final TbMainSecurityStore securityStore;

    @SuppressWarnings("deprecation")
    private StaticCertificateVerifier staticCertificateVerifier;

    @Value("${transport.lwm2m.server.security.skip_validity_check_for_client_cert:false}")
    private boolean skipValidityCheckForClientCert;

    @Override
    public List<CertificateType> getSupportedCertificateType() {
        return Arrays.asList(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY);
    }

    @PostConstruct
    public void init() {
        try {
            /* by default trust all */
            X509Certificate[] trustedCertificates = new X509Certificate[0];
            if (config.getKeyStoreValue() != null) {
                X509Certificate rootCAX509Cert = (X509Certificate) config.getKeyStoreValue().getCertificate(config.getRootCertificateAlias());
                if (rootCAX509Cert != null) {
                    trustedCertificates = new X509Certificate[1];
                    trustedCertificates[0] = rootCAX509Cert;
                }
            }
            staticCertificateVerifier = new StaticCertificateVerifier(trustedCertificates);
        } catch (Exception e) {
            log.info("Failed to initialize the ");
        }
    }

    @Override
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName, Boolean clientUsage, boolean truncateCertificatePath, CertificateMessage message, DTLSSession session) {
        CertPath certChain = message.getCertificateChain();
        if (certChain == null) {
            //We trust all RPK on this layer, and use TbLwM2MAuthorizer
            PublicKey publicKey = message.getPublicKey();
            return new CertificateVerificationResult(cid, publicKey, null);
        } else {
            try {
                boolean x509CredentialsFound = false;
                CertPath certpath = message.getCertificateChain();
                X509Certificate[] chain = certpath.getCertificates().toArray(new X509Certificate[0]);
                for (X509Certificate cert : chain) {
                    try {
                        if (!skipValidityCheckForClientCert) {
                            cert.checkValidity();
                        }

                        String strCert = SslUtil.getCertificateString(cert);
                        String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
                        TbLwM2MSecurityInfo securityInfo;
                        try {
                            securityInfo = securityInfoValidator.getEndpointSecurityInfoByCredentialsId(sha3Hash, CLIENT);
                        } catch (LwM2MAuthException e) {
                            securityInfo = null;
                        }
                        ValidateDeviceCredentialsResponse msg = securityInfo != null ? securityInfo.getMsg() : null;
                        if (msg != null && org.thingsboard.server.common.data.StringUtils.isNotEmpty(msg.getCredentials())) {
                            LwM2MCredentials credentials = JacksonUtil.fromString(msg.getCredentials(), LwM2MCredentials.class);
                            if (!credentials.getClient().getSecurityConfigClientMode().equals(LwM2MSecurityMode.X509)) {
                                continue;
                            }
                            X509ClientCredentials config = (X509ClientCredentials) credentials.getClient();
                            String certBody = config.getCert();
                            String endpoint = config.getEndpoint();
                            if (strCert.equals(certBody)) {
                                x509CredentialsFound = true;
                                DeviceProfile deviceProfile = msg.getDeviceProfile();
                                if (msg.hasDeviceInfo() && deviceProfile != null) {
                                    sessionStorage.put(endpoint, new TbX509DtlsSessionInfo(cert.getSubjectX500Principal().getName(), msg));
                                    try {
                                        securityStore.putX509(securityInfo);
                                    } catch (NonUniqueSecurityInfoException e) {
                                        log.trace("Failed to add security info: {}", securityInfo, e);
                                    }
                                    break;
                                }
                            } else {
                                log.trace("[{}][{}] Certificate mismatch. Expected: {}, Actual: {}", endpoint, sha3Hash, strCert, certBody);
                            }
                        }
                    } catch (CertificateEncodingException |
                            CertificateExpiredException |
                            CertificateNotYetValidException e) {
                        log.error(e.getMessage(), e);
                    }
                }
                if (!x509CredentialsFound) {
                    if (staticCertificateVerifier != null) {
                        staticCertificateVerifier.verifyCertificate(message, session);
                    } else {
                        AlertMessage alert = new AlertMessage(AlertMessage.AlertLevel.FATAL, AlertMessage.AlertDescription.INTERNAL_ERROR,
                                session.getPeer());
                        throw new HandshakeException("x509 verification not enabled!", alert);
                    }
                }
                return new CertificateVerificationResult(cid, certpath, null);
            } catch (HandshakeException e) {
                log.trace("Certificate validation failed!", e);
                return new CertificateVerificationResult(cid, e, null);
            }
        }
    }

    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return CertPathUtil.toSubjects(null);
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {

    }
}
