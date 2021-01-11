/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
