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
import org.springframework.stereotype.Service;
import org.thingsboard.server.transport.lwm2m.secure.LWM2MGenerationPSkRPkECC;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Service("LwM2MTransportServerInitializer")
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true' ) || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true')")
public class LwM2MTransportServerInitializer {

    @Autowired
    @Qualifier("LeshanServerCert")
    private LeshanServer lhServerCert;

    @Autowired
    @Qualifier("leshanServerNoSecPskRpk")
    private LeshanServer lhServerNoSecPskRpk;

    @Autowired
    private LwM2MTransportContextServer context;

    @PostConstruct
    public void init() {
        if (this.context.getCtxServer().getEnableGenPskRpk()) new LWM2MGenerationPSkRPkECC();
        if (this.context.getCtxServer().isServerStartAll()) {
            this.lhServerCert.start();
            this.lhServerNoSecPskRpk.start();
        }
        else {
            if (this.context.getCtxServer().getServerDtlsMode() == LwM2MSecurityMode.X509.code) {
                this.lhServerCert.start();
            }
            else {
                this.lhServerNoSecPskRpk.start();
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping LwM2M transport Server!");
        try {
            lhServerCert.destroy();
            lhServerNoSecPskRpk.destroy();
        } finally {
        }
        log.info("LwM2M transport Server stopped!");
    }
}
