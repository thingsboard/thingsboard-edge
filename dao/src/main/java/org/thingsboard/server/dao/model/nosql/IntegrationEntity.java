/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.IntegrationTypeCodec;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.*;

@Table(name = INTEGRATION_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public class IntegrationEntity implements SearchTextEntity<Integration> {

    @PartitionKey
    @Column(name = ID_PROPERTY)
    private UUID id;

    @ClusteringColumn
    @Column(name = INTEGRATION_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = INTEGRATION_CONVERTER_ID_PROPERTY)
    private UUID converterId;

    @Column(name = INTEGRATION_TYPE_PROPERTY, codec = IntegrationTypeCodec.class)
    private IntegrationType type;

    @Column(name = INTEGRATION_ROUTING_KEY_PROPERTY)
    private String routingKey;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = INTEGRATION_CONFIGURATION_PROPERTY, codec = JsonCodec.class)
    private JsonNode configuration;

    @Column(name = INTEGRATION_ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;

    public IntegrationEntity() {
        super();
    }

    public IntegrationEntity(Integration integration) {
        if (integration.getId() != null) {
            this.id = integration.getId().getId();
        }
        if (integration.getTenantId() != null) {
            this.tenantId = integration.getTenantId().getId();
        }
        if (integration.getDefaultConverterId() != null) {
            this.converterId = integration.getDefaultConverterId().getId();
        }
        this.routingKey = integration.getName();
        this.type = integration.getType();
        this.configuration = integration.getConfiguration();
        this.additionalInfo = integration.getAdditionalInfo();
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getConverterId() {
        return converterId;
    }

    public void setConverterId(UUID converterId) {
        this.converterId = converterId;
    }

    public IntegrationType getType() {
        return type;
    }

    public void setType(IntegrationType type) {
        this.type = type;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
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
    public String getSearchTextSource() {
        return getRoutingKey();
    }

    public String getSearchText() {
        return searchText;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public Integration toData() {
        Integration integration = new Integration(new IntegrationId(id));
        integration.setCreatedTime(UUIDs.unixTimestamp(id));
        if (tenantId != null) {
            integration.setTenantId(new TenantId(tenantId));
        }
        if (converterId != null) {
            integration.setDefaultConverterId(new ConverterId(converterId));
        }
        integration.setRoutingKey(routingKey);
        integration.setType(type);
        integration.setConfiguration(configuration);
        integration.setAdditionalInfo(additionalInfo);
        return integration;
    }
}
