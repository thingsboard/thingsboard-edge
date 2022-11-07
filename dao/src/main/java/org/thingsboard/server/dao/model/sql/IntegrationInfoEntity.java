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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_ALLOW_CREATE_DEVICES_OR_ASSETS;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_DEBUG_MODE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_ENABLED_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_IS_REMOTE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.INTEGRATION_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;
import static org.thingsboard.server.dao.sql.integration.IntegrationInfoRepository.FIND_ALL_INTEGRATION_INFOS_WITH_STATS_QUERY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = INTEGRATION_COLUMN_FAMILY_NAME)
@SqlResultSetMapping(
        name = "integrationInfoMapping",
        classes = {
                @ConstructorResult(
                        targetClass = IntegrationInfoEntity.class,
                        columns = {
                                @ColumnResult(name = "id", type = UUID.class),
                                @ColumnResult(name = "created_time", type = Long.class),
                                @ColumnResult(name = "tenant_id", type = UUID.class),
                                @ColumnResult(name = "name", type = String.class),
                                @ColumnResult(name = "type", type = String.class),
                                @ColumnResult(name = "debug_mode", type = Boolean.class),
                                @ColumnResult(name = "enabled", type = Boolean.class),
                                @ColumnResult(name = "is_remote", type = Boolean.class),
                                @ColumnResult(name = "allow_create_devices_or_assets", type = Boolean.class),
                                @ColumnResult(name = "is_edge_template", type = Boolean.class),
                                @ColumnResult(name = "stats", type = String.class),
                                @ColumnResult(name = "status", type = String.class)
                        }
                )
        }
)

@NamedNativeQuery(
        name = "IntegrationInfoEntity.findAllIntegrationInfosWithStats",
        query = FIND_ALL_INTEGRATION_INFOS_WITH_STATS_QUERY,
        resultSetMapping = "integrationInfoMapping")
public class IntegrationInfoEntity extends BaseSqlEntity<IntegrationInfo> implements SearchTextEntity<IntegrationInfo> {

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

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.INTEGRATION_IS_EDGE_TEMPLATE_MODE_PROPERTY)
    private Boolean edgeTemplate;

    @Transient
    private String stats;

    @Transient
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

    public String getSearchText() {
        return searchText;
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
