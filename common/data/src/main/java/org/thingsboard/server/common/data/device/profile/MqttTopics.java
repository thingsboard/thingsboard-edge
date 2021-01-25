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
package org.thingsboard.server.common.data.device.profile;

/**
 * Created by ashvayka on 19.01.17.
 */
public class MqttTopics {

    private static final String REQUEST = "/request";
    private static final String RESPONSE = "/response";
    private static final String RPC = "/rpc";
    private static final String CONNECT = "/connect";
    private static final String DISCONNECT = "/disconnect";
    private static final String TELEMETRY = "/telemetry";
    private static final String ATTRIBUTES = "/attributes";
    private static final String CLAIM = "/claim";
    private static final String SUB_TOPIC = "+";
    private static final String PROVISION = "/provision";

    private static final String ATTRIBUTES_RESPONSE = ATTRIBUTES + RESPONSE;
    private static final String ATTRIBUTES_REQUEST = ATTRIBUTES + REQUEST;

    private static final String DEVICE_RPC_RESPONSE = RPC + RESPONSE + "/";
    private static final String DEVICE_RPC_REQUEST = RPC + REQUEST + "/";

    private static final String DEVICE_ATTRIBUTES_RESPONSE = ATTRIBUTES_RESPONSE + "/";
    private static final String DEVICE_ATTRIBUTES_REQUEST = ATTRIBUTES_REQUEST + "/";

    // V1_JSON topics

    public static final String BASE_DEVICE_API_TOPIC = "v1/devices/me";

    public static final String DEVICE_RPC_RESPONSE_TOPIC = BASE_DEVICE_API_TOPIC + DEVICE_RPC_RESPONSE;
    public static final String DEVICE_RPC_RESPONSE_SUB_TOPIC = DEVICE_RPC_RESPONSE_TOPIC + SUB_TOPIC;
    public static final String DEVICE_RPC_REQUESTS_TOPIC = BASE_DEVICE_API_TOPIC + DEVICE_RPC_REQUEST;
    public static final String DEVICE_RPC_REQUESTS_SUB_TOPIC = DEVICE_RPC_REQUESTS_TOPIC + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX = BASE_DEVICE_API_TOPIC + DEVICE_ATTRIBUTES_RESPONSE;
    public static final String DEVICE_ATTRIBUTES_RESPONSES_TOPIC = DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX + SUB_TOPIC;
    public static final String DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX = BASE_DEVICE_API_TOPIC + DEVICE_ATTRIBUTES_REQUEST;
    public static final String DEVICE_TELEMETRY_TOPIC = BASE_DEVICE_API_TOPIC + TELEMETRY;
    public static final String DEVICE_CLAIM_TOPIC = BASE_DEVICE_API_TOPIC + CLAIM;
    public static final String DEVICE_ATTRIBUTES_TOPIC = BASE_DEVICE_API_TOPIC + ATTRIBUTES;
    public static final String DEVICE_PROVISION_REQUEST_TOPIC = PROVISION + REQUEST;
    public static final String DEVICE_PROVISION_RESPONSE_TOPIC = PROVISION + RESPONSE;

    // V1_JSON gateway topics

    public static final String BASE_GATEWAY_API_TOPIC = "v1/gateway";
    public static final String GATEWAY_CONNECT_TOPIC = BASE_GATEWAY_API_TOPIC + CONNECT;
    public static final String GATEWAY_DISCONNECT_TOPIC = BASE_GATEWAY_API_TOPIC + DISCONNECT;
    public static final String GATEWAY_ATTRIBUTES_TOPIC = BASE_GATEWAY_API_TOPIC + ATTRIBUTES;
    public static final String GATEWAY_TELEMETRY_TOPIC = BASE_GATEWAY_API_TOPIC + TELEMETRY;
    public static final String GATEWAY_CLAIM_TOPIC = BASE_GATEWAY_API_TOPIC + CLAIM;
    public static final String GATEWAY_RPC_TOPIC = BASE_GATEWAY_API_TOPIC + RPC;
    public static final String GATEWAY_ATTRIBUTES_REQUEST_TOPIC = BASE_GATEWAY_API_TOPIC + ATTRIBUTES_REQUEST;
    public static final String GATEWAY_ATTRIBUTES_RESPONSE_TOPIC = BASE_GATEWAY_API_TOPIC + ATTRIBUTES_RESPONSE;

    private MqttTopics() {
    }
}
