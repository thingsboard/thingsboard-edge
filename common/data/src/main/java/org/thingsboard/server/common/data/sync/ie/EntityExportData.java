/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import lombok.Data;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.sync.JsonTbEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "entityType", include = As.EXISTING_PROPERTY, visible = true, defaultImpl = EntityExportData.class)
@JsonSubTypes({
        @Type(name = "DEVICE", value = DeviceExportData.class),
        @Type(name = "RULE_CHAIN", value = RuleChainExportData.class),
        @Type(name = "WIDGETS_BUNDLE", value = WidgetsBundleExportData.class),
        @Type(name = "ENTITY_GROUP", value = EntityGroupExportData.class),
        @Type(name = "ASSET", value = GroupEntityExportData.class),
        @Type(name = "DASHBOARD", value = GroupEntityExportData.class),
        @Type(name = "CUSTOMER", value = GroupEntityExportData.class),
        @Type(name = "ENTITY_VIEW", value = GroupEntityExportData.class)
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class EntityExportData<E extends ExportableEntity<? extends EntityId>> {

    public static final Comparator<EntityRelation> relationsComparator = Comparator
            .comparing(EntityRelation::getFrom, Comparator.comparing(EntityId::getId))
            .thenComparing(EntityRelation::getTo, Comparator.comparing(EntityId::getId))
            .thenComparing(EntityRelation::getTypeGroup)
            .thenComparing(EntityRelation::getType);

    public static final Comparator<AttributeExportData> attrComparator = Comparator
            .comparing(AttributeExportData::getKey).thenComparing(AttributeExportData::getLastUpdateTs);

    @JsonProperty(index = 2)
    @JsonTbEntity
    private E entity;
    @JsonProperty(index = 1)
    private EntityType entityType;

    @JsonProperty(index = 100)
    private List<EntityRelation> relations;
    @JsonProperty(index = 101)
    private Map<String, List<AttributeExportData>> attributes;

    public EntityExportData<E> sort() {
        if (relations != null && !relations.isEmpty()) {
            relations.sort(relationsComparator);
        }
        if (attributes != null && !attributes.isEmpty()) {
            attributes.values().forEach(list -> list.sort(attrComparator));
        }
        return this;
    }

    @JsonIgnore
    public EntityId getExternalId() {
        return entity.getExternalId() != null ? entity.getExternalId() : entity.getId();
    }

    @JsonIgnore
    public boolean hasCredentials() {
        return false;
    }

    @JsonIgnore
    public boolean hasAttributes() {
        return attributes != null;
    }

    @JsonIgnore
    public boolean hasRelations() {
        return relations != null;
    }

    @JsonIgnore
    public boolean hasPermissions() {
        return false;
    }

    @JsonIgnore
    public boolean hasGroupEntities() {
        return false;
    }

}

