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
package org.thingsboard.server.common.transport.lwm2m;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.security.PublicKey;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true') || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core'")
public class LwM2MTransportConfigBootstrap {

    @Getter
    @Value("${transport.lwm2m.bootstrap.enable:}")
    private Boolean bootstrapEnable;

    @Getter
    @Value("${transport.lwm2m.bootstrap.id:}")
    private Integer bootstrapServerId;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.start_psk:}")
    private Boolean bootstrapStartPsk;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.start_rpk:}")
    private Boolean bootstrapStartRpk;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.start_x509:}")
    private Boolean bootstrapStartX509;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_address:}")
    private String bootstrapHost;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.bind_address:}")
    private String bootstrapSecureHost;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_port_no_sec_psk:}")
    private Integer bootstrapPortNoSecPsk;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_port_no_sec_rpk:}")
    private Integer bootstrapPortNoSecRpk;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_port_no_sec_x509:}")
    private Integer bootstrapPortNoSecX509;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.bind_port_psk:}")
    private Integer bootstrapSecurePortPsk;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.bind_port_rpk:}")
    private Integer bootstrapSecurePortRpk;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.bind_port_x509:}")
    private Integer bootstrapSecurePortX509;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.public_x:}")
    private String bootstrapPublicX;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.public_y:}")
    private String bootstrapPublicY;

    @Getter
    @Setter
    private PublicKey bootstrapPublicKey;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.private_s:}")
    private String bootstrapPrivateS;

    @Getter
    @Value("${transport.lwm2m.bootstrap.secure.alias:}")
    private String bootstrapAlias;

    @Getter
    @Setter
    private Map<String /** clientEndPoint */, TransportProtos.ValidateDeviceCredentialsResponseMsg> sessions;
}
