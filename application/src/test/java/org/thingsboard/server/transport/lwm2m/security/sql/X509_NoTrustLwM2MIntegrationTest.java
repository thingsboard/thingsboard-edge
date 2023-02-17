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
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.util.Hex;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.Base64Utils;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredential;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import javax.servlet.http.HttpServletResponse;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.eclipse.leshan.client.object.Security.x509;
import static org.eclipse.leshan.client.object.Security.x509Bootstrap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.X509;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class X509_NoTrustLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    //Lwm2m only
    @Test
    public void testWithX509NoTrustConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST_NO;
        X509Certificate certificate = clientX509CertTrustNo;
        PrivateKey privateKey = clientPrivateKeyFromCertTrustNo;
        X509ClientCredential clientCredentials = new X509ClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setCert(Base64Utils.encodeToString(certificate.getEncoded()));
        Security security = x509(SECURE_URI,
                shortServerId,
                certificate.getEncoded(),
                privateKey.getEncoded(),
                serverX509Cert.getEncoded());
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(X509, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, false);
        this.basicTestConnection(security,
                deviceCredentials,
                COAP_CONFIG,
                clientEndpoint,
                transportConfiguration,
                "await on client state (X509_Trust_Lwm2m)",
                expectedStatusesRegistrationLwm2mSuccess,
                false,
                ON_REGISTRATION_SUCCESS,
                true);
    }

    @Test
    public void testWithX509NoTrustValidationPublicKeyBase64format_BAD_REQUEST() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST_NO + "BadPublicKey";
        X509Certificate certificate = clientX509CertTrustNo;
        PrivateKey privateKey = clientPrivateKeyFromCertTrustNo;
        X509ClientCredential clientCredentials = new X509ClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setCert(Hex.encodeHexString(certificate.getEncoded()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(X509, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, false);
        createDeviceProfile(transportConfiguration);
        MvcResult result = createDeviceWithMvcResult(deviceCredentials, clientEndpoint);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getResponse().getStatus());
        String msgExpected = "LwM2M client X509 certificate must be in DER-encoded X509v3 format and support only EC algorithm and then encoded to Base64 format!";
        assertTrue(result.getResponse().getContentAsString().contains(msgExpected));
    }

    @Test
    public void testWithX509NoTrustValidationPrivateKeyBase64format_BAD_REQUEST() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST_NO + "BadPrivateKey";
        X509Certificate certificate = clientX509CertTrustNo;
        PrivateKey privateKey = clientPrivateKeyFromCertTrustNo;
        X509ClientCredential clientCredentials = new X509ClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setCert(Base64Utils.encodeToString(certificate.getEncoded()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(X509, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, true);
        createDeviceProfile(transportConfiguration);
        MvcResult result = createDeviceWithMvcResult(deviceCredentials, clientEndpoint);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getResponse().getStatus());
        String msgExpected = "Bootstrap server client X509 secret key must be in PKCS#8 format (DER encoding, standard [RFC5958]) and then encoded to Base64 format!";
        assertTrue(result.getResponse().getContentAsString().contains(msgExpected));
    }

    // Bootstrap + Lwm2m
    @Test
    public void testWithX509NoTrustConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST_NO;
        X509Certificate certificate = clientX509CertTrustNo;
        PrivateKey privateKey = clientPrivateKeyFromCertTrustNo;
        X509ClientCredential clientCredentials = new X509ClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setCert(Base64Utils.encodeToString(certificate.getEncoded()));
        Security security = x509Bootstrap(SECURE_URI_BS,
                certificate.getEncoded(),
                privateKey.getEncoded(),
                serverX509CertBs.getEncoded());
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(X509, BOTH));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, false);
        this.basicTestConnection(security,
                deviceCredentials,
                COAP_CONFIG_BS,
                clientEndpoint,
                transportConfiguration,
                "await on client state (X509NoTrust two section)",
                expectedStatusesRegistrationBsSuccess,
                true,
                ON_REGISTRATION_SUCCESS,
                true);
    }
}
