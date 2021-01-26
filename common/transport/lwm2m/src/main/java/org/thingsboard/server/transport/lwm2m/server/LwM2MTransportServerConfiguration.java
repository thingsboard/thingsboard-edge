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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2MSetSecurityStoreServer;
import org.thingsboard.server.transport.lwm2m.server.secure.LwM2mInMemorySecurityStore;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;
import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.X509;
import static org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode.RPK;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MTransportHandler.getCoapConfig;


@Slf4j
@ComponentScan("org.thingsboard.server.transport.lwm2m.server")
@ComponentScan("org.thingsboard.server.transport.lwm2m.utils")
@Configuration("LwM2MTransportServerConfiguration")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' ) || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportServerConfiguration {

    @Autowired
    private LwM2MTransportContextServer context;

    @Autowired
    private LwM2mInMemorySecurityStore lwM2mInMemorySecurityStore;

    @Primary
    @Bean(name = "LeshanServerCert")
    public LeshanServer getLeshanServerCert() {
        log.info("Starting LwM2M transport ServerCert... PostConstruct");
        return getLeshanServer(this.context.getCtxServer().getServerPortCert(), this.context.getCtxServer().getServerSecurePortCert(), X509);
    }

    @Bean(name = "leshanServerNoSecPskRpk")
    public LeshanServer getLeshanServerNoSecPskRpk() {
        log.info("Starting LwM2M transport ServerNoSecPskRpk... PostConstruct");
        return getLeshanServer(this.context.getCtxServer().getServerPort(), this.context.getCtxServer().getServerSecurePort(), RPK);
    }

    private LeshanServer getLeshanServer(Integer serverPort, Integer serverSecurePort, LwM2MSecurityMode dtlsMode) {

        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(this.context.getCtxServer().getServerHost(), serverPort);
        builder.setLocalSecureAddress(this.context.getCtxServer().getServerSecureHost(), serverSecurePort);
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new LwM2mValueConverterImpl()));

        /** Create CoAP Config */
        builder.setCoapConfig(getCoapConfig());

        /** Define model provider (Create Models )*/
        LwM2mModelProvider modelProvider = new VersionedModelProvider(this.context.getCtxServer().getModelsValue());
        builder.setObjectModelProvider(modelProvider);

        /** Create DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setRecommendedCipherSuitesOnly(this.context.getCtxServer().isSupportDeprecatedCiphersEnable());
        /** Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        /** Use a magic converter to support bad type send by the UI. */
        builder.setEncoder(new DefaultLwM2mNodeEncoder(new LwM2mValueConverterImpl()));

        /**  Create DTLS security mode
         * There can be only one DTLS security mode
         */
        new LwM2MSetSecurityStoreServer(builder, context, lwM2mInMemorySecurityStore, dtlsMode);

        /** Create LWM2M server */
        return builder.build();
    }
}
