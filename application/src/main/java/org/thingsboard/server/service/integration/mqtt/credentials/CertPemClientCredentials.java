/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.service.integration.mqtt.credentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.jk5.mqtt.MqttClientConfig;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.springframework.util.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Optional;

@Data
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertPemClientCredentials implements MqttClientCredentials {

    private static final String TLS_VERSION = "TLSv1.2";

    private String caCert;
    private String cert;
    private String privateKey;
    private String password;

    @Override
    public Optional<SslContext> initSslContext() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            return Optional.of(SslContextBuilder.forClient()
                    .keyManager(createAndInitKeyManagerFactory())
                    .trustManager(createAndInitTrustManagerFactory())
                    .clientAuth(ClientAuth.REQUIRE)
                    .build());
        } catch (Exception e) {
            log.error("[{}:{}] Creating TLS factory failed!", caCert, cert, e);
            throw new RuntimeException("Creating TLS factory failed!", e);
        }
    }

    @Override
    public void configure(MqttClientConfig config) {

    }

    private KeyManagerFactory createAndInitKeyManagerFactory() throws Exception {
        X509Certificate certHolder = readCertFile(cert);
        Object keyObject = readPrivateKeyFile(privateKey);
        char[] passwordCharArray = "".toCharArray();
        if (!StringUtils.isEmpty(password)) {
            passwordCharArray = password.toCharArray();
        }

        JcaPEMKeyConverter keyConverter = new JcaPEMKeyConverter().setProvider("BC");

        PrivateKey privateKey;
        if (keyObject instanceof PEMEncryptedKeyPair) {
            PEMDecryptorProvider provider = new JcePEMDecryptorProviderBuilder().build(passwordCharArray);
            KeyPair key = keyConverter.getKeyPair(((PEMEncryptedKeyPair) keyObject).decryptKeyPair(provider));
            privateKey = key.getPrivate();
        } else if (keyObject instanceof PEMKeyPair) {
            KeyPair key = keyConverter.getKeyPair((PEMKeyPair) keyObject);
            privateKey = key.getPrivate();
        } else if (keyObject instanceof PrivateKey) {
            privateKey = (PrivateKey)keyObject;
        } else {
            throw new RuntimeException("Unable to get private key from object: " + keyObject.getClass());
        }

        KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        clientKeyStore.load(null, null);
        clientKeyStore.setCertificateEntry("cert", certHolder);
        clientKeyStore.setKeyEntry("private-key",
                privateKey,
                passwordCharArray,
                new Certificate[]{certHolder});

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, passwordCharArray);
        return keyManagerFactory;
    }

    private TrustManagerFactory createAndInitTrustManagerFactory() throws Exception {
        X509Certificate caCertHolder;
        caCertHolder = readCertFile(caCert);

        KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        caKeyStore.load(null, null);
        caKeyStore.setCertificateEntry("caCert-cert", caCertHolder);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(caKeyStore);
        return trustManagerFactory;
    }

    private X509Certificate readCertFile(String fileContent) throws Exception {
        X509Certificate certificate = null;
        if (fileContent != null && !fileContent.trim().isEmpty()) {
            fileContent = fileContent.replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.decodeBase64(fileContent);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            certificate = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(decoded));
        }
        return certificate;
    }

    private PrivateKey readPrivateKeyFile(String fileContent) throws Exception {
        RSAPrivateKey privateKey = null;
        if (fileContent != null && !fileContent.isEmpty()) {
            fileContent = fileContent.replaceAll(".*BEGIN.*PRIVATE KEY.*", "")
                    .replaceAll(".*END.*PRIVATE KEY.*", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.decodeBase64(fileContent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        }
        return privateKey;
    }

}
