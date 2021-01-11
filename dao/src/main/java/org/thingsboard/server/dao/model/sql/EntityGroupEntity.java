/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_CONFIGURATION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_OWNER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_OWNER_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_GROUP_TYPE_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.ENTITY_GROUP_COLUMN_FAMILY_NAME)
public class EntityGroupEntity extends BaseSqlEntity<EntityGroup> implements BaseEntity<EntityGroup> {

    @Transient
    private static final long serialVersionUID = 8050086409213322856L;

    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_GROUP_TYPE_PROPERTY)
    private EntityType type;

    @Column(name = ENTITY_GROUP_NAME_PROPERTY)
    private String name;

    @Column(name = ENTITY_GROUP_OWNER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_GROUP_OWNER_TYPE_PROPERTY)
    private EntityType ownerType;

    @Type(type = "json")
    @Column(name = ENTITY_GROUP_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Type(type = "json")
    @Column(name = ENTITY_GROUP_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    public EntityGroupEntity () {
        super();
    }

    public EntityGroupEntity (EntityGroup entityGroup) {
        if (entityGroup.getId() != null) {
            this.setUuid(entityGroup.getId().getId());
        }
        this.createdTime = entityGroup.getCreatedTime();
        this.name = entityGroup.getName();
        this.type = entityGroup.getType();
        if (entityGroup.getOwnerId() != null) {
            this.ownerId = entityGroup.getOwnerId().getId();
            this.ownerType = entityGroup.getOwnerId().getEntityType();
        }
        this.additionalInfo = entityGroup.getAdditionalInfo();
        this.configuration = entityGroup.getConfiguration();
    }

    @Override
    public EntityGroup toData() {
        EntityGroup entityGroup = new EntityGroup(new EntityGroupId(getUuid()));
        entityGroup.setCreatedTime(createdTime);
        entityGroup.setName(name);
        entityGroup.setType(type);
        if (ownerId != null) {
            entityGroup.setOwnerId(EntityIdFactory.getByTypeAndUuid(ownerType, ownerId));
        }
        entityGroup.setAdditionalInfo(additionalInfo);
        entityGroup.setConfiguration(configuration);
        return entityGroup;
    }

}
