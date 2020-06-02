/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;

@EqualsAndHashCode(callSuper = true)
public class Converter extends SearchTextBased<ConverterId> implements HasName, TenantEntity {

    private static final long serialVersionUID = -1541581333235769915L;

    private TenantId tenantId;
    private String name;
    private ConverterType type;
    private boolean debugMode;
    private transient JsonNode configuration;
    private transient JsonNode additionalInfo;

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
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConverterType getType() {
        return type;
    }

    public void setType(ConverterType type) {
        this.type = type;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
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
