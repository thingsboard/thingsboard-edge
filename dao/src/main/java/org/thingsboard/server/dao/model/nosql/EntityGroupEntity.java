/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;
import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.type.EntityTypeCodec;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Table(name = ENTITY_GROUP_COLUMN_FAMILY_NAME)
public final class EntityGroupEntity implements BaseEntity<EntityGroup> {

    @Transient
    private static final long serialVersionUID = -1265181166806910152L;

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;

    @PartitionKey(value = 1)
    @Column(name = ENTITY_GROUP_TYPE_PROPERTY, codec = EntityTypeCodec.class)
    private EntityType type;

    @Column(name = ENTITY_GROUP_NAME_PROPERTY)
    private String name;

    @Column(name = ENTITY_GROUP_ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;

    @Column(name = ENTITY_GROUP_CONFIGURATION_PROPERTY, codec = JsonCodec.class)
    private JsonNode configuration;

    public EntityGroupEntity() {
        super();
    }

    public EntityGroupEntity(EntityGroup entityGroup) {
        if (entityGroup.getId() != null) {
            this.id = entityGroup.getId().getId();
        }
        this.type = entityGroup.getType();
        this.name = entityGroup.getName();
        this.additionalInfo = entityGroup.getAdditionalInfo();
        this.configuration = entityGroup.getConfiguration();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityGroupEntity that = (EntityGroupEntity) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (type != that.type) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (additionalInfo != null ? !additionalInfo.equals(that.additionalInfo) : that.additionalInfo != null)
            return false;
        return configuration != null ? configuration.equals(that.configuration) : that.configuration == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (additionalInfo != null ? additionalInfo.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EntityGroupEntity{");
        sb.append("id=").append(id);
        sb.append(", type=").append(type);
        sb.append(", name='").append(name).append('\'');
        sb.append(", additionalInfo=").append(additionalInfo);
        sb.append(", configuration=").append(configuration);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public EntityGroup toData() {
        EntityGroup entityGroup = new EntityGroup(new EntityGroupId(id));
        entityGroup.setCreatedTime(UUIDs.unixTimestamp(id));
        entityGroup.setType(type);
        entityGroup.setName(name);
        entityGroup.setAdditionalInfo(additionalInfo);
        entityGroup.setConfiguration(configuration);
        return entityGroup;
    }

}