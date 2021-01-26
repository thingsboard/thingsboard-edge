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
package org.thingsboard.server.transport.lwm2m.bootstrap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.californium.bootstrap.LeshanBootstrapServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.transport.lwm2m.secure.LwM2MSecurityMode;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Slf4j
@Service
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled}'=='true'&& '${transport.lwm2m.bootstrap.enable}'=='true') || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true'&& '${transport.lwm2m.bootstrap.enable}'=='true')")
public class LwM2MTransportBootstrapServerInitializer {

    @Autowired
    @Qualifier("leshanBootstrapCert")
    private LeshanBootstrapServer lhBServerCert;

    @Autowired
    @Qualifier("leshanBootstrapRPK")
    private LeshanBootstrapServer lhBServerRPK;

    @Autowired
    private LwM2MTransportContextBootstrap contextBS;

    @PostConstruct
    public void init() {
        if (this.contextBS.getCtxBootStrap().isBootstrapStartAll()) {
            this.lhBServerCert.start();
            this.lhBServerRPK.start();
        }
        else {
            if (this.contextBS.getCtxBootStrap().getBootStrapDtlsMode() == LwM2MSecurityMode.X509.code) {
                this.lhBServerCert.start();
            }
            else {
                this.lhBServerRPK.start();
            }
        }
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        log.info("Stopping LwM2M transport Bootstrap Server!");
        try {
            lhBServerCert.destroy();
            lhBServerRPK.destroy();
        } finally {
        }
        log.info("LwM2M transport Bootstrap Server stopped!");
    }
}
