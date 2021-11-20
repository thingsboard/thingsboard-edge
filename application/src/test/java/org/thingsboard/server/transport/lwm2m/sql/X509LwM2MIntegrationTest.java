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
package org.thingsboard.server.transport.lwm2m.sql;

import org.eclipse.leshan.client.object.Security;
import org.junit.Ignore;
import org.junit.Test;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredentials;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;

import static org.eclipse.leshan.client.object.Security.x509;

public class X509LwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    @Test
    public void testConnectAndObserveTelemetry() throws Exception {
        X509ClientCredentials credentials = new X509ClientCredentials();
        credentials.setEndpoint(ENDPOINT);
        Security security = x509(SECURE_URI,
                123,
                clientX509Cert.getEncoded(),
                clientPrivateKeyFromCert.getEncoded(),
                serverX509Cert.getEncoded());
        super.basicTestConnectionObserveTelemetry(security, credentials, SECURE_COAP_CONFIG, ENDPOINT);
    }

    @Ignore //See LwM2mClientContextImpl.unregister
    @Test
    public void testConnectWithCertAndObserveTelemetry() throws Exception {
        X509ClientCredentials credentials = new X509ClientCredentials();
        credentials.setEndpoint(ENDPOINT);
        credentials.setCert(SslUtil.getCertificateString(clientX509CertNotTrusted));
        Security security = x509(SECURE_URI,
                123,
                clientX509CertNotTrusted.getEncoded(),
                clientPrivateKeyFromCert.getEncoded(),
                serverX509Cert.getEncoded());
        super.basicTestConnectionObserveTelemetry(security, credentials, SECURE_COAP_CONFIG, ENDPOINT);
    }

}
