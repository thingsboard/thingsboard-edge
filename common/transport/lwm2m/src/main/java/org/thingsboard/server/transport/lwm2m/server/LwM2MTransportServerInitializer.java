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
import org.eclipse.leshan.server.californium.LeshanServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.transport.lwm2m.secure.LWM2MGenerationPSkRPkECC;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Component("LwM2MTransportServerInitializer")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' ) || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportServerInitializer {


    @Autowired
    private LwM2MTransportServiceImpl service;

    @Autowired(required = false)
    @Qualifier("leshanServerX509")
    private LeshanServer lhServerX509;

    @Autowired(required = false)
    @Qualifier("leshanServerPsk")
    private LeshanServer lhServerPsk;

    @Autowired(required = false)
    @Qualifier("leshanServerRpk")
    private LeshanServer lhServerRpk;

    @Autowired
    private LwM2MTransportContextServer context;

    @PostConstruct
    public void init() {
        if (this.context.getCtxServer().getEnableGenPskRpk()) new LWM2MGenerationPSkRPkECC();
        if (this.context.getCtxServer().isServerStartPsk()) {
            this.startLhServerPsk();
        }
        if (this.context.getCtxServer().isServerStartRpk()) {
            this.startLhServerRpk();
        }
        if (this.context.getCtxServer().isServerStartX509()) {
            this.startLhServerX509();
        }
    }

    private void startLhServerPsk() {
        this.lhServerPsk.start();
        LwM2mServerListener lhServerPskListener = new LwM2mServerListener(this.lhServerPsk, service);
        this.lhServerPsk.getRegistrationService().addListener(lhServerPskListener.registrationListener);
        this.lhServerPsk.getPresenceService().addListener(lhServerPskListener.presenceListener);
        this.lhServerPsk.getObservationService().addListener(lhServerPskListener.observationListener);
    }

    private void startLhServerRpk() {
        this.lhServerRpk.start();
        LwM2mServerListener lhServerRpkListener = new LwM2mServerListener(this.lhServerRpk, service);
        this.lhServerRpk.getRegistrationService().addListener(lhServerRpkListener.registrationListener);
        this.lhServerRpk.getPresenceService().addListener(lhServerRpkListener.presenceListener);
        this.lhServerRpk.getObservationService().addListener(lhServerRpkListener.observationListener);
    }

    private void startLhServerX509() {
        this.lhServerX509.start();
        LwM2mServerListener lhServerCertListener = new LwM2mServerListener(this.lhServerX509, service);
        this.lhServerX509.getRegistrationService().addListener(lhServerCertListener.registrationListener);
        this.lhServerX509.getPresenceService().addListener(lhServerCertListener.presenceListener);
        this.lhServerX509.getObservationService().addListener(lhServerCertListener.observationListener);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping LwM2M transport Server!");
        lhServerPsk.destroy();
        lhServerRpk.destroy();
        lhServerX509.destroy();
        log.info("LwM2M transport Server stopped!");
    }
}
