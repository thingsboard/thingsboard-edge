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
package org.thingsboard.server.common.data.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum IntegrationType {
    OCEANCONNECT(false),
    SIGFOX(false),
    THINGPARK(false),
    TPE(false),
    CHIRPSTACK(false),
    TMOBILE_IOT_CDP(false),
    HTTP(false),
    MQTT(true),
    PUB_SUB(true),
    AWS_IOT(true),
    AWS_SQS(true),
    AWS_KINESIS(false),
    IBM_WATSON_IOT(true),
    TTN(true),
    TTI(true),
    AZURE_EVENT_HUB(true),
    OPC_UA(true),
    CUSTOM(false, true),
    UDP(false, true),
    TCP(false, true),
    KAFKA(true),
    AZURE_IOT_HUB(true),
    APACHE_PULSAR(false),
    RABBITMQ(false),
    LORIOT(false),
    COAP(false),
    TUYA(false);

    IntegrationType(boolean singleton) {
        this.singleton = singleton;
        this.remoteOnly = false;
    }

    //Identifies if the Integration instance is one per cluster.
    @Getter
    private final boolean singleton;

    @Getter
    private final boolean remoteOnly;


}
