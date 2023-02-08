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
package org.thingsboard.server.common.data.converter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ApiModel
@EqualsAndHashCode(callSuper = true)
public class Converter extends SearchTextBased<ConverterId> implements HasName, TenantEntity, ExportableEntity<ConverterId> {

    private static final long serialVersionUID = -1541581333235769915L;

    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    private ConverterType type;
    private boolean debugMode;
    private JsonNode configuration;
    private JsonNode additionalInfo;
    private boolean edgeTemplate;

    @Getter @Setter
    private ConverterId externalId;

    public Converter() {
        super();
    }

    public Converter(ConverterId id) {
        super(id);
    }

    public Converter(Converter converter) {
        super(converter);
        this.tenantId = converter.getTenantId();
        this.name = converter.getName();
        this.type = converter.getType();
        this.debugMode = converter.isDebugMode();
        this.configuration = converter.getConfiguration();
        this.additionalInfo = converter.getAdditionalInfo();
        this.externalId = converter.getExternalId();
        this.edgeTemplate = converter.isEdgeTemplate();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Converter Id. " +
            "Specify this field to update the Converter. " +
            "Referencing non-existing Converter Id will cause error. " +
            "Omit this field to create new Converter.")
    @Override
    public ConverterId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the converter creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
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

    @ApiModelProperty(position = 4, required = true, value = "Unique Converter Name in scope of Tenant", example = "Http Converter")
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(position = 5, required = true, value = "The type of the converter to process incoming or outgoing messages")
    public ConverterType getType() {
        return type;
    }

    public void setType(ConverterType type) {
        this.type = type;
    }

    @ApiModelProperty(position = 6, value = "Boolean flag to enable/disable saving received messages as debug events")
    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @ApiModelProperty(position = 7, value = "JSON object representing converter configuration. It should contain one of two possible fields: 'decoder' or 'encoder'. " +
            "The former is used when the converter has UPLINK type, the latter is used - when DOWNLINK type. " +
            "It can contain both 'decoder' and 'encoder' fields, when the correct one is specified for the appropriate converter type, another one can be set to 'null'")
    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    @ApiModelProperty(position = 8, value = "Additional parameters of the converter", dataType = "com.fasterxml.jackson.databind.JsonNode")
    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @ApiModelProperty(position = 9, value = "Boolean flag that specifies that is regular or edge template converter")
    public boolean isEdgeTemplate() {
        return edgeTemplate;
    }

    public void setEdgeTemplate(boolean edgeTemplate) {
        this.edgeTemplate = edgeTemplate;
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Converter [tenantId=");
        builder.append(tenantId);
        builder.append(", name=");
        builder.append(name);
        builder.append(", type=");
        builder.append(type);
        builder.append(", debugMode=");
        builder.append(debugMode);
        builder.append(", configuration=");
        builder.append(configuration);
        builder.append(", additionalInfo=");
        builder.append(additionalInfo);
        builder.append(", edgeTemplate=");
        builder.append(edgeTemplate);
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
        return EntityType.CONVERTER;
    }

}
