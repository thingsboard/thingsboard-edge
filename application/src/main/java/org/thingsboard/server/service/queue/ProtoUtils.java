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
package org.thingsboard.server.service.queue;

import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

public class ProtoUtils {

    public static TransportProtos.ComponentLifecycleMsgProto toProto(ComponentLifecycleMsg msg) {
        return TransportProtos.ComponentLifecycleMsgProto.newBuilder()
                .setTenantIdMSB(msg.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getTenantId().getId().getLeastSignificantBits())
                .setEntityType(TransportProtos.EntityType.forNumber(msg.getEntityId().getEntityType().ordinal()))
                .setEntityIdMSB(msg.getEntityId().getId().getMostSignificantBits())
                .setEntityIdLSB(msg.getEntityId().getId().getLeastSignificantBits())
                .setEvent(TransportProtos.ComponentLifecycleEvent.forNumber(msg.getEvent().ordinal()))
                .build();
    }

    public static ComponentLifecycleMsg fromProto(TransportProtos.ComponentLifecycleMsgProto proto) {
        return new ComponentLifecycleMsg(
                TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())),
                EntityIdFactory.getByTypeAndUuid(proto.getEntityTypeValue(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB())),
                ComponentLifecycleEvent.values()[proto.getEventValue()]
        );
    }

}
