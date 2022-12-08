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
package org.thingsboard.server.common.data.integration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@ToString(callSuper = true)
@ApiModel
@EqualsAndHashCode(callSuper = true)
public class Integration extends AbstractIntegration implements ExportableEntity<IntegrationId> {

    private static final long serialVersionUID = 4934987577236873728L;

    private ConverterId defaultConverterId;
    private ConverterId downlinkConverterId;
    @NoXss
    @Length(fieldName = "routingKey")
    private String routingKey;
    private IntegrationType type;

    @NoXss
    @Length(fieldName = "secret")
    private String secret;
    private JsonNode configuration;
    private JsonNode additionalInfo;

    @Getter
    @Setter
    private IntegrationId externalId;

    public Integration() {
        super();
    }

    public Integration(IntegrationId id) {
        super(id);
    }

    public Integration(Integration integration) {
        super(integration);
        this.defaultConverterId = integration.getDefaultConverterId();
        this.downlinkConverterId = integration.getDownlinkConverterId();
        this.routingKey = integration.getRoutingKey();
        this.type = integration.getType();
        this.secret = integration.getSecret();
        this.configuration = integration.getConfiguration();
        this.additionalInfo = integration.getAdditionalInfo();
        this.externalId = integration.getExternalId();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Integration Id. " +
            "Specify this field to update the Integration. " +
            "Referencing non-existing Integration Id will cause error. " +
            "Omit this field to create new Integration.")
    @Override
    public IntegrationId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the integration creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 4, value = "JSON object with the Uplink Converter Id", required = true)
    public ConverterId getDefaultConverterId() {
        return defaultConverterId;
    }

    public void setDefaultConverterId(ConverterId defaultConverterId) {
        this.defaultConverterId = defaultConverterId;
    }

    @ApiModelProperty(position = 5, value = "JSON object with the Downlink Converter Id")
    public ConverterId getDownlinkConverterId() {
        return downlinkConverterId;
    }

    public void setDownlinkConverterId(ConverterId downlinkConverterId) {
        this.downlinkConverterId = downlinkConverterId;
    }

    @ApiModelProperty(position = 6, value = "String value used by HTTP based integrations for the base URL construction and by the remote integrations. " +
            "Remote integration uses this value along with the 'secret' for kind of security and validation to be able to connect to the platform using Grpc",
            required = true, example = "ca1a01b6-4ca1-3da5-54e4-a07090b65644")
    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    @ApiModelProperty(position = 12, value = "String value used by the remote integrations. " +
            "Remote integration uses this value along with the 'routingKey' for kind of security and validation to be able to connect to the platform using Grpc", example = "nl83m1ktpwpwwmww29sm")
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @ApiModelProperty(position = 13, value = "JSON object representing integration configuration. Each integration type has specific configuration with the connectivity parameters " +
            "(like 'host' and 'port' for MQTT type or 'baseUrl' for HTTP based type, etc.) " +
            "and other important parameters dependent on the integration type", required = true)
    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    @ApiModelProperty(position = 14, value = "Additional parameters of the integration", dataType = "com.fasterxml.jackson.databind.JsonNode")
    public JsonNode getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(JsonNode additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public String getSearchText() {
        return getName();
    }


    @Override
    @JsonIgnore
    public EntityType getEntityType() {
        return EntityType.INTEGRATION;
    }

    @Builder
    public Integration(TenantId tenantId, String name, IntegrationType type,
                       Boolean enabled, Boolean isRemote, Boolean allowCreateDevicesOrAssets,
                       boolean isEdgeTemplate, ConverterId defaultConverterId, ConverterId downlinkConverterId,
                       String routingKey, IntegrationType type1, boolean debugMode, String secret,
                       JsonNode configuration, JsonNode additionalInfo, IntegrationId externalId) {
        super(tenantId, name, type, debugMode, enabled, isRemote, allowCreateDevicesOrAssets, isEdgeTemplate);
        this.defaultConverterId = defaultConverterId;
        this.downlinkConverterId = downlinkConverterId;
        this.routingKey = routingKey;
        this.type = type1;
        this.secret = secret;
        this.configuration = configuration;
        this.additionalInfo = additionalInfo;
        this.externalId = externalId;
    }

}
