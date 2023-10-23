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
package org.thingsboard.server.common.util;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import  java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProtoUtilsTest {

    TenantId tenantId = TenantId.fromUUID(UUID.fromString("35e10f77-16e7-424d-ae46-ee780f87ac4f"));
    EntityId entityId = new RuleChainId(UUID.fromString("c640b635-4f0f-41e6-b10b-25a86003094e"));
    @Test
    void toProtoComponentLifecycleMsg() {
        ComponentLifecycleMsg msg = new ComponentLifecycleMsg(tenantId, entityId, ComponentLifecycleEvent.UPDATED);

        TransportProtos.ComponentLifecycleMsgProto proto = ProtoUtils.toProto(msg);

        assertThat(proto).as("to proto").isEqualTo(TransportProtos.ComponentLifecycleMsgProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setEntityType(TransportProtos.EntityType.forNumber(entityId.getEntityType().ordinal()))
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setEvent(TransportProtos.ComponentLifecycleEvent.forNumber(ComponentLifecycleEvent.UPDATED.ordinal()))
                .build()
        );

        assertThat(ProtoUtils.fromProto(proto)).as("from proto").isEqualTo(msg);
    }

    @Test
    void fromProtoComponentLifecycleMsg() {
        TransportProtos.ComponentLifecycleMsgProto proto = TransportProtos.ComponentLifecycleMsgProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setEntityType(TransportProtos.EntityType.forNumber(entityId.getEntityType().ordinal()))
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setEvent(TransportProtos.ComponentLifecycleEvent.forNumber(ComponentLifecycleEvent.STARTED.ordinal()))
                .build();

        ComponentLifecycleMsg msg = ProtoUtils.fromProto(proto);

        assertThat(msg).as("from proto").isEqualTo(
                new ComponentLifecycleMsg(tenantId, entityId, ComponentLifecycleEvent.STARTED));

        assertThat(ProtoUtils.toProto(msg)).as("to proto").isEqualTo(proto);
    }

}