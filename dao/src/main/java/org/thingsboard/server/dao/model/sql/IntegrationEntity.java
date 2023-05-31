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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextSourceEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EXTERNAL_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_ALLOW_CREATE_DEVICES_OR_ASSETS;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_CONVERTER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_DEBUG_MODE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_DOWNLINK_CONVERTER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_ENABLED_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_IS_REMOTE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_ROUTING_KEY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_SECRET_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_TYPE_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = INTEGRATION_TABLE_NAME)
public class IntegrationEntity extends BaseSqlEntity<Integration> implements SearchTextSourceEntity<Integration> {

    @Column(name = INTEGRATION_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = INTEGRATION_NAME_PROPERTY)
    private String name;

    @Column(name = INTEGRATION_SECRET_PROPERTY)
    private String secret;

    @Column(name = INTEGRATION_CONVERTER_ID_PROPERTY)
    private UUID converterId;

    @Column(name = INTEGRATION_DOWNLINK_CONVERTER_ID_PROPERTY)
    private UUID downlinkConverterId;

    @Column(name = INTEGRATION_ROUTING_KEY_PROPERTY)
    private String routingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = INTEGRATION_TYPE_PROPERTY)
    private IntegrationType type;

    @Column(name = INTEGRATION_DEBUG_MODE_PROPERTY)
    private boolean debugMode;

    @Column(name = INTEGRATION_ENABLED_PROPERTY)
    private Boolean enabled;

    @Column(name = INTEGRATION_IS_REMOTE_PROPERTY)
    private Boolean isRemote;

    @Column(name = INTEGRATION_ALLOW_CREATE_DEVICES_OR_ASSETS)
    private Boolean allowCreateDevicesOrAssets;

    @Type(type = "json")
    @Column(name = ModelConstants.INTEGRATION_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Type(type = "json")
    @Column(name = ModelConstants.INTEGRATION_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    @Column(name = ModelConstants.INTEGRATION_IS_EDGE_TEMPLATE_MODE_PROPERTY)
    private boolean edgeTemplate;

    public IntegrationEntity() {
        super();
    }

    public IntegrationEntity(Integration integration) {
        this.createdTime = integration.getCreatedTime();
        if (integration.getId() != null) {
            this.setUuid(integration.getId().getId());
        }
        if (integration.getTenantId() != null) {
            this.tenantId = integration.getTenantId().getId();
        }
        if (integration.getDefaultConverterId() != null) {
            this.converterId = integration.getDefaultConverterId().getId();
        }
        if (integration.getDownlinkConverterId() != null) {
            this.downlinkConverterId = integration.getDownlinkConverterId().getId();
        }
        this.name = integration.getName();
        this.routingKey = integration.getRoutingKey();
        this.secret = integration.getSecret();
        this.type = integration.getType();
        this.debugMode = integration.isDebugMode();
        this.enabled = integration.isEnabled();
        this.isRemote = integration.isRemote();
        this.allowCreateDevicesOrAssets = integration.isAllowCreateDevicesOrAssets();
        this.configuration = integration.getConfiguration();
        this.additionalInfo = integration.getAdditionalInfo();
        if (integration.getExternalId() != null) {
            this.externalId = integration.getExternalId().getId();
        }
        this.edgeTemplate = integration.isEdgeTemplate();
    }

    @Override
    public String getSearchTextSource() {
        return name;
    }

    @Override
    public Integration toData() {
        Integration integration = new Integration(new IntegrationId(id));
        integration.setCreatedTime(this.createdTime);
        if (tenantId != null) {
            integration.setTenantId(new TenantId(tenantId));
        }
        if (converterId != null) {
            integration.setDefaultConverterId(new ConverterId(converterId));
        }
        if (downlinkConverterId != null) {
            integration.setDownlinkConverterId(new ConverterId(downlinkConverterId));
        }
        integration.setName(name);
        integration.setRoutingKey(routingKey);
        integration.setSecret(secret);
        integration.setType(type);
        integration.setDebugMode(debugMode);
        integration.setEnabled(enabled);
        integration.setRemote(isRemote);
        integration.setAllowCreateDevicesOrAssets(allowCreateDevicesOrAssets);
        integration.setConfiguration(configuration);
        integration.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            integration.setExternalId(new IntegrationId(externalId));
        }
        integration.setEdgeTemplate(edgeTemplate);
        return integration;
    }
}
