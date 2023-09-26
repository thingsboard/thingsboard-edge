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
package org.thingsboard.server.coapserver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import javax.security.auth.x500.X500Principal;
import java.net.InetSocketAddress;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class TbCoapDtlsCertificateVerifier implements NewAdvancedCertificateVerifier {

    private final TbCoapDtlsSessionInMemoryStorage tbCoapDtlsSessionInMemoryStorage;

    private TransportService transportService;
    private TbServiceInfoProvider serviceInfoProvider;
    private boolean skipValidityCheckForClientCert;

    public TbCoapDtlsCertificateVerifier(TransportService transportService, TbServiceInfoProvider serviceInfoProvider, long dtlsSessionInactivityTimeout, long dtlsSessionReportTimeout, boolean skipValidityCheckForClientCert) {
        this.transportService = transportService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.skipValidityCheckForClientCert = skipValidityCheckForClientCert;
        this.tbCoapDtlsSessionInMemoryStorage = new TbCoapDtlsSessionInMemoryStorage(dtlsSessionInactivityTimeout, dtlsSessionReportTimeout);
    }

    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return Collections.singletonList(CertificateType.X_509);
    }

    @Override
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName, InetSocketAddress remotePeer, boolean clientUsage, boolean verifySubject, boolean truncateCertificatePath, CertificateMessage message) {
        try {
            CertPath certpath = message.getCertificateChain();
            X509Certificate[] chain = certpath.getCertificates().toArray(new X509Certificate[0]);
            for (X509Certificate cert : chain) {
                try {
                    if (!skipValidityCheckForClientCert) {
                        cert.checkValidity();
                    }

                    String strCert = SslUtil.getCertificateString(cert);
                    String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
                    final ValidateDeviceCredentialsResponse[] deviceCredentialsResponse = new ValidateDeviceCredentialsResponse[1];
                    CountDownLatch latch = new CountDownLatch(1);
                    transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceX509CertRequestMsg.newBuilder().setHash(sha3Hash).build(),
                            new TransportServiceCallback<>() {
                                @Override
                                public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                                    if (!StringUtils.isEmpty(msg.getCredentials())) {
                                        deviceCredentialsResponse[0] = msg;
                                    }
                                    latch.countDown();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    log.error(e.getMessage(), e);
                                    latch.countDown();
                                }
                            });
                    latch.await(10, TimeUnit.SECONDS);
                    ValidateDeviceCredentialsResponse msg = deviceCredentialsResponse[0];
                    if (msg != null && strCert.equals(msg.getCredentials())) {
                        DeviceProfile deviceProfile = msg.getDeviceProfile();
                        if (msg.hasDeviceInfo() && deviceProfile != null) {
                            tbCoapDtlsSessionInMemoryStorage.put(remotePeer, new TbCoapDtlsSessionInfo(msg, deviceProfile));
                        }
                        break;
                    }
                } catch (InterruptedException |
                        CertificateEncodingException |
                        CertificateExpiredException |
                        CertificateNotYetValidException e) {
                    log.error(e.getMessage(), e);
                    AlertMessage alert = new AlertMessage(AlertMessage.AlertLevel.FATAL, AlertMessage.AlertDescription.BAD_CERTIFICATE);
                    throw new HandshakeException("Certificate chain could not be validated", alert);
                }
            }
            return new CertificateVerificationResult(cid, certpath, null);
        } catch (HandshakeException e) {
            log.trace("Certificate validation failed!", e);
            return new CertificateVerificationResult(cid, e, null);
        }
    }

    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return CertPathUtil.toSubjects(null);
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {
    }

    public ConcurrentMap<InetSocketAddress, TbCoapDtlsSessionInfo> getTbCoapDtlsSessionsMap() {
        return tbCoapDtlsSessionInMemoryStorage.getDtlsSessionsMap();
    }

    public void evictTimeoutSessions() {
        tbCoapDtlsSessionInMemoryStorage.evictTimeoutSessions();
    }

    public long getDtlsSessionReportTimeout() {
        return tbCoapDtlsSessionInMemoryStorage.getDtlsSessionReportTimeout();
    }
}
