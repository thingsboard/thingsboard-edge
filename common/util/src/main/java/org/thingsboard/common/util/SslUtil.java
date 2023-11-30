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
package org.thingsboard.common.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;
import org.thingsboard.server.common.data.StringUtils;

import java.io.StringReader;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SslUtil {

    public static final char[] EMPTY_PASS = {};

    public static final BouncyCastleProvider DEFAULT_PROVIDER = new BouncyCastleProvider();

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(DEFAULT_PROVIDER);
        }
    }

    private SslUtil() {
    }

    @SneakyThrows
    public static List<X509Certificate> readCertFile(String fileContent) {
        List<X509Certificate> certificates = new ArrayList<>();
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
        try (PEMParser pemParser = new PEMParser(new StringReader(fileContent))) {
            Object object;
            while ((object = pemParser.readObject()) != null) {
                if (object instanceof X509CertificateHolder) {
                    X509Certificate x509Cert = certConverter.getCertificate((X509CertificateHolder) object);
                    certificates.add(x509Cert);
                }
            }
        }
        return certificates;
    }

    @SneakyThrows
    public static PrivateKey readPrivateKey(String fileContent, String passStr) {
        char[] password = getPassword(passStr);

        PrivateKey privateKey = null;
        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter();
        if (StringUtils.isNotEmpty(fileContent)) {
            try (PEMParser pemParser = new PEMParser(new StringReader(fileContent))) {
                Object object;
                while ((object = pemParser.readObject()) != null) {
                    if (object instanceof PEMEncryptedKeyPair) {
                        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password);
                        privateKey = keyConverter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv)).getPrivate();
                        break;
                    } else if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
                        InputDecryptorProvider decProv =
                                new JcePKCSPBEInputDecryptorProviderBuilder().setProvider(DEFAULT_PROVIDER).build(password);
                        privateKey = keyConverter.getPrivateKey(((PKCS8EncryptedPrivateKeyInfo) object).decryptPrivateKeyInfo(decProv));
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
        return privateKey;
    }

    public static char[] getPassword(String passStr) {
        return StringUtils.isEmpty(passStr) ? EMPTY_PASS : passStr.toCharArray();
    }

}
