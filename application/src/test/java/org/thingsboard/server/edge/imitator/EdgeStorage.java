/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.edge.imitator;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.gen.edge.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.EdgeConfiguration;
import org.thingsboard.server.gen.edge.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.UpdateMsgType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
public class EdgeStorage {

    private EdgeConfiguration configuration;

    private CountDownLatch latch;

    private Map<UUID, EntityType> entities;
    private Map<String, AlarmStatus> alarms;
    private List<EntityRelation> relations;

    public EdgeStorage() {
        latch = new CountDownLatch(0);
        entities = new HashMap<>();
        alarms = new HashMap<>();
        relations = new ArrayList<>();
    }

    public ListenableFuture<Void> processEntity(UpdateMsgType msgType, EntityType type, UUID uuid) {
        switch (msgType) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                entities.put(uuid, type);
                latch.countDown();
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                if (entities.remove(uuid) != null) {
                    latch.countDown();
                }
                break;
        }
        return Futures.immediateFuture(null);
    }

    public ListenableFuture<Void> processRelation(RelationUpdateMsg relationMsg) {
        boolean result = false;
        EntityRelation relation = new EntityRelation();
        relation.setType(relationMsg.getType());
        relation.setTypeGroup(RelationTypeGroup.valueOf(relationMsg.getTypeGroup()));
        relation.setTo(EntityIdFactory.getByTypeAndUuid(relationMsg.getToEntityType(), new UUID(relationMsg.getToIdMSB(), relationMsg.getToIdLSB())));
        relation.setFrom(EntityIdFactory.getByTypeAndUuid(relationMsg.getFromEntityType(), new UUID(relationMsg.getFromIdMSB(), relationMsg.getFromIdLSB())));
        switch (relationMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
                result = relations.add(relation);
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                result = relations.remove(relation);
                break;
        }
        if (result) {
            latch.countDown();
        }
        return Futures.immediateFuture(null);
    }

    public ListenableFuture<Void> processAlarm(AlarmUpdateMsg alarmMsg) {
        switch (alarmMsg.getMsgType()) {
            case ENTITY_CREATED_RPC_MESSAGE:
            case ENTITY_UPDATED_RPC_MESSAGE:
            case ALARM_ACK_RPC_MESSAGE:
            case ALARM_CLEAR_RPC_MESSAGE:
                alarms.put(alarmMsg.getType(), AlarmStatus.valueOf(alarmMsg.getStatus()));
                latch.countDown();
                break;
            case ENTITY_DELETED_RPC_MESSAGE:
                if (alarms.remove(alarmMsg.getName()) != null) {
                    latch.countDown();
                }
                break;
        }
        return Futures.immediateFuture(null);
    }

    public Set<UUID> getEntitiesByType(EntityType type) {
        return entities.entrySet().stream()
                .filter(entry -> entry.getValue().equals(type))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).keySet();
    }

    public void waitForMessages() throws InterruptedException {
        latch.await(5, TimeUnit.SECONDS);
    }

    public void expectMessageAmount(int messageAmount) {
        latch = new CountDownLatch(messageAmount);
    }

}
