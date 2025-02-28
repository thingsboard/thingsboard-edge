/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.coap.x509;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.thingsboard.common.util.SslUtil;
import java.io.File;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

public class CertPrivateKey {
    private final X509Certificate cert;
    private PrivateKey privateKey;

    public CertPrivateKey(String certFilePathPem, String keyFilePathPem) throws Exception {
        List<X509Certificate> certs = SslUtil.readCertFile(fileRead(certFilePathPem));
        this.cert = certs.get(0);
        this.privateKey = SslUtil.readPrivateKey(fileRead(keyFilePathPem), null);
        if (this.privateKey instanceof BCECPrivateKey) {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(this.privateKey.getEncoded());
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            this.privateKey = keyFactory.generatePrivate(keySpec);
        }
        if (!(this.privateKey instanceof ECPrivateKey)) {
            throw new RuntimeException("Private key generation must be of type java.security.interfaces.ECPrivateKey, which is used in the standard Java API!");
        }
    }

    public CertPrivateKey(X509Certificate cert, PrivateKey privateKey) {
        this.cert = cert;
        this.privateKey = privateKey;
    }

    public X509Certificate getCert() {
        return this.cert;
    }

    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    private String fileRead(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }

    public static String convertCertToPEM(X509Certificate certificate) throws Exception {
        StringBuilder pemBuilder = new StringBuilder();
        pemBuilder.append("-----BEGIN CERTIFICATE-----\n");
        // Copy cert to Base64
        String base64EncodedCert = Base64.getEncoder().encodeToString(certificate.getEncoded());
        int index = 0;
        while (index < base64EncodedCert.length()) {
            pemBuilder.append(base64EncodedCert, index, Math.min(index + 64, base64EncodedCert.length()));
            pemBuilder.append("\n");
            index += 64;
        }
        pemBuilder.append("-----END CERTIFICATE-----\n");
        return pemBuilder.toString();
    }
}