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
package org.thingsboard.rule.engine.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.SslUtil;
import org.thingsboard.server.common.data.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertPemCredentials implements ClientCredentials {

    public static final String PRIVATE_KEY_ALIAS = "private-key";
    public static final String X_509 = "X.509";
    public static final String CERT_ALIAS_PREFIX = "cert-";
    public static final String CA_CERT_CERT_ALIAS_PREFIX = "caCert-cert-";

    protected String caCert;
    private String cert;
    private String privateKey;
    private String password;

    @Override
    public CredentialsType getType() {
        return CredentialsType.CERT_PEM;
    }

    @Override
    public SslContext initSslContext() {
        try {
            SslContextBuilder builder = SslContextBuilder.forClient();
            if (StringUtils.hasLength(caCert)) {
                builder.trustManager(createAndInitTrustManagerFactory());
            }
            if (StringUtils.hasLength(cert) && StringUtils.hasLength(privateKey)) {
                builder.keyManager(createAndInitKeyManagerFactory());
            }
            return builder.build();
        } catch (Exception e) {
            log.error("[{}:{}] Creating TLS factory failed!", caCert, cert, e);
            throw new RuntimeException("Creating TLS factory failed!", e);
        }
    }

    protected TrustManagerFactory createAndInitTrustManagerFactory() throws Exception {
        List<X509Certificate> caCerts = SslUtil.readCertFile(caCert);

        KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        caKeyStore.load(null, null);
        for (X509Certificate caCert : caCerts) {
            caKeyStore.setCertificateEntry(CA_CERT_CERT_ALIAS_PREFIX + caCert.getSubjectDN().getName(), caCert);
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(caKeyStore);
        return trustManagerFactory;
    }

    private KeyManagerFactory createAndInitKeyManagerFactory() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(loadKeyStore(), SslUtil.getPassword(password));
        return kmf;
    }

    protected KeyStore loadKeyStore() throws Exception {
        List<X509Certificate> certificates = SslUtil.readCertFile(this.cert);
        PrivateKey privateKey = SslUtil.readPrivateKey(this.privateKey, password);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        List<X509Certificate> unique = certificates.stream().distinct().collect(Collectors.toList());
        for (X509Certificate cert : unique) {
            keyStore.setCertificateEntry(CERT_ALIAS_PREFIX + cert.getSubjectDN().getName(), cert);
        }

        if (privateKey != null) {
            CertificateFactory factory = CertificateFactory.getInstance(X_509);
            CertPath certPath = factory.generateCertPath(certificates);
            List<? extends Certificate> path = certPath.getCertificates();
            Certificate[] x509Certificates = path.toArray(new Certificate[0]);
            keyStore.setKeyEntry(PRIVATE_KEY_ALIAS, privateKey, SslUtil.getPassword(password), x509Certificates);
        }
        return keyStore;
    }

}
