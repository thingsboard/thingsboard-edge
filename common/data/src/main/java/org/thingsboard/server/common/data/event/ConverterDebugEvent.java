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
package org.thingsboard.server.common.data.event;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

@ToString
@EqualsAndHashCode(callSuper = true)
public class ConverterDebugEvent extends Event {

    @Builder
    public ConverterDebugEvent(TenantId tenantId, UUID entityId, String serviceId, UUID id, long ts,
                               String eventType, String inMsgType, String inMsg,
                               String outMsgType, String outMsg, String metadata, String error) {
        super(tenantId, entityId, serviceId, id, ts);
        this.eventType = eventType;
        this.inMsgType = inMsgType;
        this.inMsg = inMsg;
        this.outMsgType = outMsgType;
        this.outMsg = outMsg;
        this.metadata = metadata;
        this.error = error;
    }

    @Getter
    private final String eventType;
    @Getter
    private final String inMsgType;
    @Getter
    @Setter
    private String inMsg;
    @Getter
    private final String outMsgType;
    @Getter
    @Setter
    private String outMsg;
    @Getter
    @Setter
    private String metadata;
    @Getter
    @Setter
    private String error;

    @Override
    public EventType getType() {
        return EventType.DEBUG_CONVERTER;
    }

    @Override
    public EventInfo toInfo(EntityType entityType) {
        EventInfo eventInfo = super.toInfo(entityType);
        var json = (ObjectNode) eventInfo.getBody();
        json.put("type", eventType);
        putNotNull(json, "inMessageType", inMsgType);
        putNotNull(json, "in", inMsg);
        putNotNull(json, "outMessageType", outMsgType);
        putNotNull(json, "out", outMsg);
        putNotNull(json, "metadata", metadata);
        putNotNull(json, "error", error);
        return eventInfo;
    }

}
