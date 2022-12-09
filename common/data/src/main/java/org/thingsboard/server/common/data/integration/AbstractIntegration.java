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
package org.thingsboard.server.common.data.integration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public abstract class AbstractIntegration extends SearchTextBased<IntegrationId> implements HasName, TenantEntity {

    private static final long serialVersionUID = 1934983577296873728L;

    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    private IntegrationType type;
    private boolean debugMode;
    private Boolean enabled;
    private Boolean isRemote;
    private Boolean allowCreateDevicesOrAssets;
    private boolean isEdgeTemplate;

    public AbstractIntegration() {
        super();
    }

    public AbstractIntegration(IntegrationId id) {
        super(id);
    }

    public AbstractIntegration(AbstractIntegration integration) {
        super(integration);
        this.tenantId = integration.getTenantId();
        this.name = integration.getName();
        this.type = integration.getType();
        this.debugMode = integration.isDebugMode();
        this.enabled = integration.isEnabled();
        this.isRemote = integration.isRemote();
        this.allowCreateDevicesOrAssets = integration.isAllowCreateDevicesOrAssets();
        this.isEdgeTemplate = integration.isEdgeTemplate();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Integration Id. " +
            "Specify this field to update the Integration. " +
            "Referencing non-existing Integration Id will cause error. " +
            "Omit this field to create new Integration.")
    @Override
    public IntegrationId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the integration creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(position = 7, required = true, value = "The type of the integration")
    public IntegrationType getType() {
        return type;
    }

    public void setType(IntegrationType type) {
        this.type = type;
    }

    @ApiModelProperty(position = 8, value = "Boolean flag to enable/disable saving received messages as debug events")
    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @ApiModelProperty(position = 9, value = "Boolean flag to enable/disable the integration")
    public Boolean isEnabled() {
        return !(enabled == null) && enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @ApiModelProperty(position = 10, value = "Boolean flag to enable/disable the integration to be executed remotely. Remote integration is launched in a separate microservice. " +
            "Local integration is executed by the platform core")
    public Boolean isRemote() {
        return !(isRemote == null) && isRemote;
    }

    public void setRemote(Boolean remote) {
        isRemote = remote;
    }

    @ApiModelProperty(position = 11, value = "Boolean flag to allow/disallow the integration to create devices or assets that send message and do not exist in the system yet")
    public Boolean isAllowCreateDevicesOrAssets() {
        return !(allowCreateDevicesOrAssets == null) && allowCreateDevicesOrAssets;
    }

    public void setAllowCreateDevicesOrAssets(Boolean allow) {
        allowCreateDevicesOrAssets = allow;
    }

    @ApiModelProperty(position = 15, required = true, value = "Integration Name", example = "Http Integration")
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(position = 16, value = "Boolean flag that specifies that is regular or edge template integration")
    public boolean isEdgeTemplate() {
        return isEdgeTemplate;
    }

    public void setEdgeTemplate(boolean edgeTemplate) {
        this.isEdgeTemplate = edgeTemplate;
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Integration [tenantId=");
        builder.append(tenantId);
        builder.append(", name=");
        builder.append(name);
        builder.append(", type=");
        builder.append(type);
        builder.append(", isRemote=");
        builder.append(isRemote);
        builder.append(", allowCreateDevicesOrAssets=");
        builder.append(allowCreateDevicesOrAssets);
        builder.append(", isEdgeTemplate=");
        builder.append(isEdgeTemplate);
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.INTEGRATION;
    }
}
