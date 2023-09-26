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
package org.thingsboard.server.dao.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.dao.DaoUtil;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 13.07.17.
 */
@Data
@MappedSuperclass
public abstract class BaseSqlEntity<D> implements BaseEntity<D> {

    @Id
    @Column(name = ModelConstants.ID_PROPERTY, columnDefinition = "uuid")
    protected UUID id;

    @Column(name = ModelConstants.CREATED_TIME_PROPERTY, updatable = false)
    protected long createdTime;

    @Override
    public UUID getUuid() {
        return id;
    }

    @Override
    public void setUuid(UUID id) {
        this.id = id;
    }

    @Override
    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        if (createdTime > 0) {
            this.createdTime = createdTime;
        }
    }

    protected static UUID getUuid(UUIDBased uuidBased) {
        if (uuidBased != null) {
            return uuidBased.getId();
        } else {
            return null;
        }
    }

    protected static UUID getTenantUuid(TenantId tenantId) {
        if (tenantId != null) {
            return tenantId.getId();
        } else {
            return EntityId.NULL_UUID;
        }
    }

    protected static <I> I getEntityId(UUID uuid, Function<UUID, I> creator) {
        return DaoUtil.toEntityId(uuid, creator);
    }

    protected static TenantId getTenantId(UUID uuid) {
        if (uuid != null && !uuid.equals(EntityId.NULL_UUID)) {
            return TenantId.fromUUID(uuid);
        } else {
            return TenantId.SYS_TENANT_ID;
        }
    }

    protected JsonNode toJson(Object value) {
        if (value != null) {
            return JacksonUtil.valueToTree(value);
        } else {
            return null;
        }
    }

    protected <T> T fromJson(JsonNode json, Class<T> type) {
        return JacksonUtil.convertValue(json, type);
    }

    protected String listToString(List<?> list) {
        if (list != null) {
            return StringUtils.join(list, ',');
        } else {
            return "";
        }
    }

    protected <E> List<E> listFromString(String string, Function<String, E> mappingFunction) {
        if (string != null) {
            return Arrays.stream(StringUtils.split(string, ','))
                    .filter(StringUtils::isNotBlank)
                    .map(mappingFunction).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

}
