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
import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;

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

    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    public EntityGroupEntity() {
        super();
    }

    public EntityGroupEntity(EntityGroup entityGroup) {
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
        if (entityGroup.getExternalId() != null) {
            this.externalId = entityGroup.getExternalId().getId();
        }
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
        if (externalId != null) {
            entityGroup.setExternalId(new EntityGroupId(externalId));
        }
        return entityGroup;
    }

}
