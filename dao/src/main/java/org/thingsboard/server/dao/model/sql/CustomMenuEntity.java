/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.MenuItem;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOM_MENU_ASSIGNEE_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOM_MENU_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOM_MENU_SCOPE;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;


@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Slf4j
@NoArgsConstructor
@Table(name = ModelConstants.CUSTOM_MENU_TABLE_NAME)
public class CustomMenuEntity extends BaseSqlEntity<CustomMenu> {

    @Column(name = TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    @Column(name = CUSTOM_MENU_NAME)
    private String name;

    @Column(name = CUSTOM_MENU_SCOPE)
    @Enumerated(EnumType.STRING)
    private CMScope scope;

    @Column(name = CUSTOM_MENU_ASSIGNEE_TYPE)
    @Enumerated(EnumType.STRING)
    private CMAssigneeType assigneeType;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.CUSTOM_MENU_SETTINGS)
    private JsonNode settings;

    public CustomMenuEntity(CustomMenu customMenu) {
        super(customMenu);
        this.tenantId = customMenu.getTenantId().getId();
        if (customMenu.getCustomerId() != null) {
            this.customerId = customMenu.getCustomerId().getId();
        } else {
            this.customerId = EntityId.NULL_UUID;
        }
        this.name = customMenu.getName();
        this.scope = customMenu.getScope();
        this.assigneeType = customMenu.getAssigneeType();
        this.settings = JacksonUtil.valueToTree(customMenu.getMenuItems());
    }

    @Override
    public CustomMenu toData() {
        CustomMenu customMenu = new CustomMenu(new CustomMenuId(id));
        customMenu.setCreatedTime(createdTime);
        customMenu.setTenantId(TenantId.fromUUID(tenantId));
        if (!EntityId.NULL_UUID.equals(customerId)) {
            customMenu.setCustomerId(new CustomerId(customerId));
        }
        customMenu.setName(name);
        customMenu.setScope(scope);
        customMenu.setAssigneeType(assigneeType);
        if (settings != null) {
            try {
                customMenu.setMenuItems(List.of(JacksonUtil.treeToValue(settings, MenuItem[].class)));
            } catch (IllegalArgumentException e) {
                log.error("Unable to read custom menu from JSON!", e);
                throw new IncorrectParameterException("Unable to read custom menu from JSON!");
            }
        }
       return customMenu;
    }
}
