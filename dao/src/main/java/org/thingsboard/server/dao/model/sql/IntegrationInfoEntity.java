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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Immutable;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextSourceEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_ALLOW_CREATE_DEVICES_OR_ASSETS;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_DEBUG_MODE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_ENABLED_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_IS_REMOTE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_VIEW_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Immutable
@Table(name = INTEGRATION_VIEW_NAME)
public class IntegrationInfoEntity extends BaseSqlEntity<IntegrationInfo> implements SearchTextSourceEntity<IntegrationInfo> {

    @Column(name = INTEGRATION_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = INTEGRATION_NAME_PROPERTY)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = INTEGRATION_TYPE_PROPERTY)
    private IntegrationType type;

    @Column(name = INTEGRATION_DEBUG_MODE_PROPERTY)
    private Boolean debugMode;

    @Column(name = INTEGRATION_ENABLED_PROPERTY)
    private Boolean enabled;

    @Column(name = INTEGRATION_IS_REMOTE_PROPERTY)
    private Boolean isRemote;

    @Column(name = INTEGRATION_ALLOW_CREATE_DEVICES_OR_ASSETS)
    private Boolean allowCreateDevicesOrAssets;

    @Column(name = ModelConstants.INTEGRATION_IS_EDGE_TEMPLATE_MODE_PROPERTY)
    private Boolean edgeTemplate;

    @Column(name = ModelConstants.INTEGRATION_VIEW_STATS_PROPERTY)
    private String stats;

    @Column(name = ModelConstants.INTEGRATION_VIEW_STATUS_PROPERTY)
    private String status;

    public IntegrationInfoEntity() {
        super();
    }

    public IntegrationInfoEntity(UUID id, Long createdTime, UUID tenantId, String name,
                                 String type, Boolean debugMode, Boolean enabled, Boolean isRemote,
                                 Boolean allowCreateDevicesOrAssets, Boolean edgeTemplate, String stats, String status) {
        this.id = id;
        this.createdTime = createdTime;
        this.tenantId = tenantId;
        this.name = name;
        this.type = IntegrationType.valueOf(type);
        this.debugMode = debugMode;
        this.enabled = enabled;
        this.isRemote = isRemote;
        this.allowCreateDevicesOrAssets = allowCreateDevicesOrAssets;
        this.edgeTemplate = edgeTemplate;
        this.stats = stats;
        this.status = status;
    }

    public IntegrationInfoEntity(Integration integration) {
        this.createdTime = integration.getCreatedTime();
        if (integration.getId() != null) {
            this.setUuid(integration.getId().getId());
        }
        if (integration.getTenantId() != null) {
            this.tenantId = integration.getTenantId().getId();
        }
        this.name = integration.getName();
        this.type = integration.getType();
        this.debugMode = integration.isDebugMode();
        this.enabled = integration.isEnabled();
        this.isRemote = integration.isRemote();
        this.allowCreateDevicesOrAssets = integration.isAllowCreateDevicesOrAssets();
        this.edgeTemplate = integration.isEdgeTemplate();
    }

    @Override
    public String getSearchTextSource() {
        return name;
    }

    @Override
    public IntegrationInfo toData() {
        IntegrationInfo integration = new IntegrationInfo(new IntegrationId(id));
        integration.setCreatedTime(this.createdTime);
        if (tenantId != null) {
            integration.setTenantId(new TenantId(tenantId));
        }
        integration.setName(name);
        integration.setType(type);
        integration.setDebugMode(debugMode);
        integration.setEnabled(enabled);
        integration.setRemote(isRemote);
        integration.setAllowCreateDevicesOrAssets(allowCreateDevicesOrAssets);
        integration.setEdgeTemplate(edgeTemplate);
        integration.setStats(StringUtils.isEmpty(stats) ?
                JacksonUtil.OBJECT_MAPPER.createArrayNode() : JacksonUtil.fromString(stats, ArrayNode.class));

        if (StringUtils.isNotEmpty(status)) {
            integration.setStatus(JacksonUtil.fromString(status, ObjectNode.class));
        }

        return integration;
    }
}
