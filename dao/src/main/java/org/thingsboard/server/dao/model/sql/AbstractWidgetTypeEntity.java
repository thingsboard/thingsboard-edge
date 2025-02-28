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
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractWidgetTypeEntity<T extends BaseWidgetType> extends BaseVersionedEntity<T> {

    @Column(name = ModelConstants.WIDGET_TYPE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.WIDGET_TYPE_FQN_PROPERTY)
    private String fqn;

    @Column(name = ModelConstants.WIDGET_TYPE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.WIDGET_TYPE_DEPRECATED_PROPERTY)
    private boolean deprecated;

    @Column(name = ModelConstants.WIDGET_TYPE_SCADA_PROPERTY)
    private boolean scada;

    public AbstractWidgetTypeEntity() {
        super();
    }

    public AbstractWidgetTypeEntity(T widgetType) {
        super(widgetType);
        if (widgetType.getTenantId() != null) {
            this.tenantId = widgetType.getTenantId().getId();
        }
        this.fqn = widgetType.getFqn();
        this.name = widgetType.getName();
        this.deprecated = widgetType.isDeprecated();
        this.scada = widgetType.isScada();
    }

    public AbstractWidgetTypeEntity(AbstractWidgetTypeEntity widgetTypeEntity) {
        super(widgetTypeEntity);
        this.tenantId = widgetTypeEntity.getTenantId();
        this.fqn = widgetTypeEntity.getFqn();
        this.name = widgetTypeEntity.getName();
        this.deprecated = widgetTypeEntity.isDeprecated();
        this.scada = widgetTypeEntity.isScada();
    }

    protected BaseWidgetType toBaseWidgetType() {
        BaseWidgetType widgetType = new BaseWidgetType(new WidgetTypeId(getUuid()));
        widgetType.setCreatedTime(createdTime);
        widgetType.setVersion(version);
        if (tenantId != null) {
            widgetType.setTenantId(TenantId.fromUUID(tenantId));
        }
        widgetType.setFqn(fqn);
        widgetType.setName(name);
        widgetType.setDeprecated(deprecated);
        widgetType.setScada(scada);
        return widgetType;
    }

}
