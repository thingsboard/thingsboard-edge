/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOM_MENU_ASSIGNEE_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOM_MENU_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOM_MENU_SCOPE;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;


@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractCustomMenuEntity<T extends CustomMenuInfo> extends BaseSqlEntity<T> {

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

    public AbstractCustomMenuEntity() {
        super();
    }

    public AbstractCustomMenuEntity(T customMenuInfo) {
        super(customMenuInfo);
        if (customMenuInfo.getTenantId() != null) {
            this.tenantId = customMenuInfo.getTenantId().getId();
        }
        if (customMenuInfo.getCustomerId() != null) {
            this.customerId = customMenuInfo.getCustomerId().getId();
        }
        this.name = customMenuInfo.getName();
        this.scope = customMenuInfo.getScope();
        this.assigneeType = customMenuInfo.getAssigneeType();
    }

    public AbstractCustomMenuEntity(CustomMenuInfoEntity customMenuInfoEntity) {
        super(customMenuInfoEntity);
        this.tenantId = customMenuInfoEntity.getTenantId();
        this.customerId = customMenuInfoEntity.getCustomerId();
        this.name = customMenuInfoEntity.getName();
        this.scope = customMenuInfoEntity.getScope();
        this.assigneeType = customMenuInfoEntity.getAssigneeType();
    }

    protected CustomMenuInfo toCustomMenuInfo() {
        CustomMenuInfo customMenuInfo = new CustomMenuInfo(new CustomMenuId(id));
        customMenuInfo.setCreatedTime(createdTime);
        if (tenantId != null) {
            customMenuInfo.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            customMenuInfo.setCustomerId(new CustomerId(customerId));
        }
        customMenuInfo.setName(name);
        customMenuInfo.setScope(scope);
        customMenuInfo.setAssigneeType(assigneeType);
        return customMenuInfo;
    }
}
