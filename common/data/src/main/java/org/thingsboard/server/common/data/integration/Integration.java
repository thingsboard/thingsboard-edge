/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2019 ThingsBoard, Inc. All Rights Reserved.
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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasEntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;

@EqualsAndHashCode(callSuper = true)
public class Integration extends SearchTextBased<IntegrationId> implements HasName, TenantEntity {

    private static final long serialVersionUID = 4934987577236873728L;

    private TenantId tenantId;
    private ConverterId defaultConverterId;
    private ConverterId downlinkConverterId;
    private String name;
    private String routingKey;
    private IntegrationType type;
    private boolean debugMode;
    private boolean isRemote;
    private String secret;
    private transient JsonNode configuration;
    private transient JsonNode additionalInfo;

    public Integration() {
        super();
    }

    public Integration(IntegrationId id) {
        super(id);
    }

    public Integration(Integration integration) {
        super(integration);
        this.tenantId = integration.getTenantId();
        this.defaultConverterId = integration.getDefaultConverterId();
        this.downlinkConverterId = integration.getDownlinkConverterId();
        this.name = integration.getName();
        this.routingKey = integration.getRoutingKey();
        this.type = integration.getType();
        this.debugMode = integration.isDebugMode();
        this.isRemote = integration.isRemote();
        this.secret = integration.getSecret();
        this.configuration = integration.getConfiguration();
        this.additionalInfo = integration.getAdditionalInfo();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    public ConverterId getDefaultConverterId() {
        return defaultConverterId;
    }

    public void setDefaultConverterId(ConverterId defaultConverterId) {
        this.defaultConverterId = defaultConverterId;
    }

    public ConverterId getDownlinkConverterId() {
        return downlinkConverterId;
    }

    public void setDownlinkConverterId(ConverterId downlinkConverterId) {
        this.downlinkConverterId = downlinkConverterId;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public IntegrationType getType() {
        return type;
    }

    public void setType(IntegrationType type) {
        this.type = type;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public boolean isRemote() {
        return isRemote;
    }

    public void setRemote(boolean remote) {
        isRemote = remote;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        builder.append(", defaultConverterId=");
        builder.append(defaultConverterId);
        builder.append(", name=");
        builder.append(name);
        builder.append(", routingKey=");
        builder.append(routingKey);
        builder.append(", type=");
        builder.append(type);
        builder.append(", debugMode=");
        builder.append(debugMode);
        builder.append(", isRemote=");
        builder.append(isRemote);
        builder.append(", secret=");
        builder.append(secret);
        builder.append(", configuration=");
        builder.append(configuration);
        builder.append(", additionalInfo=");
        builder.append(additionalInfo);
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
