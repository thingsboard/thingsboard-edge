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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by nickAS21 on 12.12.22
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SparkplugTopic {

    /**
     * The Sparkplug namespace version.
     * For the Sparkplug™ B version of the specification, the UTF-8 string constant for the namespace element will be: “spBv1.0”
     */
    private String namespace;

    /**
     * The ID of the logical grouping of Edge of Network (EoN) Nodes and devices.
     */
    private String groupId;

    /**
     * The ID of the Edge of Network (EoN) Node.
     */
    private String edgeNodeId;

    /**
     * The ID of the device.
     */
    private String deviceId;

    /**
     * The message type.
     */
    private SparkplugMessageType type;

     /**
     * Constructor (device).
     *
     * @param namespace the namespace.
     * @param groupId the group ID.
     * @param edgeNodeId the edge node ID.
     * @param deviceId the device ID.
     * @param type the message type.
     */
    public SparkplugTopic(String namespace, String groupId, String edgeNodeId, String deviceId, SparkplugMessageType type) {
        super();
        this.namespace = namespace;
        this.groupId = groupId;
        this.edgeNodeId = edgeNodeId;
        this.deviceId = deviceId;
        this.type = type;
    }

    /**
     * Constructor (node).
     *
     * @param namespace the namespace.
     * @param groupId the group ID.
     * @param edgeNodeId the edge node ID.
     * @param type the message type.
     */
    public SparkplugTopic(String namespace, String groupId, String edgeNodeId, SparkplugMessageType type) {
        super();
        this.namespace = namespace;
        this.groupId = groupId;
        this.edgeNodeId = edgeNodeId;
        this.deviceId = null;
        this.type = type;
    }

    public SparkplugTopic(SparkplugTopic sparkplugTopic,  SparkplugMessageType type) {
        super();
        this.namespace = sparkplugTopic.namespace;
        this.groupId = sparkplugTopic.groupId;
        this.edgeNodeId = sparkplugTopic.edgeNodeId;
        this.deviceId = null;
        this.type = type;
    }
    public SparkplugTopic(SparkplugTopic sparkplugTopic,  SparkplugMessageType type, String deviceId) {
        super();
        this.namespace = sparkplugTopic.namespace;
        this.groupId = sparkplugTopic.groupId;
        this.edgeNodeId = sparkplugTopic.edgeNodeId;
        this.deviceId = deviceId;
        this.type = type;
    }

    /**
     * @return the Sparkplug namespace version
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the ID of the logical grouping of Edge of Network (EoN) Nodes and devices.
     *
     * @return the group ID
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @return  the ID of the Edge of Network (EoN) Node
     */
    public String getEdgeNodeId() {
        return edgeNodeId;
    }

    /**
     * @return the device ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * @return the message type
     */
    public SparkplugMessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getNamespace()).append("/")
                .append(getGroupId()).append("/")
                .append(getType()).append("/")
                .append(getEdgeNodeId());
        if (getDeviceId() != null) {
            sb.append("/").append(getDeviceId());
        }
        return sb.toString();
    }

    /**
     * @param type the type to check
     * @return true if this topic's type matches the passes in type, false otherwise
     */
    public boolean isType(SparkplugMessageType type) {
        return this.type != null && this.type.equals(type);
    }

    public boolean isNode() {
        return this.deviceId == null;
    }

    public String getNodeDeviceName() {
        return isNode() ? edgeNodeId : deviceId;
    }
}

