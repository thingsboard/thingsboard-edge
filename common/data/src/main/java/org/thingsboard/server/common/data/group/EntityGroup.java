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
package org.thingsboard.server.common.data.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;

@Data
@NoArgsConstructor
public class EntityGroup extends BaseData<EntityGroupId> implements HasName, HasOwnerId {

    private static final long serialVersionUID = 2807349040519543363L;

    public static final EntityType[] groupTypes = new EntityType[]{
            EntityType.USER, EntityType.CUSTOMER, EntityType.DEVICE,
            EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.DASHBOARD
    };

    public static final EntityType[] sharableGroupTypes = new EntityType[]{
            EntityType.CUSTOMER, EntityType.DEVICE,
            EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.DASHBOARD
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

}
