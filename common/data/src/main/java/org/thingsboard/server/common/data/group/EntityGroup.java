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
package org.thingsboard.server.common.data.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableNoTenantIdEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityGroup extends BaseData<EntityGroupId> implements HasName, HasOwnerId, ExportableNoTenantIdEntity<EntityGroupId> {

    private static final long serialVersionUID = 2807349040519543363L;

    public static final EntityType[] groupTypes = new EntityType[]{
            EntityType.USER, EntityType.CUSTOMER, EntityType.DEVICE,
            EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.EDGE, EntityType.DASHBOARD
    };

    public static final EntityType[] sharableGroupTypes = new EntityType[]{
            EntityType.CUSTOMER, EntityType.DEVICE,
            EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.EDGE
    };

    public static final String GROUP_ALL_NAME = "All";
    public static final String GROUP_TENANT_USERS_NAME = "Tenant Users";
    public static final String GROUP_TENANT_ADMINS_NAME = "Tenant Administrators";
    public static final String GROUP_CUSTOMER_USERS_NAME = "Customer Users";
    public static final String GROUP_CUSTOMER_ADMINS_NAME = "Customer Administrators";
    public static final String GROUP_PUBLIC_USERS_NAME = "Public Users";

    public static final String GROUP_EDGE_ALL_STARTS_WITH = "[Edge]";
    public static final String GROUP_EDGE_ALL_ENDS_WITH = "All";
    public static final String GROUP_EDGE_ALL_NAME_PATTERN = GROUP_EDGE_ALL_STARTS_WITH + " %s " + GROUP_EDGE_ALL_ENDS_WITH;
    public static final String ENTITY_GROUP_TYPE_ALLOWABLE_VALUES = "CUSTOMER,ASSET,DEVICE,USER,ENTITY_VIEW,DASHBOARD,EDGE";
    public static final String EDGE_ENTITY_GROUP_TYPE_ALLOWABLE_VALUES = "ASSET,DEVICE,USER,ENTITY_VIEW,DASHBOARD";

    @ApiModelProperty(position = 5, required = true, allowableValues = ENTITY_GROUP_TYPE_ALLOWABLE_VALUES)
    private EntityType type;

    @ApiModelProperty(position = 4, required = true, value = "Name of the entity group", example = "Water meters")
    @NoXss
    @Length(fieldName = "name")
    private String name;

    @ApiModelProperty(position = 3, value = "JSON object with the owner of the group - Tenant or Customer Id.")
    private EntityId ownerId;

    @ApiModelProperty(position = 6, value = "Arbitrary JSON with additional information about the group")
    @JsonDeserialize(using = ConfigurationDeserializer.class)
    private JsonNode additionalInfo;

    @ApiModelProperty(position = 7, value = "JSON with the configuration for UI components: list of columns, settings, actions, etc ")
    @JsonDeserialize(using = ConfigurationDeserializer.class)
    private JsonNode configuration;

    private EntityGroupId externalId;

    public EntityGroup(EntityGroupId id) {
        super(id);
    }

    public EntityGroup(EntityGroup entityGroup) {
        super(entityGroup);
        this.type = entityGroup.getType();
        this.name = entityGroup.getName();
        this.ownerId = entityGroup.getOwnerId();
        this.additionalInfo = entityGroup.getAdditionalInfo();
        this.configuration = entityGroup.getConfiguration();
        this.externalId = entityGroup.getExternalId();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    public EntityId getOwnerId() {
        return ownerId;
    }

    @ApiModelProperty(position = 8, value = "Indicates special group 'All' that contains all entities and can't be deleted.")
    public boolean isGroupAll() {
        return GROUP_ALL_NAME.equals(name);
    }

    @ApiModelProperty(position = 9, value = "Indicates special edge group 'All' that contains all entities and can't be deleted.",
            accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public boolean isEdgeGroupAll() {
        return EdgeUtils.isEdgeGroupAll(name);
    }

    @JsonIgnore
    public boolean isPublic() {
        if (getAdditionalInfo() != null && getAdditionalInfo().has("isPublic")) {
            return getAdditionalInfo().get("isPublic").asBoolean();
        }
        return false;
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the entity group creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the EntityGroupId Id. " +
            "Specify this field to update the Entity Group. " +
            "Referencing non-existing Entity Group Id will cause error. " +
            "Omit this field to create new Entity Group." )
    @Override
    public EntityGroupId getId() {
        return super.getId();
    }
}
