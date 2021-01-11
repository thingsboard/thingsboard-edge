/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.ROLE_TABLE_FAMILY_NAME)
@Slf4j
public class RoleEntity extends BaseSqlEntity<Role> implements SearchTextEntity<Role> {

    @Column(name = ROLE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ROLE_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = ROLE_TYPE_PROPERTY)
    private RoleType type;

    @Column(name = ROLE_NAME_PROPERTY)
    private String name;

    @Type(type = "json")
    @Column(name = ROLE_PERMISSIONS_PROPERTY)
    private JsonNode permissions;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Type(type = "json")
    @Column(name = ModelConstants.ENTITY_VIEW_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    private static final ObjectMapper mapper = new ObjectMapper();

    public RoleEntity() {
        super();
    }

    public RoleEntity(Role role) {
        if (role.getId() != null) {
            this.setUuid(role.getId().getId());
        }
        this.createdTime = role.getCreatedTime();
        if (role.getTenantId() != null) {
            this.tenantId = role.getTenantId().getId();
        }
        if (role.getCustomerId() != null) {
            this.customerId = role.getCustomerId().getId();
        }
        this.type = role.getType();
        this.name = role.getName();
        this.permissions = role.getPermissions();
        this.searchText = role.getSearchText();
        this.additionalInfo = role.getAdditionalInfo();
    }

    @Override
    public String getSearchTextSource() {
        return name;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public Role toData() {
        Role role = new Role(new RoleId(getUuid()));
        role.setCreatedTime(createdTime);

        if (tenantId != null) {
            role.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            role.setCustomerId(new CustomerId(customerId));
        }
        role.setType(type);
        role.setName(name);
        role.setPermissions(permissions);
        role.setAdditionalInfo(additionalInfo);
        return role;
    }
}
