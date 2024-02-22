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
package org.thingsboard.server.transport.lwm2m.bootstrap;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServer;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.TbLwM2MDtlsBootstrapCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MInMemoryBootstrapConfigStore;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MAuthorizer;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MDtlsCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.server.store.TbSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.when;

@ExtendWith(MockitoExtension.class)
public class LwM2MTransportBootstrapServiceTest {

    @Mock
    private LwM2MTransportServerConfig serverConfig;
    @Mock
    private LwM2MTransportBootstrapConfig bootstrapConfig;
    @Mock
    private LwM2MBootstrapSecurityStore lwM2MBootstrapSecurityStore;
    @Mock
    private LwM2MInMemoryBootstrapConfigStore lwM2MInMemoryBootstrapConfigStore;
    @Mock
    private TransportService transportService;
    @Mock
    private TbLwM2MDtlsBootstrapCertificateVerifier certificateVerifier;


    @Test
    public void getLHServer_creates_ConnectionIdGenerator_when_connection_id_length_not_null(){
        final Integer CONNECTION_ID_LENGTH = 6;
        when(serverConfig.getDtlsConnectionIdLength()).thenReturn(CONNECTION_ID_LENGTH);
        var lwM2MBootstrapService = createLwM2MBootstrapService();

        var server = lwM2MBootstrapService.getLhBootstrapServer();
        var securedEndpoint = (CoapEndpoint) ReflectionTestUtils.getField(server, "securedEndpoint");
        assertThat(securedEndpoint).isNotNull();

        var config = (DtlsConnectorConfig) ReflectionTestUtils.getField(securedEndpoint.getConnector(), "config");
        assertThat(config).isNotNull();
        assertThat(config.getConnectionIdGenerator()).isNotNull();
        assertThat((Integer) ReflectionTestUtils.getField(config.getConnectionIdGenerator(), "connectionIdLength"))
                .isEqualTo(CONNECTION_ID_LENGTH);
    }

    @Test
    public void getLHServer_creates_no_ConnectionIdGenerator_when_connection_id_length_is_null(){
        when(serverConfig.getDtlsConnectionIdLength()).thenReturn(null);
        var lwM2MBootstrapService = createLwM2MBootstrapService();

        var server = lwM2MBootstrapService.getLhBootstrapServer();
        var securedEndpoint = (CoapEndpoint) ReflectionTestUtils.getField(server, "securedEndpoint");
        assertThat(securedEndpoint).isNotNull();

        var config = (DtlsConnectorConfig) ReflectionTestUtils.getField(securedEndpoint.getConnector(), "config");
        assertThat(config).isNotNull();
        assertThat(config.getConnectionIdGenerator()).isNull();
    }

    private LwM2MTransportBootstrapService createLwM2MBootstrapService() {
        setDefaultConfigVariables();
        return new LwM2MTransportBootstrapService(serverConfig, bootstrapConfig, lwM2MBootstrapSecurityStore,
                lwM2MInMemoryBootstrapConfigStore, transportService, certificateVerifier);
    }

    private void setDefaultConfigVariables(){
        when(bootstrapConfig.getPort()).thenReturn(5683);
        when(bootstrapConfig.getSecurePort()).thenReturn(5684);
        when(serverConfig.isRecommendedCiphers()).thenReturn(false);
        when(serverConfig.getDtlsRetransmissionTimeout()).thenReturn(9000);
    }


}