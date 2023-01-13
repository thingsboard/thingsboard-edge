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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;

import java.util.HashMap;
import java.util.Map;

@ApiModel
@NoArgsConstructor
public class ShortEntityView implements HasId<EntityId>, HasName {

    @ApiModelProperty(position = 1, value = "Entity Id object", required = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private EntityId id;
    @ApiModelProperty(position = 2, value = "Map of entity fields that is configurable in the Entity Group", required = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private Map<String, String> properties = new HashMap<>();

    @JsonIgnore
    private boolean skipEntity = false;

    public ShortEntityView(EntityId id) {
        super();
        this.id = id;
    }

    @Override
    public EntityId getId() {
        return id;
    }

    @JsonAnyGetter
    public Map<String, String> properties() {
        return this.properties;
    }

    @JsonAnySetter
    public void put(String name, String value) {
        this.properties.put(name, value);
    }

    @ApiModelProperty(position = 3, value = "Name of the entity", required = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return this.properties.get(EntityField.NAME.name().toLowerCase());
    }

    public boolean isSkipEntity() {
        return skipEntity;
    }

    public void setSkipEntity(boolean skipEntity) {
        this.skipEntity = skipEntity;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ShortEntityView that = (ShortEntityView) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }
}
