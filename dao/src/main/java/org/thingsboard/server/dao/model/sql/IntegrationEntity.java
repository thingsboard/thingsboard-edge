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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.*;

import static org.thingsboard.server.dao.model.ModelConstants.*;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = INTEGRATION_COLUMN_FAMILY_NAME)
public class IntegrationEntity extends BaseSqlEntity<Integration> implements SearchTextEntity<Integration> {

    @Column(name = INTEGRATION_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = INTEGRATION_NAME_PROPERTY)
    private String name;

    @Column(name = INTEGRATION_SECRET_PROPERTY)
    private String secret;

    @Column(name = INTEGRATION_CONVERTER_ID_PROPERTY)
    private String converterId;

    @Column(name = INTEGRATION_DOWNLINK_CONVERTER_ID_PROPERTY)
    private String downlinkConverterId;

    @Column(name = INTEGRATION_ROUTING_KEY_PROPERTY)
    private String routingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = INTEGRATION_TYPE_PROPERTY)
    private IntegrationType type;

    @Column(name = INTEGRATION_DEBUG_MODE_PROPERTY)
    private boolean debugMode;

    @Column(name = INTEGRATION_ENABLED_PROPERTY)
    private boolean enabled;

    @Column(name = INTEGRATION_IS_REMOTE_PROPERTY)
    private Boolean isRemote;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Type(type = "json")
    @Column(name = ModelConstants.INTEGRATION_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    @Type(type = "json")
    @Column(name = ModelConstants.INTEGRATION_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public IntegrationEntity() {
        super();
    }

    public IntegrationEntity(Integration integration) {
        if (integration.getId() != null) {
            this.setId(integration.getId().getId());
        }
        if (integration.getTenantId() != null) {
            this.tenantId = UUIDConverter.fromTimeUUID(integration.getTenantId().getId());
        }
        if (integration.getDefaultConverterId() != null) {
            this.converterId = UUIDConverter.fromTimeUUID(integration.getDefaultConverterId().getId());
        }
        if (integration.getDownlinkConverterId() != null) {
            this.downlinkConverterId = UUIDConverter.fromTimeUUID(integration.getDownlinkConverterId().getId());
        }
        this.name = integration.getName();
        this.routingKey = integration.getRoutingKey();
        this.secret = integration.getSecret();
        this.type = integration.getType();
        this.debugMode = integration.isDebugMode();
        this.enabled = integration.isEnabled();
        this.isRemote = integration.isRemote();
        this.configuration = integration.getConfiguration();
        this.additionalInfo = integration.getAdditionalInfo();
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
    public Integration toData() {
        Integration integration = new Integration(new IntegrationId(UUIDConverter.fromString(id)));
        integration.setCreatedTime(UUIDs.unixTimestamp(UUIDConverter.fromString(id)));
        if (tenantId != null) {
            integration.setTenantId(new TenantId(UUIDConverter.fromString(tenantId)));
        }
        if (converterId != null) {
            integration.setDefaultConverterId(new ConverterId(UUIDConverter.fromString(converterId)));
        }
        if (downlinkConverterId != null) {
            integration.setDownlinkConverterId(new ConverterId(UUIDConverter.fromString(downlinkConverterId)));
        }
        integration.setName(name);
        integration.setRoutingKey(routingKey);
        integration.setSecret(secret);
        integration.setType(type);
        integration.setDebugMode(debugMode);
        integration.setEnabled(enabled);
        integration.setRemote(isRemote);
        integration.setConfiguration(configuration);
        integration.setAdditionalInfo(additionalInfo);
        return integration;
    }
}
