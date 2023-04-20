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
package org.thingsboard.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.coapserver.TbCoapTransportComponent;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.transport.coap.efento.CoapEfentoTransportResource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.UnknownHostException;

@Service("CoapTransportService")
@TbCoapTransportComponent
@Slf4j
public class CoapTransportService implements TbTransportService {

    private static final String V1 = "v1";
    private static final String API = "api";
    private static final String EFENTO = "efento";
    public static final String MEASUREMENTS = "m";
    public static final String DEVICE_INFO = "i";
    public static final String CONFIGURATION = "c";
    public static final String CURRENT_TIMESTAMP = "t";

    @Autowired
    private CoapServerService coapServerService;

    @Autowired
    private CoapTransportContext coapTransportContext;

    private CoapServer coapServer;

    @PostConstruct
    public void init() throws UnknownHostException {
        log.info("Starting CoAP transport...");
        coapServer = coapServerService.getCoapServer();
        CoapResource api = new CoapResource(API);
        api.add(new CoapTransportResource(coapTransportContext, coapServerService, V1));

        CoapEfentoTransportResource efento = new CoapEfentoTransportResource(coapTransportContext, EFENTO);
        efento.add(new CoapResource(MEASUREMENTS));
        efento.add(new CoapResource(DEVICE_INFO));
        efento.add(new CoapResource(CONFIGURATION));
        efento.add(new CoapResource(CURRENT_TIMESTAMP));
        coapServer.add(api);
        coapServer.add(efento);
        coapServer.add(new OtaPackageTransportResource(coapTransportContext, OtaPackageType.FIRMWARE));
        coapServer.add(new OtaPackageTransportResource(coapTransportContext, OtaPackageType.SOFTWARE));
        log.info("CoAP transport started!");
    }

    @PreDestroy
    public void shutdown() {
        log.info("CoAP transport stopped!");
    }

    @Override
    public String getName() {
        return DataConstants.COAP_TRANSPORT_NAME;
    }
}
