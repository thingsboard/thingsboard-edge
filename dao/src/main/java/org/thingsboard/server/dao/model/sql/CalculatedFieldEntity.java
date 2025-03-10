/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseVersionedEntity;
import org.thingsboard.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_CONFIGURATION;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_CONFIGURATION_VERSION;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_ENTITY_ID;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_ENTITY_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_TENANT_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_TYPE;
import static org.thingsboard.server.dao.model.ModelConstants.CALCULATED_FIELD_VERSION;
import static org.thingsboard.server.dao.model.ModelConstants.DEBUG_SETTINGS;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = CALCULATED_FIELD_TABLE_NAME)
public class CalculatedFieldEntity extends BaseVersionedEntity<CalculatedField> implements BaseEntity<CalculatedField> {

    @Column(name = CALCULATED_FIELD_TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = CALCULATED_FIELD_ENTITY_TYPE)
    private String entityType;

    @Column(name = CALCULATED_FIELD_ENTITY_ID)
    private UUID entityId;

    @Column(name = CALCULATED_FIELD_TYPE)
    private String type;

    @Column(name = CALCULATED_FIELD_NAME)
    private String name;

    @Column(name = CALCULATED_FIELD_CONFIGURATION_VERSION)
    private int configurationVersion;

    @Convert(converter = JsonConverter.class)
    @Column(name = CALCULATED_FIELD_CONFIGURATION)
    private JsonNode configuration;

    @Column(name = CALCULATED_FIELD_VERSION)
    private Long version;

    @Column(name = DEBUG_SETTINGS)
    private String debugSettings;

    public CalculatedFieldEntity() {
        super();
    }

    public CalculatedFieldEntity(CalculatedField calculatedField) {
        this.setUuid(calculatedField.getUuidId());
        this.createdTime = calculatedField.getCreatedTime();
        this.tenantId = calculatedField.getTenantId().getId();
        this.entityType = calculatedField.getEntityId().getEntityType().name();
        this.entityId = calculatedField.getEntityId().getId();
        this.type = calculatedField.getType().name();
        this.name = calculatedField.getName();
        this.configurationVersion = calculatedField.getConfigurationVersion();
        this.configuration = JacksonUtil.valueToTree(calculatedField.getConfiguration());
        this.version = calculatedField.getVersion();
        this.debugSettings = JacksonUtil.toString(calculatedField.getDebugSettings());
    }

    @Override
    public CalculatedField toData() {
        CalculatedField calculatedField = new CalculatedField(new CalculatedFieldId(id));
        calculatedField.setCreatedTime(createdTime);
        calculatedField.setTenantId(TenantId.fromUUID(tenantId));
        calculatedField.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        calculatedField.setType(CalculatedFieldType.valueOf(type));
        calculatedField.setName(name);
        calculatedField.setConfigurationVersion(configurationVersion);
        calculatedField.setConfiguration(JacksonUtil.treeToValue(configuration, CalculatedFieldConfiguration.class));
        calculatedField.setVersion(version);
        calculatedField.setDebugSettings(JacksonUtil.fromString(debugSettings, DebugSettings.class));
        return calculatedField;
    }

}
