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
package org.thingsboard.server.common.data.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum IntegrationType {
    OCEANCONNECT(false, null),
    SIGFOX(false, "Sigfox"),
    THINGPARK(false, "ThingPark"),
    TPE(false, "ThingParkEnterprise"),
    CHIRPSTACK(false, "ChirpStack"),
    PARTICLE(false, "Particle"),
    TMOBILE_IOT_CDP(false, null),
    HTTP(false, "HTTP"),
    MQTT(true, "MQTT"),
    PUB_SUB(true, "PubSub"),
    AWS_IOT(true, "AWS IoT"),
    AWS_SQS(true, "Amazon SQS"),
    AWS_KINESIS(false, "Amazon Kinesis"),
    IBM_WATSON_IOT(true, "IBM Watson IoT"),
    TTN(true, "ThingsStackIndustries"),
    TTI(true, "ThingsStackIndustries"),
    AZURE_EVENT_HUB(true, "Azure Event Hub"),
    OPC_UA(true, "OPC UA"),
    CUSTOM(false, true, null),
    UDP(false, true, "UDP"),
    TCP(false, true, "TCP"),
    KAFKA(true, "Apache Kafka"),
    AZURE_IOT_HUB(true, "Azure IoT Hub"),
    APACHE_PULSAR(true, "Apache Pulsar"),
    RABBITMQ(false, "RabbitMQ"),
    LORIOT(false, "LORIOT"),
    COAP(false, "CoAP"),
    TUYA(true, "Tuya"),
    AZURE_SERVICE_BUS(true, "Azure Service Bus"),
    KPN(false, "KPN");


    //Identifies if the Integration instance is one per cluster.
    private final boolean singleton;
    private final boolean remoteOnly;
    private final String directory;

    IntegrationType(boolean singleton, String directory) {
        this(singleton, false, directory);
    }
}
