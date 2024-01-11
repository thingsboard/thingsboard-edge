/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

import lombok.Data;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.model.ToData;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.WIDGETS_BUNDLE_WIDGET_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_ORDER_PROPERTY;

@Data
@Entity
@Table(name = WIDGETS_BUNDLE_WIDGET_TABLE_NAME)
@IdClass(WidgetsBundleWidgetCompositeKey.class)
public final class WidgetsBundleWidgetEntity implements ToData<WidgetsBundleWidget> {

    @Id
    @Column(name = "widgets_bundle_id", columnDefinition = "uuid")
    private UUID widgetsBundleId;

    @Id
    @Column(name = "widget_type_id", columnDefinition = "uuid")
    private UUID widgetTypeId;

    @Column(name = WIDGET_TYPE_ORDER_PROPERTY)
    private int widgetTypeOrder;

    public WidgetsBundleWidgetEntity() {
        super();
    }

    public WidgetsBundleWidgetEntity(WidgetsBundleWidget widgetsBundleWidget) {
        widgetsBundleId = widgetsBundleWidget.getWidgetsBundleId().getId();
        widgetTypeId = widgetsBundleWidget.getWidgetTypeId().getId();
        widgetTypeOrder = widgetsBundleWidget.getWidgetTypeOrder();
    }

    public WidgetsBundleWidgetEntity(UUID widgetsBundleId, UUID widgetTypeId, int widgetTypeOrder) {
        this.widgetsBundleId = widgetsBundleId;
        this.widgetTypeId = widgetTypeId;
        this.widgetTypeOrder = widgetTypeOrder;
    }

    @Override
    public WidgetsBundleWidget toData() {
        WidgetsBundleWidget result = new WidgetsBundleWidget();
        result.setWidgetsBundleId(new WidgetsBundleId(widgetsBundleId));
        result.setWidgetTypeId(new WidgetTypeId(widgetTypeId));
        result.setWidgetTypeOrder(widgetTypeOrder);
        return result;
    }
}
