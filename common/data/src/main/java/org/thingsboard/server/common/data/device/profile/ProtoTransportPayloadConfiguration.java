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
package org.thingsboard.server.common.data.device.profile;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.squareup.wire.schema.Location;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;

@Slf4j
@Data
public class ProtoTransportPayloadConfiguration implements TransportPayloadTypeConfiguration {

    public static final Location LOCATION = new Location("", "", -1, -1);
    public static final String ATTRIBUTES_PROTO_SCHEMA = "attributes proto schema";
    public static final String TELEMETRY_PROTO_SCHEMA = "telemetry proto schema";
    public static final String RPC_RESPONSE_PROTO_SCHEMA = "rpc response proto schema";
    public static final String RPC_REQUEST_PROTO_SCHEMA = "rpc request proto schema";
    private static final String PROTO_3_SYNTAX = "proto3";

    private String deviceTelemetryProtoSchema;
    private String deviceAttributesProtoSchema;
    private String deviceRpcRequestProtoSchema;
    private String deviceRpcResponseProtoSchema;

    private boolean enableCompatibilityWithJsonPayloadFormat;
    private boolean useJsonPayloadFormatForDefaultDownlinkTopics;

    @Override
    public TransportPayloadType getTransportPayloadType() {
        return TransportPayloadType.PROTOBUF;
    }

    public Descriptors.Descriptor getTelemetryDynamicMessageDescriptor(String deviceTelemetryProtoSchema) {
        return DynamicProtoUtils.getDescriptor(deviceTelemetryProtoSchema, TELEMETRY_PROTO_SCHEMA);
    }

    public Descriptors.Descriptor getAttributesDynamicMessageDescriptor(String deviceAttributesProtoSchema) {
        return DynamicProtoUtils.getDescriptor(deviceAttributesProtoSchema, ATTRIBUTES_PROTO_SCHEMA);
    }

    public Descriptors.Descriptor getRpcResponseDynamicMessageDescriptor(String deviceRpcResponseProtoSchema) {
        return DynamicProtoUtils.getDescriptor(deviceRpcResponseProtoSchema, RPC_RESPONSE_PROTO_SCHEMA);
    }

    public DynamicMessage.Builder getRpcRequestDynamicMessageBuilder(String deviceRpcRequestProtoSchema) {
        return DynamicProtoUtils.getDynamicMessageBuilder(deviceRpcRequestProtoSchema, RPC_REQUEST_PROTO_SCHEMA);
    }

    public String getDeviceRpcResponseProtoSchema() {
        if (StringUtils.isNotEmpty(deviceRpcResponseProtoSchema)) {
            return deviceRpcResponseProtoSchema;
        } else {
            return "syntax =\"proto3\";\n" +
                    "package rpc;\n" +
                    "\n" +
                    "message RpcResponseMsg {\n" +
                    "  optional string payload = 1;\n" +
                    "}";
        }
    }

    public String getDeviceRpcRequestProtoSchema() {
        if (StringUtils.isNotEmpty(deviceRpcRequestProtoSchema)) {
            return deviceRpcRequestProtoSchema;
        } else {
            return "syntax =\"proto3\";\n" +
                    "package rpc;\n" +
                    "\n" +
                    "message RpcRequestMsg {\n" +
                    "  optional string method = 1;\n" +
                    "  optional int32 requestId = 2;\n" +
                    "  optional string params = 3;\n" +
                    "}";
        }
    }

}
