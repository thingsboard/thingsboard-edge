/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.ShortEntityGroupInfo;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
public class EntityGroup extends BaseData<EntityGroupId> implements HasName, HasOwnerId {

    private static final long serialVersionUID = 2807349040519543363L;

    public static final EntityType[] groupTypes = new EntityType[]{
            EntityType.USER, EntityType.CUSTOMER, EntityType.DEVICE,
            EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.EDGE, EntityType.DASHBOARD
    };

    public static final String GROUP_ALL_NAME = "All";
    public static final String GROUP_TENANT_USERS_NAME = "Tenant Users";
    public static final String GROUP_TENANT_ADMINS_NAME = "Tenant Administrators";
    public static final String GROUP_CUSTOMER_USERS_NAME = "Customer Users";
    public static final String GROUP_CUSTOMER_ADMINS_NAME = "Customer Administrators";
    public static final String GROUP_PUBLIC_USERS_NAME = "Public Users";

    @ApiModelProperty(required = true, allowableValues = "CUSTOMER,ASSET,DEVICE,USER,ENTITY_VIEW,DASHBOARD")
    private EntityType type;

    @ApiModelProperty(required = true)
    private String name;

    private EntityId ownerId;

    @JsonDeserialize(using = ConfigurationDeserializer.class)
    private JsonNode additionalInfo;

    @JsonDeserialize(using = ConfigurationDeserializer.class)
    private JsonNode configuration;

    @Getter
    @Setter
    private Set<ShortEntityGroupInfo> assignedEdgeGroups;

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
        this.assignedEdgeGroups = entityGroup.getAssignedEdgeGroups();
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

    public boolean isGroupAll() {
        return GROUP_ALL_NAME.equals(name);
    }

    @JsonIgnore
    public boolean isPublic() {
        if (getAdditionalInfo() != null && getAdditionalInfo().has("isPublic")) {
            return getAdditionalInfo().get("isPublic").asBoolean();
        }

        return false;
    }

    @JsonIgnore
    public ShortEntityGroupInfo toShortEntityGroupInfo() {
        return new ShortEntityGroupInfo(id, name);
    }

    public boolean isAssignedToEdgeGroup(EntityGroupId entityGroupId) {
        return EdgeUtils.isAssignedToEdgeGroup(this.assignedEdgeGroups, entityGroupId);
    }

    public ShortEntityGroupInfo getAssignedEdgeGroupInfo(EntityGroupId entityGroupId) {
        return EdgeUtils.getAssignedEdgeGroupInfo(this.assignedEdgeGroups, entityGroupId);
    }

    public boolean addAssignedEdgeGroup(ShortEntityGroupInfo entityGroup) {
        if (this.assignedEdgeGroups == null) {
            this.assignedEdgeGroups = new HashSet<>();
        }
        return EdgeUtils.addAssignedEdgeGroup(this.assignedEdgeGroups, entityGroup);
    }

    public boolean updateAssignedEdgeGroup(ShortEntityGroupInfo entityGroup) {
        return EdgeUtils.updateAssignedEdgeGroup(this.assignedEdgeGroups, entityGroup);
    }

    public boolean removeAssignedEdgeGroup(ShortEntityGroupInfo entityGroup) {
        return EdgeUtils.removeAssignedEdgeGroup(this.assignedEdgeGroups, entityGroup);
    }



}
