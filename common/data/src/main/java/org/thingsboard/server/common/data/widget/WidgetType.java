/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.widget;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;

@EqualsAndHashCode(callSuper = true)
public class WidgetType extends BaseData<WidgetTypeId> implements HasTenantId {

    private static final long serialVersionUID = 8388684344603660756L;

    private TenantId tenantId;
    private String bundleAlias;
    private String alias;
    private String name;
    private transient JsonNode descriptor;

    public WidgetType() {
        super();
    }

    public WidgetType(WidgetTypeId id) {
        super(id);
    }

    public WidgetType(WidgetType widgetType) {
        super(widgetType);
        this.tenantId = widgetType.getTenantId();
        this.bundleAlias = widgetType.getBundleAlias();
        this.alias = widgetType.getAlias();
        this.name = widgetType.getName();
        this.descriptor = widgetType.getDescriptor();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    public String getBundleAlias() {
        return bundleAlias;
    }

    public void setBundleAlias(String bundleAlias) {
        this.bundleAlias = bundleAlias;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonNode getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(JsonNode descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WidgetType{");
        sb.append("tenantId=").append(tenantId);
        sb.append(", bundleAlias='").append(bundleAlias).append('\'');
        sb.append(", alias='").append(alias).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", descriptor=").append(descriptor);
        sb.append('}');
        return sb.toString();
    }

}
