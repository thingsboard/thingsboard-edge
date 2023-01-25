/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.integration.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.server.common.data.integration.IntegrationType;

public class IntegrationUtil {

    public static ThingsboardPlatformIntegration<?> createPlatformIntegration(IntegrationType type, JsonNode configuration, boolean remote, Object param) throws Exception {
        switch (type) {
            case HTTP:
                return newInstance("org.thingsboard.integration.http.basic.BasicHttpIntegration");
            case LORIOT:
                return newInstance("org.thingsboard.integration.http.loriot.LoriotIntegration");
            case SIGFOX:
                return newInstance("org.thingsboard.integration.http.sigfox.SigFoxIntegration");
            case OCEANCONNECT:
                return newInstance("org.thingsboard.integration.http.oc.OceanConnectIntegration");
            case THINGPARK:
                return newInstance("org.thingsboard.integration.http.thingpark.ThingParkIntegration");
            case TPE:
                return newInstance("org.thingsboard.integration.http.thingpark.ThingParkIntegrationEnterprise");
            case TMOBILE_IOT_CDP:
                return newInstance("org.thingsboard.integration.http.tmobile.TMobileIotCdpIntegration");
            case CHIRPSTACK:
                return newInstance("org.thingsboard.integration.http.chirpstack.ChirpStackIntegration");
            case MQTT:
                return newInstance("org.thingsboard.integration.mqtt.basic.BasicMqttIntegration");
            case AWS_IOT:
                return newInstance("org.thingsboard.integration.mqtt.aws.AwsIotIntegration");
            case PUB_SUB:
                return newInstance("org.thingsboard.gcloud.pubsub.PubSubIntegration");
            case IBM_WATSON_IOT:
                return newInstance("org.thingsboard.integration.mqtt.ibm.IbmWatsonIotIntegration");
            case TTI:
            case TTN:
                return newInstance("org.thingsboard.integration.mqtt.ttn.TtnIntegration");
            case AZURE_EVENT_HUB:
                return newInstance("org.thingsboard.integration.azure.AzureEventHubIntegration");
            case AZURE_IOT_HUB:
                return newInstance("org.thingsboard.integration.mqtt.azure.AzureIotHubIntegration");
            case OPC_UA:
                return newInstance("org.thingsboard.integration.opcua.OpcUaIntegration");
            case AWS_SQS:
                return newInstance("org.thingsboard.integration.aws.sqs.AwsSqsIntegration");
            case AWS_KINESIS:
                return newInstance("org.thingsboard.integration.aws.kinesis.AwsKinesisIntegration");
            case KAFKA:
                return newInstance("org.thingsboard.integration.kafka.basic.BasicKafkaIntegration");
            case RABBITMQ:
                return newInstance("org.thingsboard.integration.rabbitmq.basic.BasicRabbitMQIntegration");
            case APACHE_PULSAR:
                return newInstance("org.thingsboard.integration.apache.pulsar.basic.BasicPulsarIntegration");
            case TUYA:
                return newInstance("org.thingsboard.integration.tuya.TuyaIntegration");
            case COAP:
                return newInstance("org.thingsboard.integration.coap.CoapIntegration", param);
            case TCP:
                if (remote) {
                    return newInstance("org.thingsboard.integration.tcpip.tcp.BasicTcpIntegration");
                } else {
                    throw new RuntimeException("TCP Integration should be executed remotely!");
                }
            case UDP:
                if (remote) {
                    return newInstance("org.thingsboard.integration.tcpip.udp.BasicUdpIntegration");
                } else {
                    throw new RuntimeException("TCP Integration should be executed remotely!");
                }
            case CUSTOM:
                if (remote) {
                    return newInstance(configuration.get("clazz").asText());
                } else {
                    throw new RuntimeException("Custom Integrations should be executed remotely!");
                }
            default:
                throw new RuntimeException("Not Implemented!");
        }
    }

    private static ThingsboardPlatformIntegration<?> newInstance(String clazz) throws Exception {
        return newInstance(clazz, null);
    }

    private static ThingsboardPlatformIntegration<?> newInstance(String clazz, Object param) throws Exception {
        if (param != null) {
            return (ThingsboardPlatformIntegration<?>) Class.forName(clazz).getDeclaredConstructors()[0].newInstance(param);
        } else {
            return (ThingsboardPlatformIntegration<?>) Class.forName(clazz).getDeclaredConstructor().newInstance();
        }
    }

}
