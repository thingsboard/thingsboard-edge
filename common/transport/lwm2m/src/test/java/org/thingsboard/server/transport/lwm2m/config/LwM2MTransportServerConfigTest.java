/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = LwM2MTransportServerConfig.class)
@ContextConfiguration(classes = {LwM2MTransportServerConfig.class}, loader = SpringBootContextLoader.class)
@TestPropertySource(properties = {
        "transport.sessions.report_timeout=10",
        "transport.lwm2m.security.recommended_ciphers=true",
        "transport.lwm2m.security.recommended_supported_groups=true",
        "transport.lwm2m.downlink_pool_size=10",
        "transport.lwm2m.uplink_pool_size=10",
        "transport.lwm2m.ota_pool_size=10",
        "transport.lwm2m.clean_period_in_sec=2",
        "transport.lwm2m.dtls.connection_id_length="

})
class LwM2MTransportServerConfigTest {

    @MockBean(name = "lwm2mServerCredentials")
    private SslCredentialsConfig credentialsConfig;

    @MockBean(name = "lwm2mTrustCredentials")
    private SslCredentialsConfig trustCredentialsConfig;

    @Autowired
    private LwM2MTransportServerConfig serverConfig;

    @Test
    void getDtlsConnectionIdLength_return_null_is_property_is_empty() {
        // note: transport.lwm2m.dtls.connect_id_length is set in TestPropertySource
        assertThat(serverConfig.getDtlsConnectionIdLength()).isNull();
    }
}