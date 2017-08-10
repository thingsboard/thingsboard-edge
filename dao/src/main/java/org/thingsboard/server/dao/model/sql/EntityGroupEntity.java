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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;

import static org.thingsboard.server.dao.model.ModelConstants.*;

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
            this.setId(entityGroup.getId().getId());
        }
        this.name = entityGroup.getName();
        this.type = entityGroup.getType();
        this.additionalInfo = entityGroup.getAdditionalInfo();
        this.configuration = entityGroup.getConfiguration();
    }

    @Override
    public EntityGroup toData() {
        EntityGroup entityGroup = new EntityGroup(new EntityGroupId(getId()));
        entityGroup.setCreatedTime(UUIDs.unixTimestamp(getId()));
        entityGroup.setName(name);
        entityGroup.setType(type);
        entityGroup.setAdditionalInfo(additionalInfo);
        entityGroup.setConfiguration(configuration);
        return entityGroup;
    }

}
