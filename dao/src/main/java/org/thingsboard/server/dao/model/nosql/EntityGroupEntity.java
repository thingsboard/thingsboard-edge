/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        entityGroup.setConfiguration(additionalInfo);
        return entityGroup;
    }

}