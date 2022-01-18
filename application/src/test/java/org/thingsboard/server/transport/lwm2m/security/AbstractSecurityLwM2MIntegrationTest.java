/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.security;

import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;
import org.thingsboard.server.transport.lwm2m.client.LwM2MTestClient;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

@DaoSqlTest
public abstract class AbstractSecurityLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    protected final String CREDENTIALS_PATH = "lwm2m/credentials/";             // client public key or id used for PSK
    //             Get keys PSK
    protected final String CLIENT_PSK_IDENTITY = "SOME_PSK_ID";                                 // client public key or id used for PSK
    protected final String CLIENT_PSK_KEY = "73656372657450534b73656372657450";                  // client private/secret key used for PSK

    // Server
    protected static final String SERVER_JKS_FOR_TEST = "lwm2mserver";
    protected static final String SERVER_STORE_PWD = "server_ks_password";
    protected static final String SERVER_CERT_ALIAS = "server";
protected final X509Certificate serverX509Cert;                                                 // server certificate signed by rootCA
    protected final PublicKey serverPublicKeyFromCert;                                          // server public key used for RPK

    // Client
    protected LwM2MTestClient client;
    protected static final String CLIENT_ENDPOINT_NO_SEC = "LwNoSec00000000";
    protected static final String CLIENT_ENDPOINT_PSK = "LwPsk00000000";
    protected static final String CLIENT_ENDPOINT_RPK = "LwRpk00000000";
    protected static final String CLIENT_ENDPOINT_X509_TRUST = "LwX50900000000";
    protected static final String CLIENT_ENDPOINT_X509_TRUST_NO = "LwX509TrustNo";
    protected static final String CLIENT_JKS_FOR_TEST = "lwm2mclient";
    protected static final String CLIENT_STORE_PWD = "client_ks_password";
    protected static final String CLIENT_ALIAS_CERT_TRUST = "client_alias_00000000";
    protected static final String CLIENT_ALIAS_CERT_TRUST_NO = "client_alias_trust_no";

    protected final X509Certificate clientX509CertTrust;         // client certificate signed by intermediate, rootCA with a good CN ("host name")
    protected final PrivateKey clientPrivateKeyFromCertTrust;    // client private key used for X509 and RPK
    protected final PublicKey clientPublicKeyFromCertTrust;      // client public key used for RPK
    protected final X509Certificate clientX509CertTrustNo;         // client certificate signed by intermediate, rootCA with a good CN ("host name")
    protected final PrivateKey clientPrivateKeyFromCertTrustNo;    // client private key used for X509 and RPK
    protected final PublicKey clientPublicKeyFromCertTrustNo;      // client public key used for RPK
    private final  String[] RESOURCES_SECURITY = new String[]{"1.xml", "2.xml", "3.xml", "5.xml", "9.xml"};


    private final LwM2MBootstrapClientCredentials defaultBootstrapCredentials;



    public AbstractSecurityLwM2MIntegrationTest() {
        // create client credentials
        setResources(this.RESOURCES_SECURITY);
        try {
            // Get certificates from key store
            char[] clientKeyStorePwd = CLIENT_STORE_PWD.toCharArray();
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream clientKeyStoreFile = this.getClass().getClassLoader().getResourceAsStream(CREDENTIALS_PATH + CLIENT_JKS_FOR_TEST + ".jks")) {
                clientKeyStore.load(clientKeyStoreFile, clientKeyStorePwd);
            }
            // Trust
            clientPrivateKeyFromCertTrust = (PrivateKey) clientKeyStore.getKey(CLIENT_ALIAS_CERT_TRUST, clientKeyStorePwd);
            clientX509CertTrust = (X509Certificate) clientKeyStore.getCertificate(CLIENT_ALIAS_CERT_TRUST);
            clientPublicKeyFromCertTrust = clientX509CertTrust != null ? clientX509CertTrust.getPublicKey() : null;
            // No trust
            clientPrivateKeyFromCertTrustNo = (PrivateKey) clientKeyStore.getKey(CLIENT_ALIAS_CERT_TRUST_NO, clientKeyStorePwd);
            clientX509CertTrustNo = (X509Certificate) clientKeyStore.getCertificate(CLIENT_ALIAS_CERT_TRUST_NO);
            clientPublicKeyFromCertTrustNo = clientX509CertTrustNo != null ? clientX509CertTrustNo.getPublicKey() : null;

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        // create server credentials
        try {
            // Get certificates from key store
            char[] serverKeyStorePwd = SERVER_STORE_PWD.toCharArray();
            KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream serverKeyStoreFile = this.getClass().getClassLoader().getResourceAsStream(CREDENTIALS_PATH + SERVER_JKS_FOR_TEST + ".jks")) {
                serverKeyStore.load(serverKeyStoreFile, serverKeyStorePwd);
            }

            serverX509Cert = (X509Certificate) serverKeyStore.getCertificate(SERVER_CERT_ALIAS);
            serverPublicKeyFromCert = serverX509Cert.getPublicKey();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        defaultBootstrapCredentials = new LwM2MBootstrapClientCredentials();

        NoSecBootstrapClientCredential serverCredentials = new NoSecBootstrapClientCredential();

        defaultBootstrapCredentials.setBootstrapServer(serverCredentials);
        defaultBootstrapCredentials.setLwm2mServer(serverCredentials);
    }
}
