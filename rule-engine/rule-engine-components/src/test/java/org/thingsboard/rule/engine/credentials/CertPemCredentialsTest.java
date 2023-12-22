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

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.common.util.SslUtil;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Stream;

import static org.thingsboard.rule.engine.credentials.CertPemCredentials.CERT_ALIAS_PREFIX;
import static org.thingsboard.rule.engine.credentials.CertPemCredentials.PRIVATE_KEY_ALIAS;

public class CertPemCredentialsTest {

    private static final String PASS = "test";
    private static final String EMPTY_PASS = "";
    private static final String RSA = "RSA";
    private static final String EC = "EC";

    @Test
    public void testChainOfCertificates() throws Exception {
        String fileContent = fileContent("pem/tb-cloud-chain.pem");

        List<X509Certificate> x509Certificates = SslUtil.readCertFile(fileContent);

        Assert.assertEquals(4, x509Certificates.size());
        Assert.assertEquals("CN=*.thingsboard.cloud, O=\"ThingsBoard, Inc.\", ST=New York, C=US",
                x509Certificates.get(0).getSubjectDN().getName());
        Assert.assertEquals("CN=Sectigo ECC Organization Validation Secure Server CA, O=Sectigo Limited, L=Salford, ST=Greater Manchester, C=GB",
                x509Certificates.get(1).getSubjectDN().getName());
        Assert.assertEquals("CN=USERTrust ECC Certification Authority, O=The USERTRUST Network, L=Jersey City, ST=New Jersey, C=US",
                x509Certificates.get(2).getSubjectDN().getName());
        Assert.assertEquals("CN=AAA Certificate Services, O=Comodo CA Limited, L=Salford, ST=Greater Manchester, C=GB",
                x509Certificates.get(3).getSubjectDN().getName());
    }

    @Test
    public void testSingleCertificate() throws Exception {
        String fileContent = fileContent("pem/tb-cloud.pem");

        List<X509Certificate> x509Certificates = SslUtil.readCertFile(fileContent);

        Assert.assertEquals(1, x509Certificates.size());
        Assert.assertEquals("CN=*.thingsboard.cloud, O=\"ThingsBoard, Inc.\", ST=New York, C=US",
                x509Certificates.get(0).getSubjectDN().getName());
    }

    @Test
    public void testEmptyFileContent() throws Exception {
        String fileContent = fileContent("pem/empty.pem");

        List<X509Certificate> x509Certificates = SslUtil.readCertFile(fileContent);

        Assert.assertEquals(0, x509Certificates.size());
    }

    private static Stream<Arguments> testLoadKeyStore() {
        return Stream.of(
                Arguments.of("pem/rsa_cert.pem", "pem/rsa_key.pem", EMPTY_PASS, RSA),
                Arguments.of("pem/rsa_encrypted_cert.pem", "pem/rsa_encrypted_key.pem", PASS, RSA),
                Arguments.of("pem/rsa_encrypted_traditional_cert.pem", "pem/rsa_encrypted_traditional_key.pem", PASS, RSA),
                Arguments.of("pem/ec_cert.pem", "pem/ec_key.pem", EMPTY_PASS, EC)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testLoadKeyStore(String certPath, String keyPath, String password, String algorithm) throws Exception {
        CertPemCredentials certPemCredentials = new CertPemCredentials();
        String certContent = fileContent(certPath);
        certPemCredentials.setCert(certContent);
        certPemCredentials.setPrivateKey(fileContent(keyPath));
        certPemCredentials.setPassword(password);
        KeyStore keyStore = certPemCredentials.loadKeyStore();
        Assertions.assertNotNull(keyStore);
        Key key = keyStore.getKey(PRIVATE_KEY_ALIAS, password.toCharArray());
        Assertions.assertNotNull(key);
        Assertions.assertEquals(algorithm, key.getAlgorithm());

        List<X509Certificate> certs = SslUtil.readCertFile(certContent);
        for (X509Certificate cert : certs) {
            String alias = CERT_ALIAS_PREFIX + cert.getIssuerDN().getName();
            Certificate certificate = keyStore.getCertificate(alias);
            Assertions.assertNotNull(certificate);
            Assertions.assertEquals(new String(cert.getEncoded()), new String(certificate.getEncoded()));
        }
    }

    private String fileContent(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }
}
