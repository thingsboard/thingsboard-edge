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
package org.thingsboard.server.common.transport.config.ssl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.thingsboard.server.common.data.ResourceUtils;
import org.thingsboard.server.common.data.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = false)
public class PemSslCredentials extends AbstractSslCredentials {

    private static final String DEFAULT_KEY_ALIAS = "server";

    private String certFile;
    private String keyFile;
    private String keyPassword;

    @Override
    protected boolean canUse() {
        return ResourceUtils.resourceExists(this, this.certFile);
    }

    @Override
    protected KeyStore loadKeyStore(boolean trustsOnly, char[] keyPasswordArray) throws IOException, GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        List<X509Certificate> certificates = new ArrayList<>();
        PrivateKey privateKey = null;
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();
        try (InputStream inStream = ResourceUtils.getInputStream(this, this.certFile)) {
            try (PEMParser pemParser = new PEMParser(new InputStreamReader(inStream))) {
                Object object;
                while((object = pemParser.readObject()) != null) {
                    if (object instanceof X509CertificateHolder) {
                        X509Certificate x509Cert = certConverter.getCertificate((X509CertificateHolder) object);
                        certificates.add(x509Cert);
                    } else if (object instanceof PEMEncryptedKeyPair) {
                        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(keyPasswordArray);
                        privateKey = keyConverter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
                    } else if (object instanceof PEMKeyPair) {
                        privateKey = keyConverter.getKeyPair((PEMKeyPair) object).getPrivate();
                    } else if (object instanceof PrivateKeyInfo) {
                        privateKey = keyConverter.getPrivateKey((PrivateKeyInfo) object);
                    }
                }
            }
        }
        if (privateKey == null && !StringUtils.isEmpty(this.keyFile)) {
            if (ResourceUtils.resourceExists(this, this.keyFile)) {
                try (InputStream inStream = ResourceUtils.getInputStream(this, this.keyFile)) {
                    try (PEMParser pemParser = new PEMParser(new InputStreamReader(inStream))) {
                        Object object;
                        while ((object = pemParser.readObject()) != null) {
                            if (object instanceof PEMEncryptedKeyPair) {
                                PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(keyPasswordArray);
                                privateKey = keyConverter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
                                break;
                            } else if (object instanceof PEMKeyPair) {
                                privateKey = keyConverter.getKeyPair((PEMKeyPair) object).getPrivate();
                                break;
                            } else if (object instanceof PrivateKeyInfo) {
                                privateKey = keyConverter.getPrivateKey((PrivateKeyInfo) object);
                            }
                        }
                    }
                }
            }
        }
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("No certificates found in certFile: " + this.certFile);
        }
        if (privateKey == null && !trustsOnly) {
            throw new IllegalArgumentException("Unable to load private key neither from certFile: " + this.certFile + " nor from keyFile: " + this.keyFile);
        }
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        if (trustsOnly) {
            List<Certificate> unique = certificates.stream().distinct().collect(Collectors.toList());
            for (int i = 0; i < unique.size(); i++) {
                keyStore.setCertificateEntry("root-" + i, unique.get(i));
            }
        }
        if (privateKey != null) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            CertPath certPath = factory.generateCertPath(certificates);
            List<? extends Certificate> path = certPath.getCertificates();
            Certificate[] x509Certificates = path.toArray(new Certificate[0]);
            keyStore.setKeyEntry(DEFAULT_KEY_ALIAS, privateKey, keyPasswordArray, x509Certificates);
        }
        return keyStore;
    }

    @Override
    public String getKeyAlias() {
        return DEFAULT_KEY_ALIAS;
    }

    @Override
    protected void updateKeyAlias(String keyAlias) {
    }
}
