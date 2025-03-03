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
package org.thingsboard.server.coapserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TbCoapDtlsSettings.class)
@TestPropertySource(properties = {
        "coap.dtls.enabled=true",
        "coap.dtls.bind_address=192.168.1.1",
        "coap.dtls.bind_port=1234",
        "coap.dtls.retransmission_timeout=100",
        "coap.dtls.connection_id_length=500",
        "coap.dtls.x509.skip_validity_check_for_client_cert=true",
        "coap.dtls.x509.dtls_session_inactivity_timeout=1000",
        "coap.dtls.x509.dtls_session_report_timeout=3000",
})
class TbCoapDtlsSettingsTest {

    @Autowired
    TbCoapDtlsSettings coapDtlsSettings;
    @MockBean
    SslCredentialsConfig sslCredentialsConfig;
    @MockBean
    private TransportService transportService;
    @MockBean
    private TbServiceInfoProvider serviceInfoProvider;

    @Test
    public void testCoapDtlsProperties() {
        assertThat(coapDtlsSettings).as("bean created").isNotNull();
        assertThat(coapDtlsSettings.getHost()).as("host").isEqualTo("192.168.1.1");
        assertThat(coapDtlsSettings.getPort()).as("port").isEqualTo(1234);
        assertThat(coapDtlsSettings.getDtlsRetransmissionTimeout()).as("retransmission_timeout").isEqualTo(100);
        assertThat(coapDtlsSettings.isSkipValidityCheckForClientCert()).as("skip_validity_check_for_client_cert").isTrue();
        assertThat(coapDtlsSettings.getDtlsSessionInactivityTimeout()).as("dtls_session_inactivity_timeout").isEqualTo(1000);
        assertThat(coapDtlsSettings.getDtlsSessionReportTimeout()).as("dtls_session_report_timeout").isEqualTo(3000);
    }

}
