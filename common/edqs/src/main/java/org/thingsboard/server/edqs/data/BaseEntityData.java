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
package org.thingsboard.server.edqs.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.EntityFields;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.edqs.data.dp.BoolDataPoint;
import org.thingsboard.server.common.data.edqs.DataPoint;
import org.thingsboard.server.edqs.data.dp.LongDataPoint;
import org.thingsboard.server.edqs.data.dp.StringDataPoint;
import org.thingsboard.server.edqs.query.DataKey;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ToString
public abstract class BaseEntityData<T extends EntityFields> implements EntityData<T> {

    @Getter
    private final UUID id;
    @Getter
    protected final Map<Integer, DataPoint> serverAttrMap;
    @Getter
    private final Map<Integer, DataPoint> tMap;

    @Getter
    @Setter
    private volatile UUID customerId;

    @Setter
    protected TenantRepo repo;

    @Getter
    @Setter
    protected volatile T fields;

    public BaseEntityData(UUID id) {
        this.id = id;
        this.serverAttrMap = new ConcurrentHashMap<>();
        this.tMap = new ConcurrentHashMap<>();
    }

    @Override
    public DataPoint getAttr(Integer keyId, EntityKeyType entityKeyType) {
        return switch (entityKeyType) {
            case ATTRIBUTE, SERVER_ATTRIBUTE -> serverAttrMap.get(keyId);
            default -> null;
        };
    }

    @Override
    public boolean putAttr(Integer keyId, AttributeScope scope, DataPoint value) {
        return serverAttrMap.put(keyId, value) == null;
    }

    @Override
    public boolean removeAttr(Integer keyId, AttributeScope scope) {
        return serverAttrMap.remove(keyId) != null;
    }

    @Override
    public DataPoint getTs(Integer keyId) {
        return tMap.get(keyId);
    }

    @Override
    public boolean putTs(Integer keyId, DataPoint value) {
        return tMap.put(keyId, value) == null;
    }

    @Override
    public boolean removeTs(Integer keyId) {
        return tMap.remove(keyId) != null;
    }

    @Override
    public EntityType getOwnerType() {
        return customerId != null ? EntityType.CUSTOMER : EntityType.TENANT;
    }

    @Override
    public DataPoint getDataPoint(DataKey key, QueryContext ctx) {
        return switch (key.type()) {
            case TIME_SERIES -> getTs(key.keyId());
            case ATTRIBUTE, SERVER_ATTRIBUTE, CLIENT_ATTRIBUTE, SHARED_ATTRIBUTE -> getAttr(key.keyId(), key.type());
            case ENTITY_FIELD -> getField(key, ctx);
            default -> throw new RuntimeException(key.type() + " not supported");
        };
    }

    private DataPoint getField(DataKey newKey, QueryContext ctx) {
        if (fields == null) {
            return null;
        }
        String key = newKey.key();
        return switch (key) {
            case "createdTime" -> new LongDataPoint(System.currentTimeMillis(), fields.getCreatedTime());
            case "edgeTemplate" -> new BoolDataPoint(System.currentTimeMillis(), fields.isEdgeTemplate());
            case "parentId" -> new StringDataPoint(System.currentTimeMillis(), getRelatedParentId(ctx));
            default -> new StringDataPoint(System.currentTimeMillis(), getField(key), false);
        };
    }

    @Override
    public String getField(String name) {
        if (fields == null) {
            return null;
        }
        return switch (name) {
            case "name" -> getEntityName();
            case "ownerName" -> getEntityOwnerName();
            case "ownerType" -> customerId != null ? EntityType.CUSTOMER.name() : EntityType.TENANT.name();
            case "entityType" -> Optional.ofNullable(getEntityType()).map(EntityType::name).orElse("");
            default -> fields.getAsString(name);
        };
    }

    public String getEntityOwnerName() {
        return repo.getOwnerName(getCustomerId() == null || CustomerId.NULL_UUID.equals(getCustomerId()) ? null :
                new CustomerId(getCustomerId()));
    }

    public String getEntityName() {
        return getFields().getName();
    }

    private String getRelatedParentId(QueryContext ctx) {
        return Optional.ofNullable(ctx.getRelatedParentIdMap().get(getId()))
                .map(UUID::toString)
                .orElse("");
    }

    @Override
    public EntityType getEntityType() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return fields == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntityData<?> that = (BaseEntityData<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
