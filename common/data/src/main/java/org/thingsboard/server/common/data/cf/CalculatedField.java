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
package org.thingsboard.server.common.data.cf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasDebugSettings;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serial;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class CalculatedField extends BaseData<CalculatedFieldId> implements HasName, TenantEntity, HasVersion, HasDebugSettings {

    @Serial
    private static final long serialVersionUID = 4491966747773381420L;

    private TenantId tenantId;
    private EntityId entityId;

    @NoXss
    @Length(fieldName = "type")
    private CalculatedFieldType type;
    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "User defined name of the calculated field.")
    private String name;
    @Deprecated
    @Schema(description = "Enable/disable debug. ", example = "false", deprecated = true)
    private boolean debugMode;
    @Schema(description = "Debug settings object.")
    private DebugSettings debugSettings;
    @Schema(description = "Version of calculated field configuration.", example = "0")
    private int configurationVersion;
    @Schema(implementation = SimpleCalculatedFieldConfiguration.class)
    private transient CalculatedFieldConfiguration configuration;
    @Getter
    @Setter
    private Long version;

    public CalculatedField() {
        super();
    }

    public CalculatedField(CalculatedFieldId id) {
        super(id);
    }

    public CalculatedField(TenantId tenantId, EntityId entityId, CalculatedFieldType type, String name, int configurationVersion, CalculatedFieldConfiguration configuration, Long version) {
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.type = type;
        this.name = name;
        this.configurationVersion = configurationVersion;
        this.configuration = configuration;
        this.version = version;
    }

    public CalculatedField(CalculatedField calculatedField) {
        super(calculatedField);
        this.tenantId = calculatedField.tenantId;
        this.entityId = calculatedField.entityId;
        this.type = calculatedField.type;
        this.name = calculatedField.name;
        this.debugMode = calculatedField.debugMode;
        this.debugSettings = calculatedField.debugSettings;
        this.configurationVersion = calculatedField.configurationVersion;
        this.configuration = calculatedField.configuration;
        this.version = calculatedField.version;
    }

    @Schema(description = "JSON object with the Calculated Field Id. Referencing non-existing Calculated Field Id will cause error.")
    @Override
    public CalculatedFieldId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the calculated field creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    // Getter is ignored for serialization
    @JsonIgnore
    public boolean isDebugMode() {
        return debugMode;
    }

    // Setter is annotated for deserialization
    @JsonSetter
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CALCULATED_FIELD;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("CalculatedField[")
                .append("tenantId=").append(tenantId)
                .append(", entityId=").append(entityId)
                .append(", type='").append(type)
                .append(", name='").append(name)
                .append(", configurationVersion=").append(configurationVersion)
                .append(", configuration=").append(configuration)
                .append(", version=").append(version)
                .append(", createdTime=").append(createdTime)
                .append(", id=").append(id).append(']')
                .toString();
    }

}
