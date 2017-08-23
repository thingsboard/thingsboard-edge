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
package org.thingsboard.server.common.data.group;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.EntityGroupId;

import io.swagger.annotations.ApiModelProperty;

public class EntityGroup extends BaseData<EntityGroupId> implements HasName {

    private static final long serialVersionUID = 2807349040519543363L;

    public static final String GROUP_ALL_NAME = "All";

    @ApiModelProperty(required = true, allowableValues = "CUSTOMER,ASSET,DEVICE")
    private EntityType type;

    @ApiModelProperty(required = true)
    private String name;

    private JsonNode additionalInfo;
    private JsonNode configuration;

    public EntityGroup() {
        super();
    }

    public EntityGroup(EntityGroupId id) {
        super(id);
    }

    public EntityGroup(EntityGroup entityGroup) {
        super(entityGroup);
        this.type = entityGroup.getType();
        this.name = entityGroup.getName();
        this.additionalInfo = entityGroup.getAdditionalInfo();
        this.configuration = entityGroup.getConfiguration();
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    @Override
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

    public boolean isGroupAll() {
        return GROUP_ALL_NAME.equals(name);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (additionalInfo != null ? additionalInfo.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EntityGroup that = (EntityGroup) o;

        if (type != that.type) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (additionalInfo != null ? !additionalInfo.equals(that.additionalInfo) : that.additionalInfo != null)
            return false;
        return configuration != null ? configuration.equals(that.configuration) : that.configuration == null;

    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EntityGroup{");
        sb.append("type=").append(type);
        sb.append(", name='").append(name).append('\'');
        sb.append(", additionalInfo=").append(additionalInfo);
        sb.append(", configuration=").append(configuration);
        sb.append('}');
        return sb.toString();
    }
}
