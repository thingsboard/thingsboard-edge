/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.attributes;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.attributes.AttributesDao;
import org.thingsboard.server.dao.model.sql.AttributeKvCompositeKey;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;

@Component
@Slf4j
@SqlDao
public class JpaAttributeDao extends JpaAbstractDaoListeningExecutorService implements AttributesDao {

    @Autowired
    private AttributeKvRepository attributeKvRepository;

    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(EntityId entityId, String attributeType, String attributeKey) {
        AttributeKvCompositeKey compositeKey =
                getAttributeKvCompositeKey(entityId, attributeType, attributeKey);
        return Futures.immediateFuture(
                Optional.ofNullable(DaoUtil.getData(attributeKvRepository.findOne(compositeKey))));
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(EntityId entityId, String attributeType, Collection<String> attributeKeys) {
        List<AttributeKvCompositeKey> compositeKeys =
                attributeKeys
                        .stream()
                        .map(attributeKey ->
                                getAttributeKvCompositeKey(entityId, attributeType, attributeKey))
                        .collect(Collectors.toList());
        return Futures.immediateFuture(
                DaoUtil.convertDataList(Lists.newArrayList(attributeKvRepository.findAll(compositeKeys))));
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(EntityId entityId, String attributeType) {
        return Futures.immediateFuture(
                DaoUtil.convertDataList(Lists.newArrayList(
                        attributeKvRepository.findAllByEntityTypeAndEntityIdAndAttributeType(
                                entityId.getEntityType(),
                                UUIDConverter.fromTimeUUID(entityId.getId()),
                                attributeType))));
    }

    @Override
    public ListenableFuture<Void> save(EntityId entityId, String attributeType, AttributeKvEntry attribute) {
        AttributeKvEntity entity = new AttributeKvEntity();
        entity.setEntityType(entityId.getEntityType());
        entity.setEntityId(fromTimeUUID(entityId.getId()));
        entity.setAttributeType(attributeType);
        entity.setAttributeKey(attribute.getKey());
        entity.setLastUpdateTs(attribute.getLastUpdateTs());
        entity.setStrValue(attribute.getStrValue().orElse(null));
        entity.setDoubleValue(attribute.getDoubleValue().orElse(null));
        entity.setLongValue(attribute.getLongValue().orElse(null));
        entity.setBooleanValue(attribute.getBooleanValue().orElse(null));
        return service.submit(() -> {
            attributeKvRepository.save(entity);
            return null;
        });
    }

    @Override
    public ListenableFuture<List<Void>> removeAll(EntityId entityId, String attributeType, List<String> keys) {
        List<AttributeKvEntity> entitiesToDelete = keys
                .stream()
                .map(key -> {
                    AttributeKvEntity entityToDelete = new AttributeKvEntity();
                    entityToDelete.setEntityType(entityId.getEntityType());
                    entityToDelete.setEntityId(fromTimeUUID(entityId.getId()));
                    entityToDelete.setAttributeType(attributeType);
                    entityToDelete.setAttributeKey(key);
                    return entityToDelete;
                }).collect(Collectors.toList());

        return service.submit(() -> {
            attributeKvRepository.delete(entitiesToDelete);
            return null;
        });
    }

    private AttributeKvCompositeKey getAttributeKvCompositeKey(EntityId entityId, String attributeType, String attributeKey) {
        return new AttributeKvCompositeKey(
                entityId.getEntityType(),
                fromTimeUUID(entityId.getId()),
                attributeType,
                attributeKey);
    }
}
