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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.io.IOException;
import java.util.HashSet;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.DASHBOARD_ASSIGNED_CUSTOMERS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.DASHBOARD_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.DASHBOARD_CONFIGURATION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.DASHBOARD_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.DASHBOARD_TITLE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Table(name = DASHBOARD_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
@Slf4j
public final class DashboardEntity implements SearchTextEntity<Dashboard> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final JavaType assignedCustomersType =
            objectMapper.getTypeFactory().constructCollectionType(HashSet.class, ShortCustomerInfo.class);

    @PartitionKey(value = 0)
    @Column(name = ID_PROPERTY)
    private UUID id;
    
    @PartitionKey(value = 1)
    @Column(name = DASHBOARD_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = DASHBOARD_TITLE_PROPERTY)
    private String title;
    
    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = DASHBOARD_ASSIGNED_CUSTOMERS_PROPERTY)
    private String assignedCustomers;

    @Column(name = DASHBOARD_CONFIGURATION_PROPERTY, codec = JsonCodec.class)
    private JsonNode configuration;

    public DashboardEntity() {
        super();
    }

    public DashboardEntity(Dashboard dashboard) {
        if (dashboard.getId() != null) {
            this.id = dashboard.getId().getId();
        }
        if (dashboard.getTenantId() != null) {
            this.tenantId = dashboard.getTenantId().getId();
        }
        this.title = dashboard.getTitle();
        if (dashboard.getAssignedCustomers() != null) {
            try {
                this.assignedCustomers = objectMapper.writeValueAsString(dashboard.getAssignedCustomers());
            } catch (JsonProcessingException e) {
                log.error("Unable to serialize assigned customers to string!", e);
            }
        }
        this.configuration = dashboard.getConfiguration();
    }
    
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAssignedCustomers() {
        return assignedCustomers;
    }

    public void setAssignedCustomers(String assignedCustomers) {
        this.assignedCustomers = assignedCustomers;
    }

    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }
    
    @Override
    public String getSearchTextSource() {
        return getTitle();
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
    
    public String getSearchText() {
        return searchText;
    }

    @Override
    public Dashboard toData() {
        Dashboard dashboard = new Dashboard(new DashboardId(id));
        dashboard.setCreatedTime(UUIDs.unixTimestamp(id));
        if (tenantId != null) {
            dashboard.setTenantId(new TenantId(tenantId));
        }
        dashboard.setTitle(title);
        if (!StringUtils.isEmpty(assignedCustomers)) {
            try {
                dashboard.setAssignedCustomers(objectMapper.readValue(assignedCustomers, assignedCustomersType));
            } catch (IOException e) {
                log.warn("Unable to parse assigned customers!", e);
            }
        }
        dashboard.setConfiguration(configuration);
        return dashboard;
    }

}